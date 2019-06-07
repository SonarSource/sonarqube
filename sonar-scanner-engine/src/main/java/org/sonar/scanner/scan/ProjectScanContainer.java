/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import javax.annotation.Nullable;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.config.ScannerProperties;
import org.sonar.core.extension.CoreExtensionsInstaller;
import org.sonar.core.metric.ScannerMetrics;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.DefaultFileLinesContextFactory;
import org.sonar.scanner.ProjectInfo;
import org.sonar.scanner.analysis.AnalysisTempFolderProvider;
import org.sonar.scanner.analysis.DefaultAnalysisMode;
import org.sonar.scanner.bootstrap.ExtensionInstaller;
import org.sonar.scanner.bootstrap.ExtensionMatcher;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.bootstrap.MetricProvider;
import org.sonar.scanner.bootstrap.PostJobExtensionDictionnary;
import org.sonar.scanner.bootstrap.ProcessedScannerProperties;
import org.sonar.scanner.ci.CiConfigurationProvider;
import org.sonar.scanner.ci.vendors.AppVeyor;
import org.sonar.scanner.ci.vendors.AzureDevops;
import org.sonar.scanner.ci.vendors.BitbucketPipelines;
import org.sonar.scanner.ci.vendors.Buildkite;
import org.sonar.scanner.ci.vendors.CircleCi;
import org.sonar.scanner.ci.vendors.CirrusCi;
import org.sonar.scanner.ci.vendors.DroneCi;
import org.sonar.scanner.ci.vendors.GithubActions;
import org.sonar.scanner.ci.vendors.GitlabCi;
import org.sonar.scanner.ci.vendors.Jenkins;
import org.sonar.scanner.ci.vendors.SemaphoreCi;
import org.sonar.scanner.ci.vendors.TravisCi;
import org.sonar.scanner.cpd.CpdExecutor;
import org.sonar.scanner.cpd.CpdSettings;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;
import org.sonar.scanner.deprecated.test.TestPlanBuilder;
import org.sonar.scanner.deprecated.test.TestableBuilder;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.issue.IssueFilters;
import org.sonar.scanner.issue.IssuePublisher;
import org.sonar.scanner.issue.ignore.EnforceIssuesFilter;
import org.sonar.scanner.issue.ignore.IgnoreIssuesFilter;
import org.sonar.scanner.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.scanner.mediumtest.AnalysisObservers;
import org.sonar.scanner.notifications.DefaultAnalysisWarnings;
import org.sonar.scanner.postjob.DefaultPostJobContext;
import org.sonar.scanner.postjob.PostJobOptimizer;
import org.sonar.scanner.postjob.PostJobsExecutor;
import org.sonar.scanner.report.ActiveRulesPublisher;
import org.sonar.scanner.report.AnalysisContextReportPublisher;
import org.sonar.scanner.report.AnalysisWarningsPublisher;
import org.sonar.scanner.report.ChangedLinesPublisher;
import org.sonar.scanner.report.ComponentsPublisher;
import org.sonar.scanner.report.ContextPropertiesPublisher;
import org.sonar.scanner.report.MetadataPublisher;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.report.SourcePublisher;
import org.sonar.scanner.report.TestExecutionPublisher;
import org.sonar.scanner.repository.ContextPropertiesCache;
import org.sonar.scanner.repository.DefaultProjectRepositoriesLoader;
import org.sonar.scanner.repository.DefaultQualityProfileLoader;
import org.sonar.scanner.repository.ProjectRepositoriesLoader;
import org.sonar.scanner.repository.ProjectRepositoriesSupplier;
import org.sonar.scanner.repository.QualityProfileLoader;
import org.sonar.scanner.repository.QualityProfilesProvider;
import org.sonar.scanner.repository.language.DefaultLanguagesRepository;
import org.sonar.scanner.repository.settings.DefaultProjectSettingsLoader;
import org.sonar.scanner.repository.settings.ProjectSettingsLoader;
import org.sonar.scanner.rule.ActiveRulesLoader;
import org.sonar.scanner.rule.ActiveRulesProvider;
import org.sonar.scanner.rule.DefaultActiveRulesLoader;
import org.sonar.scanner.rule.DefaultRulesLoader;
import org.sonar.scanner.rule.QProfileVerifier;
import org.sonar.scanner.rule.RulesLoader;
import org.sonar.scanner.rule.RulesProvider;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchConfigurationProvider;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.ProjectBranchesProvider;
import org.sonar.scanner.scan.branch.ProjectPullRequestsProvider;
import org.sonar.scanner.scan.filesystem.DefaultProjectFileSystem;
import org.sonar.scanner.scan.filesystem.FileIndexer;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.scan.filesystem.LanguageDetection;
import org.sonar.scanner.scan.filesystem.MetadataGenerator;
import org.sonar.scanner.scan.filesystem.ProjectCoverageAndDuplicationExclusions;
import org.sonar.scanner.scan.filesystem.ProjectExclusionFilters;
import org.sonar.scanner.scan.filesystem.ProjectFileIndexer;
import org.sonar.scanner.scan.filesystem.ScannerComponentIdGenerator;
import org.sonar.scanner.scan.filesystem.StatusDetection;
import org.sonar.scanner.scan.measure.DefaultMetricFinder;
import org.sonar.scanner.scm.ScmChangedFilesProvider;
import org.sonar.scanner.scm.ScmConfiguration;
import org.sonar.scanner.scm.ScmPublisher;
import org.sonar.scanner.scm.ScmRevisionImpl;
import org.sonar.scanner.sensor.DefaultSensorStorage;
import org.sonar.scanner.sensor.ProjectSensorContext;
import org.sonar.scanner.sensor.ProjectSensorExtensionDictionnary;
import org.sonar.scanner.sensor.ProjectSensorOptimizer;
import org.sonar.scanner.sensor.ProjectSensorsExecutor;

import static org.sonar.api.batch.InstantiationStrategy.PER_BATCH;
import static org.sonar.core.extension.CoreExtensionsInstaller.noExtensionFilter;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isDeprecatedScannerSide;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isInstantiationStrategy;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isScannerSide;

public class ProjectScanContainer extends ComponentContainer {

  private static final Logger LOG = Loggers.get(ProjectScanContainer.class);

  public ProjectScanContainer(ComponentContainer globalContainer) {
    super(globalContainer);
  }

  @Override
  protected void doBeforeStart() {
    addScannerExtensions();
    addScannerComponents();
    ProjectLock lock = getComponentByType(ProjectLock.class);
    lock.tryLock();
    getComponentByType(WorkDirectoriesInitializer.class).execute();
  }

  private void addScannerComponents() {
    add(
      new ExternalProjectKeyAndOrganizationProvider(),
      ProcessedScannerProperties.class,
      ScanProperties.class,
      ProjectReactorBuilder.class,
      WorkDirectoriesInitializer.class,
      new MutableProjectReactorProvider(),
      ProjectBuildersExecutor.class,
      ProjectLock.class,
      ResourceTypes.class,
      ProjectReactorValidator.class,
      MetricProvider.class,
      ProjectInfo.class,
      new RulesProvider(),
      new BranchConfigurationProvider(),
      new ProjectBranchesProvider(),
      new ProjectPullRequestsProvider(),
      DefaultAnalysisMode.class,
      ProjectRepositoriesSupplier.class,
      new ProjectServerSettingsProvider(),

      // temp
      new AnalysisTempFolderProvider(),

      // file system
      ModuleIndexer.class,
      InputComponentStore.class,
      PathResolver.class,
      new InputProjectProvider(),
      new InputModuleHierarchyProvider(),
      ScannerComponentIdGenerator.class,
      new ScmChangedFilesProvider(),
      StatusDetection.class,
      LanguageDetection.class,
      MetadataGenerator.class,
      FileMetadata.class,
      FileIndexer.class,
      ProjectFileIndexer.class,
      ProjectExclusionFilters.class,

      // rules
      new ActiveRulesProvider(),
      new QualityProfilesProvider(),
      CheckFactory.class,
      QProfileVerifier.class,

      // issues
      DefaultNoSonarFilter.class,
      IssueFilters.class,
      IssuePublisher.class,

      // metrics
      DefaultMetricFinder.class,

      // tests
      TestPlanBuilder.class,
      TestableBuilder.class,

      // lang
      Languages.class,
      DefaultLanguagesRepository.class,

      // issue exclusions
      IssueInclusionPatternInitializer.class,
      IssueExclusionPatternInitializer.class,
      IssueExclusionsLoader.class,
      EnforceIssuesFilter.class,
      IgnoreIssuesFilter.class,

      // context
      ContextPropertiesCache.class,
      ContextPropertiesPublisher.class,

      DefaultAnalysisWarnings.class,

      SensorStrategy.class,

      MutableProjectSettings.class,
      ScannerProperties.class,
      new ProjectConfigurationProvider(),

      ProjectCoverageAndDuplicationExclusions.class,

      // Report
      ScannerMetrics.class,
      ReportPublisher.class,
      AnalysisContextReportPublisher.class,
      MetadataPublisher.class,
      ActiveRulesPublisher.class,
      AnalysisWarningsPublisher.class,
      ComponentsPublisher.class,
      TestExecutionPublisher.class,
      SourcePublisher.class,
      ChangedLinesPublisher.class,

      // Cpd
      CpdExecutor.class,
      CpdSettings.class,
      SonarCpdBlockIndex.class,

      // PostJobs
      PostJobsExecutor.class,
      PostJobOptimizer.class,
      DefaultPostJobContext.class,
      PostJobExtensionDictionnary.class,

      // SCM
      ScmConfiguration.class,
      ScmPublisher.class,
      ScmRevisionImpl.class,

      // Sensors
      DefaultSensorStorage.class,
      DefaultFileLinesContextFactory.class,
      ProjectSensorContext.class,
      ProjectSensorOptimizer.class,
      ProjectSensorsExecutor.class,
      ProjectSensorExtensionDictionnary.class,

      // Filesystem
      DefaultProjectFileSystem.class,

      // CI
      new CiConfigurationProvider(),
      AppVeyor.class,
      AzureDevops.class,
      BitbucketPipelines.class,
      Buildkite.class,
      CircleCi.class,
      CirrusCi.class,
      DroneCi.class,
      GithubActions.class,
      GitlabCi.class,
      Jenkins.class,
      SemaphoreCi.class,
      TravisCi.class,

      AnalysisObservers.class);

    addIfMissing(DefaultProjectSettingsLoader.class, ProjectSettingsLoader.class);
    addIfMissing(DefaultRulesLoader.class, RulesLoader.class);
    addIfMissing(DefaultActiveRulesLoader.class, ActiveRulesLoader.class);
    addIfMissing(DefaultQualityProfileLoader.class, QualityProfileLoader.class);
    addIfMissing(DefaultProjectRepositoriesLoader.class, ProjectRepositoriesLoader.class);
  }

  private void addScannerExtensions() {
    getComponentByType(CoreExtensionsInstaller.class)
      .install(this, noExtensionFilter(), extension -> getScannerProjectExtensionsFilter().accept(extension));
    getComponentByType(ExtensionInstaller.class)
      .install(this, getScannerProjectExtensionsFilter());
  }

  static ExtensionMatcher getScannerProjectExtensionsFilter() {
    return extension -> {
      if (isDeprecatedScannerSide(extension)) {
        return isInstantiationStrategy(extension, PER_BATCH);
      }
      return isScannerSide(extension);
    };
  }

  @Override
  protected void doAfterStart() {
    GlobalAnalysisMode analysisMode = getComponentByType(GlobalAnalysisMode.class);
    InputModuleHierarchy tree = getComponentByType(InputModuleHierarchy.class);
    ScanProperties properties = getComponentByType(ScanProperties.class);
    properties.validate();

    properties.organizationKey().ifPresent(k -> LOG.info("Organization key: {}", k));
    properties.get("sonar.branch").ifPresent(deprecatedBranch -> {
      throw MessageException.of("The 'sonar.branch' parameter is no longer supported. You should stop using it. " +
        "Branch analysis is available in Developer Edition and above. See https://redirect.sonarsource.com/editions/developer.html for more information.");
    });

    BranchConfiguration branchConfig = getComponentByType(BranchConfiguration.class);
    if (branchConfig.branchType() == BranchType.PULL_REQUEST) {
      LOG.info("Pull request {} for merge into {} from {}", branchConfig.pullRequestKey(), pullRequestBaseToDisplayName(branchConfig.targetBranchName()),
        branchConfig.branchName());
    } else if (branchConfig.branchName() != null) {
      LOG.info("Branch name: {}, type: {}", branchConfig.branchName(), branchTypeToDisplayName(branchConfig.branchType()));
    }

    getComponentByType(ProjectFileIndexer.class).index();

    // Log detected languages and their profiles after FS is indexed and languages detected
    getComponentByType(QProfileVerifier.class).execute();

    scanRecursively(tree, tree.root());

    LOG.info("------------- Run sensors on project");
    getComponentByType(ProjectSensorsExecutor.class).execute();

    getComponentByType(ScmPublisher.class).publish();

    getComponentByType(CpdExecutor.class).execute();
    getComponentByType(ReportPublisher.class).execute();

    getComponentByType(PostJobsExecutor.class).execute();

    if (analysisMode.isMediumTest()) {
      getComponentByType(AnalysisObservers.class).notifyEndOfScanTask();
    }
  }

  private static String pullRequestBaseToDisplayName(@Nullable String pullRequestBase) {
    return pullRequestBase != null ? pullRequestBase : "default branch";
  }

  private static String branchTypeToDisplayName(BranchType branchType) {
    switch (branchType) {
      case LONG:
        return "long living";
      case SHORT:
        return "short living";
      default:
        throw new UnsupportedOperationException("unknown branch type: " + branchType);
    }
  }

  private void scanRecursively(InputModuleHierarchy tree, DefaultInputModule module) {
    for (DefaultInputModule child : tree.children(module)) {
      scanRecursively(tree, child);
    }
    LOG.info("------------- Run sensors on module {}", module.definition().getName());
    scan(module);
  }

  void scan(DefaultInputModule module) {
    new ModuleScanContainer(this, module).execute();
  }

}
