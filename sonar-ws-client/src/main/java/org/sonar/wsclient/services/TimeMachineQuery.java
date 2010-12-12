package org.sonar.wsclient.services;

import java.util.Date;

public class TimeMachineQuery extends Query<TimeMachineData> {

  public static final String BASE_URL = "/api/timemachine";

  private String resourceKeyOrId;
  private String[] metrics;
  private Date from;
  private Date to;

  public TimeMachineQuery(String resourceKeyOrId) {
    this.resourceKeyOrId = resourceKeyOrId;
  }

  public String[] getMetrics() {
    return metrics;
  }

  public TimeMachineQuery setMetrics(String... metrics) {
    this.metrics = metrics;
    return this;
  }

  public Date getFrom() {
    return from;
  }

  public TimeMachineQuery setFrom(Date from) {
    this.from = from;
    return this;
  }

  public Date getTo() {
    return to;
  }

  public TimeMachineQuery setTo(Date to) {
    this.to = to;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    url.append('?');
    appendUrlParameter(url, "resource", resourceKeyOrId);
    appendUrlParameter(url, "metrics", metrics);
    appendUrlParameter(url, "first_date", from);
    appendUrlParameter(url, "last_date", to);
    return url.toString();
  }

  @Override
  public Class<TimeMachineData> getModelClass() {
    return TimeMachineData.class;
  }

  public static TimeMachineQuery create(String resourceKeyOrId) {
    return new TimeMachineQuery(resourceKeyOrId);
  }

}
