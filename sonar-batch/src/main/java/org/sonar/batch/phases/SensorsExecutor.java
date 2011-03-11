/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.batch.phases;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.MavenPluginExecutor;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.events.SensorExecutionEvent;
import org.sonar.batch.events.SensorsPhaseEvent;

public class SensorsExecutor implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(SensorsExecutor.class);

  private Collection<Sensor> sensors;
  private MavenPluginExecutor mavenExecutor;
  private EventBus eventBus;

  public SensorsExecutor(BatchExtensionDictionnary selector, Project project, MavenPluginExecutor mavenExecutor, EventBus eventBus) {
    this.sensors = selector.select(Sensor.class, project, true);
    this.mavenExecutor = mavenExecutor;
    this.eventBus = eventBus;
  }

  public void execute(Project project, SensorContext context) {
    eventBus.fireEvent(new SensorsPhaseEvent(sensors, true));

    for (Sensor sensor : sensors) {
      executeMavenPlugin(project, sensor);

      eventBus.fireEvent(new SensorExecutionEvent(sensor, true));
      sensor.analyse(project, context);
      eventBus.fireEvent(new SensorExecutionEvent(sensor, false));
    }

    eventBus.fireEvent(new SensorsPhaseEvent(sensors, false));
  }

  private void executeMavenPlugin(Project project, Sensor sensor) {
    if (sensor instanceof DependsUponMavenPlugin) {
      MavenPluginHandler handler = ((DependsUponMavenPlugin) sensor).getMavenPluginHandler(project);
      if (handler != null) {
        TimeProfiler profiler = new TimeProfiler(LOG).start("Execute maven plugin " + handler.getArtifactId());
        mavenExecutor.execute(project, handler);
        profiler.stop();
      }
    }
  }
}
