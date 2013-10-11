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
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.SonarException;
import org.sonar.core.component.ComponentKeys;
import org.sonar.plugins.core.issue.ignore.pattern.ExclusionPatternInitializer;
import org.sonar.plugins.core.issue.ignore.pattern.InclusionPatternInitializer;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

@Phase(name = Phase.Name.PRE)
public final class SourceScanner implements Sensor {

  private final RegexpScanner regexpScanner;
  private final ExclusionPatternInitializer exclusionPatternInitializer;
  private final InclusionPatternInitializer inclusionPatternInitializer;
  private final ModuleFileSystem fileSystem;
  private final PathResolver pathResolver;

  public SourceScanner(RegexpScanner regexpScanner, ExclusionPatternInitializer exclusionPatternInitializer, InclusionPatternInitializer inclusionPatternInitializer,
      ModuleFileSystem fileSystem) {
    this.regexpScanner = regexpScanner;
    this.exclusionPatternInitializer = exclusionPatternInitializer;
    this.inclusionPatternInitializer = inclusionPatternInitializer;
    this.fileSystem = fileSystem;
    this.pathResolver = new PathResolver();
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

    // TODO use InputFile
    List<File> files;
    List<File> dirs;
    if (isTest) {
      files = fileSystem.files(FileQuery.onTest().onLanguage(project.getLanguageKey()));
      dirs = fileSystem.testDirs();
    } else {
      files = fileSystem.files(FileQuery.onSource().onLanguage(project.getLanguageKey()));
      dirs = fileSystem.sourceDirs();
    }

    for (File inputFile : files) {
      try {
        // TODO reuse InputFile.attribute(DefaultInputFile.COMPONENT_KEY ?
        String componentKey = resolveComponent(inputFile, dirs, project, isTest);
        if (componentKey != null) {

          String relativePath = pathResolver.relativePath(dirs, inputFile).path();
          inclusionPatternInitializer.initializePatternsForPath(relativePath, componentKey);
          exclusionPatternInitializer.initializePatternsForPath(relativePath, componentKey);
          if (exclusionPatternInitializer.hasFileContentPattern()) {
            regexpScanner.scan(componentKey, inputFile, sourcesEncoding);
          }
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
  private String resolveComponent(File inputFile, List<File> sourceDirs, Project project, boolean isTest) {
    Resource resource;

    if (Java.KEY.equals(project.getLanguageKey()) && Java.isJavaFile(inputFile)) {

      resource = JavaFile.fromIOFile(inputFile, sourceDirs, isTest);
    } else {
      resource = new org.sonar.api.resources.File(pathResolver.relativePath(sourceDirs, inputFile).path());
    }

    if (resource == null) {
      return null;
    } else {
      return ComponentKeys.createKey(project, resource);
    }
  }

  @Override
  public String toString() {
    return "Issues Exclusions - Source Scanner";
  }

}
