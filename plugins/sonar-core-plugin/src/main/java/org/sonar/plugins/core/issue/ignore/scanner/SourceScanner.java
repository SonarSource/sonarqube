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
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.core.issue.ignore.pattern.PatternsInitializer;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

@Phase(name = Phase.Name.PRE)
public final class SourceScanner implements Sensor {

  private final RegexpScanner regexpScanner;
  private final PatternsInitializer patternsInitializer;
  private final ModuleFileSystem fileSystem;

  public SourceScanner(RegexpScanner regexpScanner, PatternsInitializer patternsInitializer, ModuleFileSystem fileSystem) {
    this.regexpScanner = regexpScanner;
    this.patternsInitializer = patternsInitializer;
    this.fileSystem = fileSystem;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return patternsInitializer.getAllFilePatterns().size() > 0 || patternsInitializer.getBlockPatterns().size() > 0;
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

    List<File> files;
    if (isTest) {
      files = fileSystem.files(FileQuery.onTest().onLanguage(project.getLanguageKey()));
    } else {
      files = fileSystem.files(FileQuery.onSource().onLanguage(project.getLanguageKey()));
    }

    for (File inputFile : files) {
      try {
        String resource = defineResource(inputFile, fileSystem, project, isTest);
        if (resource != null) {
          regexpScanner.scan(resource, inputFile, sourcesEncoding);
        }
      } catch (Exception e) {
        throw new SonarException("Unable to read the source file : '" + inputFile.getAbsolutePath() + "' with the charset : '"
          + sourcesEncoding.name() + "'.", e);
      }
    }
  }

  /*
   * This method is necessary because Java resources are not treated as every other resource...
   */
  private String defineResource(File inputFile, ModuleFileSystem fileSystem, Project project, boolean isTest) {
    if (Java.KEY.equals(project.getLanguageKey()) && Java.isJavaFile(inputFile)) {
      List<File> sourceDirs = null;
      if (isTest) {
        sourceDirs = fileSystem.testDirs();
      } else {
        sourceDirs = fileSystem.sourceDirs();
      }
      JavaFile file = JavaFile.fromIOFile(inputFile, sourceDirs, isTest);
      if (file == null) {
        return null;
      } else {
        return file.getKey();
      }
    }
    return inputFile.getPath();
  }

  @Override
  public String toString() {
    return "Ignore Issues - Source Scanner";
  }

}
