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

import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.scanner.util.ProgressReport;

/**
 * Index project input files into {@link InputComponentStore}.
 */
public class ProjectFileIndexer {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectFileIndexer.class);
  private final AbstractExclusionFilters projectExclusionFilters;
  private final DefaultInputProject project;
  private final InputComponentStore componentStore;
  private final InputModuleHierarchy inputModuleHierarchy;
  private final FileIndexer fileIndexer;

  private ProgressReport progressReport;

  public ProjectFileIndexer(DefaultInputProject project, InputComponentStore componentStore, AbstractExclusionFilters exclusionFilters,
    InputModuleHierarchy inputModuleHierarchy,
    FileIndexer fileIndexer) {
    this.project = project;
    this.componentStore = componentStore;
    this.inputModuleHierarchy = inputModuleHierarchy;
    this.fileIndexer = fileIndexer;
    this.projectExclusionFilters = exclusionFilters;
  }

  public void index() {
    progressReport = new ProgressReport("Report about progress of file indexation", TimeUnit.SECONDS.toMillis(10));
    progressReport.start("Index files");

    AtomicInteger excludedByPatternsCount = new AtomicInteger(0);

    indexModulesRecursively(inputModuleHierarchy.root(), excludedByPatternsCount);

    int totalIndexed = componentStore.allFiles().size();
    progressReport.stop(totalIndexed + " " + pluralizeFiles(totalIndexed) + " indexed");

    if (projectExclusionFilters.hasPattern()) {
      int excludedFileCount = excludedByPatternsCount.get();
      LOG.info("{} {} ignored because of inclusion/exclusion patterns", excludedFileCount, pluralizeFiles(excludedFileCount));
    }
  }

  private void indexModulesRecursively(DefaultInputModule module, AtomicInteger excludedByPatternsCount) {
    inputModuleHierarchy.children(module).forEach(m -> indexModulesRecursively(m, excludedByPatternsCount));
    index(module, excludedByPatternsCount);
  }

  private void index(DefaultInputModule module, AtomicInteger excludedByPatternsCount) {
    ModuleExclusionFilters moduleExclusionFilters = new ModuleExclusionFilters(module);
    indexFiles(module, moduleExclusionFilters, module.getSourceDirsOrFiles(), Type.MAIN, excludedByPatternsCount);
    indexFiles(module, moduleExclusionFilters, module.getTestDirsOrFiles(), Type.TEST, excludedByPatternsCount);
  }

  private static String pluralizeFiles(int count) {
    return count == 1 ? "file" : "files";
  }

  private void indexFiles(DefaultInputModule module, AbstractExclusionFilters moduleExclusionFilters, List<Path> sources, Type type, AtomicInteger excludedByPatternsCount) {
    try {
      for (Path dirOrFile : sources) {
        if (dirOrFile.toFile().isDirectory()) {
          indexDirectory(module, moduleExclusionFilters, dirOrFile, type, excludedByPatternsCount);
        } else {
          fileIndexer.indexFile(module, moduleExclusionFilters, dirOrFile, type, progressReport, excludedByPatternsCount);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to index files", e);
    }
  }

  private void indexDirectory(DefaultInputModule module, AbstractExclusionFilters moduleExclusionFilters, Path dirToIndex, Type type, AtomicInteger excludedByPatternsCount)
    throws IOException {
    Files.walkFileTree(dirToIndex.normalize(), Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
      new IndexFileVisitor(module, moduleExclusionFilters, type, excludedByPatternsCount));
  }

  private class IndexFileVisitor implements FileVisitor<Path> {
    private final DefaultInputModule module;
    private final AbstractExclusionFilters moduleExclusionFilters;
    private final Type type;
    private final AtomicInteger excludedByPatternsCount;

    IndexFileVisitor(DefaultInputModule module, AbstractExclusionFilters moduleExclusionFilters, Type type, AtomicInteger excludedByPatternsCount) {
      this.module = module;
      this.moduleExclusionFilters = moduleExclusionFilters;
      this.type = type;
      this.excludedByPatternsCount = excludedByPatternsCount;
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
        fileIndexer.indexFile(module, moduleExclusionFilters, file, type, progressReport, excludedByPatternsCount);
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

}
