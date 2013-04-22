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
package org.sonar.batch.scan;

import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.batch.DefaultFileLinesContextFactory;
import org.sonar.batch.DefaultResourceCreationLock;
import org.sonar.batch.ProjectConfigurator;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.bootstrap.ExtensionInstaller;
import org.sonar.batch.bootstrap.ExtensionMatcher;
import org.sonar.batch.bootstrap.ExtensionUtils;
import org.sonar.batch.bootstrap.MetricProvider;
import org.sonar.batch.index.*;
import org.sonar.batch.issue.*;
import org.sonar.batch.phases.GraphPersister;
import org.sonar.batch.profiling.PhasesSumUpTimeProfiler;
import org.sonar.batch.scan.maven.FakeMavenPluginExecutor;
import org.sonar.batch.scan.maven.MavenPluginExecutor;
import org.sonar.batch.scan.source.HighlightableBuilder;
import org.sonar.batch.scan.source.SymbolPerspectiveBuilder;
import org.sonar.core.component.ScanGraph;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.test.TestPlanBuilder;
import org.sonar.core.test.TestPlanPerspectiveLoader;
import org.sonar.core.test.TestableBuilder;
import org.sonar.core.test.TestablePerspectiveLoader;

public class ProjectScanContainer extends ComponentContainer {
  public ProjectScanContainer(ComponentContainer taskContainer) {
    super(taskContainer);
  }

  @Override
  protected void doBeforeStart() {
    addBatchComponents();
    fixMavenExecutor();
    addBatchExtensions();
    Settings settings = getComponentByType(Settings.class);
    if (settings != null && settings.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY)) {
      add(PhasesSumUpTimeProfiler.class);
    }
  }

  private void addBatchComponents() {
    add(
      DefaultResourceCreationLock.class,
      DefaultPersistenceManager.class,
      DependencyPersister.class,
      EventPersister.class,
      LinkPersister.class,
      MeasurePersister.class,
      MemoryOptimizer.class,
      DefaultResourcePersister.class,
      SourcePersister.class,
      DefaultNotificationManager.class,
      MetricProvider.class,
      ProjectConfigurator.class,
      DefaultIndex.class,
      DefaultFileLinesContextFactory.class,
      ProjectLock.class,
      LastSnapshots.class,
      Caches.class,
      SnapshotCache.class,
      ComponentDataCache.class,
      ComponentDataPersister.class,

      // issues
      IssueWorkflow.class,
      ScanIssues.class,
      DeprecatedViolations.class,
      IssueCache.class,
      IssuePersister.class,

      // tests
      TestPlanPerspectiveLoader.class,
      TestablePerspectiveLoader.class,
      TestPlanBuilder.class,
      TestableBuilder.class,
      ScanGraph.create(),
      GraphPersister.class,

      // lang
      HighlightableBuilder.class,
      SymbolPerspectiveBuilder.class
    );
  }

  private void fixMavenExecutor() {
    if (getComponentByType(MavenPluginExecutor.class) == null) {
      add(FakeMavenPluginExecutor.class);
    }
  }

  private void addBatchExtensions() {
    getComponentByType(ExtensionInstaller.class).install(this, new BatchExtensionFilter());
  }

  @Override
  protected void doAfterStart() {
    ProjectTree tree = getComponentByType(ProjectTree.class);
    scanRecursively(tree.getRootProject());
  }

  private void scanRecursively(Project module) {
    for (Project subModules : module.getModules()) {
      scanRecursively(subModules);
    }
    scan(module);
  }

  private void scan(Project module) {
    new ModuleScanContainer(this, module).execute();
  }

  static class BatchExtensionFilter implements ExtensionMatcher {
    public boolean accept(Object extension) {
      return ExtensionUtils.isType(extension, BatchExtension.class)
        && ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_BATCH);
    }
  }
}
