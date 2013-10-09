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

import org.apache.commons.io.FileUtils;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FileSystemFilter;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.batch.bootstrap.TempDirectories;

import java.io.File;
import java.util.List;

/**
 * @since 3.5
 */
public class ModuleFileSystemProvider extends ProviderAdapter {

  private PathResolver pathResolver = new PathResolver();
  private DefaultModuleFileSystem singleton;

  public DefaultModuleFileSystem provide(
    ProjectDefinition module, TempDirectories tempDirectories,Settings settings, InputFileCache cache, FileIndexer indexer) {

    if (singleton == null) {
      DefaultModuleFileSystem fs = new DefaultModuleFileSystem(module.getKey(), settings, cache, indexer);
      fs.setBaseDir(module.getBaseDir());
      fs.setBuildDir(module.getBuildDir());
      fs.setWorkingDir(guessWorkingDir(module, tempDirectories));
      initBinaryDirs(module, fs);
      initSources(module, fs);
      initTests(module, fs);
      fs.index();
      singleton = fs;
    }
    return singleton;
  }

  private File guessWorkingDir(ProjectDefinition module, TempDirectories tempDirectories) {
    File workDir = module.getWorkDir();
    if (workDir == null) {
      workDir = tempDirectories.getDir("work");
    } else {
      try {
        FileUtils.forceMkdir(workDir);
      } catch (Exception e) {
        throw new IllegalStateException("Fail to create working dir: " + workDir.getAbsolutePath(), e);
      }
    }
    return workDir;
  }

  private void initSources(ProjectDefinition module, DefaultModuleFileSystem fs) {
    for (String sourcePath : module.getSourceDirs()) {
      File dir = pathResolver.relativeFile(module.getBaseDir(), sourcePath);
      if (dir.isDirectory() && dir.exists()) {
        fs.addSourceDir(dir);
      }
    }
    List<File> sourceFiles = pathResolver.relativeFiles(module.getBaseDir(), module.getSourceFiles());
    fs.setAdditionalSourceFiles(sourceFiles);
  }

  private void initTests(ProjectDefinition module, DefaultModuleFileSystem fs) {
    for (String testPath : module.getTestDirs()) {
      File dir = pathResolver.relativeFile(module.getBaseDir(), testPath);
      if (dir.exists() && dir.isDirectory()) {
        fs.addTestDir(dir);
      }
    }
    List<File> testFiles = pathResolver.relativeFiles(module.getBaseDir(), module.getTestFiles());
    fs.setAdditionalTestFiles(testFiles);
  }

  private void initBinaryDirs(ProjectDefinition module, DefaultModuleFileSystem fs) {
    for (String path : module.getBinaries()) {
      File dir = pathResolver.relativeFile(module.getBaseDir(), path);
      fs.addBinaryDir(dir);
    }
  }
}
