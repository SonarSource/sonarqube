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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.batch.scan.maven.MavenPluginExecutor;

import java.util.Collection;

public class InitializersExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(SensorsExecutor.class);

  private MavenPluginExecutor mavenExecutor;

  private DefaultModuleFileSystem fs;
  private Project project;
  private BatchExtensionDictionnary selector;
  private EventBus eventBus;

  public InitializersExecutor(BatchExtensionDictionnary selector, Project project, DefaultModuleFileSystem fs, MavenPluginExecutor mavenExecutor, EventBus eventBus) {
    this.selector = selector;
    this.mavenExecutor = mavenExecutor;
    this.project = project;
    this.fs = fs;
    this.eventBus = eventBus;
  }

  public void execute() {
    Collection<Initializer> initializers = selector.select(Initializer.class, project, true);
    eventBus.fireEvent(new InitializersPhaseEvent(Lists.newArrayList(initializers), true));
    if (LOG.isDebugEnabled()) {
      LOG.debug("Initializers : {}", StringUtils.join(initializers, " -> "));
    }

    for (Initializer initializer : initializers) {
      eventBus.fireEvent(new InitializerExecutionEvent(initializer, true));
      executeMavenPlugin(initializer);

      TimeProfiler profiler = new TimeProfiler(LOG).start("Initializer " + initializer);
      initializer.execute(project);
      profiler.stop();
      eventBus.fireEvent(new InitializerExecutionEvent(initializer, false));
    }

    eventBus.fireEvent(new InitializersPhaseEvent(Lists.newArrayList(initializers), false));
  }

  private void executeMavenPlugin(Initializer sensor) {
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
