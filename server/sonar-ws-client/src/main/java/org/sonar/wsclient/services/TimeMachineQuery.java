/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.services;

import java.util.Date;

/**
 * @since 2.5
 */
public class TimeMachineQuery extends Query<TimeMachine> {

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
  public Class<TimeMachine> getModelClass() {
    return TimeMachine.class;
  }

  public static TimeMachineQuery createForMetrics(String resourceKeyOrId, String... metricKeys) {
    return new TimeMachineQuery(resourceKeyOrId).setMetrics(metricKeys);
  }

  public static TimeMachineQuery createForMetrics(Resource resource, String... metricKeys) {
    Integer id = resource.getId();
    if (id == null) {
      throw new IllegalArgumentException("id must be set");
    }
    return new TimeMachineQuery(id.toString()).setMetrics(metricKeys);
  }

}
