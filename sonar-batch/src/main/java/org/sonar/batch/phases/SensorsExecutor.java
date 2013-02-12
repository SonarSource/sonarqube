/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.MavenPluginExecutor;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;

import java.util.Collection;

public class SensorsExecutor implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(SensorsExecutor.class);

  private MavenPluginExecutor mavenExecutor;
  private EventBus eventBus;
  private Project project;
  private DefaultModuleFileSystem fs;
  private BatchExtensionDictionnary selector;

  public SensorsExecutor(BatchExtensionDictionnary selector, Project project, DefaultModuleFileSystem fs, MavenPluginExecutor mavenExecutor, EventBus eventBus) {
    this.selector = selector;
    this.mavenExecutor = mavenExecutor;
    this.eventBus = eventBus;
    this.project = project;
    this.fs = fs;
  }

  public void execute(SensorContext context) {
    Collection<Sensor> sensors = selector.select(Sensor.class, project, true);
    eventBus.fireEvent(new SensorsPhaseEvent(Lists.newArrayList(sensors), true));

    for (Sensor sensor : sensors) {
      executeMavenPlugin(sensor);

      eventBus.fireEvent(new SensorExecutionEvent(sensor, true));
      sensor.analyse(project, context);
      eventBus.fireEvent(new SensorExecutionEvent(sensor, false));
    }

    eventBus.fireEvent(new SensorsPhaseEvent(Lists.newArrayList(sensors), false));
  }

  private void executeMavenPlugin(Sensor sensor) {
    if (sensor instanceof DependsUponMavenPlugin) {
      MavenPluginHandler handler = ((DependsUponMavenPlugin) sensor).getMavenPluginHandler(project);
      if (handler != null) {
        TimeProfiler profiler = new TimeProfiler(LOG).start("Execute maven plugin " + handler.getArtifactId());
        mavenExecutor.execute(project, fs, handler);
        profiler.stop();
      }
    }
  }
}
