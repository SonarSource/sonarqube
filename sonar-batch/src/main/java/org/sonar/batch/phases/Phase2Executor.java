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
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.batch.rule.QProfileVerifier;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.batch.scan.filesystem.FileSystemLogger;
import org.sonar.batch.scan.maven.MavenPluginsConfigurator;

import java.util.Collection;

public final class Phase2Executor {

  public static final Logger LOGGER = LoggerFactory.getLogger(Phase2Executor.class);

  private final EventBus eventBus;
  private final Phases phases;
  private final MavenPluginsConfigurator mavenPluginsConfigurator;
  private final InitializersExecutor initializersExecutor;
  private final SensorsExecutor sensorsExecutor;
  private final SensorContext sensorContext;
  private final ProjectInitializer pi;
  private final FileSystemLogger fsLogger;
  private final DefaultModuleFileSystem fs;
  private final QProfileVerifier profileVerifier;
  private final IssueExclusionsLoader issueExclusionsLoader;

  public Phase2Executor(Phases phases,
    MavenPluginsConfigurator mavenPluginsConfigurator, InitializersExecutor initializersExecutor,
    SensorsExecutor sensorsExecutor,
    SensorContext sensorContext,
    EventBus eventBus, ProjectInitializer pi,
    FileSystemLogger fsLogger, DefaultModuleFileSystem fs, QProfileVerifier profileVerifier,
    IssueExclusionsLoader issueExclusionsLoader) {
    this.phases = phases;
    this.mavenPluginsConfigurator = mavenPluginsConfigurator;
    this.initializersExecutor = initializersExecutor;
    this.sensorsExecutor = sensorsExecutor;
    this.sensorContext = sensorContext;
    this.eventBus = eventBus;
    this.pi = pi;
    this.fsLogger = fsLogger;
    this.fs = fs;
    this.profileVerifier = profileVerifier;
    this.issueExclusionsLoader = issueExclusionsLoader;
  }

  public static Collection<Class> getPhaseClasses() {
    return Lists.<Class>newArrayList(SensorsExecutor.class, InitializersExecutor.class, ProjectInitializer.class);
  }

  /**
   * Executed on each module
   */
  public void execute(Project module) {
    pi.execute(module);

    eventBus.fireEvent(new ProjectAnalysisEvent(module, true));

    executeMavenPhase(module);

    executeInitializersPhase();

    // Index and lock the filesystem
    fs.index();

    // Log detected languages and their profiles after FS is indexed and languages detected
    profileVerifier.execute();

    // Initialize issue exclusions
    issueExclusionsLoader.execute();

    sensorsExecutor.execute(sensorContext);

    eventBus.fireEvent(new ProjectAnalysisEvent(module, false));
  }

  private void executeInitializersPhase() {
    if (phases.isEnabled(Phases.Phase.INIT)) {
      initializersExecutor.execute();
      fsLogger.log();
    }
  }

  private void executeMavenPhase(Project module) {
    if (phases.isEnabled(Phases.Phase.MAVEN)) {
      eventBus.fireEvent(new MavenPhaseEvent(true));
      mavenPluginsConfigurator.execute(module);
      eventBus.fireEvent(new MavenPhaseEvent(false));
    }
  }
}
