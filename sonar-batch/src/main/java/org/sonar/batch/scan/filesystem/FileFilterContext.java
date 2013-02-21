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

import org.apache.commons.io.FilenameUtils;
import org.sonar.api.batch.FileFilter;
import org.sonar.api.scan.filesystem.FileType;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

import java.io.File;

class FileFilterContext implements FileFilter.Context {
  private final ModuleFileSystem fileSystem;
  private FileType type;
  private File sourceDir;
  private String fileRelativePath;
  private String fileCanonicalPath;

  FileFilterContext(ModuleFileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  public ModuleFileSystem fileSystem() {
    return fileSystem;
  }

  public FileType type() {
    return type;
  }

  FileFilterContext setType(FileType t) {
    this.type = t;
    return this;
  }

  public File relativeDir() {
    return sourceDir;
  }

  public String relativePath() {
    return fileRelativePath;
  }

  public String canonicalPath() {
    return fileCanonicalPath;
  }

  FileFilterContext setRelativeDir(File d) {
    this.sourceDir = d;
    return this;
  }

  FileFilterContext setRelativePath(String s) {
    this.fileRelativePath = s;
    return this;
  }

  FileFilterContext setCanonicalPath(String s) {
    this.fileCanonicalPath = FilenameUtils.separatorsToUnix(s);
    return this;
  }
}
