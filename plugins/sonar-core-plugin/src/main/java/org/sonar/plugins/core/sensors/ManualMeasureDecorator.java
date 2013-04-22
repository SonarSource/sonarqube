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
package org.sonar.plugins.core.sensors;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.Phase;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.jpa.entity.ManualMeasure;

import java.util.List;

@Phase(name = Phase.Name.PRE)
public class ManualMeasureDecorator implements Decorator {

  private DatabaseSession session;
  private MetricFinder metricFinder;

  public ManualMeasureDecorator(DatabaseSession session, MetricFinder metricFinder) {
    this.session = session;
    this.metricFinder = metricFinder;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (resource.getId() != null) {
      List<ManualMeasure> manualMeasures = session.getResults(ManualMeasure.class, "resourceId", resource.getId());
      for (ManualMeasure manualMeasure : manualMeasures) {
        context.saveMeasure(copy(manualMeasure));
      }
    }
  }

  private Measure copy(ManualMeasure manualMeasure) {
    Measure measure = new Measure(metricFinder.findById(manualMeasure.getMetricId()));
    measure.setValue(manualMeasure.getValue(), 5);
    measure.setData(manualMeasure.getTextValue());
    measure.setDescription(manualMeasure.getDescription());
    return measure;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
