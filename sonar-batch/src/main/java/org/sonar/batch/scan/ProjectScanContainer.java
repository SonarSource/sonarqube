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
package org.sonar.batch.scan;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.bootstrap.ProjectBootstrapper;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.DefaultFileLinesContextFactory;
import org.sonar.batch.ProjectConfigurator;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.bootstrap.*;
import org.sonar.batch.debt.DebtModelProvider;
import org.sonar.batch.debt.IssueChangelogDebtCalculator;
import org.sonar.batch.deprecated.components.DefaultResourceCreationLock;
import org.sonar.batch.deprecated.components.PeriodsDefinition;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.*;
import org.sonar.batch.issue.DefaultProjectIssues;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.issue.tracking.InitialOpenIssuesStack;
import org.sonar.batch.issue.tracking.LocalIssueTracking;
import org.sonar.batch.issue.tracking.ServerIssueRepository;
import org.sonar.batch.mediumtest.ScanTaskObservers;
import org.sonar.batch.phases.GraphPersister;
import org.sonar.batch.profiling.PhasesSumUpTimeProfiler;
import org.sonar.batch.qualitygate.QualityGateProvider;
import org.sonar.batch.report.*;
import org.sonar.batch.repository.ProjectRepositoriesProvider;
import org.sonar.batch.repository.language.DefaultLanguagesRepository;
import org.sonar.batch.rule.ActiveRulesProvider;
import org.sonar.batch.rule.RulesProvider;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.batch.source.CodeColorizers;
import org.sonar.core.component.ScanGraph;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.workflow.FunctionExecutor;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.technicaldebt.DefaultTechnicalDebtModel;
import org.sonar.core.test.TestPlanBuilder;
import org.sonar.core.test.TestPlanPerspectiveLoader;
import org.sonar.core.test.TestableBuilder;
import org.sonar.core.test.TestablePerspectiveLoader;
import org.sonar.core.user.DefaultUserFinder;

public class ProjectScanContainer extends ComponentContainer {

  private DefaultAnalysisMode analysisMode;

  public ProjectScanContainer(ComponentContainer taskContainer) {
    super(taskContainer);
    analysisMode = taskContainer.getComponentByType(DefaultAnalysisMode.class);
  }

  @Override
  protected void doBeforeStart() {
    projectBootstrap();
    addBatchComponents();
    if (analysisMode.isDb()) {
      addDataBaseComponents();
    }
    addBatchExtensions();
    Settings settings = getComponentByType(Settings.class);
    if (settings != null && settings.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY)) {
      add(PhasesSumUpTimeProfiler.class);
    }
  }

  private void projectBootstrap() {
    // Views pass a custom ProjectReactor
    ProjectReactor reactor = getComponentByType(ProjectReactor.class);
    if (reactor == null) {
      // OK, not present, so look for a deprecated custom ProjectBootstrapper for old versions of SQ Runner
      ProjectBootstrapper bootstrapper = getComponentByType(ProjectBootstrapper.class);
      Settings settings = getComponentByType(Settings.class);
      if (bootstrapper == null
        // Starting from Maven plugin 2.3 then only DefaultProjectBootstrapper should be used.
        || "true".equals(settings.getString("sonar.mojoUseRunner"))) {
        // Use default SonarRunner project bootstrapper
        ProjectReactorBuilder builder = getComponentByType(ProjectReactorBuilder.class);
        reactor = builder.execute();
      } else {
        reactor = bootstrapper.bootstrap();
      }
      if (reactor == null) {
        throw new SonarException(bootstrapper + " has returned null as ProjectReactor");
      }
      add(reactor);
    }
  }

  private void addBatchComponents() {
    add(
      new ProjectRepositoriesProvider(),
      DefaultResourceCreationLock.class,
      CodeColorizers.class,
      DefaultNotificationManager.class,
      MetricProvider.class,
      ProjectConfigurator.class,
      DefaultIndex.class,
      DefaultFileLinesContextFactory.class,
      Caches.class,
      ResourceCache.class,

      // file system
      InputPathCache.class,
      PathResolver.class,

      // rules
      new ActiveRulesProvider(),

      // issues
      IssueUpdater.class,
      FunctionExecutor.class,
      IssueWorkflow.class,
      IssueCache.class,
      DefaultProjectIssues.class,
      IssueChangelogDebtCalculator.class,
      LocalIssueTracking.class,
      ServerIssueRepository.class,

      // tests
      TestPlanPerspectiveLoader.class,
      TestablePerspectiveLoader.class,
      TestPlanBuilder.class,
      TestableBuilder.class,
      ScanGraph.create(),

      // lang
      Languages.class,
      DefaultLanguagesRepository.class,

      // Differential periods
      PeriodsDefinition.class,

      // Measures
      MeasureCache.class,

      // Duplications
      DuplicationCache.class,

      // Dependencies
      DependencyPersister.class,

      // Quality Gate
      new QualityGateProvider(),

      // Events
      EventCache.class,

      ProjectSettings.class,

      // Report
      ReportPublisher.class,
      ComponentsPublisher.class,
      IssuesPublisher.class,
      MeasuresPublisher.class,
      DuplicationsPublisher.class,
      CoveragePublisher.class,
      SourcePublisher.class,
      TestExecutionAndCoveragePublisher.class,

      ScanTaskObservers.class);
  }

  private void addDataBaseComponents() {
    add(
      ResourcePersister.class,
      ResourceKeyMigration.class,
      GraphPersister.class,

      // Users
      DefaultUserFinder.class,

      // Rules
      new RulesProvider(),
      new DebtModelProvider(),

      // technical debt
      DefaultTechnicalDebtModel.class,

      // Issue tracking
      InitialOpenIssuesStack.class,

      ProjectLock.class);
  }

  private void addBatchExtensions() {
    getComponentByType(ExtensionInstaller.class).install(this, new BatchExtensionFilter());
  }

  @Override
  protected void doAfterStart() {
    ProjectTree tree = getComponentByType(ProjectTree.class);
    scanRecursively(tree.getRootProject());
    if (analysisMode.isMediumTest()) {
      getComponentByType(ScanTaskObservers.class).notifyEndOfScanTask();
    }
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
    @Override
    public boolean accept(Object extension) {
      return ExtensionUtils.isType(extension, BatchComponent.class)
        && ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_BATCH);
    }
  }
}
