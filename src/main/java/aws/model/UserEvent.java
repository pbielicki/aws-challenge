package aws.model;

import static aws.Util.INPUT_PATTERN;
import static aws.Util.MESSAGE;
import static aws.Util.TIMESTAMP;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserEvent {
  private static final Log LOG = LogFactory.getLog(UserEvent.class);

  @JsonProperty("name")
  private String name;
  @JsonProperty("message")
  private String message;
  @JsonProperty("timestamp")
  private String timestamp;
  @JsonProperty("email")
  private String email;

  @SuppressWarnings("serial")
  public Map<String, Object> toEventMap() {
    return new LinkedHashMap<String, Object>() {
      {
        try {
          put(TIMESTAMP, new SimpleDateFormat(INPUT_PATTERN).parse(timestamp).getTime());
        } catch (ParseException e) {
          LOG.error(
              "Unable to parse timestamp [" + timestamp + "] with [" + INPUT_PATTERN + "] pattern", e);
          
          throw new IllegalArgumentException(
              "Unable to parse timestamp [" + timestamp + "] with [" + INPUT_PATTERN + "] pattern", e);
        }
        put(MESSAGE, message);
      }
    };
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getEmail() {
    return email;
  }

  public String getMessage() {
    return message;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "UserEvent{email=" + email + ",name=" + name + ",timestamp=" + timestamp + ",message=" + message + "}";
  }
}