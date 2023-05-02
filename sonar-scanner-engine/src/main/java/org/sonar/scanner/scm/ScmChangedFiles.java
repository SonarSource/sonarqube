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
package org.sonar.scanner.scm;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.scm.git.ChangedFile;

import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Immutable
public class ScmChangedFiles {
  @Nullable
  private final Set<ChangedFile> changedFiles;
  private final Map<Path, ChangedFile> changedFilesByPath;

  public ScmChangedFiles(@Nullable Set<ChangedFile> changedFiles) {
    this.changedFiles = changedFiles;
    this.changedFilesByPath = toChangedFilesByPathMap(changedFiles);
  }

  public boolean isChanged(Path file) {
    if (!isValid()) {
      throw new IllegalStateException("Scm didn't provide valid data");
    }

    return this.getChangedFile(file).isPresent();
  }

  public boolean isValid() {
    return changedFiles != null;
  }

  @CheckForNull
  public Collection<ChangedFile> get() {
    return changedFiles;
  }

  @CheckForNull
  public String getOldRelativeFilePath(Path absoluteFilePath) {
    return this.getChangedFile(absoluteFilePath)
      .filter(ChangedFile::isMovedFile)
      .map(ChangedFile::getOldRelativeFilePathReference)
      .orElse(null);
  }

  private Optional<ChangedFile> getChangedFile(Path absoluteFilePath) {
    return Optional.ofNullable(changedFilesByPath.get(absoluteFilePath));
  }

  private static Map<Path, ChangedFile> toChangedFilesByPathMap(@Nullable Set<ChangedFile> changedFiles) {
    return Optional.ofNullable(changedFiles)
      .map(files -> files.stream().collect(toMap(ChangedFile::getAbsolutFilePath, identity())))
      .orElse(emptyMap());
  }
}
