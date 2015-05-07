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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.util.ProgressReport;

import java.io.File;
import java.nio.file.Path;
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
import java.util.concurrent.TimeUnit;

/**
 * Index input files into {@link InputPathCache}.
 */
@BatchSide
public class FileIndexer {

  private static final Logger LOG = LoggerFactory.getLogger(FileIndexer.class);

  private static final IOFileFilter DIR_FILTER = FileFilterUtils.and(HiddenFileFilter.VISIBLE, FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter(".")));
  private static final IOFileFilter FILE_FILTER = HiddenFileFilter.VISIBLE;

  private final List<InputFileFilter> filters;
  private final boolean isAggregator;
  private final ExclusionFilters exclusionFilters;
  private final InputFileBuilderFactory inputFileBuilderFactory;

  private ProgressReport progressReport;
  private ExecutorService executorService;
  private List<Future<Void>> tasks;

  public FileIndexer(List<InputFileFilter> filters, ExclusionFilters exclusionFilters, InputFileBuilderFactory inputFileBuilderFactory,
    ProjectDefinition def) {
    this.filters = filters;
    this.exclusionFilters = exclusionFilters;
    this.inputFileBuilderFactory = inputFileBuilderFactory;
    this.isAggregator = !def.getSubProjects().isEmpty();
  }

  void index(DefaultModuleFileSystem fileSystem) {
    if (isAggregator) {
      // No indexing for an aggregator module
      return;
    }
    progressReport = new ProgressReport("Report about progress of file indexation", TimeUnit.SECONDS.toMillis(10));
    progressReport.start("Index files");
    exclusionFilters.prepare();

    Progress progress = new Progress();

    InputFileBuilder inputFileBuilder = inputFileBuilderFactory.create(fileSystem);
    executorService = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    tasks = new ArrayList<Future<Void>>();
    indexFiles(fileSystem, progress, inputFileBuilder, fileSystem.sources(), InputFile.Type.MAIN);
    indexFiles(fileSystem, progress, inputFileBuilder, fileSystem.tests(), InputFile.Type.TEST);

    waitForTasksToComplete();

    progressReport.stop(progress.count() + " files indexed");

    if (exclusionFilters.hasPattern()) {
      LOG.info(progress.excludedByPatternsCount() + " files ignored because of inclusion/exclusion patterns");
    }
  }

  private void waitForTasksToComplete() {
    executorService.shutdown();
    for (Future<Void> task : tasks) {
      try {
        task.get();
      } catch (ExecutionException e) {
        // Unwrap ExecutionException
        throw e.getCause() instanceof RuntimeException ? (RuntimeException) e.getCause() : new IllegalStateException(e.getCause());
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
    }
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
    if (inputFile != null) {
      // Set basedir on input file prior to adding it to the FS since exclusions filters may require the absolute path
      inputFile.setModuleBaseDir(fileSystem.baseDirPath());
      if (exclusionFilters.accept(inputFile, type)) {
        indexFile(inputFileBuilder, fileSystem, progress, inputFile, type);
      } else {
        progress.increaseExcludedByPatternsCount();
      }
    }
  }

  private void indexFile(final InputFileBuilder inputFileBuilder, final DefaultModuleFileSystem fs,
    final Progress status, final DeprecatedDefaultInputFile inputFile, final InputFile.Type type) {

    tasks.add(executorService.submit(new Callable<Void>() {
      @Override
      public Void call() {
        DeprecatedDefaultInputFile completedInputFile = inputFileBuilder.completeAndComputeMetadata(inputFile, type);
        if (completedInputFile != null && accept(completedInputFile)) {
          fs.add(completedInputFile);
          status.markAsIndexed(completedInputFile);
          File parentDir = completedInputFile.file().getParentFile();
          String relativePath = new PathResolver().relativePath(fs.baseDir(), parentDir);
          if (relativePath != null) {
            DefaultInputDir inputDir = new DefaultInputDir(fs.moduleKey(), relativePath);
            fs.add(inputDir);
          }
        }
        return null;
      }
    }));

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

  private class Progress {
    private final Set<Path> indexed = new HashSet<>();
    private int excludedByPatternsCount = 0;

    synchronized void markAsIndexed(InputFile inputFile) {
      if (indexed.contains(inputFile.path())) {
        throw MessageException.of("File " + inputFile + " can't be indexed twice. Please check that inclusion/exclusion patterns produce "
          + "disjoint sets for main and test files");
      }
      indexed.add(inputFile.path());
      progressReport.message(indexed.size() + " files indexed...  (last one was " + inputFile.relativePath() + ")");
    }

    void increaseExcludedByPatternsCount() {
      excludedByPatternsCount++;
    }

    public int excludedByPatternsCount() {
      return excludedByPatternsCount;
    }

    int count() {
      return indexed.size();
    }
  }

}
