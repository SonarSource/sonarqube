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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.batch.fs.InputFileFilter;
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
  private final boolean isAggregator;
  private final ExclusionFilters exclusionFilters;
  private final IndexedFileBuilder indexedFileBuilder;
  private final MetadataGenerator metadataGenerator;
  private final DefaultComponentTree componentTree;
  private final DefaultInputModule module;
  private final BatchIdGenerator batchIdGenerator;
  private final InputComponentStore componentStore;

  private ProgressReport progressReport;

  public FileIndexer(BatchIdGenerator batchIdGenerator, InputComponentStore componentStore, DefaultInputModule module, ExclusionFilters exclusionFilters,
    DefaultComponentTree componentTree, IndexedFileBuilder indexedFileBuilder, MetadataGenerator inputFileBuilder, ProjectDefinition def, InputFileFilter[] filters) {
    this.batchIdGenerator = batchIdGenerator;
    this.componentStore = componentStore;
    this.module = module;
    this.componentTree = componentTree;
    this.indexedFileBuilder = indexedFileBuilder;
    this.metadataGenerator = inputFileBuilder;
    this.filters = filters;
    this.exclusionFilters = exclusionFilters;
    this.isAggregator = !def.getSubProjects().isEmpty();
  }

  public FileIndexer(BatchIdGenerator batchIdGenerator, InputComponentStore componentStore, DefaultInputModule module, ExclusionFilters exclusionFilters,
    DefaultComponentTree componentTree, IndexedFileBuilder indexedFileBuilder, MetadataGenerator inputFileBuilder, ProjectDefinition def) {
    this(batchIdGenerator, componentStore, module, exclusionFilters, componentTree, indexedFileBuilder, inputFileBuilder, def, new InputFileFilter[0]);
  }

  void index(DefaultModuleFileSystem fileSystem) {
    fileSystem.add(module);
    if (isAggregator) {
      // No indexing for an aggregator module
      return;
    }
    progressReport = new ProgressReport("Report about progress of file indexation", TimeUnit.SECONDS.toMillis(10));
    progressReport.start("Index files");
    exclusionFilters.prepare();

    Progress progress = new Progress();

    indexFiles(fileSystem, progress, fileSystem.sources(), InputFile.Type.MAIN);
    indexFiles(fileSystem, progress, fileSystem.tests(), InputFile.Type.TEST);

    progressReport.stop(progress.count() + " files indexed");

    if (exclusionFilters.hasPattern()) {
      LOG.info("{} files ignored because of inclusion/exclusion patterns", progress.excludedByPatternsCount());
    }
  }

  private void indexFiles(DefaultModuleFileSystem fileSystem, Progress progress, List<File> sources, InputFile.Type type) {
    try {
      for (File dirOrFile : sources) {
        if (dirOrFile.isDirectory()) {
          indexDirectory(fileSystem, progress, dirOrFile.toPath(), type);
        } else {
          indexFile(fileSystem, progress, dirOrFile.toPath(), type);
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

  private void indexFile(DefaultModuleFileSystem fileSystem, Progress progress, Path sourceFile, InputFile.Type type) throws IOException {
    // get case of real file without resolving link
    Path realFile = sourceFile.toRealPath(LinkOption.NOFOLLOW_LINKS);
    DefaultIndexedFile indexedFile = indexedFileBuilder.create(realFile, type, fileSystem.baseDirPath());
    if (indexedFile != null) {
      InputFile inputFile = new DefaultInputFile(indexedFile, f -> metadataGenerator.readMetadata(f, fileSystem.encoding()));
      if (exclusionFilters.accept(indexedFile, type) && accept(inputFile)) {
        fileSystem.add(inputFile);
        indexParentDir(fileSystem, inputFile);
        progress.markAsIndexed(indexedFile);
        LOG.debug("'{}' indexed {} with language '{}'", indexedFile.relativePath(), type == Type.TEST ? "as test " : "", indexedFile.language());
      } else {
        progress.increaseExcludedByPatternsCount();
      }
    }
  }

  private void indexParentDir(DefaultModuleFileSystem fileSystem, InputFile inputFile) {
    Path parentDir = inputFile.path().getParent();
    String relativePath = new PathResolver().relativePath(fileSystem.baseDirPath(), parentDir);
    if (relativePath == null) {
      throw new IllegalStateException("Failed to compute relative path of file: " + inputFile);
    }

    DefaultInputDir inputDir = (DefaultInputDir) componentStore.getDir(module.key(), relativePath);
    if (inputDir == null) {
      inputDir = new DefaultInputDir(fileSystem.moduleKey(), relativePath, batchIdGenerator.get());
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
        indexFile(fileSystem, status, file, type);
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

    synchronized void markAsIndexed(IndexedFile inputFile) {
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
