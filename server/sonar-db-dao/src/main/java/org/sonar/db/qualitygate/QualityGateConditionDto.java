/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric.ValueType;

/**
 * @since 4.3
 */
public class QualityGateConditionDto {

  public static final String OPERATOR_EQUALS = "EQ";

  public static final String OPERATOR_NOT_EQUALS = "NE";

  public static final String OPERATOR_GREATER_THAN = "GT";

  public static final String OPERATOR_LESS_THAN = "LT";

  public static final List<String> ALL_OPERATORS = ImmutableList.of(
    OPERATOR_LESS_THAN,
    OPERATOR_GREATER_THAN,
    OPERATOR_EQUALS,
    OPERATOR_NOT_EQUALS);

  private static final List<String> NUMERIC_OPERATORS = ImmutableList.of(
    OPERATOR_LESS_THAN,
    OPERATOR_GREATER_THAN,
    OPERATOR_EQUALS,
    OPERATOR_NOT_EQUALS);

  private static final List<String> STRING_OPERATORS = ImmutableList.of(
    OPERATOR_EQUALS,
    OPERATOR_NOT_EQUALS,
    OPERATOR_LESS_THAN,
    OPERATOR_GREATER_THAN);

  private static final List<String> LEVEL_OPERATORS = ImmutableList.of(
    OPERATOR_EQUALS,
    OPERATOR_NOT_EQUALS);

  private static final List<String> BOOLEAN_OPERATORS = ImmutableList.of(
    OPERATOR_EQUALS);

  private static final List<String> RATING_OPERATORS = ImmutableList.of(
    OPERATOR_GREATER_THAN);

  private static final Map<ValueType, List<String>> OPERATORS_BY_TYPE = ImmutableMap.<ValueType, List<String>>builder()
    .put(ValueType.BOOL, BOOLEAN_OPERATORS)
    .put(ValueType.LEVEL, LEVEL_OPERATORS)
    .put(ValueType.STRING, STRING_OPERATORS)
    .put(ValueType.INT, NUMERIC_OPERATORS)
    .put(ValueType.FLOAT, NUMERIC_OPERATORS)
    .put(ValueType.PERCENT, NUMERIC_OPERATORS)
    .put(ValueType.MILLISEC, NUMERIC_OPERATORS)
    .put(ValueType.RATING, RATING_OPERATORS)
    .put(ValueType.WORK_DUR, NUMERIC_OPERATORS)
    .build();

  private long id;

  private long qualityGateId;

  private long metricId;

  private String metricKey;

  private Integer period;

  private String operator;

  private String warningThreshold;

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

  @CheckForNull
  public Integer getPeriod() {
    return period;
  }

  public QualityGateConditionDto setPeriod(@Nullable Integer period) {
    this.period = period;
    return this;
  }

  public String getOperator() {
    return operator;
  }

  public QualityGateConditionDto setOperator(String operator) {
    this.operator = operator;
    return this;
  }

  public String getWarningThreshold() {
    return warningThreshold;
  }

  public QualityGateConditionDto setWarningThreshold(@Nullable String warningThreshold) {
    this.warningThreshold = warningThreshold;
    return this;
  }

  public String getErrorThreshold() {
    return errorThreshold;
  }

  public QualityGateConditionDto setErrorThreshold(@Nullable String errorThreshold) {
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

  public static boolean isOperatorAllowed(String operator, ValueType metricType) {
    return getOperatorsForType(metricType).contains(operator);
  }

  public static Collection<String> getOperatorsForType(ValueType metricType) {
    if (OPERATORS_BY_TYPE.containsKey(metricType)) {
      return OPERATORS_BY_TYPE.get(metricType);
    } else {
      return Collections.emptySet();
    }
  }
}
