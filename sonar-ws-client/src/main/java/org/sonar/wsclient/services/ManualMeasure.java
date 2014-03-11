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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;

/**
 * @since 2.10
 */
public class ManualMeasure extends Model {

  private Long id;
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

  @CheckForNull
  public Long getId() {
    return id;
  }

  public ManualMeasure setId(@Nullable Long id) {
    this.id = id;
    return this;
  }

  @CheckForNull
  public String getMetricKey() {
    return metricKey;
  }

  public ManualMeasure setMetricKey(@Nullable String metricKey) {
    this.metricKey = metricKey;
    return this;
  }

  @CheckForNull
  public Double getValue() {
    return value;
  }

  public ManualMeasure setValue(@Nullable Double value) {
    this.value = value;
    return this;
  }

  @CheckForNull
  public String getTextValue() {
    return textValue;
  }

  public ManualMeasure setTextValue(@Nullable String textValue) {
    this.textValue = textValue;
    return this;
  }

  @CheckForNull
  public Date getCreatedAt() {
    return createdAt;
  }

  public ManualMeasure setCreatedAt(@Nullable Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @CheckForNull
  public Date getUpdatedAt() {
    return updatedAt;
  }

  public ManualMeasure setUpdatedAt(@Nullable Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  @CheckForNull
  public String getUserLogin() {
    return userLogin;
  }

  public ManualMeasure setUserLogin(@Nullable String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  @CheckForNull
  public String getUsername() {
    return username;
  }

  public ManualMeasure setUsername(@Nullable String username) {
    this.username = username;
    return this;
  }

  @CheckForNull
  public String getResourceKey() {
    return resourceKey;
  }

  public ManualMeasure setResourceKey(@Nullable String resourceKey) {
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
