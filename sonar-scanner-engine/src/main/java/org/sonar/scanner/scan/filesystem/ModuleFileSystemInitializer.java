/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan.filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.TempFolder;

import static org.sonar.scanner.config.DefaultConfiguration.parseAsCsv;

/**
 * @since 3.5
 */
@ScannerSide
public class ModuleFileSystemInitializer {

  private File baseDir;
  private File workingDir;
  private List<File> sourceDirsOrFiles = new ArrayList<>();
  private List<File> testDirsOrFiles = new ArrayList<>();

  public ModuleFileSystemInitializer(ProjectDefinition module, TempFolder tempUtils, PathResolver pathResolver) {
    baseDir = module.getBaseDir();
    initWorkingDir(module, tempUtils);
    initSources(module, pathResolver);
    initTests(module, pathResolver);
  }

  private void initWorkingDir(ProjectDefinition module, TempFolder tempUtils) {
    workingDir = module.getWorkDir();
    if (workingDir == null) {
      workingDir = tempUtils.newDir("work");
    } else {
      try {
        FileUtils.forceMkdir(workingDir);
      } catch (Exception e) {
        throw new IllegalStateException("Fail to create working dir: " + workingDir.getAbsolutePath(), e);
      }
    }
  }

  private void initSources(ProjectDefinition module, PathResolver pathResolver) {
    String srcPropValue = module.properties().get(ProjectDefinition.SOURCES_PROPERTY);
    if (srcPropValue != null) {
      for (String sourcePath : parseAsCsv(ProjectDefinition.SOURCES_PROPERTY, srcPropValue)) {
        File dirOrFile = pathResolver.relativeFile(module.getBaseDir(), sourcePath);
        if (dirOrFile.exists()) {
          sourceDirsOrFiles.add(dirOrFile);
        }
      }
    }
  }

  private void initTests(ProjectDefinition module, PathResolver pathResolver) {
    String testPropValue = module.properties().get(ProjectDefinition.TESTS_PROPERTY);
    if (testPropValue != null) {
      for (String testPath : parseAsCsv(ProjectDefinition.TESTS_PROPERTY, testPropValue)) {
        File dirOrFile = pathResolver.relativeFile(module.getBaseDir(), testPath);
        if (dirOrFile.exists()) {
          testDirsOrFiles.add(dirOrFile);
        }
      }
    }
  }

  File baseDir() {
    return baseDir;
  }

  File workingDir() {
    return workingDir;
  }

  List<File> sources() {
    return sourceDirsOrFiles;
  }

  List<File> tests() {
    return testDirsOrFiles;
  }

}
