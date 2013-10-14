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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.InputFiles;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.batch.bootstrap.AnalysisMode;

import javax.annotation.CheckForNull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * This class can't be immutable because of execution of maven plugins that can change the project structure (see MavenPluginHandler and sonar.phase)
 *
 * @since 3.5
 */
public class DefaultModuleFileSystem implements ModuleFileSystem, Startable {

  private final String moduleKey;
  private final FileIndex index;
  private final Settings settings;

  private File baseDir, workingDir, buildDir;
  private List<File> sourceDirs = Lists.newArrayList();
  private List<File> testDirs = Lists.newArrayList();
  private List<File> binaryDirs = Lists.newArrayList();
  private List<File> sourceFiles = Lists.newArrayList();
  private List<File> testFiles = Lists.newArrayList();
  private AnalysisMode analysisMode;

  public DefaultModuleFileSystem(ProjectDefinition module, Settings settings, FileIndex index, ModuleFileSystemInitializer initializer, AnalysisMode analysisMode) {
    this(module.getKey(), settings, index, initializer, analysisMode);
  }

  @VisibleForTesting
  DefaultModuleFileSystem(String moduleKey, Settings settings, FileIndex index, ModuleFileSystemInitializer initializer, AnalysisMode analysisMode) {
    this.moduleKey = moduleKey;
    this.settings = settings;
    this.index = index;
    this.analysisMode = analysisMode;
    this.baseDir = initializer.baseDir();
    this.workingDir = initializer.workingDir();
    this.buildDir = initializer.buildDir();
    this.sourceDirs = initializer.sourceDirs();
    this.testDirs = initializer.testDirs();
    this.binaryDirs = initializer.binaryDirs();
    this.sourceFiles = initializer.additionalSourceFiles();
    this.testFiles = initializer.additionalTestFiles();
  }

  @Override
  public String moduleKey() {
    return moduleKey;
  }

  @Override
  public File baseDir() {
    return baseDir;
  }

  @Override
  @CheckForNull
  public File buildDir() {
    return buildDir;
  }

  @Override
  public List<File> sourceDirs() {
    return sourceDirs;
  }

  @Override
  public List<File> testDirs() {
    return testDirs;
  }

  @Override
  public List<File> binaryDirs() {
    return binaryDirs;
  }

  @Override
  public File workingDir() {
    return workingDir;
  }

  List<File> sourceFiles() {
    return sourceFiles;
  }

  List<File> testFiles() {
    return testFiles;
  }

  @Override
  public Charset sourceCharset() {
    final Charset charset;
    String encoding = settings.getString(CoreProperties.ENCODING_PROPERTY);
    if (StringUtils.isNotEmpty(encoding)) {
      charset = Charset.forName(StringUtils.trim(encoding));
    } else {
      charset = Charset.defaultCharset();
    }
    return charset;
  }

  boolean isDefaultSourceCharset() {
    return !settings.hasKey(CoreProperties.ENCODING_PROPERTY);
  }

  /**
   * @since 4.0
   */
  @Override
  public Iterable<InputFile> inputFiles(FileQuery query) {
    List<InputFile> result = Lists.newArrayList();
    FileQueryFilter filter = new FileQueryFilter(analysisMode, query);
    for (InputFile input : index.inputFiles(moduleKey)) {
      if (filter.accept(input)) {
        result.add(input);
      }
    }
    return result;
  }

  @Override
  public List<File> files(FileQuery query) {
    return InputFiles.toFiles(inputFiles(query));
  }

  @Override
  public void start() {
    index();
  }

  @Override
  public void stop() {
    // nothing to do
  }

  public void resetDirs(File basedir, File buildDir, List<File> sourceDirs, List<File> testDirs, List<File> binaryDirs) {
    Preconditions.checkNotNull(basedir, "Basedir can't be null");
    this.baseDir = basedir;
    this.buildDir = buildDir;
    this.sourceDirs = existingDirs(sourceDirs);
    this.testDirs = existingDirs(testDirs);
    this.binaryDirs = existingDirs(binaryDirs);
    index();
  }

  public void index() {
    index.index(this);
  }

  private List<File> existingDirs(List<File> dirs) {
    ImmutableList.Builder<File> builder = ImmutableList.builder();
    for (File dir : dirs) {
      if (dir.exists() && dir.isDirectory()) {
        builder.add(dir);
      }
    }
    return builder.build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultModuleFileSystem that = (DefaultModuleFileSystem) o;
    return moduleKey.equals(that.moduleKey);
  }

  @Override
  public int hashCode() {
    return moduleKey.hashCode();
  }
}
