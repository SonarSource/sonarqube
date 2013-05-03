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

import com.google.common.annotations.VisibleForTesting;
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
import org.sonar.batch.bootstrap.*;
import org.sonar.batch.index.*;
import org.sonar.batch.issue.DeprecatedViolations;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.issue.IssuePersister;
import org.sonar.batch.issue.ScanIssueStorage;
import org.sonar.batch.phases.GraphPersister;
import org.sonar.batch.profiling.PhasesSumUpTimeProfiler;
import org.sonar.batch.scan.maven.FakeMavenPluginExecutor;
import org.sonar.batch.scan.maven.MavenPluginExecutor;
import org.sonar.batch.scan.source.HighlightableBuilder;
import org.sonar.batch.scan.source.SymbolPerspectiveBuilder;
import org.sonar.batch.source.SymbolizableBuilder;
import org.sonar.core.component.ScanGraph;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.workflow.FunctionExecutor;
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
      IssueUpdater.class,
      FunctionExecutor.class,
      IssueWorkflow.class,
      DeprecatedViolations.class,
      IssueCache.class,
      ScanIssueStorage.class,
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
      SymbolPerspectiveBuilder.class,
      SymbolizableBuilder.class);
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
    BatchSettings settings = getComponentByType(BatchSettings.class);
    settings.init(tree.getProjectDefinition(tree.getRootProject()));
    scanRecursively(tree.getRootProject());
  }

  public void stop() {
    // Remove project specific settings
    BatchSettings settings = getComponentByType(BatchSettings.class);
    settings.restore();
  }

  private void scanRecursively(Project module) {
    for (Project subModules : module.getModules()) {
      scanRecursively(subModules);
    }
    scan(module);
  }

  @VisibleForTesting
  void scan(Project module) {
    new ModuleScanContainer(this, module).execute();
  }

  static class BatchExtensionFilter implements ExtensionMatcher {
    public boolean accept(Object extension) {
      return ExtensionUtils.isType(extension, BatchExtension.class)
        && ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_BATCH);
    }
  }
}
