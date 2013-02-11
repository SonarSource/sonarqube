/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.filesystem;

import org.sonar.api.batch.FileFilter;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

import java.io.File;

class FileFilterContext implements FileFilter.Context {
  private final ModuleFileSystem fileSystem;
  private final FileFilter.FileType fileType;
  private File sourceDir;
  private String fileRelativePath;

  FileFilterContext(ModuleFileSystem fileSystem, FileFilter.FileType fileType) {
    this.fileSystem = fileSystem;
    this.fileType = fileType;
  }

  public ModuleFileSystem fileSystem() {
    return fileSystem;
  }

  public FileFilter.FileType fileType() {
    return fileType;
  }

  public File sourceDir() {
    return sourceDir;
  }

  public String fileRelativePath() {
    return fileRelativePath;
  }

  FileFilterContext setSourceDir(File sourceDir) {
    this.sourceDir = sourceDir;
    return this;
  }

  FileFilterContext setFileRelativePath(String fileRelativePath) {
    this.fileRelativePath = fileRelativePath;
    return this;
  }
}
