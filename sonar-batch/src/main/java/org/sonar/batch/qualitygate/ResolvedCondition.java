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
package org.sonar.batch.qualitygate;

import org.sonar.api.measures.Metric;
import org.sonar.wsclient.qualitygate.QualityGateCondition;

public class ResolvedCondition implements QualityGateCondition {

  private QualityGateCondition wrapped;

  private Metric metric;

  public ResolvedCondition(QualityGateCondition condition, Metric metric) {
    this.wrapped = condition;
    this.metric = metric;
  }

  public Metric metric() {
    return metric;
  }

  @Override
  public Long id() {
    return wrapped.id();
  }

  @Override
  public String metricKey() {
    return wrapped.metricKey();
  }

  @Override
  public String operator() {
    return wrapped.operator();
  }

  @Override
  public String warningThreshold() {
    return wrapped.warningThreshold();
  }

  @Override
  public String errorThreshold() {
    return wrapped.errorThreshold();
  }

  @Override
  public Integer period() {
    return wrapped.period();
  }
}
