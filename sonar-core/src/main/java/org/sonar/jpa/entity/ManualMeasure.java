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
package org.sonar.jpa.entity;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.persistence.*;

@Entity
@Table(name = "manual_measures")
public final class ManualMeasure {
  private static final int MAX_TEXT_SIZE = 4000;

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Long id;

  @Column(name = "value", updatable = true, nullable = true, precision = 30, scale = 20)
  private Double value = null;

  @Column(name = "text_value", updatable = true, nullable = true, length = MAX_TEXT_SIZE)
  private String textValue;

  @Column(name = "metric_id", updatable = false, nullable = false)
  private Integer metricId;

  @Column(name = "resource_id", updatable = true, nullable = true)
  private Integer resourceId;

  @Column(name = "description", updatable = true, nullable = true, length = MAX_TEXT_SIZE)
  private String description;

  @Column(name = "created_at", updatable = true, nullable = true)
  private Long createdAt;

  @Column(name = "updated_at", updatable = true, nullable = true)
  private Long updatedAt;

  @Column(name = "user_login", updatable = true, nullable = true, length = 40)
  private String userLogin;

  public Long getId() {
    return id;
  }

  public Double getValue() {
    return value;
  }

  public String getTextValue() {
    return textValue;
  }

  public String getDescription() {
    return description;
  }

  public Integer getMetricId() {
    return metricId;
  }

  public Integer getResourceId() {
    return resourceId;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public Long getUpdatedAt() {
    return updatedAt;
  }

  public String getUserLogin() {
    return userLogin;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }
}
