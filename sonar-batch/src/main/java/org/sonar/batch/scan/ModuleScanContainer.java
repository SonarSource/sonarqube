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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.bootstrap.BatchExtensionDictionnary;
import org.sonar.batch.bootstrap.DefaultAnalysisMode;
import org.sonar.batch.bootstrap.ExtensionInstaller;
import org.sonar.batch.bootstrap.ExtensionMatcher;
import org.sonar.batch.bootstrap.ExtensionUtils;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.debt.DebtDecorator;
import org.sonar.batch.debt.IssueChangelogDebtCalculator;
import org.sonar.batch.debt.NewDebtDecorator;
import org.sonar.batch.debt.SqaleRatingDecorator;
import org.sonar.batch.debt.SqaleRatingSettings;
import org.sonar.batch.deprecated.DeprecatedSensorContext;
import org.sonar.batch.deprecated.ResourceFilters;
import org.sonar.batch.deprecated.components.DefaultProjectClasspath;
import org.sonar.batch.deprecated.components.DefaultTimeMachine;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.issue.IssuableFactory;
import org.sonar.batch.issue.IssueFilters;
import org.sonar.batch.issue.ModuleIssues;
import org.sonar.batch.issue.ignore.EnforceIssuesFilter;
import org.sonar.batch.issue.ignore.IgnoreIssuesFilter;
import org.sonar.batch.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.batch.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.batch.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.batch.issue.ignore.scanner.IssueExclusionsRegexpScanner;
import org.sonar.batch.issue.tracking.InitialOpenIssuesSensor;
import org.sonar.batch.issue.tracking.IssueHandlers;
import org.sonar.batch.issue.tracking.IssueTrackingDecorator;
import org.sonar.batch.language.LanguageDistributionDecorator;
import org.sonar.batch.phases.DatabaseLessPhaseExecutor;
import org.sonar.batch.phases.DatabaseModePhaseExecutor;
import org.sonar.batch.phases.DecoratorsExecutor;
import org.sonar.batch.phases.InitializersExecutor;
import org.sonar.batch.phases.PhaseExecutor;
import org.sonar.batch.phases.PhasesTimeProfiler;
import org.sonar.batch.phases.PostJobsExecutor;
import org.sonar.batch.phases.ProjectInitializer;
import org.sonar.batch.phases.SensorsExecutor;
import org.sonar.batch.qualitygate.GenerateQualityGateEvents;
import org.sonar.batch.qualitygate.QualityGateProvider;
import org.sonar.batch.qualitygate.QualityGateVerifier;
import org.sonar.batch.report.ComponentsPublisher;
import org.sonar.batch.report.IssuesPublisher;
import org.sonar.batch.report.PublishReportJob;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.batch.rule.QProfileDecorator;
import org.sonar.batch.rule.QProfileEventsDecorator;
import org.sonar.batch.rule.QProfileSensor;
import org.sonar.batch.rule.QProfileVerifier;
import org.sonar.batch.rule.RuleFinderCompatibility;
import org.sonar.batch.rule.RulesProfileProvider;
import org.sonar.batch.scan.filesystem.ComponentIndexer;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.batch.scan.filesystem.DeprecatedFileFilters;
import org.sonar.batch.scan.filesystem.ExclusionFilters;
import org.sonar.batch.scan.filesystem.FileIndexer;
import org.sonar.batch.scan.filesystem.FileMetadata;
import org.sonar.batch.scan.filesystem.FileSystemLogger;
import org.sonar.batch.scan.filesystem.InputFileBuilderFactory;
import org.sonar.batch.scan.filesystem.LanguageDetectionFactory;
import org.sonar.batch.scan.filesystem.ModuleFileSystemInitializer;
import org.sonar.batch.scan.filesystem.ModuleInputFileCache;
import org.sonar.batch.scan.filesystem.ProjectFileSystemAdapter;
import org.sonar.batch.scan.filesystem.StatusDetectionFactory;
import org.sonar.batch.scan.report.IssuesReports;
import org.sonar.batch.sensor.AnalyzerOptimizer;
import org.sonar.batch.sensor.DefaultSensorContext;
import org.sonar.batch.sensor.DefaultSensorStorage;
import org.sonar.batch.sensor.coverage.CoverageExclusions;
import org.sonar.core.component.ScanPerspectives;

public class ModuleScanContainer extends ComponentContainer {
  private static final Logger LOG = LoggerFactory.getLogger(ModuleScanContainer.class);
  private final Project module;
  private DefaultAnalysisMode analysisMode;

  public ModuleScanContainer(ProjectScanContainer parent, Project module) {
    super(parent);
    this.module = module;
    analysisMode = parent.getComponentByType(DefaultAnalysisMode.class);
  }

  @Override
  protected void doBeforeStart() {
    LOG.info("-------------  Scan {}", module.getName());
    addCoreComponents();
    if (analysisMode.isDb()) {
      addDataBaseComponents();
    }
    addExtensions();
  }

  private void addCoreComponents() {
    ProjectDefinition moduleDefinition = getComponentByType(ProjectTree.class).getProjectDefinition(module);
    add(
      moduleDefinition,
      module,
      ModuleSettings.class);

    // hack to initialize settings before ExtensionProviders
    ModuleSettings moduleSettings = getComponentByType(ModuleSettings.class);
    module.setSettings(moduleSettings);

    if (analysisMode.isDb()) {
      add(DatabaseModePhaseExecutor.class);
    } else {
      add(RuleFinderCompatibility.class,
        DatabaseLessPhaseExecutor.class);
    }

    add(
      EventBus.class,
      PhasesTimeProfiler.class,
      PostJobsExecutor.class,
      SensorsExecutor.class,
      InitializersExecutor.class,
      ProjectInitializer.class,
      PublishReportJob.class,
      ComponentsPublisher.class,
      IssuesPublisher.class,
      moduleDefinition.getContainerExtensions(),

      // file system
      ModuleInputFileCache.class,
      FileExclusions.class,
      ExclusionFilters.class,
      DeprecatedFileFilters.class,
      InputFileBuilderFactory.class,
      FileMetadata.class,
      StatusDetectionFactory.class,
      LanguageDetectionFactory.class,
      FileIndexer.class,
      ComponentIndexer.class,
      LanguageVerifier.class,
      FileSystemLogger.class,
      DefaultProjectClasspath.class,
      DefaultModuleFileSystem.class,
      ModuleFileSystemInitializer.class,
      ProjectFileSystemAdapter.class,
      QProfileVerifier.class,

      AnalyzerOptimizer.class,

      DefaultSensorContext.class,
      DefaultSensorStorage.class,
      DeprecatedSensorContext.class,
      BatchExtensionDictionnary.class,
      DefaultTimeMachine.class,
      IssueFilters.class,
      CoverageExclusions.class,
      ResourceFilters.class,

      // rules
      ModuleQProfiles.class,
      new RulesProfileProvider(),
      QProfileSensor.class,
      QProfileDecorator.class,
      CheckFactory.class,

      // report
      IssuesReports.class,

      // issues
      IssuableFactory.class,
      ModuleIssues.class,
      org.sonar.api.issue.NoSonarFilter.class,

      // issue exclusions
      IssueInclusionPatternInitializer.class,
      IssueExclusionPatternInitializer.class,
      IssueExclusionsRegexpScanner.class,
      IssueExclusionsLoader.class,
      EnforceIssuesFilter.class,
      IgnoreIssuesFilter.class,
      NoSonarFilter.class,

      ScanPerspectives.class);
  }

  private void addDataBaseComponents() {
    add(DecoratorsExecutor.class,

      // quality gates
      new QualityGateProvider(),
      QualityGateVerifier.class,
      GenerateQualityGateEvents.class,

      // language
      LanguageDistributionDecorator.class,

      // Debt
      IssueChangelogDebtCalculator.class,
      DebtDecorator.class,
      NewDebtDecorator.class,
      SqaleRatingDecorator.class,
      SqaleRatingSettings.class,

      // Issue tracking
      IssueTrackingDecorator.class,
      IssueHandlers.class,
      InitialOpenIssuesSensor.class,

      QProfileEventsDecorator.class,

      TimeMachineConfiguration.class);

  }

  private void addExtensions() {
    ExtensionInstaller installer = getComponentByType(ExtensionInstaller.class);
    installer.install(this, new ExtensionMatcher() {
      @Override
      public boolean accept(Object extension) {
        if (ExtensionUtils.isType(extension, BatchComponent.class) && ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_PROJECT)) {
          // Special use-case: the extension point ProjectBuilder is used in a Maven environment to define some
          // new sub-projects without pom.
          // Example : C# plugin adds sub-projects at runtime, even if they are not defined in root pom.
          return !ExtensionUtils.isMavenExtensionOnly(extension) || module.getPom() != null;
        }
        return false;
      }
    });
  }

  @Override
  protected void doAfterStart() {
    DefaultIndex index = getComponentByType(DefaultIndex.class);
    index.setCurrentProject(module,
      getComponentByType(ModuleIssues.class));

    getComponentByType(PhaseExecutor.class).execute(module);

    // Free memory since module settings are no more used
    module.setSettings(null);
  }

}
