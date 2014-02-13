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
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.InputFileFilter;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Index input files into {@link InputFileCache}.
 */
public class FileIndex implements BatchComponent {

  private static final String FILE_IS_NOT_DECLARED_IN_MODULE_BASEDIR = "File '%s' is not declared in module basedir %s";

  private static class Progress {
    private final Set<String> removedPaths;
    private final Set<String> indexed;

    Progress(Set<String> removedPaths) {
      this.removedPaths = removedPaths;
      this.indexed = new HashSet<String>();
    }

    void markAsIndexed(String relativePath) {
      if (indexed.contains(relativePath)) {
        throw new SonarException("File " + relativePath + " can't be indexed twice. Please check that inclusion/exclusion patterns produce "
          + "disjoint sets for main and test files");
      }
      removedPaths.remove(relativePath);
      indexed.add(relativePath);
    }

    int count() {
      return indexed.size();
    }
  }

  private static final IOFileFilter DIR_FILTER = FileFilterUtils.and(HiddenFileFilter.VISIBLE, FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter(".")));
  private static final IOFileFilter FILE_FILTER = HiddenFileFilter.VISIBLE;

  private final PathResolver pathResolver;
  private final List<InputFileFilter> filters;
  private final InputFileCache fileCache;
  private final Project module;
  private final ExclusionFilters exclusionFilters;
  private final InputFileBuilderFactory inputFileBuilderFactory;

  public FileIndex(List<InputFileFilter> filters, ExclusionFilters exclusionFilters, InputFileBuilderFactory inputFileBuilderFactory,
    InputFileCache cache, PathResolver pathResolver, Project project) {
    this.filters = filters;
    this.exclusionFilters = exclusionFilters;
    this.inputFileBuilderFactory = inputFileBuilderFactory;
    this.fileCache = cache;
    this.pathResolver = pathResolver;
    this.module = project;
  }

  void index(DefaultModuleFileSystem fileSystem) {
    Logger logger = LoggerFactory.getLogger(FileIndex.class);
    if (!module.getModules().isEmpty()) {
      // No indexing for an aggregator module
      return;
    }
    logger.info("Index files");
    exclusionFilters.prepare(fileSystem);
    // TODO log configuration too (replace FileSystemLogger)

    Progress progress = new Progress(fileCache.fileRelativePaths(fileSystem.moduleKey()));

    InputFileBuilder inputFileBuilder = inputFileBuilderFactory.create(fileSystem);
    if (!fileSystem.sourceFiles().isEmpty() || !fileSystem.testFiles().isEmpty()) {
      // Index only provided files
      indexFiles(inputFileBuilder, fileSystem, progress, fileSystem.sourceFiles(), InputFile.TYPE_MAIN);
      indexFiles(inputFileBuilder, fileSystem, progress, fileSystem.testFiles(), InputFile.TYPE_TEST);
    } else if (fileSystem.baseDir() != null) {
      // index from basedir
      indexDirectory(inputFileBuilder, fileSystem, progress, fileSystem.baseDir());
    }

    // Remove files that have been removed since previous indexation
    for (String path : progress.removedPaths) {
      fileCache.remove(fileSystem.moduleKey(), path);
    }

    logger.info(String.format("%d files indexed", progress.count()));

  }

  private void indexFiles(InputFileBuilder inputFileBuilder, DefaultModuleFileSystem fileSystem, Progress progress, List<File> sourceFiles, String type) {
    for (File sourceFile : sourceFiles) {
      String path = pathResolver.relativePath(fileSystem.baseDir(), sourceFile);
      if (path == null) {
        LoggerFactory.getLogger(getClass()).warn(String.format(
          FILE_IS_NOT_DECLARED_IN_MODULE_BASEDIR, sourceFile.getAbsoluteFile(), fileSystem.baseDir()
          ));
      } else {
        if (exclusionFilters.accept(sourceFile, path, type)) {
          indexFile(inputFileBuilder, fileSystem, progress, sourceFile, path, type);
        }
      }
    }
  }

  Iterable<InputFile> inputFiles(String moduleKey) {
    return fileCache.byModule(moduleKey);
  }

  InputFile inputFile(DefaultModuleFileSystem fileSystem, File ioFile) {
    String path = computeFilePath(fileSystem, ioFile);
    return fileCache.byPath(fileSystem.moduleKey(), path);
  }

  private void indexDirectory(InputFileBuilder inputFileBuilder, DefaultModuleFileSystem fileSystem, Progress status, File dirToIndex) {
    Collection<File> files = FileUtils.listFiles(dirToIndex, FILE_FILTER, DIR_FILTER);
    for (File sourceFile : files) {
      String path = pathResolver.relativePath(fileSystem.baseDir(), sourceFile);
      if (path == null) {
        LoggerFactory.getLogger(getClass()).warn(String.format(
          FILE_IS_NOT_DECLARED_IN_MODULE_BASEDIR, sourceFile.getAbsoluteFile(), fileSystem.baseDir()
          ));
      } else {
        if (exclusionFilters.accept(sourceFile, path, InputFile.TYPE_MAIN)) {
          indexFile(inputFileBuilder, fileSystem, status, sourceFile, path, InputFile.TYPE_MAIN);
        }
        if (exclusionFilters.accept(sourceFile, path, InputFile.TYPE_TEST)) {
          indexFile(inputFileBuilder, fileSystem, status, sourceFile, path, InputFile.TYPE_TEST);
        }
      }
    }
  }

  private void indexFile(InputFileBuilder inputFileBuilder, DefaultModuleFileSystem fs, Progress status, File file, String path, String type) {
    InputFile inputFile = inputFileBuilder.create(file, type);
    if (inputFile != null && accept(inputFile)) {
      fileCache.put(fs.moduleKey(), inputFile);
      status.markAsIndexed(path);
    }
  }

  private String computeFilePath(DefaultModuleFileSystem fileSystem, File file) {
    return pathResolver.relativePath(fileSystem.baseDir(), file);
  }

  private boolean accept(InputFile inputFile) {
    // InputFileFilter extensions
    for (InputFileFilter filter : filters) {
      if (!filter.accept(inputFile)) {
        return false;
      }
    }
    return true;
  }
}
