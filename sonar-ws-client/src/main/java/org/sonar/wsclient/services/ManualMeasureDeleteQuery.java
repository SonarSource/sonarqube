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

/**
 * @since 2.10
 */
public final class ManualMeasureDeleteQuery extends DeleteQuery {

  private String resourceKey;
  private String metricKey;

  private ManualMeasureDeleteQuery(String resourceKey, String metricKey) {
    this.resourceKey = resourceKey;
    this.metricKey = metricKey;
  }

  public static ManualMeasureDeleteQuery create(String resourceKey, String metricKey) {
    return new ManualMeasureDeleteQuery(resourceKey, metricKey);
  }

  public String getResourceKey() {
    return resourceKey;
  }

  public String getMetricKey() {
    return metricKey;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder();
    url.append(ManualMeasureQuery.BASE_URL);
    appendUrlParameter(url, "resource", resourceKey);
    appendUrlParameter(url, "metric", metricKey);
    return url.toString();
  }

  @Override
  public String toString() {
    return getUrl();
  }
}
