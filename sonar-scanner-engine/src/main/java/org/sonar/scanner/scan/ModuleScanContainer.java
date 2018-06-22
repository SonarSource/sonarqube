/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.core.extension.CoreExtensionsInstaller;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.DefaultFileLinesContextFactory;
import org.sonar.scanner.bootstrap.ExtensionInstaller;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.bootstrap.ScannerExtensionDictionnary;
import org.sonar.scanner.deprecated.perspectives.ScannerPerspectives;
import org.sonar.scanner.issue.IssueFilters;
import org.sonar.scanner.issue.ModuleIssues;
import org.sonar.scanner.issue.ignore.EnforceIssuesFilter;
import org.sonar.scanner.issue.ignore.IgnoreIssuesFilter;
import org.sonar.scanner.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.PatternMatcher;
import org.sonar.scanner.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.scanner.phases.AbstractPhaseExecutor;
import org.sonar.scanner.phases.CoverageExclusions;
import org.sonar.scanner.phases.IssuesPhaseExecutor;
import org.sonar.scanner.phases.PostJobsExecutor;
import org.sonar.scanner.phases.PublishPhaseExecutor;
import org.sonar.scanner.phases.SensorsExecutor;
import org.sonar.scanner.postjob.DefaultPostJobContext;
import org.sonar.scanner.postjob.PostJobOptimizer;
import org.sonar.scanner.rule.QProfileVerifier;
import org.sonar.scanner.rule.RuleFinderCompatibility;
import org.sonar.scanner.rule.RulesProfileProvider;
import org.sonar.scanner.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.scanner.scan.filesystem.ExclusionFilters;
import org.sonar.scanner.scan.filesystem.FileIndexer;
import org.sonar.scanner.scan.filesystem.InputFileBuilder;
import org.sonar.scanner.scan.filesystem.LanguageDetection;
import org.sonar.scanner.scan.filesystem.MetadataGenerator;
import org.sonar.scanner.scan.filesystem.ModuleFileSystemInitializer;
import org.sonar.scanner.scan.filesystem.ModuleInputComponentStore;
import org.sonar.scanner.scan.report.IssuesReports;
import org.sonar.scanner.sensor.DefaultSensorContext;
import org.sonar.scanner.sensor.DefaultSensorStorage;
import org.sonar.scanner.sensor.SensorOptimizer;

import static org.sonar.api.batch.InstantiationStrategy.PER_PROJECT;
import static org.sonar.core.extension.CoreExtensionsInstaller.noExtensionFilter;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isInstantiationStrategy;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isScannerSide;

public class ModuleScanContainer extends ComponentContainer {
  private static final Logger LOG = LoggerFactory.getLogger(ModuleScanContainer.class);
  private final DefaultInputModule module;
  private final GlobalAnalysisMode analysisMode;

  public ModuleScanContainer(ProjectScanContainer parent, DefaultInputModule module, GlobalAnalysisMode analysisMode) {
    super(parent);
    this.module = module;
    this.analysisMode = analysisMode;
  }

  @Override
  protected void doBeforeStart() {
    LOG.info("-------------  Scan {}", module.definition().getName());
    addCoreComponents();
    addExtensions();
  }

  private void addCoreComponents() {
    add(
      module.definition(),
      module,
      MutableModuleSettings.class,
      new ModuleSettingsProvider());

    if (analysisMode.isIssues()) {
      add(
        IssuesPhaseExecutor.class,
        IssuesReports.class);
    } else {
      add(
        PublishPhaseExecutor.class);
    }

    add(
      RuleFinderCompatibility.class,
      PostJobsExecutor.class,
      SensorsExecutor.class,

      // file system
      ModuleInputComponentStore.class,
      FileExclusions.class,
      ExclusionFilters.class,
      MetadataGenerator.class,
      FileMetadata.class,
      LanguageDetection.class,
      FileIndexer.class,
      InputFileBuilder.class,
      DefaultModuleFileSystem.class,
      ModuleFileSystemInitializer.class,
      QProfileVerifier.class,

      SensorOptimizer.class,
      PostJobOptimizer.class,

      DefaultPostJobContext.class,
      DefaultSensorStorage.class,
      DefaultSensorContext.class,
      ScannerExtensionDictionnary.class,
      IssueFilters.class,
      CoverageExclusions.class,

      // rules
      new RulesProfileProvider(),
      CheckFactory.class,

      // issues
      ModuleIssues.class,
      NoSonarFilter.class,

      // issue exclusions
      IssueInclusionPatternInitializer.class,
      IssueExclusionPatternInitializer.class,
      PatternMatcher.class,
      IssueExclusionsLoader.class,
      EnforceIssuesFilter.class,
      IgnoreIssuesFilter.class,

      // Perspectives
      ScannerPerspectives.class,

      DefaultFileLinesContextFactory.class);
  }

  private void addExtensions() {
    CoreExtensionsInstaller coreExtensionsInstaller = getComponentByType(CoreExtensionsInstaller.class);
    coreExtensionsInstaller.install(this, noExtensionFilter(), t -> isInstantiationStrategy(t, PER_PROJECT));
    ExtensionInstaller pluginInstaller = getComponentByType(ExtensionInstaller.class);
    pluginInstaller.install(this, e -> isScannerSide(e) && isInstantiationStrategy(e, PER_PROJECT));
  }

  @Override
  protected void doAfterStart() {
    getComponentByType(AbstractPhaseExecutor.class).execute(module);
  }

}
