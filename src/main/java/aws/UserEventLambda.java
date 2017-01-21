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

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

import aws.model.UserEvent;

public class UserEventLambda extends AbstractLambda {

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
}
