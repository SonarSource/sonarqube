/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.scm.git.ChangedFile;

@Immutable
public class ScmChangedFiles {
  @Nullable
  private final Collection<ChangedFile> changedFiles;

  public ScmChangedFiles(@Nullable Collection<ChangedFile> changedFiles) {
    this.changedFiles = changedFiles;
  }

  public boolean isChanged(Path file) {
    if (!isValid()) {
      throw new IllegalStateException("Scm didn't provide valid data");
    }

    return this.findFile(file).isPresent();
  }

  public boolean isValid() {
    return changedFiles != null;
  }

  @CheckForNull
  public Collection<ChangedFile> get() {
    return changedFiles;
  }

  @CheckForNull
  public String getFileOldPath(Path absoluteFilePath) {
    return this.findFile(absoluteFilePath)
      .filter(ChangedFile::isMoved)
      .map(ChangedFile::getOldFilePath)
      .orElse(null);
  }

  private Optional<ChangedFile> findFile(Path absoluteFilePath) {
    Predicate<ChangedFile> isTargetFile = file -> file.getAbsolutFilePath().equals(absoluteFilePath);

    return Optional.ofNullable(this.get())
      .orElseGet(List::of)
      .stream()
      .filter(isTargetFile)
      .findFirst();
  }
}
