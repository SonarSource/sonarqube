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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.TimeProfiler;

import java.util.Collection;

public class SensorsExecutor implements CoreJob {
  private static final Logger logger = LoggerFactory.getLogger(SensorsExecutor.class);
  
  private Collection<Sensor> sensors;
  private DatabaseSession session;
  private MavenPluginExecutor mavenExecutor;

  public SensorsExecutor(BatchExtensionDictionnary selector, Project project, DatabaseSession session, MavenPluginExecutor mavenExecutor) {
    this.sensors = selector.select(Sensor.class, project, true);
    this.session = session;
    this.mavenExecutor = mavenExecutor;
  }

  public void execute(Project project, SensorContext context) {
    if (logger.isDebugEnabled()) {
      logger.debug("Sensors : {}", StringUtils.join(sensors, " -> "));
    }

    for (Sensor sensor : sensors) {
      executeMavenPlugin(project, sensor);

      TimeProfiler profiler = new TimeProfiler(logger).start("Sensor "+ sensor);
      sensor.analyse(project, context);
      session.commit();
      profiler.stop();
    }
  }

  private void executeMavenPlugin(Project project, Sensor sensor) {
    if (sensor instanceof DependsUponMavenPlugin) {
      MavenPluginHandler handler = ((DependsUponMavenPlugin) sensor).getMavenPluginHandler(project);
      if (handler != null) {
        TimeProfiler profiler = new TimeProfiler(logger).start("Execute maven plugin " + handler.getArtifactId());
        mavenExecutor.execute(project, handler);
        profiler.stop();
      }
    }
  }
}
