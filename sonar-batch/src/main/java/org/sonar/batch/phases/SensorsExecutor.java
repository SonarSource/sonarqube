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
package org.sonar.batch.phases;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.bootstrap.BatchExtensionDictionnary;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.batch.scan.maven.MavenPluginExecutor;

import java.util.Collection;

public class SensorsExecutor implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(SensorsExecutor.class);

  private MavenPluginExecutor mavenExecutor;
  private EventBus eventBus;
  private Project module;
  private DefaultModuleFileSystem fs;
  private BatchExtensionDictionnary selector;
  private final DatabaseSession session;
  private final SensorMatcher sensorMatcher;

  public SensorsExecutor(BatchExtensionDictionnary selector, Project project, DefaultModuleFileSystem fs, MavenPluginExecutor mavenExecutor, EventBus eventBus,
    DatabaseSession session, SensorMatcher sensorMatcher) {
    this.selector = selector;
    this.mavenExecutor = mavenExecutor;
    this.eventBus = eventBus;
    this.module = project;
    this.fs = fs;
    this.session = session;
    this.sensorMatcher = sensorMatcher;
  }

  public void execute(SensorContext context) {
    Collection<Sensor> sensors = selector.select(Sensor.class, module, true, sensorMatcher);
    eventBus.fireEvent(new SensorsPhaseEvent(Lists.newArrayList(sensors), true));

    for (Sensor sensor : sensors) {
      // SONAR-2965 In case the sensor takes too much time we close the session to not face a timeout
      session.commitAndClose();

      executeSensor(context, sensor);
    }

    eventBus.fireEvent(new SensorsPhaseEvent(Lists.newArrayList(sensors), false));
  }

  private void executeSensor(SensorContext context, Sensor sensor) {
    eventBus.fireEvent(new SensorExecutionEvent(sensor, true));
    executeMavenPlugin(sensor);
    sensor.analyse(module, context);
    eventBus.fireEvent(new SensorExecutionEvent(sensor, false));
  }

  private void executeMavenPlugin(Sensor sensor) {
    if (sensor instanceof DependsUponMavenPlugin) {
      MavenPluginHandler handler = ((DependsUponMavenPlugin) sensor).getMavenPluginHandler(module);
      if (handler != null) {
        TimeProfiler profiler = new TimeProfiler(LOG).start("Execute maven plugin " + handler.getArtifactId());
        mavenExecutor.execute(module, fs, handler);
        profiler.stop();
      }
    }
  }
}
