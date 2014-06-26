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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.utils.MessageException;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Index input files into {@link InputFileCache}.
 */
public class FileIndexer implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(FileIndexer.class);

  private static final IOFileFilter DIR_FILTER = FileFilterUtils.and(HiddenFileFilter.VISIBLE, FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter(".")));
  private static final IOFileFilter FILE_FILTER = HiddenFileFilter.VISIBLE;

  private final List<InputFileFilter> filters;
  private final InputFileCache fileCache;
  private final boolean isAggregator;
  private final ExclusionFilters exclusionFilters;
  private final InputFileBuilderFactory inputFileBuilderFactory;

  private ExecutorService executor;

  public FileIndexer(List<InputFileFilter> filters, ExclusionFilters exclusionFilters, InputFileBuilderFactory inputFileBuilderFactory,
    InputFileCache cache, ProjectDefinition def) {
    this(filters, exclusionFilters, inputFileBuilderFactory, cache, !def.getSubProjects().isEmpty());
  }

  private FileIndexer(List<InputFileFilter> filters, ExclusionFilters exclusionFilters, InputFileBuilderFactory inputFileBuilderFactory,
    InputFileCache cache, boolean isAggregator) {
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

    Progress progress = new Progress(fileCache.byModule(fileSystem.moduleKey()));

    executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

    InputFileBuilder inputFileBuilder = inputFileBuilderFactory.create(fileSystem);
    if (!fileSystem.sourceFiles().isEmpty() || !fileSystem.testFiles().isEmpty()) {
      // Index only provided files
      indexFiles(inputFileBuilder, fileSystem, progress, fileSystem.sourceFiles(), InputFile.Type.MAIN);
      indexFiles(inputFileBuilder, fileSystem, progress, fileSystem.testFiles(), InputFile.Type.TEST);
    } else {
      for (File mainDir : fileSystem.sourceDirs()) {
        indexDirectory(inputFileBuilder, fileSystem, progress, mainDir, InputFile.Type.MAIN);
      }
      for (File testDir : fileSystem.testDirs()) {
        indexDirectory(inputFileBuilder, fileSystem, progress, testDir, InputFile.Type.TEST);
      }

    }

    executor.shutdown();
    try {
      executor.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new IllegalStateException("FileIndexer was interrupted", e);
    }

    // Populate FS in a synchronous way because PersistIt Exchange is not concurrent
    for (InputFile indexed : progress.indexed) {
      fileSystem.add(indexed);
    }

    // Remove files that have been removed since previous indexation
    for (InputFile removed : progress.removed) {
      fileCache.remove(fileSystem.moduleKey(), removed);
    }

    LOG.info(String.format("%d files indexed", progress.count()));

  }

  private void indexFiles(InputFileBuilder inputFileBuilder, DefaultModuleFileSystem fileSystem, Progress progress, List<File> sourceFiles, InputFile.Type type) {
    for (File sourceFile : sourceFiles) {
      DeprecatedDefaultInputFile inputFile = inputFileBuilder.create(sourceFile);
      if (inputFile != null && exclusionFilters.accept(inputFile, type)) {
        indexFile(inputFileBuilder, fileSystem, progress, inputFile, type);
      }
    }
  }

  private void indexDirectory(InputFileBuilder inputFileBuilder, DefaultModuleFileSystem fileSystem, Progress status, File dirToIndex, InputFile.Type type) {
    Collection<File> files = FileUtils.listFiles(dirToIndex, FILE_FILTER, DIR_FILTER);
    for (File file : files) {
      DeprecatedDefaultInputFile inputFile = inputFileBuilder.create(file);
      if (inputFile != null && exclusionFilters.accept(inputFile, type)) {
        indexFile(inputFileBuilder, fileSystem, status, inputFile, type);
      }
    }
  }

  private void indexFile(final InputFileBuilder inputFileBuilder, final DefaultModuleFileSystem fs,
    final Progress status, final DeprecatedDefaultInputFile inputFile, final InputFile.Type type) {

    Runnable worker = new Runnable() {

      @Override
      public void run() {
        InputFile completedFile = inputFileBuilder.complete(inputFile, type);
        if (completedFile != null && accept(completedFile)) {
          status.markAsIndexed(completedFile);
        }
      }
    };
    executor.execute(worker);

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
    private final Set<InputFile> indexed;

    Progress(Iterable<InputFile> removed) {
      this.removed = Sets.newHashSet(removed);
      this.indexed = new HashSet<InputFile>();
    }

    synchronized void markAsIndexed(InputFile inputFile) {
      if (indexed.contains(inputFile)) {
        throw MessageException.of("File " + inputFile + " can't be indexed twice. Please check that inclusion/exclusion patterns produce "
          + "disjoint sets for main and test files");
      }
      removed.remove(inputFile);
      indexed.add(inputFile);
    }

    int count() {
      return indexed.size();
    }
  }

}
