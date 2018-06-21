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
package org.sonar.server.computation.task.projectanalysis.filemove;

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import org.sonar.server.computation.task.projectanalysis.component.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class MutableMovedFilesRepositoryImpl implements MutableMovedFilesRepository {
  private final Map<String, OriginalFile> originalFiles = new HashMap<>();

  @Override
  public void setOriginalFile(Component file, OriginalFile originalFile) {
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

  @Override
  public Optional<OriginalFile> getOriginalFile(Component file) {
    requireNonNull(file, "file can't be null");
    if (file.getType() != Component.Type.FILE) {
      return Optional.absent();
    }

    return Optional.fromNullable(originalFiles.get(file.getKey()));
  }
}
