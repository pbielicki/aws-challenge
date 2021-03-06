package aws;

import static aws.Messages.EMAIL_MSG_HI;
import static aws.Messages.EMAIL_MSG_LINE;
import static aws.Messages.getString;
import static aws.Util.DEFAULT_NOTIFICATION_DELAY;
import static aws.Util.EMAIL;
import static aws.Util.EVENTS;
import static aws.Util.MESSAGE;
import static aws.Util.MINUTE;
import static aws.Util.NAME;
import static aws.Util.NOTIFICATION_DELAY;
import static aws.Util.OLDEST_EVENT_TIMESTAMP;
import static aws.Util.OUTPUT_PATTERN;
import static aws.Util.TABLE_USER_EVENT;
import static aws.Util.TABLE_USER_NOTIFICATION;
import static aws.Util.TIMESTAMP;
import static aws.Util.getenv;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;

import aws.model.ScheduledEvent;

public class EventScannerLambda extends AbstractLambda {
  
  private static final Log LOG = LogFactory.getLog(EventScannerLambda.class);
  
  private final long notificationDelay;
  
  public EventScannerLambda() {
    notificationDelay = Long.parseLong(getenv(NOTIFICATION_DELAY, DEFAULT_NOTIFICATION_DELAY)) * MINUTE;
  }

  public void handleEvent(ScheduledEvent event, Context context) {
    Table userEventTable = dynamoDB.getTable(TABLE_USER_EVENT);
    Table userNotifTable = dynamoDB.getTable(TABLE_USER_NOTIFICATION);
    ScanSpec scanSpec = new ScanSpec()
        .withMaxResultSize(maxResultSize)
        .withFilterExpression(OLDEST_EVENT_TIMESTAMP + " < :" + OLDEST_EVENT_TIMESTAMP)
        .withValueMap(
            new ValueMap()
            .withNumber(":" + OLDEST_EVENT_TIMESTAMP, System.currentTimeMillis() - notificationDelay));

    SimpleDateFormat format = new SimpleDateFormat(OUTPUT_PATTERN);
    userEventTable.scan(scanSpec).forEach(item -> {
      StringBuilder sb = new StringBuilder(String.format(getString(EMAIL_MSG_HI), item.getString(NAME)));
      List<Map<String, Object>> events = item.getList(EVENTS);
      
      // should we sort events by timestamp first?
      events.forEach(
          e -> sb.append(String.format(getString(EMAIL_MSG_LINE),
                  format.format(new Date(((BigDecimal) e.get(TIMESTAMP)).longValue())),
                  e.get(MESSAGE)))
      );
      
      LOG.info("Putting item for [" + item.getString(EMAIL) + "] into " + TABLE_USER_NOTIFICATION);
      userNotifTable.putItem(new Item()
          .withPrimaryKey(EMAIL, item.getString(EMAIL))
          .withString(NAME, item.getString(NAME))
          .withString(MESSAGE, sb.toString()));
      
      LOG.info("Removing item [" + item.getString(EMAIL) + "] from " + TABLE_USER_EVENT);
      userEventTable.deleteItem(new PrimaryKey(EMAIL, item.getString(EMAIL)));
    });
  }
}
