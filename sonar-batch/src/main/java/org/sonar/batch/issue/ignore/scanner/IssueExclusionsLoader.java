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

package org.sonar.batch.issue.ignore.scanner;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.batch.issue.ignore.pattern.IssueInclusionPatternInitializer;

import java.nio.charset.Charset;

public final class IssueExclusionsLoader {

  private final IssueExclusionsRegexpScanner regexpScanner;
  private final IssueExclusionPatternInitializer exclusionPatternInitializer;
  private final IssueInclusionPatternInitializer inclusionPatternInitializer;
  private final FileSystem fileSystem;

  public IssueExclusionsLoader(IssueExclusionsRegexpScanner regexpScanner, IssueExclusionPatternInitializer exclusionPatternInitializer,
    IssueInclusionPatternInitializer inclusionPatternInitializer,
    FileSystem fileSystem) {
    this.regexpScanner = regexpScanner;
    this.exclusionPatternInitializer = exclusionPatternInitializer;
    this.inclusionPatternInitializer = inclusionPatternInitializer;
    this.fileSystem = fileSystem;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return inclusionPatternInitializer.hasConfiguredPatterns()
      || exclusionPatternInitializer.hasConfiguredPatterns();
  }

  /**
   * {@inheritDoc}
   */
  public void execute() {
    Charset sourcesEncoding = fileSystem.encoding();

    for (InputFile inputFile : fileSystem.inputFiles(fileSystem.predicates().all())) {
      try {
        String componentEffectiveKey = ((DeprecatedDefaultInputFile) inputFile).key();
        if (componentEffectiveKey != null) {
          String path = inputFile.relativePath();
          inclusionPatternInitializer.initializePatternsForPath(path, componentEffectiveKey);
          exclusionPatternInitializer.initializePatternsForPath(path, componentEffectiveKey);
          if (exclusionPatternInitializer.hasFileContentPattern()) {
            regexpScanner.scan(componentEffectiveKey, inputFile.file(), sourcesEncoding);
          }
        }
      } catch (Exception e) {
        throw new SonarException("Unable to read the source file : '" + inputFile.absolutePath() + "' with the charset : '"
          + sourcesEncoding.name() + "'.", e);
      }
    }
  }

  @Override
  public String toString() {
    return "Issues Exclusions - Source Scanner";
  }

}
