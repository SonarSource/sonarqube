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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.SonarException;

import javax.annotation.CheckForNull;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * This class can't be immutable because of execution of maven plugins that can change the project structure (see MavenPluginHandler and sonar.phase)
 *
 * @since 3.5
 */
public class DefaultModuleFileSystem implements ModuleFileSystem {

  private final String moduleKey;
  private final InputFileCache cache;
  private final FileIndexer indexer;
  private final Settings settings;

  private File baseDir, workingDir, buildDir;
  private List<File> sourceDirs = Lists.newArrayList();
  private List<File> testDirs = Lists.newArrayList();
  private List<File> binaryDirs = Lists.newArrayList();
  private List<File> additionalSourceFiles = Lists.newArrayList();
  private List<File> additionalTestFiles = Lists.newArrayList();

  public DefaultModuleFileSystem(String moduleKey, Settings settings, InputFileCache cache, FileIndexer indexer) {
    this.moduleKey = moduleKey;
    this.settings = settings;
    this.cache = cache;
    this.indexer = indexer;
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

  @Override
  public File workingDir() {
    return workingDir;
  }

  List<File> additionalSourceFiles() {
    return additionalSourceFiles;
  }

  List<File> additionalTestFiles() {
    return additionalTestFiles;
  }

  void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  void setWorkingDir(File workingDir) {
    this.workingDir = workingDir;
  }

  void setBuildDir(File buildDir) {
    this.buildDir = buildDir;
  }

  void addSourceDir(File d) {
    this.sourceDirs.add(d);
  }

  void addTestDir(File d) {
    this.testDirs.add(d);
  }

  void addBinaryDir(File d) {
    this.binaryDirs.add(d);
  }

  void setAdditionalSourceFiles(List<File> files) {
    this.additionalSourceFiles = files;
  }

  void setAdditionalTestFiles(List<File> files) {
    this.additionalTestFiles = files;
  }

  /**
   * @since 4.0
   */
  public Iterable<InputFile> inputFiles(FileQuery query) {
    List<InputFile> result = Lists.newArrayList();

    FileQueryFilter filter = new FileQueryFilter(settings, query);
    for (InputFile input : cache.byModule(moduleKey)) {
      if (filter.accept(input)) {
        result.add(input);
      }
    }
    return result;
  }

  @Override
  public List<File> files(FileQuery query) {
    return InputFile.toFiles(inputFiles(query));
  }

  public void resetDirs(File basedir, File buildDir, List<File> sourceDirs, List<File> testDirs, List<File> binaryDirs) {
    Preconditions.checkNotNull(basedir, "Basedir can't be null");
    this.baseDir = basedir;
    this.buildDir = buildDir;
    this.sourceDirs = existingDirs(sourceDirs);
    this.testDirs = existingDirs(testDirs);
    this.binaryDirs = existingDirs(binaryDirs);
    indexer.index(this);
  }

  void index() {
    indexer.index(this);
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
