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
package org.sonar.batch.scan.filesystem;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.batch.bootstrap.TempDirectories;

import java.io.File;
import java.util.List;

/**
 * @since 3.5
 */
public class ModuleFileSystemInitializer implements BatchComponent {

  private File baseDir, workingDir, buildDir;
  private List<File> sourceDirs = Lists.newArrayList();
  private List<File> testDirs = Lists.newArrayList();
  private List<File> binaryDirs = Lists.newArrayList();
  private List<File> additionalSourceFiles;
  private List<File> additionalTestFiles;

  public ModuleFileSystemInitializer(ProjectDefinition module, TempDirectories tempDirectories, PathResolver pathResolver) {
    baseDir = module.getBaseDir();
    buildDir = module.getBuildDir();
    initWorkingDir(module, tempDirectories);
    initBinaryDirs(module, pathResolver);
    initSources(module, pathResolver);
    initTests(module, pathResolver);
  }

  private void initWorkingDir(ProjectDefinition module, TempDirectories tempDirectories) {
    workingDir = module.getWorkDir();
    if (workingDir == null) {
      workingDir = tempDirectories.getDir("work");
    } else {
      try {
        FileUtils.forceMkdir(workingDir);
      } catch (Exception e) {
        throw new IllegalStateException("Fail to create working dir: " + workingDir.getAbsolutePath(), e);
      }
    }
  }

  private void initSources(ProjectDefinition module, PathResolver pathResolver) {
    for (String sourcePath : module.getSourceDirs()) {
      File dir = pathResolver.relativeFile(module.getBaseDir(), sourcePath);
      if (dir.isDirectory() && dir.exists()) {
        sourceDirs.add(dir);
      }
    }
    additionalSourceFiles = pathResolver.relativeFiles(module.getBaseDir(), module.getSourceFiles());
  }

  private void initTests(ProjectDefinition module, PathResolver pathResolver) {
    for (String testPath : module.getTestDirs()) {
      File dir = pathResolver.relativeFile(module.getBaseDir(), testPath);
      if (dir.exists() && dir.isDirectory()) {
        testDirs.add(dir);
      }
    }
    additionalTestFiles = pathResolver.relativeFiles(module.getBaseDir(), module.getTestFiles());
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

  File buildDir() {
    return buildDir;
  }

  List<File> sourceDirs() {
    return sourceDirs;
  }

  List<File> testDirs() {
    return testDirs;
  }

  List<File> binaryDirs() {
    return binaryDirs;
  }

  List<File> additionalSourceFiles() {
    return additionalSourceFiles;
  }

  List<File> additionalTestFiles() {
    return additionalTestFiles;
  }
}
