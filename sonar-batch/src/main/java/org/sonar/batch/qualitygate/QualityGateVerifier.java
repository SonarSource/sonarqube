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

import org.sonar.api.batch.*;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

public class QualityGateVerifier implements Decorator {

  private QualityGate qualityGate;

  public QualityGateVerifier(QualityGate qualityGate) {
    this.qualityGate = qualityGate;
  }

  @DependedUpon
  public Metric generatesQualityGateStatus() {
    return CoreMetrics.QUALITY_GATE_STATUS;
  }

  @DependsUpon
  public String dependsOnVariations() {
    return DecoratorBarriers.END_OF_TIME_MACHINE;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return qualityGate.isEnabled();
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (ResourceUtils.isRootProject(resource)) {
      checkProjectConditions(context);
    }
  }

  private void checkProjectConditions(DecoratorContext context) {
    Metric.Level globalLevel = Metric.Level.OK;
    Measure globalMeasure = new Measure(CoreMetrics.QUALITY_GATE_STATUS, globalLevel);
    globalMeasure.setAlertStatus(globalLevel);
    globalMeasure.setAlertText("");
    context.saveMeasure(globalMeasure);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
