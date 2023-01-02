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
package org.sonar.scm.git;

import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

@Immutable
public class ChangedFile {
  private final Path absoluteFilePath;
  private final String oldRelativeFilePathReference;

  private ChangedFile(Path absoluteFilePath, @Nullable String oldRelativeFilePathReference) {
    this.absoluteFilePath = absoluteFilePath;
    this.oldRelativeFilePathReference = oldRelativeFilePathReference;
  }

  public Path getAbsolutFilePath() {
    return absoluteFilePath;
  }

  @CheckForNull
  public String getOldRelativeFilePathReference() {
    return oldRelativeFilePathReference;
  }

  public boolean isMovedFile() {
    return this.getOldRelativeFilePathReference() != null;
  }

  public static ChangedFile of(Path path) {
    return new ChangedFile(path, null);
  }

  public static ChangedFile of(Path path, @Nullable String oldRelativeFilePathReference) {
    return new ChangedFile(path, oldRelativeFilePathReference);
  }

  public static ChangedFile of(DefaultInputFile file) {
    return new ChangedFile(file.path(), file.oldRelativePath());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ChangedFile that = (ChangedFile) o;

    return Objects.equals(oldRelativeFilePathReference, that.oldRelativeFilePathReference)
      && Objects.equals(absoluteFilePath, that.absoluteFilePath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(oldRelativeFilePathReference, absoluteFilePath);
  }
}
