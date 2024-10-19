/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.filemove;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonar.ce.task.projectanalysis.component.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class MutableMovedFilesRepositoryImpl implements MutableMovedFilesRepository {
  private final Map<String, OriginalFile> originalFiles = new HashMap<>();
  private final Map<String, OriginalFile> originalPullRequestFiles = new HashMap<>();

  @Override
  public void setOriginalFile(Component file, OriginalFile originalFile) {
    storeOriginalFileInCache(originalFiles, file, originalFile);
  }

  @Override
  public void setOriginalPullRequestFile(Component file, OriginalFile originalFile) {
    storeOriginalFileInCache(originalPullRequestFiles, file, originalFile);
  }

  @Override
  public Optional<OriginalFile> getOriginalFile(Component file) {
    return retrieveOriginalFileFromCache(originalFiles, file);
  }

  @Override
  public Optional<OriginalFile> getOriginalPullRequestFile(Component file) {
    return retrieveOriginalFileFromCache(originalPullRequestFiles, file);
  }

  private static void storeOriginalFileInCache(Map<String, OriginalFile> originalFiles, Component file, OriginalFile originalFile) {
    requireNonNull(file, "file can't be null");
    requireNonNull(originalFile, "originalFile can't be null");
    checkArgument(file.getType() == Component.Type.FILE, "file must be of type FILE");

    OriginalFile existingOriginalFile = originalFiles.get(file.getKey());

    checkState(existingOriginalFile == null || existingOriginalFile.equals(originalFile),
      "Original file %s already registered for file %s. Unable to register %s.", existingOriginalFile, file, originalFile);

    if (existingOriginalFile == null) {
      originalFiles.put(file.getKey(), originalFile);
    }
  }

  private static Optional<OriginalFile> retrieveOriginalFileFromCache(Map<String, OriginalFile> originalFiles, Component file) {
    requireNonNull(file, "file can't be null");

    if (file.getType() != Component.Type.FILE) {
      return Optional.empty();
    }

    return Optional.ofNullable(originalFiles.get(file.getKey()));
  }
}
