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
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.bootstrap.*;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.debt.*;
import org.sonar.batch.deprecated.DeprecatedSensorContext;
import org.sonar.batch.deprecated.ResourceFilters;
import org.sonar.batch.deprecated.components.DefaultProjectClasspath;
import org.sonar.batch.deprecated.components.DefaultTimeMachine;
import org.sonar.batch.deprecated.perspectives.BatchPerspectives;
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
import org.sonar.batch.phases.*;
import org.sonar.batch.postjob.DefaultPostJobContext;
import org.sonar.batch.postjob.PostJobOptimizer;
import org.sonar.batch.qualitygate.GenerateQualityGateEvents;
import org.sonar.batch.qualitygate.QualityGateVerifier;
import org.sonar.batch.rule.*;
import org.sonar.batch.scan.filesystem.*;
import org.sonar.batch.scan.report.IssuesReports;
import org.sonar.batch.sensor.DefaultSensorContext;
import org.sonar.batch.sensor.DefaultSensorStorage;
import org.sonar.batch.sensor.SensorOptimizer;
import org.sonar.batch.sensor.coverage.CoverageExclusions;
import org.sonar.batch.source.HighlightableBuilder;
import org.sonar.batch.source.SymbolizableBuilder;

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

    add(
      PhaseExecutor.class,
      RuleFinderCompatibility.class,
      EventBus.class,
      PhasesTimeProfiler.class,
      PostJobsExecutor.class,
      DecoratorsExecutor.class,
      SensorsExecutor.class,
      PersistersExecutor.class,
      InitializersExecutor.class,
      ProjectInitializer.class,
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

      SensorOptimizer.class,
      PostJobOptimizer.class,

      DefaultSensorContext.class,
      DefaultPostJobContext.class,
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

      // Perspectives
      BatchPerspectives.class,
      HighlightableBuilder.class,
      SymbolizableBuilder.class);
  }

  private void addDataBaseComponents() {
    add(
      // Quality Gate
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
    index.setCurrentProject(module, getComponentByType(ModuleIssues.class));

    getComponentByType(PhaseExecutor.class).execute(module);

    // Free memory since module settings are no more used
    module.setSettings(null);
  }

}
