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

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.scanner.rule.QProfileVerifier;
import org.sonar.scanner.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.scanner.scan.filesystem.FileIndexer;

public abstract class AbstractPhaseExecutor {

  private static final Logger LOG = Loggers.get(AbstractPhaseExecutor.class);

  private final PostJobsExecutor postJobsExecutor;
  private final SensorsExecutor sensorsExecutor;
  private final DefaultModuleFileSystem fs;
  private final QProfileVerifier profileVerifier;
  private final IssueExclusionsLoader issueExclusionsLoader;
  private final InputModuleHierarchy hierarchy;
  private final FileIndexer fileIndexer;
  private final CoverageExclusions coverageExclusions;

  public AbstractPhaseExecutor(PostJobsExecutor postJobsExecutor, SensorsExecutor sensorsExecutor, InputModuleHierarchy hierarchy, DefaultModuleFileSystem fs,
    QProfileVerifier profileVerifier, IssueExclusionsLoader issueExclusionsLoader, FileIndexer fileIndexer, CoverageExclusions coverageExclusions) {
    this.postJobsExecutor = postJobsExecutor;
    this.sensorsExecutor = sensorsExecutor;
    this.fs = fs;
    this.profileVerifier = profileVerifier;
    this.issueExclusionsLoader = issueExclusionsLoader;
    this.hierarchy = hierarchy;
    this.fileIndexer = fileIndexer;
    this.coverageExclusions = coverageExclusions;
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
    initCoverageExclusions();

    sensorsExecutor.execute();

    afterSensors();

    if (hierarchy.isRoot(module)) {
      executeOnRoot();
      postJobsExecutor.execute();
    }
  }

  private void initCoverageExclusions() {
    if (coverageExclusions.shouldExecute()) {
      coverageExclusions.log();

      for (InputFile inputFile : fs.inputFiles(fs.predicates().all())) {
        boolean excluded = coverageExclusions.isExcluded((DefaultInputFile) inputFile);
        if (excluded) {
          ((DefaultInputFile) inputFile).setExcludedForCoverage(true);
          LOG.debug("File {} excluded for coverage", inputFile);
        }
      }

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
