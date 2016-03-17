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
package org.sonar.batch.scan.filesystem;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.scan.filesystem.FileSystemFilter;
import org.sonar.api.scan.filesystem.FileType;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

public class DeprecatedFileFilters implements InputFileFilter {
  private final FileSystemFilter[] filters;

  public DeprecatedFileFilters(FileSystemFilter[] filters) {
    this.filters = filters;
  }

  public DeprecatedFileFilters() {
    this(new FileSystemFilter[0]);
  }

  @Override
  public boolean accept(InputFile inputFile) {
    if (filters.length > 0) {
      DeprecatedContext context = new DeprecatedContext(inputFile);
      for (FileSystemFilter filter : filters) {
        if (!filter.accept(inputFile.file(), context)) {
          return false;
        }
      }
    }
    return true;
  }

  static class DeprecatedContext implements FileSystemFilter.Context {
    private final InputFile inputFile;

    DeprecatedContext(InputFile inputFile) {
      this.inputFile = inputFile;
    }

    @Override
    public ModuleFileSystem fileSystem() {
      throw new UnsupportedOperationException("Not supported since 4.0");
    }

    @Override
    public FileType type() {
      String type = inputFile.type().name();
      return FileType.valueOf(type);
    }

    @Override
    public String relativePath() {
      return inputFile.relativePath();
    }

    @Override
    public String canonicalPath() {
      return inputFile.absolutePath();
    }
  }
}
