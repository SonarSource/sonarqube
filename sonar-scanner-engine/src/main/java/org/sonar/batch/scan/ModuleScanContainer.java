/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.scan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.batch.DefaultProjectTree;
import org.sonar.batch.bootstrap.BatchExtensionDictionnary;
import org.sonar.batch.bootstrap.ExtensionInstaller;
import org.sonar.batch.bootstrap.ExtensionMatcher;
import org.sonar.batch.bootstrap.ExtensionUtils;
import org.sonar.batch.deprecated.DeprecatedSensorContext;
import org.sonar.batch.deprecated.perspectives.BatchPerspectives;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.BatchComponentCache;
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
import org.sonar.batch.phases.AbstractPhaseExecutor;
import org.sonar.batch.phases.InitializersExecutor;
import org.sonar.batch.phases.IssuesPhaseExecutor;
import org.sonar.batch.phases.PostJobsExecutor;
import org.sonar.batch.phases.ProjectInitializer;
import org.sonar.batch.phases.PublishPhaseExecutor;
import org.sonar.batch.phases.SensorsExecutor;
import org.sonar.batch.postjob.DefaultPostJobContext;
import org.sonar.batch.postjob.PostJobOptimizer;
import org.sonar.batch.rule.QProfileVerifier;
import org.sonar.batch.rule.RuleFinderCompatibility;
import org.sonar.batch.rule.RulesProfileProvider;
import org.sonar.batch.scan.filesystem.ComponentIndexer;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.batch.scan.filesystem.DeprecatedFileFilters;
import org.sonar.batch.scan.filesystem.ExclusionFilters;
import org.sonar.batch.scan.filesystem.FileIndexer;
import org.sonar.batch.scan.filesystem.FileSystemLogger;
import org.sonar.batch.scan.filesystem.InputFileBuilderFactory;
import org.sonar.batch.scan.filesystem.LanguageDetectionFactory;
import org.sonar.batch.scan.filesystem.ModuleFileSystemInitializer;
import org.sonar.batch.scan.filesystem.ModuleInputFileCache;
import org.sonar.batch.scan.filesystem.StatusDetectionFactory;
import org.sonar.batch.scan.report.IssuesReports;
import org.sonar.batch.sensor.DefaultSensorStorage;
import org.sonar.batch.sensor.SensorOptimizer;
import org.sonar.batch.sensor.coverage.CoverageExclusions;
import org.sonar.batch.source.HighlightableBuilder;
import org.sonar.batch.source.SymbolizableBuilder;
import org.sonar.core.platform.ComponentContainer;

public class ModuleScanContainer extends ComponentContainer {
  private static final Logger LOG = LoggerFactory.getLogger(ModuleScanContainer.class);
  private final Project module;

  public ModuleScanContainer(ProjectScanContainer parent, Project module) {
    super(parent);
    this.module = module;
  }

  @Override
  protected void doBeforeStart() {
    LOG.info("-------------  Scan {}", module.getName());
    addCoreComponents();
    addExtensions();
  }

  private void addCoreComponents() {
    ProjectDefinition moduleDefinition = getComponentByType(DefaultProjectTree.class).getProjectDefinition(module);
    add(
      moduleDefinition,
      module,
      getComponentByType(BatchComponentCache.class).get(module).inputComponent(),
      ModuleSettings.class);

    // hack to initialize settings before ExtensionProviders
    ModuleSettings moduleSettings = getComponentByType(ModuleSettings.class);
    module.setSettings(moduleSettings);

    if (getComponentByType(AnalysisMode.class).isIssues()) {
      add(IssuesPhaseExecutor.class,
        IssuesReports.class);
    } else {
      add(PublishPhaseExecutor.class);
    }

    add(
      EventBus.class,
      RuleFinderCompatibility.class,
      PostJobsExecutor.class,
      SensorsExecutor.class,
      InitializersExecutor.class,
      ProjectInitializer.class,

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
      DefaultModuleFileSystem.class,
      ModuleFileSystemInitializer.class,
      QProfileVerifier.class,

      SensorOptimizer.class,
      PostJobOptimizer.class,

      DefaultPostJobContext.class,
      DefaultSensorStorage.class,
      DeprecatedSensorContext.class,
      BatchExtensionDictionnary.class,
      IssueFilters.class,
      CoverageExclusions.class,

      // rules
      new RulesProfileProvider(),
      CheckFactory.class,

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

      // Perspectives
      BatchPerspectives.class,
      HighlightableBuilder.class,
      SymbolizableBuilder.class);
  }

  private void addExtensions() {
    ExtensionInstaller installer = getComponentByType(ExtensionInstaller.class);
    installer.install(this, new ExtensionMatcher() {
      @Override
      public boolean accept(Object extension) {
        return ExtensionUtils.isBatchSide(extension) && ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_PROJECT);
      }
    });
  }

  @Override
  protected void doAfterStart() {
    DefaultIndex index = getComponentByType(DefaultIndex.class);
    index.setCurrentProject(module, getComponentByType(DefaultSensorStorage.class));

    getComponentByType(AbstractPhaseExecutor.class).execute(module);

    // Free memory since module settings are no more used
    module.setSettings(null);
  }

}
