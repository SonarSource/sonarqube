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

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.MessageException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Index input files into {@link InputPathCache}.
 */
public class FileIndexer implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(FileIndexer.class);

  private static final IOFileFilter DIR_FILTER = FileFilterUtils.and(HiddenFileFilter.VISIBLE, FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter(".")));
  private static final IOFileFilter FILE_FILTER = HiddenFileFilter.VISIBLE;

  private final List<InputFileFilter> filters;
  private final InputPathCache fileCache;
  private final boolean isAggregator;
  private final ExclusionFilters exclusionFilters;
  private final InputFileBuilderFactory inputFileBuilderFactory;

  public FileIndexer(List<InputFileFilter> filters, ExclusionFilters exclusionFilters, InputFileBuilderFactory inputFileBuilderFactory,
    InputPathCache cache, ProjectDefinition def) {
    this(filters, exclusionFilters, inputFileBuilderFactory, cache, !def.getSubProjects().isEmpty());
  }

  private FileIndexer(List<InputFileFilter> filters, ExclusionFilters exclusionFilters, InputFileBuilderFactory inputFileBuilderFactory,
    InputPathCache cache, boolean isAggregator) {
    this.filters = filters;
    this.exclusionFilters = exclusionFilters;
    this.inputFileBuilderFactory = inputFileBuilderFactory;
    this.fileCache = cache;
    this.isAggregator = isAggregator;
  }

  void index(DefaultModuleFileSystem fileSystem) {
    if (isAggregator) {
      // No indexing for an aggregator module
      return;
    }
    LOG.info("Index files");
    exclusionFilters.prepare();

    Progress progress = new Progress(fileCache.filesByModule(fileSystem.moduleKey()), fileCache.dirsByModule(fileSystem.moduleKey()));

    InputFileBuilder inputFileBuilder = inputFileBuilderFactory.create(fileSystem);
    indexFiles(fileSystem, progress, inputFileBuilder, fileSystem.sources(), InputFile.Type.MAIN);
    indexFiles(fileSystem, progress, inputFileBuilder, fileSystem.tests(), InputFile.Type.TEST);

    indexAllConcurrently(progress);

    // Populate FS in a synchronous way because PersistIt Exchange is not concurrent
    for (InputFile indexed : progress.indexed) {
      fileSystem.add(indexed);
    }
    for (InputDir indexed : progress.indexedDir) {
      fileSystem.add(indexed);
    }

    // Remove paths that have been removed since previous indexation
    for (InputFile removed : progress.removed) {
      fileCache.remove(fileSystem.moduleKey(), removed);
    }
    for (InputDir removed : progress.removedDir) {
      fileCache.remove(fileSystem.moduleKey(), removed);
    }

    LOG.info(String.format("%d files indexed", progress.count()));

  }

  private void indexAllConcurrently(Progress progress) {
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    try {
      List<Future<Void>> all = executor.invokeAll(progress.indexingTasks);
      for (Future<Void> future : all) {
        future.get();
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException("FileIndexer was interrupted", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw new IllegalStateException("Error during file indexing", e);
      }
    }
    executor.shutdown();
  }

  private void indexFiles(DefaultModuleFileSystem fileSystem, Progress progress, InputFileBuilder inputFileBuilder, List<File> sources, InputFile.Type type) {
    for (File dirOrFile : sources) {
      if (dirOrFile.isDirectory()) {
        indexDirectory(inputFileBuilder, fileSystem, progress, dirOrFile, type);
      } else {
        indexFile(inputFileBuilder, fileSystem, progress, dirOrFile, type);
      }
    }
  }

  private void indexDirectory(InputFileBuilder inputFileBuilder, DefaultModuleFileSystem fileSystem, Progress status, File dirToIndex, InputFile.Type type) {
    Collection<File> files = FileUtils.listFiles(dirToIndex, FILE_FILTER, DIR_FILTER);
    for (File file : files) {
      indexFile(inputFileBuilder, fileSystem, status, file, type);
    }
  }

  private void indexFile(InputFileBuilder inputFileBuilder, DefaultModuleFileSystem fileSystem, Progress progress, File sourceFile, InputFile.Type type) {
    DeprecatedDefaultInputFile inputFile = inputFileBuilder.create(sourceFile);
    if (inputFile != null && exclusionFilters.accept(inputFile, type)) {
      indexFile(inputFileBuilder, fileSystem, progress, inputFile, type);
    }
  }

  private void indexFile(final InputFileBuilder inputFileBuilder, final DefaultModuleFileSystem fs,
    final Progress status, final DeprecatedDefaultInputFile inputFile, final InputFile.Type type) {

    Callable<Void> task = new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        InputFile completedFile = inputFileBuilder.complete(inputFile, type);
        if (completedFile != null && accept(completedFile)) {
          status.markAsIndexed(inputFile);
          File parentDir = inputFile.file().getParentFile();
          String relativePath = new PathResolver().relativePath(fs.baseDir(), parentDir);
          if (relativePath != null) {
            DefaultInputDir inputDir = new DefaultInputDir(relativePath);
            inputDir.setFile(parentDir);
            status.markAsIndexed(inputDir);
          }
        }
        return null;
      }
    };
    status.planForIndexing(task);
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

  private static class Progress {
    private final Set<InputFile> removed;
    private final Set<InputDir> removedDir;
    private final Set<InputFile> indexed;
    private final Set<InputDir> indexedDir;
    private final List<Callable<Void>> indexingTasks;

    Progress(Iterable<InputFile> removed, Iterable<InputDir> removedDir) {
      this.removed = Sets.newHashSet(removed);
      this.removedDir = Sets.newHashSet(removedDir);
      this.indexed = new HashSet<InputFile>();
      this.indexedDir = new HashSet<InputDir>();
      this.indexingTasks = new ArrayList<Callable<Void>>();
    }

    void planForIndexing(Callable<Void> indexingTask) {
      this.indexingTasks.add(indexingTask);
    }

    synchronized void markAsIndexed(InputFile inputFile) {
      if (indexed.contains(inputFile)) {
        throw MessageException.of("File " + inputFile + " can't be indexed twice. Please check that inclusion/exclusion patterns produce "
          + "disjoint sets for main and test files");
      }
      removed.remove(inputFile);
      indexed.add(inputFile);
    }

    synchronized void markAsIndexed(InputDir inputDir) {
      removedDir.remove(inputDir);
      indexedDir.add(inputDir);
    }

    int count() {
      return indexed.size();
    }
  }

}
