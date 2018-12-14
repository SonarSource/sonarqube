/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.qualitygate;

import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @since 4.3
 */
public class QualityGateConditionDto {

  public static final String OPERATOR_GREATER_THAN = "GT";
  public static final String OPERATOR_LESS_THAN = "LT";

  private long id;

  private long qualityGateId;

  private long metricId;

  private String metricKey;

  private String operator;

  private String errorThreshold;

  private Date createdAt;

  private Date updatedAt;

  public long getId() {
    return id;
  }

  public QualityGateConditionDto setId(long id) {
    this.id = id;
    return this;
  }

  public long getQualityGateId() {
    return qualityGateId;
  }

  public QualityGateConditionDto setQualityGateId(long qualityGateId) {
    this.qualityGateId = qualityGateId;
    return this;
  }

  public long getMetricId() {
    return metricId;
  }

  public QualityGateConditionDto setMetricId(long metricId) {
    this.metricId = metricId;
    return this;
  }

  @CheckForNull
  public String getMetricKey() {
    return metricKey;
  }

  public QualityGateConditionDto setMetricKey(@Nullable String metricKey) {
    this.metricKey = metricKey;
    return this;
  }

  public String getOperator() {
    return operator;
  }

  public QualityGateConditionDto setOperator(String operator) {
    this.operator = operator;
    return this;
  }

  public String getErrorThreshold() {
    return errorThreshold;
  }

  public QualityGateConditionDto setErrorThreshold(String errorThreshold) {
    this.errorThreshold = errorThreshold;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public QualityGateConditionDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public QualityGateConditionDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

}
