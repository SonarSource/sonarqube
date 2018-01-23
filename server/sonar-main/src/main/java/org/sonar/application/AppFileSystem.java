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
package org.sonar.application;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.config.AppSettings;
import org.sonar.process.sharedmemoryfile.AllProcessesCommands;

import static java.lang.String.format;
import static java.nio.file.FileVisitResult.CONTINUE;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.sonar.process.FileUtils2.deleteDirectory;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;
import static org.sonar.process.ProcessProperties.Property.PATH_WEB;

public class AppFileSystem implements FileSystem {

  private static final Logger LOG = LoggerFactory.getLogger(AppFileSystem.class);
  private static final EnumSet<FileVisitOption> FOLLOW_LINKS = EnumSet.of(FileVisitOption.FOLLOW_LINKS);

  private final AppSettings settings;

  public AppFileSystem(AppSettings settings) {
    this.settings = settings;
  }

  @Override
  public void reset() throws IOException {
    createDirectory(PATH_DATA.getKey());
    createDirectory(PATH_WEB.getKey());
    createDirectory(PATH_LOGS.getKey());
    File tempDir = createOrCleanTempDirectory(PATH_TEMP.getKey());
    try (AllProcessesCommands allProcessesCommands = new AllProcessesCommands(tempDir)) {
      allProcessesCommands.clean();
    }
  }

  @Override
  public File getTempDir() {
    return settings.getProps().nonNullValueAsFile(PATH_TEMP.getKey());
  }

  private boolean createDirectory(String propKey) throws IOException {
    File dir = settings.getProps().nonNullValueAsFile(propKey);
    if (dir.exists()) {
      ensureIsNotAFile(propKey, dir);
      return false;
    }

    forceMkdir(dir);
    ensureIsNotAFile(propKey, dir);
    return true;
  }

  private static void ensureIsNotAFile(String propKey, File dir) {
    if (!dir.isDirectory()) {
      throw new IllegalStateException(format("Property '%s' is not valid, not a directory: %s",
        propKey, dir.getAbsolutePath()));
    }
  }

  private File createOrCleanTempDirectory(String propKey) throws IOException {
    File dir = settings.getProps().nonNullValueAsFile(propKey);
    LOG.info("Cleaning or creating temp directory {}", dir.getAbsolutePath());
    if (!createDirectory(propKey)) {
      Files.walkFileTree(dir.toPath(), FOLLOW_LINKS, CleanTempDirFileVisitor.VISIT_MAX_DEPTH, new CleanTempDirFileVisitor(dir.toPath()));
    }
    return dir;
  }

  private static class CleanTempDirFileVisitor extends SimpleFileVisitor<Path> {
    private static final Path SHAREDMEMORY_FILE = Paths.get("sharedmemory");
    static final int VISIT_MAX_DEPTH = 1;

    private final Path path;
    private final boolean symLink;

    CleanTempDirFileVisitor(Path path) {
      this.path = path;
      this.symLink = Files.isSymbolicLink(path);
    }

    @Override
    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
      File file = filePath.toFile();
      if (file.isDirectory()) {
        deleteDirectory(file);
      } else if (filePath.getFileName().equals(SHAREDMEMORY_FILE)) {
        return CONTINUE;
      } else if (!symLink || !filePath.equals(path)) {
        Files.delete(filePath);
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      if (!dir.equals(path)) {
        deleteDirectory(dir.toFile());
      }
      return CONTINUE;
    }
  }

}
