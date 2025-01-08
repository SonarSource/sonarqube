/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.util.FileUtils;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.util.Objects.requireNonNull;

/**
 * This utility class provides Java NIO based replacement for some methods of
 * {@link org.apache.commons.io.FileUtils Common IO FileUtils} class.
 * Only the methods which name ends with {@code "orThrowIOE"} may raise {@link IOException}.
 * Others wrap checked exceptions into {@link IllegalStateException}.
 */
public class Files2 {

  public static final Files2 FILES2 = new Files2();

  private Files2() {
    // use FILES2 singleton
  }

  /**
   * Deletes a directory or a file if it exists, else does nothing. In the case
   * of a directly, it is deleted recursively.
   *
   * @param fileOrDir file or directory to delete
   * @throws IOException in case deletion is unsuccessful
   */
  public void deleteIfExistsOrThrowIOE(File fileOrDir) throws IOException {
    if (!fileOrDir.exists()) {
      return;
    }
    if (fileOrDir.isDirectory()) {
      FileUtils.deleteDirectory(fileOrDir);
    } else {
      Files.delete(fileOrDir.toPath());
    }
  }

  /**
   * Like {@link #deleteIfExistsOrThrowIOE(File)} but wraps {@link IOException}
   * into {@link IllegalStateException}.
   *
   * @throws IllegalStateException in case deletion is unsuccessful
   */
  public void deleteIfExists(File fileOrDir) {
    try {
      deleteIfExistsOrThrowIOE(fileOrDir);
    } catch (IOException e) {
      throw new IllegalStateException("Can not delete " + fileOrDir, e);
    }
  }

  /**
   * Deletes a directory or a file if it exists, else does nothing. In the case
   * of a directly, it is deleted recursively. Any exception is trapped and
   * ignored.
   *
   * @param fileOrDir file or directory to delete. Can be {@code null}.
   */
  public void deleteQuietly(@Nullable File fileOrDir) {
    FileUtils.deleteQuietly(fileOrDir);
  }

  /**
   * Moves a file.
   *
   * <p>
   * When the destination file is on another file system, do a "copy and delete".
   * </p>
   *
   * @param from the file to be moved
   * @param to the destination file
   * @throws NullPointerException if source or destination is {@code null}
   * @throws IOException if the destination file exists
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs moving the file
   */
  public void moveFileOrThrowIOE(File from, File to) throws IOException {
    org.apache.commons.io.FileUtils.moveFile(from, to);
  }

  /**
   * Like {@link #moveFileOrThrowIOE(File, File)} but wraps {@link IOException}
   * into {@link IllegalStateException}.
   *
   * @param from the file to be moved
   * @param to the destination file
   * @throws IllegalStateException if the destination file exists
   * @throws IllegalStateException if source or destination is invalid
   * @throws IllegalStateException if an IO error occurs moving the file
   */
  public void moveFile(File from, File to) {
    try {
      moveFileOrThrowIOE(from, to);
    } catch (IOException e) {
      throw new IllegalStateException("Can not move file " + from + " to " + to, e);
    }
  }

  /**
   * Opens a {@link FileOutputStream} for the specified file, checking and
   * creating the parent directory if it does not exist.
   * <p>
   * The parent directory will be created if it does not exist.
   * The file will be created if it does not exist.
   *
   * @param file  the file to open for output, must not be {@code null}
   * @param append if {@code true}, then bytes will be added to the
   * end of the file rather than overwriting
   * @return a new {@link FileOutputStream} for the specified file
   * @throws IOException if the specified file is a directory
   * @throws IOException if the file can not be written to
   * @throws IOException if a parent directory can not be created
   */
  public FileOutputStream openOutputStreamOrThrowIOE(File file, boolean append) throws IOException {
    if (file.exists()) {
      checkOrThrowIOE(!file.isDirectory(), "File %s exists but is a directory", file);
      checkOrThrowIOE(file.canWrite(), "File %s can not be written to", file);
    } else {
      File parent = file.getParentFile();
      if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
        throw new IOException("Directory " + parent + " could not be created");
      }
    }
    return new FileOutputStream(file, append);
  }

  /**
   * Opens a {@link FileOutputStream} for the specified file, checking and
   * creating the parent directory if it does not exist.
   * <p>
   * The parent directory will be created if it does not exist.
   * The file will be created if it does not exist.
   *
   * @param file  the file to open for output, must not be {@code null}
   * @param append if {@code true}, then bytes will be added to the
   * end of the file rather than overwriting
   * @return a new {@link FileOutputStream} for the specified file
   * @throws IllegalStateException if the specified file is a directory
   * @throws IllegalStateException if the file can not be written to
   * @throws IllegalStateException if a parent directory can not be created
   */
  public FileOutputStream openOutputStream(File file, boolean append) {
    try {
      return openOutputStreamOrThrowIOE(file, append);
    } catch (IOException e) {
      throw new IllegalStateException("Can not open file " + file, e);
    }
  }

  /**
   * Opens a {@link FileInputStream} for the specified file, providing better
   * error messages than simply calling {@code new FileInputStream(file)}.
   *
   * @param file  the file to open, must not be {@code null}
   * @return a new {@link FileInputStream} for the specified file
   * @throws IOException if the file does not exist
   * @throws IOException if the specified file is a directory
   * @throws IOException if the file can not be read
   */
  public FileInputStream openInputStreamOrThrowIOE(File file) throws IOException {
    checkOrThrowIOE(!file.isDirectory(), "File %s exists but is a directory", file);
    checkOrThrowIOE(file.exists(), "File %s does not exist", file);
    checkOrThrowIOE(file.canRead(), "File %s can not be read", file);
    return new FileInputStream(file);
  }

  /**
   * Opens a {@link FileInputStream} for the specified file, providing better
   * error messages than simply calling {@code new FileInputStream(file)}.
   *
   * @param file  the file to open, must not be {@code null}
   * @return a new {@link FileInputStream} for the specified file
   * @throws IllegalStateException if the file does not exist
   * @throws IllegalStateException if the specified file is a directory
   * @throws IllegalStateException if the file can not be read
   */
  public FileInputStream openInputStream(File file) {
    try {
      return openInputStreamOrThrowIOE(file);
    } catch (IOException e) {
      throw new IllegalStateException("Can not open file " + file, e);
    }
  }

  /**
   * Unzips a file to the specified directory. The directory is created if it does not exist.
   *
   * @throws IOException if {@code zipFile} is a directory
   * @throws IOException if {@code zipFile} does not exist
   * @throws IOException if {@code toDir} can not be created
   */
  public void unzipToDirOrThrowIOE(File zipFile, File toDir) throws IOException {
    checkOrThrowIOE(!zipFile.isDirectory(), "File %s exists but is a directory", zipFile);
    checkOrThrowIOE(zipFile.exists(), "File %s does not exist", zipFile);
    ZipUtils.unzip(zipFile, toDir);
  }

  /**
   * Unzips a file to the specified directory. The directory is created if it does not exist.
   *
   * @throws IllegalStateException if {@code zipFile} is a directory
   * @throws IllegalStateException if {@code zipFile} does not exist
   * @throws IllegalStateException if {@code toDir} can not be created
   */
  public void unzipToDir(File zipFile, File toDir) {
    try {
      unzipToDirOrThrowIOE(zipFile, toDir);
    } catch (IOException e) {
      throw new IllegalStateException("Can not unzip file " + zipFile + " to directory " + toDir, e);
    }
  }

  /**
   * Zips the directory {@code dir} to the file {@code toFile}. If {@code toFile} is overridden
   * if it exists, else it is created.
   *
   * @throws IllegalStateException if {@code dir} is a not directory
   * @throws IllegalStateException if {@code dir} does not exist
   * @throws IllegalStateException if {@code toFile} can not be created
   */
  public void zipDirOrThrowIOE(File dir, File toFile) throws IOException {
    checkOrThrowIOE(dir.exists(), "Directory %s does not exist", dir);
    checkOrThrowIOE(dir.isDirectory(), "File %s exists but is not a directory", dir);
    ZipUtils.zipDir(dir, toFile);
  }

  /**
   * Zips the directory {@code dir} to the file {@code toFile}. If {@code toFile} is overridden
   * if it exists, else it is created.
   *
   * @throws IllegalStateException if {@code dir} is a not directory
   * @throws IllegalStateException if {@code dir} does not exist
   * @throws IllegalStateException if {@code toFile} can not be created
   */
  public void zipDir(File dir, File toFile) {
    try {
      zipDirOrThrowIOE(dir, toFile);
    } catch (IOException e) {
      throw new IllegalStateException("Can not zip directory " + dir + " to file " + toFile, e);
    }
  }

  /**
   * Creates specified directory if it does not exist yet and any non existing parent.
   *
   * @throws IllegalStateException if specified File exists but is not a directory
   * @throws IllegalStateException if directory creation failed
   */
  public void createDir(File dir) {
    Path dirPath = requireNonNull(dir, "dir can not be null").toPath();
    if (dirPath.toFile().exists()) {
      checkState(dirPath.toFile().isDirectory(), "%s is not a directory", dirPath);
    } else {
      try {
        createDirectories(dirPath);
      } catch (IOException e) {
        throw new IllegalStateException(format("Failed to create directory %s", dirPath), e);
      }
    }
  }

  private static void checkOrThrowIOE(boolean expression, @Nullable String errorMessageTemplate, @Nullable Object... errorMessageArgs) throws IOException {
    if (!expression) {
      throw new IOException(format(errorMessageTemplate, errorMessageArgs));
    }
  }
}
