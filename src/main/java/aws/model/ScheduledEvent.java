package aws.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class ScheduledEvent {
  @JsonProperty("account")
  String account;
  @JsonProperty("region")
  String region;
  @JsonProperty("detail")
  Detail detail;
  @JsonProperty("detail-type")
  String detailType;
  @JsonProperty("source")
  String source;
  @JsonProperty("time")
  String time;
  @JsonProperty("id")
  String id;
  @JsonProperty("resources")
  String[] resources;

  /**
   * @return the account
   */
  public String getAccount() {
    return account;
  }

  /**
   * @param account
   *          the account to set
   */
  public void setAccount(String account) {
    this.account = account;
  }

  /**
   * @return the region
   */
  public String getRegion() {
    return region;
  }

  /**
   * @param region
   *          the region to set
   */
  public void setRegion(String region) {
    this.region = region;
  }

  /**
   *
   * @return The detail
   */
  @JsonProperty("detail")
  public Detail getDetail() {
    return detail;
  }

  /**
   *
   * @param detail
   *          The detail
   */
  @JsonProperty("detail")
  public void setDetail(Detail detail) {
    this.detail = detail;
  }

  /**
   * @return the detailType
   */
  public String getDetailType() {
    return detailType;
  }

  /**
   * @param detailType
   *          the detailType to set
   */
  public void setDetailType(String detailType) {
    this.detailType = detailType;
  }

  /**
   * @return the soruce
   */
  public String getSource() {
    return source;
  }

  /**
   * @param soruce
   *          the soruce to set
   */
  public void setSource(String soruce) {
    this.source = soruce;
  }

  /**
   * @return the time
   */
  public String getTime() {
    return time;
  }

  /**
   * @param time
   *          the time to set
   */
  public void setTime(String time) {
    this.time = time;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return the resources
   */
  public String[] getResources() {
    return resources;
  }

  /**
   * @param resources
   *          the resources to set
   */
  public void setResources(String[] resources) {
    this.resources = resources;
  }

  public ScheduledEvent() {
  }

  @Override
  public String toString() {
    return "ScheduledEvent{" + "account='" + account + '\'' + ", region='" + region + '\'' + ", detail=" + detail
        + ", detailType='" + detailType + '\'' + ", source='" + source + '\'' + ", time='" + time + '\'' + ", id='" + id
        + '\'' + ", resources=" + Arrays.toString(resources) + '}';
  }
}
