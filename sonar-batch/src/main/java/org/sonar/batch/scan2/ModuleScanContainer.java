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
package org.sonar.batch.scan2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.batch.bootstrap.BatchExtensionDictionnary;
import org.sonar.batch.bootstrap.ExtensionInstaller;
import org.sonar.batch.bootstrap.ExtensionMatcher;
import org.sonar.batch.bootstrap.ExtensionUtils;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.issue.IssuableFactory;
import org.sonar.batch.issue.IssueFilters;
import org.sonar.batch.issue.ModuleIssues;
import org.sonar.batch.issue.ignore.EnforceIssuesFilter;
import org.sonar.batch.issue.ignore.IgnoreIssuesFilter;
import org.sonar.batch.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.batch.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.batch.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.batch.issue.ignore.scanner.IssueExclusionsRegexpScanner;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.batch.rule.QProfileVerifier;
import org.sonar.batch.scan.LanguageVerifier;
import org.sonar.batch.scan.ModuleSettings;
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

public class ModuleScanContainer extends ComponentContainer {
  private static final Logger LOG = LoggerFactory.getLogger(ModuleScanContainer.class);
  private final ProjectDefinition moduleDefinition;

  public ModuleScanContainer(ProjectScanContainer parent, ProjectDefinition moduleDefinition) {
    super(parent);
    this.moduleDefinition = moduleDefinition;
  }

  @Override
  protected void doBeforeStart() {
    LOG.info("-------------  Scan {}", moduleDefinition.getName());
    addCoreComponents();
    addExtensions();
  }

  private void addCoreComponents() {
    add(
      moduleDefinition,
      ModuleSettings.class,

      EventBus.class,
      Phase2Executor.class,
      Phase2Executor.getPhaseClasses(),
      moduleDefinition.getContainerExtensions(),
      AnalyzersExecutor.class,

      // file system
      ModuleInputFileCache.class,
      FileExclusions.class,
      ExclusionFilters.class,
      DeprecatedFileFilters.class,
      InputFileBuilderFactory.class,
      StatusDetectionFactory.class,
      LanguageDetectionFactory.class,
      FileIndexer.class,
      LanguageVerifier.class,
      FileSystemLogger.class,
      DefaultModuleFileSystem.class,
      ModuleFileSystemInitializer.class,
      QProfileVerifier.class,

      DefaultAnalyzerContext.class,
      BatchExtensionDictionnary.class,
      IssueFilters.class,

      // rules
      ModuleQProfiles.class,
      CheckFactory.class,

      // issues
      IssuableFactory.class,
      ModuleIssues.class,

      // issue exclusions
      IssueInclusionPatternInitializer.class,
      IssueExclusionPatternInitializer.class,
      IssueExclusionsRegexpScanner.class,
      IssueExclusionsLoader.class,
      EnforceIssuesFilter.class,
      IgnoreIssuesFilter.class);
  }

  private void addExtensions() {
    ExtensionInstaller installer = getComponentByType(ExtensionInstaller.class);
    installer.install(this, new ExtensionMatcher() {
      public boolean accept(Object extension) {
        return ExtensionUtils.isType(extension, BatchComponent.class)
          && ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_PROJECT);
      }
    });
  }

  @Override
  protected void doAfterStart() {
    getComponentByType(Phase2Executor.class).execute(moduleDefinition);
  }

}
