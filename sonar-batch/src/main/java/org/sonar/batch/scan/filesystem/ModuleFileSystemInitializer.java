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
package org.sonar.batch.scan.filesystem;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.sonar.api.BatchSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.TempFolder;

import javax.annotation.CheckForNull;

import java.io.File;
import java.util.List;

/**
 * @since 3.5
 */
@BatchSide
public class ModuleFileSystemInitializer {

  private File baseDir, workingDir, buildDir;
  private List<File> sourceDirsOrFiles = Lists.newArrayList();
  private List<File> testDirsOrFiles = Lists.newArrayList();
  private List<File> binaryDirs = Lists.newArrayList();

  public ModuleFileSystemInitializer(ProjectDefinition module, TempFolder tempUtils, PathResolver pathResolver) {
    baseDir = module.getBaseDir();
    buildDir = module.getBuildDir();
    initWorkingDir(module, tempUtils);
    initBinaryDirs(module, pathResolver);
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
    for (String sourcePath : module.sources()) {
      File dirOrFile = pathResolver.relativeFile(module.getBaseDir(), sourcePath);
      if (dirOrFile.exists()) {
        sourceDirsOrFiles.add(dirOrFile);
      }
    }
  }

  private void initTests(ProjectDefinition module, PathResolver pathResolver) {
    for (String testPath : module.tests()) {
      File dirOrFile = pathResolver.relativeFile(module.getBaseDir(), testPath);
      if (dirOrFile.exists()) {
        testDirsOrFiles.add(dirOrFile);
      }
    }
  }

  private void initBinaryDirs(ProjectDefinition module, PathResolver pathResolver) {
    for (String path : module.getBinaries()) {
      File dir = pathResolver.relativeFile(module.getBaseDir(), path);
      binaryDirs.add(dir);
    }
  }

  File baseDir() {
    return baseDir;
  }

  File workingDir() {
    return workingDir;
  }

  @CheckForNull
  File buildDir() {
    return buildDir;
  }

  List<File> sources() {
    return sourceDirsOrFiles;
  }

  List<File> tests() {
    return testDirsOrFiles;
  }

  /**
   * @deprecated since 4.5.1 use SonarQube Java specific API
   */
  @Deprecated
  List<File> binaryDirs() {
    return binaryDirs;
  }
}
