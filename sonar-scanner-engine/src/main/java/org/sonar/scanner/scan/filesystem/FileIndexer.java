/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scan.filesystem;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.util.ProgressReport;

/**
 * Index input files into {@link InputPathCache}.
 */
@ScannerSide
public class FileIndexer {

  private static final Logger LOG = LoggerFactory.getLogger(FileIndexer.class);
  private final InputFileFilter[] filters;
  private final boolean isAggregator;
  private final ExclusionFilters exclusionFilters;
  private final InputFileBuilderFactory inputFileBuilderFactory;

  private ProgressReport progressReport;
  private ExecutorService executorService;
  private List<Future<Void>> tasks;

  public FileIndexer(ExclusionFilters exclusionFilters, InputFileBuilderFactory inputFileBuilderFactory, ProjectDefinition def, InputFileFilter[] filters) {
    this.filters = filters;
    this.exclusionFilters = exclusionFilters;
    this.inputFileBuilderFactory = inputFileBuilderFactory;
    this.isAggregator = !def.getSubProjects().isEmpty();
  }

  public FileIndexer(ExclusionFilters exclusionFilters, InputFileBuilderFactory inputFileBuilderFactory, ProjectDefinition def) {
    this(exclusionFilters, inputFileBuilderFactory, def, new InputFileFilter[0]);
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
    int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    executorService = Executors.newFixedThreadPool(threads, new ThreadFactoryBuilder().setNameFormat("FileIndexer-%d").build());
    tasks = new ArrayList<>();
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
    try {
      for (File dirOrFile : sources) {
        if (dirOrFile.isDirectory()) {
          indexDirectory(inputFileBuilder, fileSystem, progress, dirOrFile, type);
        } else {
          indexFile(inputFileBuilder, fileSystem, progress, dirOrFile.toPath(), type);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to index files", e);
    }
  }

  private void indexDirectory(final InputFileBuilder inputFileBuilder, final DefaultModuleFileSystem fileSystem, final Progress status,
    final File dirToIndex, final InputFile.Type type) throws IOException {
    Files.walkFileTree(dirToIndex.toPath().normalize(), Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
      new IndexFileVisitor(inputFileBuilder, fileSystem, status, type));
  }

  private void indexFile(InputFileBuilder inputFileBuilder, DefaultModuleFileSystem fileSystem, Progress progress, Path sourceFile, InputFile.Type type) throws IOException {
    // get case of real file without resolving link
    Path realFile = sourceFile.toRealPath(LinkOption.NOFOLLOW_LINKS);
    DefaultInputFile inputFile = inputFileBuilder.create(realFile.toFile());
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
    final Progress status, final DefaultInputFile inputFile, final InputFile.Type type) {

    tasks.add(executorService.submit(() -> {
      DefaultInputFile completedInputFile = inputFileBuilder.completeAndComputeMetadata(inputFile, type);
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

  private class IndexFileVisitor implements FileVisitor<Path> {
    private InputFileBuilder inputFileBuilder;
    private DefaultModuleFileSystem fileSystem;
    private Progress status;
    private Type type;

    IndexFileVisitor(InputFileBuilder inputFileBuilder, DefaultModuleFileSystem fileSystem, Progress status, InputFile.Type type) {
      this.inputFileBuilder = inputFileBuilder;
      this.fileSystem = fileSystem;
      this.status = status;
      this.type = type;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      Path fileName = dir.getFileName();

      if (fileName != null && fileName.toString().length() > 1 && fileName.toString().charAt(0) == '.') {
        return FileVisitResult.SKIP_SUBTREE;
      }
      if (Files.isHidden(dir)) {
        return FileVisitResult.SKIP_SUBTREE;
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (!Files.isHidden(file)) {
        indexFile(inputFileBuilder, fileSystem, status, file, type);
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
      if (exc instanceof FileSystemLoopException) {
        LOG.warn("Not indexing due to symlink loop: {}", file.toFile());
        return FileVisitResult.CONTINUE;
      }

      throw exc;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      return FileVisitResult.CONTINUE;
    }
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
