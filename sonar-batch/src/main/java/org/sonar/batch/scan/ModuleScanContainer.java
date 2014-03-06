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

import org.sonar.batch.issue.ignore.EnforceIssuesFilter;
import org.sonar.batch.issue.ignore.IgnoreIssuesFilter;
import org.sonar.batch.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.batch.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.batch.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.batch.issue.ignore.scanner.IssueExclusionsRegexpScanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.batch.DefaultProjectClasspath;
import org.sonar.batch.DefaultSensorContext;
import org.sonar.batch.DefaultTimeMachine;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.ResourceFilters;
import org.sonar.batch.ViolationFilters;
import org.sonar.batch.bootstrap.BatchExtensionDictionnary;
import org.sonar.batch.bootstrap.ExtensionInstaller;
import org.sonar.batch.bootstrap.ExtensionMatcher;
import org.sonar.batch.bootstrap.ExtensionUtils;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.batch.issue.IssuableFactory;
import org.sonar.batch.issue.IssueFilters;
import org.sonar.batch.issue.ModuleIssues;
import org.sonar.batch.phases.PhaseExecutor;
import org.sonar.batch.phases.PhasesTimeProfiler;
import org.sonar.batch.rule.QProfileVerifier;
import org.sonar.batch.qualitygate.LegacyQualityGateLoader;
import org.sonar.batch.qualitygate.LegacyQualityGateVerifier;
import org.sonar.batch.rule.ActiveRulesProvider;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.batch.rule.QProfileSensor;
import org.sonar.batch.rule.RulesProfileProvider;
import org.sonar.batch.scan.filesystem.*;
import org.sonar.batch.scan.filesystem.FileIndexer;
import org.sonar.batch.scan.report.JsonReport;
import org.sonar.core.component.ScanPerspectives;
import org.sonar.core.measure.MeasurementFilters;

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
    ProjectDefinition moduleDefinition = getComponentByType(ProjectTree.class).getProjectDefinition(module);
    add(
      moduleDefinition,
      module.getConfiguration(),
      module,
      ModuleSettings.class);

    // hack to initialize commons-configuration before ExtensionProviders
    getComponentByType(ModuleSettings.class);

    add(
      EventBus.class,
      PhaseExecutor.class,
      PhasesTimeProfiler.class,
      UnsupportedProperties.class,
      PhaseExecutor.getPhaseClasses(),
      moduleDefinition.getContainerExtensions(),

      // file system
      ModuleInputFileCache.class,
      FileExclusions.class,
      ExclusionFilters.class,
      DeprecatedFileFilters.class,
      InputFileBuilderFactory.class,
      StatusDetectionFactory.class,
      LanguageDetectionFactory.class,
      PreviousFileHashLoader.class,
      FileIndexer.class,
      ComponentIndexer.class,
      LanguageVerifier.class,
      FileSystemLogger.class,
      DefaultProjectClasspath.class,
      DefaultModuleFileSystem.class,
      ModuleFileSystemInitializer.class,
      ProjectFileSystemAdapter.class,
      QProfileVerifier.class,

      // the Snapshot component will be removed when asynchronous measures are improved (required for AsynchronousMeasureSensor)
      getComponentByType(ResourcePersister.class).getSnapshot(module),

      TimeMachineConfiguration.class,
      DefaultSensorContext.class,
      BatchExtensionDictionnary.class,
      DefaultTimeMachine.class,
      ViolationFilters.class,
      IssueFilters.class,
      MeasurementFilters.class,
      ResourceFilters.class,

      // quality gates
      LegacyQualityGateLoader.class,
      LegacyQualityGateVerifier.class,

      // rules
      ModuleQProfiles.class,
      new ActiveRulesProvider(),
      new RulesProfileProvider(),
      QProfileSensor.class,
      CheckFactory.class,

      // report
      JsonReport.class,

      // issues
      IssuableFactory.class,
      ModuleIssues.class,

      // issue exclusions
      IssueInclusionPatternInitializer.class,
      IssueExclusionPatternInitializer.class,
      IssueExclusionsRegexpScanner.class,
      IssueExclusionsLoader.class,
      EnforceIssuesFilter.class,
      IgnoreIssuesFilter.class,

      ScanPerspectives.class);
  }

  private void addExtensions() {
    ExtensionInstaller installer = getComponentByType(ExtensionInstaller.class);
    installer.install(this, new ExtensionMatcher() {
      public boolean accept(Object extension) {
        if (ExtensionUtils.isType(extension, BatchExtension.class) && ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_PROJECT)) {
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
  }

}
