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

public final class MetricQuery extends Query<Metric> {
  public static final String BASE_URL = "/api/metrics";

  private String key;

  private MetricQuery() {
  }

  private MetricQuery(String key) {
    this.key = key;
  }

  @Override
  public String getUrl() {
    StringBuilder sb = new StringBuilder(BASE_URL);
    if (key != null && !"".equals(key)) {
      sb.append("/");
      sb.append(encode(key));
    }
    sb.append("?");
    return sb.toString();
  }

  @Override
  public Class<Metric> getModelClass() {
    return Metric.class;
  }

  public static MetricQuery all() {
    return new MetricQuery();
  }

  public static MetricQuery byKey(String metricKey) {
    return new MetricQuery(metricKey);
  }
}
