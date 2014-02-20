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

/**
 * @since 4.3
 */
public class QualityGateConditionDto {

  private long id;

  private long qualityGateId;

  private long metricId;

  private int period;

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

  public int getPeriod() {
    return period;
  }

  public QualityGateConditionDto setPeriod(int period) {
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
}
