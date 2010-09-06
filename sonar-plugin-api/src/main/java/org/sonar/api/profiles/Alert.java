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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.sonar.api.database.BaseIdentifiable;
import org.sonar.api.measures.Metric;

import javax.persistence.*;

/**
 * Class to map alerts with hibernate model
 */
@Entity
@Table(name = "alerts")
public class Alert extends BaseIdentifiable implements Cloneable {
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "profile_id")
  @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
  private RulesProfile rulesProfile;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "metric_id", nullable = true)
  @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
  private Metric metric;

  @Column(name = "operator", updatable = false, nullable = true, length = 3)
  private String operator;

  @Column(name = "value_error", updatable = false, nullable = true, length = 64)
  private String valueError;

  @Column(name = "value_warning", updatable = false, nullable = true, length = 64)
  private String valueWarning;

  /**
   * Default constructor
   */
  public Alert() {
  }

  /**
   * Creates an alert
   *
   * @param rulesProfile the profile used to trigger the alert
   * @param metric the metric tested for the alert
   * @param operator the operator defined
   * @param valueError the error value
   * @param valueWarning the warning value
   */
  public Alert(RulesProfile rulesProfile, Metric metric, String operator, String valueError, String valueWarning) {
    super();
    this.rulesProfile = rulesProfile;
    this.metric = metric;
    this.operator = operator;
    this.valueError = valueError;
    this.valueWarning = valueWarning;
  }

  /**
   * @return the alert profile
   */
  public RulesProfile getRulesProfile() {
    return rulesProfile;
  }

  /**
   * Sets the alert profile
   */
  public void setRulesProfile(RulesProfile rulesProfile) {
    this.rulesProfile = rulesProfile;
  }

  /**
   * @return the alert metric
   */
  public Metric getMetric() {
    return metric;
  }

  /**
   * Sets the alert metric
   */
  public void setMetric(Metric metric) {
    this.metric = metric;
  }

  /**
   * @return the alert operator
   */
  public String getOperator() {
    return operator;
  }

  /**
   * Sets the alert operator
   */
  public void setOperator(String operator) {
    this.operator = operator;
  }

  /**
   * @return the error value
   */
  public String getValueError() {
    return valueError;
  }

  /**
   * Sets the error value if any
   */
  public void setValueError(String valueError) {
    this.valueError = valueError;
  }

  /**
   * @return the warning value
   */
  public String getValueWarning() {
    return valueWarning;
  }

  /**
   * Sets the warning value if any
   */
  public void setValueWarning(String valueWarning) {
    this.valueWarning = valueWarning;
  }

  /**
   * @return whether the operator is greater than
   */
  public boolean isGreaterOperator() {
    return operator.equals(OPERATOR_GREATER);
  }

  /**
   * @return whether the operator is lesser than
   */
  public boolean isSmallerOperator() {
    return operator.equals(OPERATOR_SMALLER);
  }

  /**
   * @return whether the operator is equals
   */
  public boolean isEqualsOperator() {
    return operator.equals(OPERATOR_EQUALS);
  }

  /**
   * @return whether the operator is not equals
   */
  public boolean isNotEqualsOperator() {
    return operator.equals(OPERATOR_NOT_EQUALS);
  }


  public String getAlertLabel(Metric.Level level) {
    return new StringBuilder()
        .append(getMetric().getName())
        .append(" ").append(getOperator())
        .append(" ")
        .append(level.equals(Metric.Level.ERROR) ? getValueError() : getValueWarning()).toString();
  }

  @Override
  public Object clone() {
    return new Alert(getRulesProfile(), getMetric(), getOperator(), getValueError(), getValueWarning());
  }

}
