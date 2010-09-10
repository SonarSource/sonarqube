/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.profiles;

/**
 * @since 2.3
 */
public final class MetricThreshold implements Cloneable {

  /**
   * Operator strictly greater than
   */
  public static final String OPERATOR_GREATER = ">";

  /**
   * Operator strictly lesser than
   */
  public static final String OPERATOR_SMALLER = "<";

  /**
   * Operator equals
   */
  public static final String OPERATOR_EQUALS = "=";

  /**
   * Operator not equals
   */
  public static final String OPERATOR_NOT_EQUALS = "!=";

  private String metric;
  private String operator;
  private String valueError;
  private String valueWarning;

  private MetricThreshold() {
  }

  public static MetricThreshold create() {
    return new MetricThreshold();
  }

  public static MetricThreshold createForMetric(String metricKey) {
    return new MetricThreshold().setMetric(metricKey);
  }

  public String getMetric() {
    return metric;
  }

  public MetricThreshold setMetric(String metric) {
    this.metric = metric;
    return this;
  }

  public String getOperator() {
    return operator;
  }

  public MetricThreshold setOperator(String operator) {
    this.operator = operator;
    return this;
  }

  public String getValueError() {
    return valueError;
  }

  public MetricThreshold setValueError(String valueError) {
    this.valueError = valueError;
    return this;
  }

  public String getValueWarning() {
    return valueWarning;
  }

  public MetricThreshold setValueWarning(String valueWarning) {
    this.valueWarning = valueWarning;
    return this;
  }

  /**
   * @return whether the operator is greater than
   */
  public boolean isGreaterOperator() {
    return OPERATOR_GREATER.equals(operator);
  }

  /**
   * @return whether the operator is lesser than
   */
  public boolean isSmallerOperator() {
    return OPERATOR_SMALLER.equals(operator);
  }

  /**
   * @return whether the operator is equals
   */
  public boolean isEqualsOperator() {
    return OPERATOR_EQUALS.equals(operator);
  }

  /**
   * @return whether the operator is not equals
   */
  public boolean isNotEqualsOperator() {
    return OPERATOR_NOT_EQUALS.equals(operator);
  }

}
