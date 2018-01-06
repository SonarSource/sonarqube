/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ScannerSide;
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
  private final ModuleFileSystemInitializer moduleFileSystemInitializer;
  private ExecutorService executorService;
  private final List<Future<Void>> tasks;
  private final DefaultModuleFileSystem defaultModuleFileSystem;
  private final LanguageDetection langDetection;

  private ProgressReport progressReport;

  public FileIndexer(BatchIdGenerator batchIdGenerator, InputComponentStore componentStore, DefaultInputModule module, ExclusionFilters exclusionFilters,
    DefaultComponentTree componentTree, InputFileBuilder inputFileBuilder, ModuleFileSystemInitializer initializer, DefaultModuleFileSystem defaultModuleFileSystem,
    LanguageDetection languageDetection,
    InputFileFilter[] filters) {
    this.batchIdGenerator = batchIdGenerator;
    this.componentStore = componentStore;
    this.module = module;
    this.componentTree = componentTree;
    this.inputFileBuilder = inputFileBuilder;
    this.moduleFileSystemInitializer = initializer;
    this.defaultModuleFileSystem = defaultModuleFileSystem;
    this.langDetection = languageDetection;
    this.filters = filters;
    this.exclusionFilters = exclusionFilters;
    this.tasks = new ArrayList<>();
  }

  public FileIndexer(BatchIdGenerator batchIdGenerator, InputComponentStore componentStore, DefaultInputModule module, ExclusionFilters exclusionFilters,
    DefaultComponentTree componentTree, InputFileBuilder inputFileBuilder, ModuleFileSystemInitializer initializer, DefaultModuleFileSystem defaultModuleFileSystem,
    LanguageDetection languageDetection) {
    this(batchIdGenerator, componentStore, module, exclusionFilters, componentTree, inputFileBuilder, initializer, defaultModuleFileSystem, languageDetection,
      new InputFileFilter[0]);
  }

  public void index() {
    int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    this.executorService = Executors.newFixedThreadPool(threads, new ThreadFactoryBuilder()
      .setNameFormat("FileIndexer-%d")
      .setDaemon(true)
      .build());

    progressReport = new ProgressReport("Report about progress of file indexation", TimeUnit.SECONDS.toMillis(10));
    progressReport.start("Index files");
    exclusionFilters.prepare();

    Progress progress = new Progress();

    indexFiles(moduleFileSystemInitializer.sources(), InputFile.Type.MAIN, progress);
    indexFiles(moduleFileSystemInitializer.tests(), InputFile.Type.TEST, progress);

    waitForTasksToComplete(progressReport);

    progressReport.stop(progress.count() + " " + pluralizeFiles(progress.count()) + " indexed");

    if (exclusionFilters.hasPattern()) {
      LOG.info("{} {} ignored because of inclusion/exclusion patterns", progress.excludedByPatternsCount(), pluralizeFiles(progress.excludedByPatternsCount()));
    }
  }

  private void waitForTasksToComplete(ProgressReport report) {
    executorService.shutdown();
    for (Future<Void> task : tasks) {
      try {
        task.get();
      } catch (ExecutionException e) {
        // Unwrap ExecutionException
        stopAsap(report);
        throw e.getCause() instanceof RuntimeException ? (RuntimeException) e.getCause() : new IllegalStateException(e.getCause());
      } catch (InterruptedException e) {
        stopAsap(report);
        throw new IllegalStateException(e);
      }
    }
  }

  private void stopAsap(ProgressReport report) {
    report.stop(null);
    executorService.shutdownNow();
    try {
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e1) {
      // ignore, what's important is the original exception
    }
  }

  private static String pluralizeFiles(int count) {
    return count == 1 ? "file" : "files";
  }

  private void indexFiles(List<Path> sources, InputFile.Type type, Progress progress) {
    try {
      for (Path dirOrFile : sources) {
        if (dirOrFile.toFile().isDirectory()) {
          indexDirectory(dirOrFile, type, progress);
        } else {
          tasks.add(executorService.submit(() -> indexFile(dirOrFile, type, progress)));
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to index files", e);
    }
  }

  private void indexDirectory(Path dirToIndex, InputFile.Type type, Progress progress) throws IOException {
    Files.walkFileTree(dirToIndex.normalize(), Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
      new IndexFileVisitor(type, progress));
  }

  private Void indexFile(Path sourceFile, InputFile.Type type, Progress progress) throws IOException {
    // get case of real file without resolving link
    Path realAbsoluteFile = sourceFile.toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath().normalize();
    if (!realAbsoluteFile.startsWith(module.getBaseDir())) {
      LOG.warn("File '{}' is ignored. It is not located in module basedir '{}'.", realAbsoluteFile.toAbsolutePath(), module.getBaseDir());
      return null;
    }
    Path relativePath = module.getBaseDir().relativize(realAbsoluteFile);
    if (!exclusionFilters.accept(realAbsoluteFile, relativePath, type)) {
      progress.increaseExcludedByPatternsCount();
      return null;
    }
    String language = langDetection.language(realAbsoluteFile, relativePath);
    if (language == null && langDetection.getForcedLanguage() != null) {
      LOG.warn("File '{}' is ignored because it doesn't belong to the forced language '{}'", realAbsoluteFile.toAbsolutePath(), langDetection.getForcedLanguage());
      return null;
    }
    DefaultInputFile inputFile = inputFileBuilder.create(type, realAbsoluteFile, language);
    if (!accept(inputFile)) {
      progress.increaseExcludedByPatternsCount();
      return null;
    }
    String parentRelativePath = getParentRelativePath(realAbsoluteFile);
    synchronized (this) {
      progress.markAsIndexed(inputFile);
      indexFileAndParentDir(inputFile, parentRelativePath);
    }
    LOG.debug("'{}' indexed {}with language '{}'", relativePath, type == Type.TEST ? "as test " : "", inputFile.language());
    inputFileBuilder.checkMetadata(inputFile);
    return null;
  }

  private String getParentRelativePath(Path filePath) {
    Path parentDir = filePath.getParent();
    return PathResolver.relativize(module.getBaseDir(), parentDir)
      .orElseThrow(() -> new IllegalStateException("Failed to compute relative path of file: " + parentDir));
  }

  private void indexFileAndParentDir(InputFile inputFile, String parentRelativePath) {
    DefaultInputDir inputDir = (DefaultInputDir) componentStore.getDir(module.key(), parentRelativePath);
    if (inputDir == null) {
      inputDir = new DefaultInputDir(module.key(), parentRelativePath, batchIdGenerator.getAsInt());
      inputDir.setModuleBaseDir(module.getBaseDir());
      componentTree.index(inputDir, module);
      defaultModuleFileSystem.add(inputDir);
    }
    componentTree.index(inputFile, inputDir);
    defaultModuleFileSystem.add(inputFile);
  }

  private boolean accept(InputFile indexedFile) {
    // InputFileFilter extensions. Might trigger generation of metadata
    for (InputFileFilter filter : filters) {
      if (!filter.accept(indexedFile)) {
        LOG.debug("'{}' excluded by {}", indexedFile, filter.getClass().getName());
        return false;
      }
    }
    return true;
  }

  private class IndexFileVisitor implements FileVisitor<Path> {
    private final Progress status;
    private final Type type;

    IndexFileVisitor(InputFile.Type type, Progress status) {
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
        tasks.add(executorService.submit(() -> indexFile(file, type, status)));
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
    private AtomicInteger indexedCount = new AtomicInteger(0);
    private AtomicInteger excludedByPatternsCount = new AtomicInteger(0);

    void markAsIndexed(DefaultInputFile inputFile) {
      if (componentStore.getFile(inputFile.getProjectRelativePath()) != null) {
        throw MessageException.of("File " + inputFile + " can't be indexed twice. Please check that inclusion/exclusion patterns produce "
          + "disjoint sets for main and test files");
      }
      int count = indexedCount.incrementAndGet();
      progressReport.message(count + " " + pluralizeFiles(count) + " indexed...  (last one was " + inputFile.getProjectRelativePath() + ")");
    }

    void increaseExcludedByPatternsCount() {
      excludedByPatternsCount.incrementAndGet();
    }

    public int excludedByPatternsCount() {
      return excludedByPatternsCount.get();
    }

    int count() {
      return indexedCount.get();
    }
  }

}
