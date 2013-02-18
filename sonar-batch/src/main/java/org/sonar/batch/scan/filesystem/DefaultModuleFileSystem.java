/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.filesystem;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.FileSystemFilter;
import org.sonar.api.scan.filesystem.FileType;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

/**
 * This class can't be immutable because of execution of maven plugins that can change the project structure (see MavenPluginHandler and sonar.phase)
 *
 * @since 3.5
 */
public class DefaultModuleFileSystem implements ModuleFileSystem {

  private static final IOFileFilter DIR_FILTER = FileFilterUtils.and(HiddenFileFilter.VISIBLE, FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter(".")));

  private Settings settings;
  private File baseDir, workingDir, buildDir;
  private List<File> sourceDirs = Lists.newArrayList();
  private List<File> testDirs = Lists.newArrayList();
  private List<File> binaryDirs = Lists.newArrayList();
  private PathResolver pathResolver = new PathResolver();
  private List<FileSystemFilter> fsFilters = Lists.newArrayList();
  private LanguageFilters languageFilters;

  DefaultModuleFileSystem() {
  }

  public File baseDir() {
    return baseDir;
  }

  public File buildDir() {
    return buildDir;
  }

  public List<File> sourceDirs() {
    return sourceDirs;
  }

  public List<File> testDirs() {
    return testDirs;
  }

  public List<File> binaryDirs() {
    return binaryDirs;
  }

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

  public File workingDir() {
    return workingDir;
  }

  List<FileSystemFilter> filters() {
    return fsFilters;
  }

  LanguageFilters languageFilters() {
    return languageFilters;
  }

  public List<File> files(FileQuery query) {
    List<FileSystemFilter> filters = Lists.newArrayList(fsFilters);
    for (FileFilter fileFilter : query.filters()) {
      filters.add(new FileFilterWrapper(fileFilter));
    }
    for (String language : query.languages()) {
      filters.add(new FileFilterWrapper(languageFilters.forLang(language)));
    }
    for (String inclusion : query.inclusions()) {
      filters.add(new InclusionFilter(inclusion));
    }
    for (String exclusion : query.exclusions()) {
      filters.add(new ExclusionFilter(exclusion));
    }
    List<File> result = Lists.newLinkedList();
    FileFilterContext context = new FileFilterContext(this);
    for (FileType type : query.types()) {
      context.setType(type);
      switch (type) {
        case SOURCE:
          applyFilters(result, context, filters, sourceDirs);
          break;
        case TEST:
          applyFilters(result, context, filters, testDirs);
          break;
      }
    }
    return result;
  }

  private void applyFilters(List<File> result, FileFilterContext context,
                            Collection<FileSystemFilter> filters, Collection<File> dirs) {
    for (File dir : dirs) {
      if (dir.exists()) {
        context.setRelativeDir(dir);
        Collection<File> files = FileUtils.listFiles(dir, HiddenFileFilter.VISIBLE, DIR_FILTER);
        for (File file : files) {
          if (accept(file, context, filters)) {
            result.add(file);
          }
        }
      }
    }
  }

  private boolean accept(File file, FileFilterContext context, Collection<FileSystemFilter> filters) {
    context.setRelativePath(pathResolver.relativePath(context.relativeDir(), file));
    try {
      context.setCanonicalPath(file.getCanonicalPath());
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get the canonical path of: " + file);
    }
    for (FileSystemFilter filter : filters) {
      if (!filter.accept(file, context)) {
        return false;
      }
    }
    return true;
  }

  public void resetDirs(File basedir, File buildDir, List<File> sourceDirs, List<File> testDirs, List<File> binaryDirs) {
    Preconditions.checkNotNull(basedir, "Basedir can't be null");
    this.baseDir = basedir;
    this.buildDir = buildDir;
    this.sourceDirs = existingDirs(sourceDirs);
    this.testDirs = existingDirs(testDirs);
    this.binaryDirs = existingDirs(binaryDirs);
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

  DefaultModuleFileSystem setSettings(Settings settings) {
    this.settings = settings;
    return this;
  }

  DefaultModuleFileSystem setBaseDir(File baseDir) {
    this.baseDir = baseDir;
    return this;
  }

  DefaultModuleFileSystem setWorkingDir(File workingDir) {
    this.workingDir = workingDir;
    return this;
  }

  DefaultModuleFileSystem setBuildDir(File buildDir) {
    this.buildDir = buildDir;
    return this;
  }

  DefaultModuleFileSystem addFilters(FileSystemFilter... f) {
    for (FileSystemFilter filter : f) {
      this.fsFilters.add(filter);
    }
    return this;
  }

  DefaultModuleFileSystem setLanguageFilters(LanguageFilters languageFilters) {
    this.languageFilters = languageFilters;
    return this;
  }

  DefaultModuleFileSystem addSourceDir(File d) {
    this.sourceDirs.add(d);
    return this;
  }

  DefaultModuleFileSystem addTestDir(File d) {
    this.testDirs.add(d);
    return this;
  }

  DefaultModuleFileSystem addBinaryDir(File d) {
    this.binaryDirs.add(d);
    return this;
  }
}
