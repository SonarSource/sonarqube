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
package org.sonar.batch;

import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.FormulaDecorator;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;

import java.util.*;

public class DecoratorsSelector {

  private BatchExtensionDictionnary dictionnary;

  public DecoratorsSelector(BatchExtensionDictionnary dictionnary) {
    this.dictionnary = dictionnary;
  }

  public Collection<Decorator> select(Project project) {
    List<Decorator> decorators = new ArrayList<Decorator>(dictionnary.select(Decorator.class, project, false));
    Set<Metric> coveredMetrics = getMetricsCoveredByPlugins(decorators);
    for (Metric metric : dictionnary.select(Metric.class)) {
      if (metric.getFormula() != null && !coveredMetrics.contains(metric)) {
        decorators.add(new FormulaDecorator(metric));
      }
    }

    return dictionnary.sort(decorators);
  }

  private Set<Metric> getMetricsCoveredByPlugins(Collection<Decorator> pluginDecorators) {
    Set<Metric> coveredMetrics = new HashSet<Metric>();
    for (Decorator pluginDecorator : pluginDecorators) {
      List dependents = dictionnary.getDependents(pluginDecorator);
      for (Object dependent : dependents) {
        if (dependent instanceof Metric) {
          coveredMetrics.add((Metric) dependent);
        }
      }
    }
    return coveredMetrics;
  }
}
