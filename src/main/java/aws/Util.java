package aws;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.regions.Regions;

public class Util {
  public static final String CREATED = "created";
  public static final String DEFAULT_FROM_EMAIL = "pbielicki@gmail.com";
  public static final String DEFAULT_MAX_RESULT_SIZE = "100";
  public static final String DEFAULT_NOTIFICATION_DELAY = "60"; // minutes  
  public static final String DEFAULT_REGION = Regions.EU_WEST_1.getName();
  public static final String EMAIL = "email";
  public static final String EVENTS = "events";
  public static final String FROM_EMAIL = "FROM_EMAIL";
  public static final String INPUT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
  public static final String MAX_RESULT_SIZE = "MAX_RESULT_SIZE";
  public static final String MESSAGE = "message";
  public static final long MINUTE = 1000L * 60L;
  public static final String NAME = "name";
  public static final String NOTIFICATION_DELAY = "NOTIFICATION_DELAY";
  public static final String OLDEST_EVENT_TIMESTAMP = "oldestEventTimestamp";
  public static final String OUTPUT_PATTERN = "EEEE, HH:mm";
  public static final String RECORDS = "Records";
  public static final String REGION = "REGION";
  public static final String SNS = "Sns";
  public static final String SNS_MESSAGE = "Message";
  public static final String TABLE_USER_EVENT = "UserEvent";
  public static final String TABLE_USER_EVENT_ARCHIVE = "UserEventArchive";
  public static final String TABLE_USER_NOTIFICATION = "UserNotification";
  public static final String TIMESTAMP = "timestamp";

  private static final Log LOG = LogFactory.getLog(Util.class);

  public static String getenv(String name, String defaultValue) {
    if (System.getenv().containsKey(name)) {
      String value = System.getenv(name);
      LOG.debug("Returning [" + value + "] for [" + name + "] variable");
      return value;
    }
    
    LOG.debug("Returning default [" + defaultValue + "] for [" + name + "] variable");
    return defaultValue;
  }
  
  private Util() {
  }
}
