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

import org.sonar.batch.cache.ProjectPersistentCacheProvider;

import org.sonar.batch.issue.tracking.LocalIssueTracking;
import org.sonar.batch.issue.tracking.IssueTransition;
import org.sonar.batch.repository.DefaultProjectRepositoriesFactory;
import org.sonar.batch.repository.QualityProfileProvider;
import org.sonar.batch.repository.DefaultQualityProfileLoader;
import org.sonar.batch.repository.QualityProfileLoader;
import org.sonar.batch.repository.ProjectSettingsLoader;
import org.sonar.batch.repository.DefaultProjectSettingsLoader;
import org.sonar.batch.repository.ProjectSettingsProvider;
import org.sonar.batch.rule.DefaultActiveRulesLoader;
import org.sonar.batch.rule.ActiveRulesLoader;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.analysis.AnalysisWSLoaderProvider;
import org.sonar.batch.analysis.AnalysisTempFolderProvider;
import org.sonar.batch.analysis.AnalysisProperties;
import org.sonar.batch.repository.user.UserRepositoryLoader;
import org.sonar.batch.issue.tracking.DefaultServerLineHashesLoader;
import org.sonar.batch.issue.tracking.ServerLineHashesLoader;
import org.sonar.batch.repository.DefaultProjectRepositoriesLoader;
import org.sonar.batch.repository.DefaultServerIssuesLoader;
import org.sonar.batch.repository.ProjectRepositoriesLoader;
import org.sonar.batch.repository.ServerIssuesLoader;
import org.sonar.batch.issue.DefaultIssueCallback;
import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.DefaultFileLinesContextFactory;
import org.sonar.batch.DefaultProjectTree;
import org.sonar.batch.ProjectConfigurator;
import org.sonar.batch.bootstrap.ExtensionInstaller;
import org.sonar.batch.bootstrap.ExtensionMatcher;
import org.sonar.batch.bootstrap.ExtensionUtils;
import org.sonar.batch.bootstrap.MetricProvider;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.index.Caches;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.issue.DefaultProjectIssues;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.issue.tracking.ServerIssueRepository;
import org.sonar.batch.mediumtest.ScanTaskObservers;
import org.sonar.batch.phases.PhasesTimeProfiler;
import org.sonar.batch.profiling.PhasesSumUpTimeProfiler;
import org.sonar.batch.report.ActiveRulesPublisher;
import org.sonar.batch.report.AnalysisContextReportPublisher;
import org.sonar.batch.report.ComponentsPublisher;
import org.sonar.batch.report.CoveragePublisher;
import org.sonar.batch.report.DuplicationsPublisher;
import org.sonar.batch.report.MeasuresPublisher;
import org.sonar.batch.report.MetadataPublisher;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.batch.report.SourcePublisher;
import org.sonar.batch.report.TestExecutionAndCoveragePublisher;
import org.sonar.batch.repository.language.DefaultLanguagesRepository;
import org.sonar.batch.rule.ActiveRulesProvider;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.batch.scan.measure.DefaultMetricFinder;
import org.sonar.batch.scan.measure.DeprecatedMetricFinder;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.batch.source.CodeColorizers;
import org.sonar.batch.test.TestPlanBuilder;
import org.sonar.batch.test.TestableBuilder;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.workflow.FunctionExecutor;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.platform.ComponentContainer;

public class ProjectScanContainer extends ComponentContainer {

  private static final Logger LOG = Loggers.get(ProjectScanContainer.class);

  private final Object[] components;
  private final AnalysisProperties props;

  public ProjectScanContainer(ComponentContainer globalContainer, AnalysisProperties props, Object... components) {
    super(globalContainer);
    this.props = props;
    this.components = components;
  }

  @Override
  protected void doBeforeStart() {
    for (Object component : components) {
      add(component);
    }
    addBatchComponents();
    getComponentByType(ProjectLock.class).tryLock();
    addBatchExtensions();
    Settings settings = getComponentByType(Settings.class);
    if (settings != null && settings.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY)) {
      add(PhasesSumUpTimeProfiler.class);
    }
    if (isTherePreviousAnalysis()) {
      addIssueTrackingComponents();
    }
  }

  private void addBatchComponents() {
    add(
      props,
      DefaultAnalysisMode.class,
      ProjectReactorBuilder.class,
      DefaultProjectRepositoriesFactory.class,
      new MutableProjectReactorProvider(),
      new ImmutableProjectReactorProvider(),
      ProjectBuildersExecutor.class,
      ProjectLock.class,
      EventBus.class,
      PhasesTimeProfiler.class,
      ResourceTypes.class,
      DefaultProjectTree.class,
      ProjectExclusions.class,
      ProjectReactorValidator.class,
      new AnalysisWSLoaderProvider(),
      CodeColorizers.class,
      MetricProvider.class,
      ProjectConfigurator.class,
      DefaultIndex.class,
      DefaultFileLinesContextFactory.class,
      Caches.class,
      BatchComponentCache.class,
      DefaultIssueCallback.class,
      new ProjectSettingsProvider(),
      new ProjectPersistentCacheProvider(),

      // temp
      new AnalysisTempFolderProvider(),

      // file system
      InputPathCache.class,
      PathResolver.class,

      // rules
      new ActiveRulesProvider(),
      new QualityProfileProvider(),

      // issues
      IssueUpdater.class,
      FunctionExecutor.class,
      IssueWorkflow.class,
      IssueCache.class,
      DefaultProjectIssues.class,
      IssueTransition.class,

      // metrics
      DefaultMetricFinder.class,
      DeprecatedMetricFinder.class,

      // tests
      TestPlanBuilder.class,
      TestableBuilder.class,

      // lang
      Languages.class,
      DefaultLanguagesRepository.class,

      // Measures
      MeasureCache.class,

      // Duplications
      DuplicationCache.class,

      ProjectSettings.class,

      // Report
      ReportPublisher.class,
      AnalysisContextReportPublisher.class,
      MetadataPublisher.class,
      ActiveRulesPublisher.class,
      ComponentsPublisher.class,
      MeasuresPublisher.class,
      DuplicationsPublisher.class,
      CoveragePublisher.class,
      SourcePublisher.class,
      TestExecutionAndCoveragePublisher.class,

      ScanTaskObservers.class,
      UserRepositoryLoader.class);

    addIfMissing(DefaultActiveRulesLoader.class, ActiveRulesLoader.class);
    addIfMissing(DefaultQualityProfileLoader.class, QualityProfileLoader.class);
    addIfMissing(DefaultProjectRepositoriesLoader.class, ProjectRepositoriesLoader.class);
    addIfMissing(DefaultProjectSettingsLoader.class, ProjectSettingsLoader.class);
  }

  private void addIssueTrackingComponents() {
    add(
      LocalIssueTracking.class,
      ServerIssueRepository.class);
    addIfMissing(DefaultServerIssuesLoader.class, ServerIssuesLoader.class);
    addIfMissing(DefaultServerLineHashesLoader.class, ServerLineHashesLoader.class);
  }

  private boolean isTherePreviousAnalysis() {
    if (getComponentByType(DefaultAnalysisMode.class).isNotAssociated()) {
      return false;
    }

    return getComponentByType(DefaultProjectRepositoriesFactory.class).create().lastAnalysisDate() != null;
  }

  private void addBatchExtensions() {
    getComponentByType(ExtensionInstaller.class).install(this, new BatchExtensionFilter());
  }

  @Override
  protected void doAfterStart() {
    DefaultAnalysisMode analysisMode = getComponentByType(DefaultAnalysisMode.class);
    analysisMode.printMode();
    LOG.debug("Start recursive analysis of project modules");
    DefaultProjectTree tree = getComponentByType(DefaultProjectTree.class);
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
      return ExtensionUtils.isBatchSide(extension)
        && ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_BATCH);
    }
  }

}
