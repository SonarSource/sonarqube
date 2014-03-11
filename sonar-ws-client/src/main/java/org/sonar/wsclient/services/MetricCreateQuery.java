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
 * @since 3.6
 */
public class MetricCreateQuery extends CreateQuery<Metric>  {

  private String key;
  private String name;
  private String description;
  private String domain;
  private String type;

  public static MetricCreateQuery create(String metricKey) {
    return new MetricCreateQuery(metricKey);
  }

  private MetricCreateQuery(String key) {
    this.key = key;
  }

  public MetricCreateQuery setName(String name) {
    this.name = name;
    return this;
  }

  public MetricCreateQuery setDescription(String description) {
    this.description = description;
    return this;
  }

  public MetricCreateQuery setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  public MetricCreateQuery setType(String type) {
    this.type = type;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder();
    url.append(MetricQuery.BASE_URL);
    url.append("/").append(encode(key));
    url.append('?');
    appendUrlParameter(url, "name", name);
    appendUrlParameter(url, "description", description);
    appendUrlParameter(url, "domain", domain);
    appendUrlParameter(url, "val_type", type);
    return url.toString();
  }

  @Override
  public Class<Metric> getModelClass() {
    return Metric.class;
  }
}
