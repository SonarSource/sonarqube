/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scan;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.metric.ScannerMetrics;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.ProjectAnalysisInfo;
import org.sonar.scanner.analysis.AnalysisProperties;
import org.sonar.scanner.analysis.AnalysisTempFolderProvider;
import org.sonar.scanner.analysis.DefaultAnalysisMode;
import org.sonar.scanner.bootstrap.ExtensionInstaller;
import org.sonar.scanner.bootstrap.ExtensionMatcher;
import org.sonar.scanner.bootstrap.ExtensionUtils;
import org.sonar.scanner.bootstrap.MetricProvider;
import org.sonar.scanner.cpd.CpdExecutor;
import org.sonar.scanner.cpd.CpdSettings;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;
import org.sonar.scanner.deprecated.test.TestPlanBuilder;
import org.sonar.scanner.deprecated.test.TestableBuilder;
import org.sonar.scanner.events.EventBus;
import org.sonar.scanner.index.DefaultIndex;
import org.sonar.scanner.issue.DefaultIssueCallback;
import org.sonar.scanner.issue.DefaultProjectIssues;
import org.sonar.scanner.issue.IssueCache;
import org.sonar.scanner.issue.tracking.DefaultServerLineHashesLoader;
import org.sonar.scanner.issue.tracking.IssueTransition;
import org.sonar.scanner.issue.tracking.LocalIssueTracking;
import org.sonar.scanner.issue.tracking.ServerIssueRepository;
import org.sonar.scanner.issue.tracking.ServerLineHashesLoader;
import org.sonar.scanner.mediumtest.ScanTaskObservers;
import org.sonar.scanner.phases.PhasesTimeProfiler;
import org.sonar.scanner.profiling.PhasesSumUpTimeProfiler;
import org.sonar.scanner.report.ActiveRulesPublisher;
import org.sonar.scanner.report.AnalysisContextReportPublisher;
import org.sonar.scanner.report.ComponentsPublisher;
import org.sonar.scanner.report.ContextPropertiesPublisher;
import org.sonar.scanner.report.CoveragePublisher;
import org.sonar.scanner.report.MeasuresPublisher;
import org.sonar.scanner.report.MetadataPublisher;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.report.SourcePublisher;
import org.sonar.scanner.report.TestExecutionAndCoveragePublisher;
import org.sonar.scanner.repository.ContextPropertiesCache;
import org.sonar.scanner.repository.DefaultProjectRepositoriesLoader;
import org.sonar.scanner.repository.DefaultQualityProfileLoader;
import org.sonar.scanner.repository.DefaultServerIssuesLoader;
import org.sonar.scanner.repository.ProjectRepositories;
import org.sonar.scanner.repository.ProjectRepositoriesLoader;
import org.sonar.scanner.repository.ProjectRepositoriesProvider;
import org.sonar.scanner.repository.QualityProfileLoader;
import org.sonar.scanner.repository.QualityProfileProvider;
import org.sonar.scanner.repository.ServerIssuesLoader;
import org.sonar.scanner.repository.language.DefaultLanguagesRepository;
import org.sonar.scanner.rule.ActiveRulesLoader;
import org.sonar.scanner.rule.ActiveRulesProvider;
import org.sonar.scanner.rule.DefaultActiveRulesLoader;
import org.sonar.scanner.rule.DefaultRulesLoader;
import org.sonar.scanner.rule.RulesLoader;
import org.sonar.scanner.rule.RulesProvider;
import org.sonar.scanner.scan.filesystem.BatchIdGenerator;
import org.sonar.scanner.scan.filesystem.InputComponentStoreProvider;
import org.sonar.scanner.scan.measure.DefaultMetricFinder;
import org.sonar.scanner.scan.measure.DeprecatedMetricFinder;
import org.sonar.scanner.scan.measure.MeasureCache;
import org.sonar.scanner.storage.Storages;

import com.google.common.annotations.VisibleForTesting;

public class ProjectScanContainer extends ComponentContainer {

  private static final Logger LOG = Loggers.get(ProjectScanContainer.class);

  private final AnalysisProperties props;

  public ProjectScanContainer(ComponentContainer globalContainer, AnalysisProperties props) {
    super(globalContainer);
    this.props = props;
  }

  @Override
  protected void doBeforeStart() {
    addBatchComponents();
    addBatchExtensions();
    ProjectLock lock = getComponentByType(ProjectLock.class);
    lock.tryLock();
    getComponentByType(WorkDirectoryCleaner.class).execute();
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
      WorkDirectoryCleaner.class,
      new MutableProjectReactorProvider(),
      ProjectBuildersExecutor.class,
      ProjectLock.class,
      EventBus.class,
      PhasesTimeProfiler.class,
      ResourceTypes.class,
      ProjectReactorValidator.class,
      MetricProvider.class,
      ProjectAnalysisInfo.class,
      DefaultIndex.class,
      Storages.class,
      new RulesProvider(),
      new ProjectRepositoriesProvider(),

      // temp
      new AnalysisTempFolderProvider(),

      // file system
      ModuleIndexer.class,
      new InputComponentStoreProvider(),
      PathResolver.class,
      new InputModuleHierarchyProvider(),
      DefaultComponentTree.class,
      BatchIdGenerator.class,

      // rules
      new ActiveRulesProvider(),
      new QualityProfileProvider(),

      // issues
      DefaultIssueCallback.class,
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

      // context
      ContextPropertiesCache.class,
      ContextPropertiesPublisher.class,

      MutableProjectSettings.class,
      new ProjectSettingsProvider(),

      // Report
      ScannerMetrics.class,
      ReportPublisher.class,
      AnalysisContextReportPublisher.class,
      MetadataPublisher.class,
      ActiveRulesPublisher.class,
      ComponentsPublisher.class,
      MeasuresPublisher.class,
      CoveragePublisher.class,
      SourcePublisher.class,
      TestExecutionAndCoveragePublisher.class,

      // Cpd
      CpdExecutor.class,
      CpdSettings.class,
      SonarCpdBlockIndex.class,

      ScanTaskObservers.class);

    addIfMissing(DefaultRulesLoader.class, RulesLoader.class);
    addIfMissing(DefaultActiveRulesLoader.class, ActiveRulesLoader.class);
    addIfMissing(DefaultQualityProfileLoader.class, QualityProfileLoader.class);
    addIfMissing(DefaultProjectRepositoriesLoader.class, ProjectRepositoriesLoader.class);
  }

  private void addIssueTrackingComponents() {
    add(
      LocalIssueTracking.class,
      ServerIssueRepository.class);
    addIfMissing(DefaultServerIssuesLoader.class, ServerIssuesLoader.class);
    addIfMissing(DefaultServerLineHashesLoader.class, ServerLineHashesLoader.class);
  }

  private boolean isTherePreviousAnalysis() {
    return getComponentByType(ProjectRepositories.class).lastAnalysisDate() != null;
  }

  private void addBatchExtensions() {
    getComponentByType(ExtensionInstaller.class).install(this, new BatchExtensionFilter());
  }

  @Override
  protected void doAfterStart() {
    DefaultAnalysisMode analysisMode = getComponentByType(DefaultAnalysisMode.class);
    InputModuleHierarchy tree = getComponentByType(InputModuleHierarchy.class);

    analysisMode.printMode();
    LOG.info("Project key: {}", tree.root().key());
    String organization = props.property("sonar.organization");
    if (StringUtils.isNotEmpty(organization)) {
      LOG.info("Organization key: {}", organization);
    }
    String branch = tree.root().definition().getBranch();
    if (branch != null) {
      LOG.info("Branch key: {}", branch);
    }

    LOG.debug("Start recursive analysis of project modules");
    scanRecursively(tree, tree.root());

    if (analysisMode.isMediumTest()) {
      getComponentByType(ScanTaskObservers.class).notifyEndOfScanTask();
    }
  }

  private void scanRecursively(InputModuleHierarchy tree, DefaultInputModule module) {
    for (DefaultInputModule child : tree.children(module)) {
      scanRecursively(tree, child);
    }
    scan(module);
  }

  @VisibleForTesting
  void scan(DefaultInputModule module) {
    new ModuleScanContainer(this, module).execute();
  }

  static class BatchExtensionFilter implements ExtensionMatcher {
    @Override
    public boolean accept(Object extension) {
      return ExtensionUtils.isScannerSide(extension)
        && ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_BATCH);
    }
  }

}
