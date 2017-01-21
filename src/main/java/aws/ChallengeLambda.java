package aws;

import static aws.Messages.EMAIL_MSG_HI;
import static aws.Messages.EMAIL_MSG_LINE;
import static aws.Messages.EMAIL_SUBJECT;
import static aws.Messages.getString;
import static aws.Util.DEFAULT_FROM_EMAIL;
import static aws.Util.DEFAULT_NOTIFICATION_DELAY;
import static aws.Util.DEFAULT_REGION;
import static aws.Util.EMAIL;
import static aws.Util.EVENTS;
import static aws.Util.FROM_EMAIL;
import static aws.Util.MESSAGE;
import static aws.Util.MINUTE;
import static aws.Util.NAME;
import static aws.Util.NOTIFICATION_DELAY;
import static aws.Util.OLDEST_EVENT_TIMESTAMP;
import static aws.Util.OUTPUT_PATTERN;
import static aws.Util.RECORDS;
import static aws.Util.REGION;
import static aws.Util.SNS;
import static aws.Util.SNS_MESSAGE;
import static aws.Util.TABLE_USER_EVENT;
import static aws.Util.TABLE_USER_NOTIFICATION;
import static aws.Util.TIMESTAMP;
import static aws.Util.getenv;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import aws.model.ScheduledEvent;
import aws.model.UserEvent;

public class ChallengeLambda {
  private AmazonDynamoDBClient dynamoClient;
  private DynamoDB dynamoDB;
  private AmazonSimpleEmailServiceClient emailClient;
  private static final Log LOG = LogFactory.getLog(ChallengeLambda.class);
  
  public ChallengeLambda() {
    Region region = Region.getRegion(Regions.fromName(getenv(REGION, DEFAULT_REGION)));
    dynamoClient = new AmazonDynamoDBClient();
    dynamoClient.setRegion(region);
    dynamoDB = new DynamoDB(dynamoClient);
    emailClient = new AmazonSimpleEmailServiceClient();
    emailClient.setRegion(region);
  }

  public void sendEmails(ScheduledEvent event, Context context) throws Exception {
    Table userEmailTable = dynamoDB.getTable(TABLE_USER_NOTIFICATION);
    String from = getenv(FROM_EMAIL, DEFAULT_FROM_EMAIL);
    userEmailTable.scan().forEach(item -> {
      Destination destination = new Destination().withToAddresses(new String[] { item.getString(EMAIL) });
      Content subject = new Content().withData(String.format(getString(EMAIL_SUBJECT), item.getString(NAME)));
      Content textBody = new Content().withData(item.getString(MESSAGE));
      Body body = new Body().withText(textBody);
      Message message = new Message().withSubject(subject).withBody(body);
      SendEmailRequest request = new SendEmailRequest()
          .withSource(from)
          .withDestination(destination)
          .withMessage(message);
      
      try {
        LOG.debug("Trying to send email to " + item.getString(EMAIL));
        emailClient.sendEmail(request);
      } catch (RuntimeException ex) {
        LOG.error("The email was not sent. Error message: " + ex.getMessage());
        return;
      }

      userEmailTable.deleteItem(new PrimaryKey(EMAIL, item.getString(EMAIL)));
    });
  }

  @SuppressWarnings("unchecked")
  public void handleUserEvent(Map<String, Object> req, Context context) {
    Table userEventTable = dynamoDB.getTable(TABLE_USER_EVENT);
    List<Map<String, Object>> request = (List<Map<String, Object>>) req.get(RECORDS);
    request.forEach(record -> {
      UserEvent userEvent;
      try {
        userEvent = new ObjectMapper()
            .readValue(((Map<String, String>) record.get(SNS)).get(SNS_MESSAGE), UserEvent.class);
      } catch (Exception e) {
        throw new IllegalArgumentException("Unable to parse Message", e);
      }
      
      Item item = userEventTable.getItem(EMAIL, userEvent.getEmail());
      List<Map<String, Object>> events;
      long timestamp;
      if (item == null) {
        timestamp = System.currentTimeMillis();
        events = new ArrayList<>();
      } else {
        events = item.getList(EVENTS);
        timestamp = item.getLong(OLDEST_EVENT_TIMESTAMP);
      }
      
      // we don't check for duplicates
      events.add(userEvent.toEventMap());
      item = new Item().withPrimaryKey(EMAIL, userEvent.getEmail())
          .withString(NAME, userEvent.getName())
          .withLong(OLDEST_EVENT_TIMESTAMP, timestamp)
          .withList(EVENTS, events);
      
      userEventTable.putItem(item);
    });
  }
  
  public void scanEvents(ScheduledEvent event, Context context) {
    Table userEventTable = dynamoDB.getTable(TABLE_USER_EVENT);
    Table userEmailTable = dynamoDB.getTable(TABLE_USER_NOTIFICATION);
    long delay = Long.parseLong(getenv(NOTIFICATION_DELAY, DEFAULT_NOTIFICATION_DELAY)) * MINUTE;
    ScanSpec scanSpec = new ScanSpec()
        .withFilterExpression(OLDEST_EVENT_TIMESTAMP + " < :" + OLDEST_EVENT_TIMESTAMP)
        .withValueMap(
            new ValueMap()
            .withNumber(":" + OLDEST_EVENT_TIMESTAMP, System.currentTimeMillis() - delay));

    SimpleDateFormat format = new SimpleDateFormat(OUTPUT_PATTERN);
    userEventTable.scan(scanSpec).forEach(item -> {
      StringBuilder sb = new StringBuilder(String.format(getString(EMAIL_MSG_HI), item.getString(NAME)));
      List<Map<String, Object>> events = item.getList(EVENTS);
      events.forEach(
          e -> sb.append(String.format(getString(EMAIL_MSG_LINE),
                  format.format(new Date(((BigDecimal) e.get(TIMESTAMP)).longValue())),
                  e.get(MESSAGE)))
      );
      
      userEmailTable.putItem(new Item()
          .withPrimaryKey(EMAIL, item.getString(EMAIL))
          .withString(NAME, item.getString(NAME))
          .withString(MESSAGE, sb.toString()));
      
      userEventTable.deleteItem(new PrimaryKey(EMAIL, item.getString(EMAIL)));
    });
  }

}