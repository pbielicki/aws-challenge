package aws;

import static aws.Util.EMAIL;
import static aws.Util.EVENTS;
import static aws.Util.NAME;
import static aws.Util.OLDEST_EVENT_TIMESTAMP;
import static aws.Util.RECORDS;
import static aws.Util.SNS;
import static aws.Util.SNS_MESSAGE;
import static aws.Util.TABLE_USER_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

import aws.model.UserEvent;

public class EventHandlerLambda extends AbstractLambda {

  private static final Log LOG = LogFactory.getLog(EventHandlerLambda.class);
  
  @SuppressWarnings("unchecked")
  public void handleEvent(Map<String, Object> req, Context context) {
    Table table = dynamoDB.getTable("Log");
    table.putItem(new Item().withPrimaryKey("created", System.currentTimeMillis()).withString("value", String.valueOf(req)));
    Table userEventTable = dynamoDB.getTable(TABLE_USER_EVENT);
    List<Map<String, Object>> request = (List<Map<String, Object>>) req.get(RECORDS);
    request.forEach(record -> {
      UserEvent userEvent;
      try {
        userEvent = new ObjectMapper()
            .readValue(((Map<String, String>) record.get(SNS)).get(SNS_MESSAGE), UserEvent.class);
      } catch (Exception e) {
        LOG.error("Unable to parse Sns Message", e);
        throw new IllegalArgumentException("Unable to parse Sns Message", e);
      }
      
      Item item = userEventTable.getItem(EMAIL, userEvent.getEmail());
      List<Map<String, Object>> events;
      long timestamp;
      if (item == null) {
        LOG.info("New item for [" + userEvent.getEmail() + "]");
        timestamp = System.currentTimeMillis();
        events = new ArrayList<>();
      } else {
        LOG.info("Existing item for [" + userEvent.getEmail() 
          + "] created [" + item.getLong(OLDEST_EVENT_TIMESTAMP) + "]");
        
        events = item.getList(EVENTS);
        timestamp = item.getLong(OLDEST_EVENT_TIMESTAMP);
      }
      
      // should we check for duplicates?
      // should we throttle list size to avoid OOM?
      // should we truncate message size to avoid OOM?
      events.add(userEvent.toEventMap());
      LOG.info("Current events list size is [" + events.size() + "] for [" + userEvent.getEmail() + "]");
      item = new Item().withPrimaryKey(EMAIL, userEvent.getEmail())
          .withString(NAME, userEvent.getName())
          .withLong(OLDEST_EVENT_TIMESTAMP, timestamp)
          .withList(EVENTS, events);
      
      LOG.info("Putting item for [" + userEvent.getEmail() + "]");
      userEventTable.putItem(item);
    });
  }
}
