/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan.filesystem;

import org.sonar.api.scan.filesystem.*;

import java.io.File;

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

  class DeprecatedContext implements FileSystemFilter.Context {
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
      String type = inputFile.attribute(InputFile.ATTRIBUTE_TYPE);
      return type == null ? null : FileType.valueOf(type.toUpperCase());
    }

    @Override
    public File relativeDir() {
      String path = inputFile.attribute(InputFile.ATTRIBUTE_SOURCEDIR_PATH);
      return path != null ? new File(path) : null;
    }

    @Override
    public String relativePath() {
      return inputFile.attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH);
    }

    @Override
    public String canonicalPath() {
      return inputFile.absolutePath();
    }
  }
}
