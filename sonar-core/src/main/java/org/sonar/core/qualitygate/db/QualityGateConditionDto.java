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
package org.sonar.core.qualitygate.db;

import com.google.common.collect.ImmutableList;
import org.sonar.api.measures.Metric.ValueType;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @since 4.3
 */
public class QualityGateConditionDto {

  public static final String OPERATOR_EQUALS = "EQ";

  public static final String OPERATOR_NOT_EQUALS = "NE";

  public static final String OPERATOR_GREATER_THAN = "GT";

  public static final String OPERATOR_LESS_THAN = "LT";

  private static final List<String> NUMERIC_OPERATORS = ImmutableList.of(
      OPERATOR_LESS_THAN,
      OPERATOR_GREATER_THAN,
      OPERATOR_EQUALS,
      OPERATOR_NOT_EQUALS
  );
  private static final List<String> STRING_OPERATORS = ImmutableList.of(
      OPERATOR_EQUALS,
      OPERATOR_NOT_EQUALS,
      OPERATOR_LESS_THAN,
      OPERATOR_GREATER_THAN
  );
  private static final List<String> LEVEL_OPERATORS = ImmutableList.of(
    OPERATOR_EQUALS,
    OPERATOR_NOT_EQUALS
  );
  private static final List<String> BOOLEAN_OPERATORS = ImmutableList.of(
    OPERATOR_EQUALS
  );

  private long id;

  private long qualityGateId;

  private long metricId;

  private transient String metricKey;

  private Integer period;

  private String operator;

  private String warningThreshold;

  private String errorThreshold;

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

  public QualityGateConditionDto setMetricKey(String metricKey) {
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

  public QualityGateConditionDto setWarningThreshold(String warningThreshold) {
    this.warningThreshold = warningThreshold;
    return this;
  }

  public String getErrorThreshold() {
    return errorThreshold;
  }

  public QualityGateConditionDto setErrorThreshold(String errorThreshold) {
    this.errorThreshold = errorThreshold;
    return this;
  }

  public static boolean isOperatorAllowed(String operator, ValueType metricType) {
    return getOperatorsForType(metricType).contains(operator);
  }

  public static Collection<String> getOperatorsForType(ValueType metricType) {
    if (metricType == null) {
      return Collections.emptySet();
    } else {
      switch(metricType) {
        case BOOL:
          return BOOLEAN_OPERATORS;
        case LEVEL:
          return LEVEL_OPERATORS;
        case STRING:
          return STRING_OPERATORS;
        case INT:
        case FLOAT:
        case PERCENT:
        case MILLISEC:
        case RATING:
          return NUMERIC_OPERATORS;
        default:
          return Collections.emptySet();
      }
    }
  }
}
