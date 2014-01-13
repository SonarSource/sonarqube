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

package org.sonar.plugins.core.issue.ignore.scanner;

import org.sonar.api.batch.Phase;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;
import org.sonar.api.scan.filesystem.internal.InputFile;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.plugins.core.issue.ignore.pattern.ExclusionPatternInitializer;
import org.sonar.plugins.core.issue.ignore.pattern.InclusionPatternInitializer;

import java.nio.charset.Charset;

@Phase(name = Phase.Name.PRE)
public final class SourceScanner implements Sensor {

  private final RegexpScanner regexpScanner;
  private final ExclusionPatternInitializer exclusionPatternInitializer;
  private final InclusionPatternInitializer inclusionPatternInitializer;
  private final DefaultModuleFileSystem fileSystem;

  public SourceScanner(RegexpScanner regexpScanner, ExclusionPatternInitializer exclusionPatternInitializer, InclusionPatternInitializer inclusionPatternInitializer,
    DefaultModuleFileSystem fileSystem) {
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
  public void analyse(Project project, SensorContext context) {
    parseDirs(project, false);
    parseDirs(project, true);
  }

  protected void parseDirs(Project project, boolean isTest) {
    Charset sourcesEncoding = fileSystem.sourceCharset();

    Iterable<InputFile> files;
    if (isTest) {
      files = fileSystem.inputFiles(FileQuery.onTest().onLanguage(project.getLanguageKey()));
    } else {
      files = fileSystem.inputFiles(FileQuery.onSource().onLanguage(project.getLanguageKey()));
    }

    for (InputFile inputFile : files) {
      try {
        String componentEffectiveKey = inputFile.attribute(DefaultInputFile.ATTRIBUTE_COMPONENT_KEY);
        if (componentEffectiveKey != null) {
          String relativePathFromSource = inputFile.attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH);
          inclusionPatternInitializer.initializePatternsForPath(relativePathFromSource, componentEffectiveKey);
          exclusionPatternInitializer.initializePatternsForPath(relativePathFromSource, componentEffectiveKey);
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
