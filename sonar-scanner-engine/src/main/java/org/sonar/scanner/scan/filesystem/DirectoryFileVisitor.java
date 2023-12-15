/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.fs.InputModuleHierarchy;

public class DirectoryFileVisitor implements FileVisitor<Path> {

  private static final Logger LOG = LoggerFactory.getLogger(DirectoryFileVisitor.class);

  private final FileVisitAction fileVisitAction;
  private final DefaultInputModule module;
  private final ModuleExclusionFilters moduleExclusionFilters;

  private final InputModuleHierarchy inputModuleHierarchy;
  private final InputFile.Type type;

  DirectoryFileVisitor(FileVisitAction fileVisitAction, DefaultInputModule module, ModuleExclusionFilters moduleExclusionFilters, InputModuleHierarchy inputModuleHierarchy, InputFile.Type type) {
    this.fileVisitAction = fileVisitAction;
    this.module = module;
    this.moduleExclusionFilters = moduleExclusionFilters;
    this.inputModuleHierarchy = inputModuleHierarchy;
    this.type = type;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    return isHidden(dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
    if (!Files.isHidden(file)) {
      fileVisitAction.execute(file);
    }
    return FileVisitResult.CONTINUE;
  }

  /**
   * <p>Overridden method to handle exceptions while visiting files in the analysis.</p>
   *
   * <p>
   *   <ul>
   *     <li>FileSystemLoopException - We show a warning that a symlink loop exists and we skip the file.</li>
   *     <li>AccessDeniedException for excluded files/directories - We skip the file, as files excluded from the analysis, shouldn't throw access exceptions.</li>
   *   </ul>
   * </p>
   *
   * @param file a reference to the file
   * @param exc  the I/O exception that prevented the file from being visited
   * @throws IOException
   */
  @Override
  public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
    if (exc instanceof FileSystemLoopException) {
      LOG.warn("Not indexing due to symlink loop: {}", file.toFile());
      return FileVisitResult.CONTINUE;
    } else if (exc instanceof AccessDeniedException && isExcluded(file)) {
      return FileVisitResult.CONTINUE;
    }
    throw exc;
  }

  /**
   * <p>Checks if the directory is excluded in the analysis or not. Only the exclusions are checked.</p>
   *
   * <p>The inclusions cannot be checked for directories, since the current implementation of pattern matching is intended only for files.</p>
   *
   * @param path The file or directory.
   * @return True if file/directory is excluded from the analysis, false otherwise.
   */
  private boolean isExcluded(Path path) throws IOException {
    Path realAbsoluteFile = path.toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath().normalize();
    return isExcludedDirectory(moduleExclusionFilters, realAbsoluteFile, inputModuleHierarchy.root().getBaseDir(), module.getBaseDir(), type);
  }

  /**
   * <p>Checks if the path is a directory that is excluded.</p>
   *
   * <p>Exclusions patterns are checked both at project and module level.</p>
   *
   * @param moduleExclusionFilters The exclusion filters.
   * @param realAbsoluteFile       The path to be checked.
   * @param projectBaseDir         The project base directory.
   * @param moduleBaseDir          The module base directory.
   * @param type                   The input file type.
   * @return True if path is an excluded directory, false otherwise.
   */
  private static boolean isExcludedDirectory(ModuleExclusionFilters moduleExclusionFilters, Path realAbsoluteFile, Path projectBaseDir, Path moduleBaseDir,
    InputFile.Type type) {
    Path projectRelativePath = projectBaseDir.relativize(realAbsoluteFile);
    Path moduleRelativePath = moduleBaseDir.relativize(realAbsoluteFile);
    return moduleExclusionFilters.isExcludedAsParentDirectoryOfExcludedChildren(realAbsoluteFile, projectRelativePath, projectBaseDir, type)
      || moduleExclusionFilters.isExcludedAsParentDirectoryOfExcludedChildren(realAbsoluteFile, moduleRelativePath, moduleBaseDir, type);
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
    return FileVisitResult.CONTINUE;
  }

  private static boolean isHidden(Path path) throws IOException {
    if (SystemUtils.IS_OS_WINDOWS) {
      try {
        DosFileAttributes dosFileAttributes = Files.readAttributes(path, DosFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return dosFileAttributes.isHidden();
      } catch (UnsupportedOperationException e) {
        return path.toFile().isHidden();
      }
    } else {
      return Files.isHidden(path);
    }
  }

  @FunctionalInterface
  interface FileVisitAction {
    void execute(Path file) throws IOException;
  }
}

