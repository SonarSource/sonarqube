/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.MessageException;
import org.sonar.core.extension.CoreExtensionsInstaller;
import org.sonar.core.metric.ScannerMetrics;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.scanner.DefaultFileLinesContextFactory;
import org.sonar.scanner.ProjectInfo;
import org.sonar.scanner.analysis.AnalysisTempFolderProvider;
import org.sonar.scanner.cache.AnalysisCacheEnabled;
import org.sonar.scanner.cache.AnalysisCacheMemoryStorage;
import org.sonar.scanner.cache.AnalysisCacheProvider;
import org.sonar.scanner.cache.DefaultAnalysisCacheLoader;
import org.sonar.scanner.ci.CiConfigurationProvider;
import org.sonar.scanner.ci.vendors.AppVeyor;
import org.sonar.scanner.ci.vendors.AwsCodeBuild;
import org.sonar.scanner.ci.vendors.AzureDevops;
import org.sonar.scanner.ci.vendors.Bamboo;
import org.sonar.scanner.ci.vendors.BitbucketPipelines;
import org.sonar.scanner.ci.vendors.Bitrise;
import org.sonar.scanner.ci.vendors.Buildkite;
import org.sonar.scanner.ci.vendors.CircleCi;
import org.sonar.scanner.ci.vendors.CirrusCi;
import org.sonar.scanner.ci.vendors.CodeMagic;
import org.sonar.scanner.ci.vendors.DroneCi;
import org.sonar.scanner.ci.vendors.GithubActions;
import org.sonar.scanner.ci.vendors.GitlabCi;
import org.sonar.scanner.ci.vendors.Jenkins;
import org.sonar.scanner.ci.vendors.SemaphoreCi;
import org.sonar.scanner.ci.vendors.TravisCi;
import org.sonar.scanner.cpd.CpdExecutor;
import org.sonar.scanner.cpd.CpdSettings;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;
import org.sonar.scanner.issue.IssueFilters;
import org.sonar.scanner.issue.IssuePublisher;
import org.sonar.scanner.issue.ignore.EnforceIssuesFilter;
import org.sonar.scanner.issue.ignore.IgnoreIssuesFilter;
import org.sonar.scanner.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.scanner.report.ActiveRulesPublisher;
import org.sonar.scanner.report.AnalysisCachePublisher;
import org.sonar.scanner.report.AnalysisContextReportPublisher;
import org.sonar.scanner.report.AnalysisWarningsPublisher;
import org.sonar.scanner.report.CeTaskReportDataHolder;
import org.sonar.scanner.report.ChangedLinesPublisher;
import org.sonar.scanner.report.ComponentsPublisher;
import org.sonar.scanner.report.ContextPropertiesPublisher;
import org.sonar.scanner.report.JavaArchitectureInformationProvider;
import org.sonar.scanner.report.MetadataPublisher;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.report.ScannerFileStructureProvider;
import org.sonar.scanner.report.SourcePublisher;
import org.sonar.scanner.report.TestExecutionPublisher;
import org.sonar.scanner.repository.ContextPropertiesCache;
import org.sonar.scanner.repository.DefaultProjectRepositoriesLoader;
import org.sonar.scanner.repository.DefaultQualityProfileLoader;
import org.sonar.scanner.repository.ProjectRepositoriesProvider;
import org.sonar.scanner.repository.QualityProfilesProvider;
import org.sonar.scanner.repository.ReferenceBranchSupplier;
import org.sonar.scanner.repository.language.DefaultLanguagesLoader;
import org.sonar.scanner.repository.language.DefaultLanguagesRepository;
import org.sonar.scanner.repository.settings.DefaultProjectSettingsLoader;
import org.sonar.scanner.rule.ActiveRulesProvider;
import org.sonar.scanner.rule.DefaultActiveRulesLoader;
import org.sonar.scanner.rule.QProfileVerifier;
import org.sonar.scanner.scan.DeprecatedPropertiesWarningGenerator;
import org.sonar.scanner.scan.InputModuleHierarchyProvider;
import org.sonar.scanner.scan.InputProjectProvider;
import org.sonar.scanner.scan.ModuleIndexer;
import org.sonar.scanner.scan.MutableProjectReactorProvider;
import org.sonar.scanner.scan.MutableProjectSettings;
import org.sonar.scanner.scan.ProjectBuildersExecutor;
import org.sonar.scanner.scan.ProjectConfigurationProvider;
import org.sonar.scanner.scan.ProjectLock;
import org.sonar.scanner.scan.ProjectReactorBuilder;
import org.sonar.scanner.scan.ProjectReactorValidator;
import org.sonar.scanner.scan.ProjectServerSettingsProvider;
import org.sonar.scanner.scan.ScanProperties;
import org.sonar.scanner.scan.SonarGlobalPropertiesFilter;
import org.sonar.scanner.scan.SpringProjectScanContainer;
import org.sonar.scanner.scan.WorkDirectoriesInitializer;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchConfigurationProvider;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.ProjectBranchesProvider;
import org.sonar.scanner.scan.filesystem.DefaultProjectFileSystem;
import org.sonar.scanner.scan.filesystem.FilePreprocessor;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.scan.filesystem.LanguageDetection;
import org.sonar.scanner.scan.filesystem.MetadataGenerator;
import org.sonar.scanner.scan.filesystem.ModuleRelativePathWarner;
import org.sonar.scanner.scan.filesystem.ProjectCoverageAndDuplicationExclusions;
import org.sonar.scanner.scan.filesystem.ProjectExclusionFilters;
import org.sonar.scanner.scan.filesystem.ProjectFilePreprocessor;
import org.sonar.scanner.scan.filesystem.ScannerComponentIdGenerator;
import org.sonar.scanner.scan.filesystem.StatusDetection;
import org.sonar.scanner.scan.measure.DefaultMetricFinder;
import org.sonar.scanner.scm.ScmChangedFilesProvider;
import org.sonar.scanner.scm.ScmConfiguration;
import org.sonar.scanner.scm.ScmPublisher;
import org.sonar.scanner.scm.ScmRevisionImpl;
import org.sonar.scanner.sensor.DefaultSensorStorage;
import org.sonar.scanner.sensor.ExecutingSensorContext;
import org.sonar.scanner.sensor.ProjectSensorContext;
import org.sonar.scanner.sensor.UnchangedFilesHandler;
import org.sonar.scm.git.GitScmSupport;
import org.sonar.scm.svn.SvnScmSupport;

import static org.sonar.api.batch.InstantiationStrategy.PER_BATCH;
import static org.sonar.core.extension.CoreExtensionsInstaller.noExtensionFilter;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isDeprecatedScannerSide;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isInstantiationStrategy;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isScannerSide;

@Priority(3)
public class SpringScannerContainer extends SpringComponentContainer {
  private static final Logger LOG = LoggerFactory.getLogger(SpringScannerContainer.class);

  public SpringScannerContainer(SpringComponentContainer globalContainer) {
    super(globalContainer);
  }

  @Override
  protected void doBeforeStart() {
    addSuffixesDeprecatedProperties();
    addScannerExtensions();
    addComponents();
  }

  private void addSuffixesDeprecatedProperties() {
    add(
    /* This is needed to support properly the deprecated sonar.rpg.suffixes property when the download optimization feature is enabled.
       The value of the property is needed at the preprocessing stage, but being defined by an optional analyzer means that at preprocessing
       it won't be properly available. This will be removed in SQ 11.0 together with the drop of the property from the rpg analyzer.
       See SONAR-21514 */
      PropertyDefinition.builder("sonar.rpg.file.suffixes")
        .deprecatedKey("sonar.rpg.suffixes")
        .multiValues(true)
        .build());
  }

  private void addScannerExtensions() {
    getParentComponentByType(CoreExtensionsInstaller.class)
      .install(this, noExtensionFilter(), extension -> getScannerProjectExtensionsFilter().accept(extension));
    getParentComponentByType(ExtensionInstaller.class)
      .install(this, getScannerProjectExtensionsFilter());
  }

  private void addComponents() {
    add(
      ScanProperties.class,
      ProjectReactorBuilder.class,
      WorkDirectoriesInitializer.class,
      new MutableProjectReactorProvider(),
      ProjectBuildersExecutor.class,
      ProjectLock.class,
      ProjectReactorValidator.class,
      ProjectInfo.class,
      new BranchConfigurationProvider(),
      new ProjectBranchesProvider(),
      ProjectRepositoriesProvider.class,
      new ProjectServerSettingsProvider(),
      AnalysisCacheEnabled.class,
      DeprecatedPropertiesWarningGenerator.class,

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
      ModuleRelativePathWarner.class,
      FilePreprocessor.class,
      ProjectFilePreprocessor.class,
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

      // issue exclusions
      IssueInclusionPatternInitializer.class,
      IssueExclusionPatternInitializer.class,
      IssueExclusionsLoader.class,
      EnforceIssuesFilter.class,
      IgnoreIssuesFilter.class,

      // context
      ContextPropertiesCache.class,

      MutableProjectSettings.class,
      SonarGlobalPropertiesFilter.class,
      ProjectConfigurationProvider.class,

      ProjectCoverageAndDuplicationExclusions.class,

      // Plugin cache
      AnalysisCacheProvider.class,
      AnalysisCacheMemoryStorage.class,
      DefaultAnalysisCacheLoader.class,

      // Report
      ReferenceBranchSupplier.class,
      ScannerMetrics.class,
      JavaArchitectureInformationProvider.class,
      ReportPublisher.class,
      ScannerFileStructureProvider.class,
      AnalysisContextReportPublisher.class,
      MetadataPublisher.class,
      ActiveRulesPublisher.class,
      ComponentsPublisher.class,
      ContextPropertiesPublisher.class,
      AnalysisCachePublisher.class,
      TestExecutionPublisher.class,
      SourcePublisher.class,
      ChangedLinesPublisher.class,
      AnalysisWarningsPublisher.class,

      CeTaskReportDataHolder.class,

      // Cpd
      CpdExecutor.class,
      CpdSettings.class,
      SonarCpdBlockIndex.class,

      // SCM
      ScmConfiguration.class,
      ScmPublisher.class,
      ScmRevisionImpl.class,

      // Sensors
      DefaultSensorStorage.class,
      DefaultFileLinesContextFactory.class,
      ProjectSensorContext.class,
      ExecutingSensorContext.class,

      UnchangedFilesHandler.class,

      // Filesystem
      DefaultProjectFileSystem.class,

      // CI
      new CiConfigurationProvider(),
      AppVeyor.class,
      AwsCodeBuild.class,
      AzureDevops.class,
      Bamboo.class,
      BitbucketPipelines.class,
      Bitrise.class,
      Buildkite.class,
      CircleCi.class,
      CirrusCi.class,
      DroneCi.class,
      GithubActions.class,
      CodeMagic.class,
      GitlabCi.class,
      Jenkins.class,
      SemaphoreCi.class,
      TravisCi.class
    );

    add(GitScmSupport.getObjects());
    add(SvnScmSupport.getObjects());

    add(DefaultProjectSettingsLoader.class,
      DefaultActiveRulesLoader.class,
      DefaultQualityProfileLoader.class,
      DefaultProjectRepositoriesLoader.class,
      DefaultLanguagesLoader.class,
      DefaultLanguagesRepository.class);
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
    ScanProperties properties = getComponentByType(ScanProperties.class);
    properties.validate();

    properties.get("sonar.branch").ifPresent(deprecatedBranch -> {
      throw MessageException.of("The 'sonar.branch' parameter is no longer supported. You should stop using it. " +
        "Branch analysis is available in Developer Edition and above. See https://www.sonarsource.com/plans-and-pricing/developer/ for more information.");
    });

    BranchConfiguration branchConfig = getComponentByType(BranchConfiguration.class);
    if (branchConfig.branchType() == BranchType.PULL_REQUEST && LOG.isInfoEnabled()) {
      LOG.info("Pull request {} for merge into {} from {}", branchConfig.pullRequestKey(), pullRequestBaseToDisplayName(branchConfig.targetBranchName()),
        branchConfig.branchName());
    } else if (branchConfig.branchName() != null && LOG.isInfoEnabled()) {
      LOG.info("Branch name: {}", branchConfig.branchName());
    }

    getComponentByType(DeprecatedPropertiesWarningGenerator.class).execute();

    getComponentByType(ProjectFilePreprocessor.class).execute();
    new SpringProjectScanContainer(this).execute();
  }

  private static String pullRequestBaseToDisplayName(@Nullable String pullRequestBase) {
    return pullRequestBase != null ? pullRequestBase : "default branch";
  }

}
