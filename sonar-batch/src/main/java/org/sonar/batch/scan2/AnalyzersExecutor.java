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
package org.sonar.batch.scan2;

import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.batch.bootstrap.BatchExtensionDictionnary;

import java.util.Collection;

public class AnalyzersExecutor implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(AnalyzersExecutor.class);

  private BatchExtensionDictionnary selector;
  private AnalyzerOptimizer optimizer;

  public AnalyzersExecutor(BatchExtensionDictionnary selector, AnalyzerOptimizer optimizer) {
    this.selector = selector;
    this.optimizer = optimizer;
  }

  public void execute(SensorContext context) {
    Collection<Sensor> analyzers = selector.select(Sensor.class, null, true, null);

    for (Sensor analyzer : analyzers) {

      DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
      analyzer.describe(descriptor);

      if (!optimizer.shouldExecute(descriptor)) {
        LOG.debug("Analyzer skipped: " + descriptor.name());
        continue;
      }

      LOG.info("Execute analyzer: " + descriptor.name());

      executeSensor(context, analyzer);
    }

  }

  private void executeSensor(SensorContext context, Sensor analyzer) {
    analyzer.execute(context);
  }

}
