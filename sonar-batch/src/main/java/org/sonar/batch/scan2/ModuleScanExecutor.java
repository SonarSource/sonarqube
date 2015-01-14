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

import com.google.common.collect.Lists;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.batch.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.batch.rule.QProfileVerifier;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.batch.scan.filesystem.FileSystemLogger;

import java.util.Collection;

public final class ModuleScanExecutor {

  private final SensorsExecutor analyzersExecutor;
  private final SensorContext analyzerContext;
  private final FileSystemLogger fsLogger;
  private final DefaultModuleFileSystem fs;
  private final QProfileVerifier profileVerifier;
  private final IssueExclusionsLoader issueExclusionsLoader;

  public ModuleScanExecutor(SensorsExecutor analyzersExecutor,
    SensorContext analyzerContext,
    FileSystemLogger fsLogger, DefaultModuleFileSystem fs, QProfileVerifier profileVerifier,
    IssueExclusionsLoader issueExclusionsLoader) {
    this.analyzersExecutor = analyzersExecutor;
    this.analyzerContext = analyzerContext;
    this.fsLogger = fsLogger;
    this.fs = fs;
    this.profileVerifier = profileVerifier;
    this.issueExclusionsLoader = issueExclusionsLoader;
  }

  public static Collection<Class> getPhaseClasses() {
    return Lists.<Class>newArrayList(SensorsExecutor.class);
  }

  /**
   * Executed on each module
   */
  public void execute() {
    fsLogger.log();

    // Index and lock the filesystem
    fs.index();

    // Log detected languages and their profiles after FS is indexed and languages detected
    profileVerifier.execute();

    // Initialize issue exclusions
    issueExclusionsLoader.execute();

    analyzersExecutor.execute(analyzerContext);

  }
}
