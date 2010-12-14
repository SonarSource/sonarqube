package org.sonar.wsclient.services;

import java.util.Date;

/**
 * @since 2.5
 */
public class TimeMachineQuery extends Query<TimeMachineData> {

  public static final String BASE_URL = "/api/timemachine";

  private String resourceKeyOrId;
  private String[] metrics;
  private Date from;
  private Date to;

  private String model;
  private String[] characteristicKeys;

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

  public TimeMachineQuery setCharacteristicKeys(String model, String... keys) {
    this.model = model;
    this.characteristicKeys = keys;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    url.append('?');
    appendUrlParameter(url, "resource", resourceKeyOrId);
    appendUrlParameter(url, "metrics", metrics);
    appendUrlParameter(url, "fromDateTime", from, true);
    appendUrlParameter(url, "toDateTime", to, true);
    appendUrlParameter(url, "model", model);
    appendUrlParameter(url, "characteristics", characteristicKeys);
    return url.toString();
  }

  @Override
  public Class<TimeMachineData> getModelClass() {
    return TimeMachineData.class;
  }

  public static TimeMachineQuery createForMetrics(String resourceKeyOrId, String... metricKeys) {
    return new TimeMachineQuery(resourceKeyOrId)
        .setMetrics(metricKeys);
  }

  public static TimeMachineQuery createForResource(Resource resource, String... metricKeys) {
    return new TimeMachineQuery(resource.getId().toString())
        .setMetrics(metricKeys);
  }

}
