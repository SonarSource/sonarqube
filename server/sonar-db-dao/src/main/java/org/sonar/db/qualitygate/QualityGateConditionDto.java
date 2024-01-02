/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

  private String uuid;

  private String qualityGateUuid;

  private String metricUuid;

  private String metricKey;

  private String operator;

  private String errorThreshold;

  private Date createdAt;

  private Date updatedAt;

  public String getUuid() {
    return uuid;
  }

  public QualityGateConditionDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getQualityGateUuid() {
    return qualityGateUuid;
  }

  public QualityGateConditionDto setQualityGateUuid(String qualityGateUuid) {
    this.qualityGateUuid = qualityGateUuid;
    return this;
  }

  public String getMetricUuid() {
    return metricUuid;
  }

  public QualityGateConditionDto setMetricUuid(String metricUuid) {
    this.metricUuid = metricUuid;
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
