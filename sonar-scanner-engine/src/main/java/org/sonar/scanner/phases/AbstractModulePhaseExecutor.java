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
package org.sonar.scanner.phases;

import java.util.Arrays;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.AbstractProjectOrModule;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.scanner.rule.QProfileVerifier;
import org.sonar.scanner.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.scanner.scan.filesystem.FileIndexer;

public abstract class AbstractModulePhaseExecutor {

  private static final Logger LOG = Loggers.get(AbstractModulePhaseExecutor.class);

  private final PostJobsExecutor postJobsExecutor;
  private final SensorsExecutor sensorsExecutor;
  private final DefaultModuleFileSystem fs;
  private final QProfileVerifier profileVerifier;
  private final IssueExclusionsLoader issueExclusionsLoader;
  private final InputModuleHierarchy hierarchy;
  private final FileIndexer fileIndexer;
  private final ModuleCoverageExclusions moduleCoverageExclusions;
  private final ProjectCoverageExclusions projectCoverageExclusions;
  private final AnalysisWarnings analysisWarnings;
  private boolean warnCoverageAlreadyLogged;

  public AbstractModulePhaseExecutor(PostJobsExecutor postJobsExecutor, SensorsExecutor sensorsExecutor, InputModuleHierarchy hierarchy, DefaultModuleFileSystem fs,
                                     QProfileVerifier profileVerifier, IssueExclusionsLoader issueExclusionsLoader, FileIndexer fileIndexer,
                                     ModuleCoverageExclusions moduleCoverageExclusions, ProjectCoverageExclusions projectCoverageExclusions,
                                     AnalysisWarnings analysisWarnings) {
    this.postJobsExecutor = postJobsExecutor;
    this.sensorsExecutor = sensorsExecutor;
    this.fs = fs;
    this.profileVerifier = profileVerifier;
    this.issueExclusionsLoader = issueExclusionsLoader;
    this.hierarchy = hierarchy;
    this.fileIndexer = fileIndexer;
    this.moduleCoverageExclusions = moduleCoverageExclusions;
    this.projectCoverageExclusions = projectCoverageExclusions;
    this.analysisWarnings = analysisWarnings;
  }

  /**
   * Executed on each module
   */
  public final void execute(DefaultInputModule module) {
    // Index the filesystem
    fileIndexer.index();

    // Log detected languages and their profiles after FS is indexed and languages detected
    profileVerifier.execute();

    // Initialize issue exclusions
    initIssueExclusions();

    // Initialize coverage exclusions
    evaluateCoverageExclusions(module);

    sensorsExecutor.execute();

    afterSensors();

    if (hierarchy.isRoot(module)) {
      executeOnRoot();
      postJobsExecutor.execute();
    }
  }

  private void evaluateCoverageExclusions(AbstractProjectOrModule module) {
    if (!Arrays.equals(moduleCoverageExclusions.getCoverageExclusionConfig(), projectCoverageExclusions.getCoverageExclusionConfig())) {
      moduleCoverageExclusions.log();
    }
    for (InputFile inputFile : fs.inputFiles(fs.predicates().all())) {
      boolean excludedByProjectConfiguration = projectCoverageExclusions.isExcluded((DefaultInputFile) inputFile);
      if (excludedByProjectConfiguration) {
        ((DefaultInputFile) inputFile).setExcludedForCoverage(true);
        LOG.debug("File {} excluded for coverage", inputFile);
        continue;
      }
      boolean excludedByModuleConfig = moduleCoverageExclusions.isExcluded((DefaultInputFile) inputFile);
      if (excludedByModuleConfig) {
        ((DefaultInputFile) inputFile).setExcludedForCoverage(true);
        if (Arrays.equals(moduleCoverageExclusions.getCoverageExclusionConfig(), projectCoverageExclusions.getCoverageExclusionConfig())) {
          warnOnce("File '" + inputFile + "' was excluded from coverage because patterns are still evaluated using module relative paths but this is deprecated. " +
            "Please update '" + CoreProperties.PROJECT_COVERAGE_EXCLUSIONS_PROPERTY + "' configuration so that patterns refer to project relative paths");
        } else {
          warnOnce("Defining coverage exclusions at module level is deprecated. " +
            "Move '" + CoreProperties.PROJECT_COVERAGE_EXCLUSIONS_PROPERTY + "' from module '" + module.getName() + "' " +
            "to the root project and update patterns to refer to project relative paths");
        }
        LOG.debug("File {} excluded for coverage", inputFile);
      }
    }

  }

  private void warnOnce(String msg) {
    if (!warnCoverageAlreadyLogged) {
      LOG.warn(msg);
      analysisWarnings.addUnique(msg);
      warnCoverageAlreadyLogged = true;
    }
  }

  protected void afterSensors() {
  }

  protected abstract void executeOnRoot();

  private void initIssueExclusions() {
    if (issueExclusionsLoader.shouldExecute()) {
      for (InputFile inputFile : fs.inputFiles(fs.predicates().all())) {
        issueExclusionsLoader.addMulticriteriaPatterns(((DefaultInputFile) inputFile).getModuleRelativePath(), inputFile.key());
      }
    }
  }
}
