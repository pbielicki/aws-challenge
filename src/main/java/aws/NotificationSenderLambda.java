package aws;

import static aws.Messages.EMAIL_SUBJECT;
import static aws.Messages.getString;
import static aws.Util.DEFAULT_FROM_EMAIL;
import static aws.Util.EMAIL;
import static aws.Util.FROM_EMAIL;
import static aws.Util.MESSAGE;
import static aws.Util.NAME;
import static aws.Util.TABLE_USER_NOTIFICATION;
import static aws.Util.getenv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

import aws.model.ScheduledEvent;

public class NotificationSenderLambda extends AbstractLambda {
	
	private final AmazonSimpleEmailServiceClient emailClient;
	private static final Log LOG = LogFactory.getLog(NotificationSenderLambda.class);
	
	public NotificationSenderLambda() {
    emailClient = new AmazonSimpleEmailServiceClient();
    emailClient.setRegion(region);
  }

  public void handleEvent(ScheduledEvent event, Context context) throws Exception {
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
}
