/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.api.measures;

import org.sonar.api.resources.Scopes;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @since 2.0
 *
 * Used to consolidate a distribution measure throughout the resource tree
 */
public class SumChildDistributionFormula implements Formula {

  private String minimumScopeToPersist= Scopes.FILE;

  @Override
  public List<Metric> dependsUponMetrics() {
    return Collections.emptyList();
  }

  public String getMinimumScopeToPersist() {
    return minimumScopeToPersist;
  }

  public SumChildDistributionFormula setMinimumScopeToPersist(String s) {
    this.minimumScopeToPersist = s;
    return this;
  }

  @Override
  public Measure calculate(FormulaData data, FormulaContext context) {
    Collection<Measure> measures = data.getChildrenMeasures(context.getTargetMetric());
    if (measures == null || measures.isEmpty()) {
      return null;
    }
    RangeDistributionBuilder distribution = new RangeDistributionBuilder(context.getTargetMetric());
    for (Measure measure : measures) {
      distribution.add(measure);
    }
    Measure measure = distribution.build();
    if (!Scopes.isHigherThanOrEquals(context.getResource().getScope(), minimumScopeToPersist)) {
      measure.setPersistenceMode(PersistenceMode.MEMORY);
    }
    return measure;
  }
}
