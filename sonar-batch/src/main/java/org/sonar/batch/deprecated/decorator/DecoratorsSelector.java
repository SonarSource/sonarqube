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
package org.sonar.batch.deprecated.decorator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.sonar.api.batch.Decorator;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.batch.bootstrap.BatchExtensionDictionnary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class DecoratorsSelector {

  private BatchExtensionDictionnary batchExtDictionnary;

  public DecoratorsSelector(BatchExtensionDictionnary dictionnary) {
    this.batchExtDictionnary = dictionnary;
  }

  public Collection<Decorator> select(Project project) {
    List<Decorator> decorators = new ArrayList<Decorator>(batchExtDictionnary.select(Decorator.class, project, false, null));
    SetMultimap<Metric, Decorator> decoratorsByGeneratedMetric = getDecoratorsByMetric(decorators);
    for (Metric metric : batchExtDictionnary.select(Metric.class, null, false, null)) {
      if (metric.getFormula() != null) {
        decorators.add(new FormulaDecorator(metric, decoratorsByGeneratedMetric.get(metric)));
      }
    }

    return batchExtDictionnary.sort(decorators);
  }

  private SetMultimap<Metric, Decorator> getDecoratorsByMetric(Collection<Decorator> pluginDecorators) {
    SetMultimap<Metric, Decorator> decoratorsByGeneratedMetric = HashMultimap.create();
    for (Decorator decorator : pluginDecorators) {
      List dependents = batchExtDictionnary.getDependents(decorator);
      for (Object dependent : dependents) {
        if (dependent instanceof Metric) {
          decoratorsByGeneratedMetric.put((Metric) dependent, decorator);
        }
      }
    }
    return decoratorsByGeneratedMetric;
  }
}
