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
package org.sonar.api.ce.posttask;

import java.util.Collection;
import javax.annotation.CheckForNull;
import org.sonar.api.measures.Metric;

/**
 * @since 5.5
 */
public interface QualityGate {
  /**
   * The unique identifier of the Quality Gate.
   */
  String getId();

  /**
   * Name of the Quality Gate.
   */
  String getName();

  /**
   * Status of the Quality Gate for the current project processing.
   */
  Status getStatus();

  /**
   * Conditions of the Quality Gate.
   */
  Collection<Condition> getConditions();

  enum Status {
    /** at least one threshold is defined, no threshold is reached */
    OK,
    /** at least one warning threshold is reached, no error threshold is reached */
    WARN,
    /** at least one error threshold is reached */
    ERROR
  }

  interface Condition {
    /**
     * Evaluation status of this condition
     */
    EvaluationStatus getStatus();

    /**
     * The key of the metric this condition has been evaluated on.
     * <p>
     * The {@link org.sonar.api.measures.Metric} for the returned key can be retrieved using a
     * {@link org.sonar.api.measures.MetricFinder} instance.
     * 
     *
     * @see org.sonar.api.batch.measure.MetricFinder#findByKey(String)
     */
    String getMetricKey();

    /**
     * The operator used to evaluate the error and/or warning thresholds against the value of the measure
     */
    Operator getOperator();

    /**
     * <p>
     * At least one of {@link #getErrorThreshold()} and {@link #getWarningThreshold()} is guaranteed to be non {@code null}.
     * 
     *
     * @see #getWarningThreshold()
     */
    @CheckForNull
    String getErrorThreshold();

    /**
     *
     * <p>
     * At least one of {@link #getErrorThreshold()} and {@link #getWarningThreshold()} is guaranteed to be non {@code null}.
     * 
     *
     * @see #getErrorThreshold()
     */
    @CheckForNull
    String getWarningThreshold();

    /**
     * Whether this condition is defined on the leak period or on an absolute value
     */
    boolean isOnLeakPeriod();

    /**
     * The value of the measure.
     * <p>
     * If the type of the metric (which key is provided by {@link #getMetricKey()}) is numerical, the value can be parsed
     * using {@link Integer#valueOf(String)}, {@link Long#valueOf(String)} or {@link Double#valueOf(String)}.
     * 
     *
     * @throws IllegalStateException if {@link #getStatus()} is {@link EvaluationStatus#NO_VALUE}
     *
     * @see Metric#getType() 
     */
    String getValue();

  }

  /**
   * Quality Gate condition operator.
   */
  enum Operator {
    EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN
  }

  /**
   * Quality gate condition evaluation status.
   */
  enum EvaluationStatus {
    /**
     * No measure found or measure had no value. The condition has not been evaluated and therefor ignored in
     * the computation of the Quality Gate status.
     */
    NO_VALUE,
    /**
     * Condition evaluated as OK, neither error nor warning thresholds have been reached.
     */
    OK,
    /**
     * Condition evaluated as WARN, only warning thresholds has been reached.
     */
    WARN,
    /**
     * Condition evaluated as ERROR, error thresholds has been reached (and most likely warning thresholds too).
     */
    ERROR
  }
}
