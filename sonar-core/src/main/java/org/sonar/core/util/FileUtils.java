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
package org.sonar.core.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * This utility class provides Java NIO based replacement for some methods of
 * {@link org.apache.commons.io.FileUtils Common IO FileUtils} class.
 */
public final class FileUtils {

  private static final String DIRECTORY_CAN_NOT_BE_NULL = "Directory can not be null";
  private static final EnumSet<FileVisitOption> FOLLOW_LINKS = EnumSet.of(FileVisitOption.FOLLOW_LINKS);

  private FileUtils() {
    // prevents instantiation
  }

  /**
   * Deletes a directory recursively.
   *
   * @param directory  directory to delete
   * @throws IOException in case deletion is unsuccessful
   */
  public static void deleteDirectory(File directory) throws IOException {
    requireNonNull(directory, DIRECTORY_CAN_NOT_BE_NULL);
    deleteDirectoryImpl(directory.toPath());
  }

  /**
   * Deletes a directory recursively.
   *
   * @param directory  directory to delete
   * @throws IOException in case deletion is unsuccessful
   */
  public static void deleteDirectory(Path directory) throws IOException {
    requireNonNull(directory, DIRECTORY_CAN_NOT_BE_NULL);
    deleteDirectoryImpl(directory);
  }

  /**
   * Cleans a directory recursively.
   *
   * @param directory  directory to delete
   * @throws IOException in case deletion is unsuccessful
   */
  public static void cleanDirectory(File directory) throws IOException {
    requireNonNull(directory, DIRECTORY_CAN_NOT_BE_NULL);

    Path path = directory.toPath();
    if (!path.toFile().exists()) {
      return;
    }

    cleanDirectoryImpl(path);
  }

  /**
   * Deletes a file, never throwing an exception. If file is a directory, delete it and all sub-directories.
   * <p>
   * The difference between File.delete() and this method are:
   * <ul>
   * <li>A directory to be deleted does not have to be empty.</li>
   * <li>No exceptions are thrown when a file or directory cannot be deleted.</li>
   * </ul>
   *
   * @param file  file or directory to delete, can be {@code null}
   * @return {@code true} if the file or directory was deleted, otherwise {@code false}
   */
  public static boolean deleteQuietly(@Nullable File file) {
    if (file == null) {
      return false;
    }
    return deleteQuietly(file.toPath());
  }

  /**
   * Deletes a file, never throwing an exception. If file is a directory, delete it and all sub-directories.
   * <p>
   * The difference between File.delete() and this method are:
   * <ul>
   * <li>A directory to be deleted does not have to be empty.</li>
   * <li>No exceptions are thrown when a file or directory cannot be deleted.</li>
   * </ul>
   *
   * @param file  file or directory to delete, can be {@code null}
   * @return {@code true} if the file or directory was deleted, otherwise {@code false}
   */
  public static boolean deleteQuietly(@Nullable Path path) {
    if (path == null) {
      return false;
    }

    try {
      if (Files.isDirectory(path)) {
        deleteDirectory(path);
      } else {
        Files.delete(path);
      }
      return true;
    } catch (IOException | SecurityException ignored) {
      return false;
    }
  }

  private static void checkIO(boolean condition, String pattern, Object... arguments) throws IOException {
    if (!condition) {
      throw new IOException(format(pattern, arguments));
    }
  }

  private static void cleanDirectoryImpl(Path path) throws IOException {
    checkArgument(path.toFile().isDirectory(), "'%s' is not a directory", path);

    Files.walkFileTree(path, FOLLOW_LINKS, CleanDirectoryFileVisitor.VISIT_MAX_DEPTH, new CleanDirectoryFileVisitor(path));
  }

  private static void deleteDirectoryImpl(Path path) throws IOException {
    requireNonNull(path, DIRECTORY_CAN_NOT_BE_NULL);
    File file = path.toFile();
    if (!file.exists()) {
      return;
    }

    checkIO(!Files.isSymbolicLink(path), "Directory '%s' is a symbolic link", path);
    checkIO(!file.isFile(), "Directory '%s' is a file", path);

    Files.walkFileTree(path, DeleteRecursivelyFileVisitor.INSTANCE);

    checkIO(!file.exists(), "Unable to delete directory '%s'", path);
  }
  
  
  public static Path getPack200FilePath(Path jarFilePath) {
    String jarFileName = jarFilePath.getFileName().toString();
    String filename = jarFileName.substring(0, jarFileName.length() - 3) + "pack.gz";
    return jarFilePath.resolveSibling(filename);
  }

  /**
   * This visitor is intended to be used to visit direct children of directory <strong>or a symLink to a directory</strong>,
   * so, with a max depth of {@link #VISIT_MAX_DEPTH 1}. Each direct children will either be directly deleted (if file)
   * or recursively deleted (if directory).
   */
  private static class CleanDirectoryFileVisitor extends SimpleFileVisitor<Path> {
    public static final int VISIT_MAX_DEPTH = 1;

    private final Path path;
    private final boolean symLink;

    public CleanDirectoryFileVisitor(Path path) {
      this.path = path;
      this.symLink = Files.isSymbolicLink(path);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (file.toFile().isDirectory()) {
        deleteDirectoryImpl(file);
      } else if (!symLink || !file.equals(path)) {
        Files.delete(file);
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      if (!dir.equals(path)) {
        deleteDirectoryImpl(dir);
      }
      return FileVisitResult.CONTINUE;
    }
  }

  private static final class DeleteRecursivelyFileVisitor extends SimpleFileVisitor<Path> {
    public static final DeleteRecursivelyFileVisitor INSTANCE = new DeleteRecursivelyFileVisitor();

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      Files.delete(file);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      Files.delete(dir);
      return FileVisitResult.CONTINUE;
    }
  }

}
