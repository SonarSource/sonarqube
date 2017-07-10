/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.scan.DefaultComponentTree;
import org.sonar.scanner.util.ProgressReport;

/**
 * Index input files into {@link InputComponentStore}.
 */
@ScannerSide
public class FileIndexer {

  private static final Logger LOG = LoggerFactory.getLogger(FileIndexer.class);
  private final InputFileFilter[] filters;
  private final ExclusionFilters exclusionFilters;
  private final InputFileBuilder inputFileBuilder;
  private final DefaultComponentTree componentTree;
  private final DefaultInputModule module;
  private final BatchIdGenerator batchIdGenerator;
  private final InputComponentStore componentStore;
  private ExecutorService executorService;
  private final List<Future<Void>> tasks;

  private ProgressReport progressReport;

  public FileIndexer(BatchIdGenerator batchIdGenerator, InputComponentStore componentStore, DefaultInputModule module, ExclusionFilters exclusionFilters,
    DefaultComponentTree componentTree, InputFileBuilder inputFileBuilder, InputFileFilter[] filters) {
    this.batchIdGenerator = batchIdGenerator;
    this.componentStore = componentStore;
    this.module = module;
    this.componentTree = componentTree;
    this.inputFileBuilder = inputFileBuilder;
    this.filters = filters;
    this.exclusionFilters = exclusionFilters;
    this.tasks = new ArrayList<>();
  }

  public FileIndexer(BatchIdGenerator batchIdGenerator, InputComponentStore componentStore, DefaultInputModule module, ExclusionFilters exclusionFilters,
    DefaultComponentTree componentTree, InputFileBuilder inputFileBuilder) {
    this(batchIdGenerator, componentStore, module, exclusionFilters, componentTree, inputFileBuilder, new InputFileFilter[0]);
  }

  void index(DefaultModuleFileSystem fileSystem) {
    int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    this.executorService = Executors.newFixedThreadPool(threads, new ThreadFactoryBuilder().setNameFormat("FileIndexer-%d").build());

    progressReport = new ProgressReport("Report about progress of file indexation", TimeUnit.SECONDS.toMillis(10));
    progressReport.start("Index files");
    exclusionFilters.prepare();

    Progress progress = new Progress();

    indexFiles(fileSystem, progress, fileSystem.sources(), InputFile.Type.MAIN);
    indexFiles(fileSystem, progress, fileSystem.tests(), InputFile.Type.TEST);

    waitForTasksToComplete();

    progressReport.stop(progress.count() + " " + pluralizeFiles(progress.count()) + " indexed");

    if (exclusionFilters.hasPattern()) {
      LOG.info("{} {} ignored because of inclusion/exclusion patterns", progress.excludedByPatternsCount(), pluralizeFiles(progress.excludedByPatternsCount()));
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

  private static String pluralizeFiles(int count) {
    return count == 1 ? "file" : "files";
  }

  private void indexFiles(DefaultModuleFileSystem fileSystem, Progress progress, List<File> sources, InputFile.Type type) {
    try {
      for (File dirOrFile : sources) {
        if (dirOrFile.isDirectory()) {
          indexDirectory(fileSystem, progress, dirOrFile.toPath(), type);
        } else {
          tasks.add(executorService.submit(() -> indexFile(fileSystem, progress, dirOrFile.toPath(), type)));
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to index files", e);
    }
  }

  private void indexDirectory(final DefaultModuleFileSystem fileSystem, final Progress status, final Path dirToIndex, final InputFile.Type type) throws IOException {
    Files.walkFileTree(dirToIndex.normalize(), Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
      new IndexFileVisitor(fileSystem, status, type));
  }

  private Void indexFile(DefaultModuleFileSystem fileSystem, Progress progress, Path sourceFile, InputFile.Type type) throws IOException {
    // get case of real file without resolving link
    Path realFile = sourceFile.toRealPath(LinkOption.NOFOLLOW_LINKS);
    DefaultInputFile inputFile = inputFileBuilder.create(realFile, type, fileSystem.encoding());
    if (inputFile != null) {
      if (exclusionFilters.accept(inputFile, type) && accept(inputFile)) {
        String parentRelativePath = getParentRelativePath(fileSystem, inputFile);
        synchronized (this) {
          fileSystem.add(inputFile);
          indexParentDir(fileSystem, inputFile, parentRelativePath);
          progress.markAsIndexed(inputFile);
        }
        LOG.debug("'{}' indexed {}with language '{}'", inputFile.relativePath(), type == Type.TEST ? "as test " : "", inputFile.language());
        inputFileBuilder.checkMetadata(inputFile);
      } else {
        progress.increaseExcludedByPatternsCount();
      }
    }
    return null;
  }

  private static String getParentRelativePath(DefaultModuleFileSystem fileSystem, InputFile inputFile) {
    Path parentDir = inputFile.path().getParent();
    String relativePath = new PathResolver().relativePath(fileSystem.baseDirPath(), parentDir);
    if (relativePath == null) {
      throw new IllegalStateException("Failed to compute relative path of file: " + inputFile);
    }
    return relativePath;
  }

  private void indexParentDir(DefaultModuleFileSystem fileSystem, InputFile inputFile, String parentRelativePath) {
    DefaultInputDir inputDir = (DefaultInputDir) componentStore.getDir(module.key(), parentRelativePath);
    if (inputDir == null) {
      inputDir = new DefaultInputDir(fileSystem.moduleKey(), parentRelativePath, batchIdGenerator.get());
      inputDir.setModuleBaseDir(fileSystem.baseDirPath());
      fileSystem.add(inputDir);
      componentTree.index(inputDir, module);
    }
    componentTree.index(inputFile, inputDir);
  }

  private boolean accept(InputFile indexedFile) {
    // InputFileFilter extensions. Might trigger generation of metadata
    for (InputFileFilter filter : filters) {
      if (!filter.accept(indexedFile)) {
        LOG.debug("'{}' excluded by {}", indexedFile.relativePath(), filter.getClass().getName());
        return false;
      }
    }
    return true;
  }

  private class IndexFileVisitor implements FileVisitor<Path> {
    private DefaultModuleFileSystem fileSystem;
    private Progress status;
    private Type type;

    IndexFileVisitor(DefaultModuleFileSystem fileSystem, Progress status, InputFile.Type type) {
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
        tasks.add(executorService.submit(() -> indexFile(fileSystem, status, file, type)));
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
    private AtomicInteger excludedByPatternsCount = new AtomicInteger(0);

    void markAsIndexed(IndexedFile inputFile) {
      if (indexed.contains(inputFile.path())) {
        throw MessageException.of("File " + inputFile + " can't be indexed twice. Please check that inclusion/exclusion patterns produce "
          + "disjoint sets for main and test files");
      }
      indexed.add(inputFile.path());
      progressReport.message(indexed.size() + " " + pluralizeFiles(indexed.size()) + " indexed...  (last one was " + inputFile.relativePath() + ")");
    }

    void increaseExcludedByPatternsCount() {
      excludedByPatternsCount.incrementAndGet();
    }

    public int excludedByPatternsCount() {
      return excludedByPatternsCount.get();
    }

    int count() {
      return indexed.size();
    }
  }

}
