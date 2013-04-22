/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
 * @since 2.10
 */
public class ManualMeasure extends Model {

  private long id;
  private String metricKey;
  private String resourceKey;
  private Double value;
  private String textValue;
  private Date createdAt;
  private Date updatedAt;
  private String userLogin;
  private String username;

  public ManualMeasure() {
  }

  public long getId() {
    return id;
  }

  public ManualMeasure setId(long id) {
    this.id = id;
    return this;
  }

  public String getMetricKey() {
    return metricKey;
  }

  public ManualMeasure setMetricKey(String metricKey) {
    this.metricKey = metricKey;
    return this;
  }

  public Double getValue() {
    return value;
  }

  public ManualMeasure setValue(Double value) {
    this.value = value;
    return this;
  }

  public String getTextValue() {
    return textValue;
  }

  public ManualMeasure setTextValue(String textValue) {
    this.textValue = textValue;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public ManualMeasure setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public ManualMeasure setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public ManualMeasure setUserLogin(String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public String getUsername() {
    return username;
  }

  public ManualMeasure setUsername(String username) {
    this.username = username;
    return this;
  }

  public String getResourceKey() {
    return resourceKey;
  }

  public ManualMeasure setResourceKey(String resourceKey) {
    this.resourceKey = resourceKey;
    return this;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("Measure{")
        .append("id='").append(id).append('\'')
        .append("resourceKey='").append(resourceKey).append('\'')
        .append("metricKey='").append(metricKey).append('\'')
        .append(", value=").append(value)
        .append(", textValue='").append(textValue).append('\'')
        .append('}').toString();
  }
}
