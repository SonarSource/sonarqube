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
package org.sonar.api.database.daos;

import org.sonar.api.measures.Metric;

import java.util.Collection;
import java.util.List;

/**
 * @deprecated since 2.3
 */
@Deprecated
public class MeasuresDao {

  private org.sonar.jpa.dao.MeasuresDao target;

  public MeasuresDao(org.sonar.jpa.dao.MeasuresDao target) {
    this.target = target;
  }

  public Metric getMetric(Metric metric) {
    return target.getMetric(metric);
  }

  public List<Metric> getMetrics(List<Metric> metrics) {
    return target.getMetrics(metrics);
  }

  public Metric getMetric(String metricName) {
    return target.getMetric(metricName);
  }

  public Collection<Metric> getMetrics() {
    return target.getMetrics();
  }

  public Collection<Metric> getEnabledMetrics() {
    return target.getEnabledMetrics();
  }

  public Collection<Metric> getUserDefinedMetrics() {
    return target.getUserDefinedMetrics();
  }

  public void disableAutomaticMetrics() {
    target.disableAutomaticMetrics();
  }

  public void registerMetrics(Collection<Metric> metrics) {
    target.registerMetrics(metrics);
  }

  public void persistMetric(Metric metric) {
    target.persistMetric(metric);
  }

  public void disabledMetrics(Collection<Metric> metrics) {
    target.disabledMetrics(metrics);
  }

}
