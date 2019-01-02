/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.source;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.hash.SourceHashComputer;
import org.sonar.core.util.CloseableIterator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class SourceHashRepositoryImpl implements SourceHashRepository {
  private static final String SOURCE_OR_HASH_FAILURE_ERROR_MSG = "Failed to read source and compute hashes for component %s";

  private final SourceLinesRepository sourceLinesRepository;
  private final Map<String, String> rawSourceHashesByKey = new HashMap<>();

  public SourceHashRepositoryImpl(SourceLinesRepository sourceLinesRepository) {
    this.sourceLinesRepository = sourceLinesRepository;
  }

  @Override
  public String getRawSourceHash(Component file) {
    checkComponentArgument(file);
    if (rawSourceHashesByKey.containsKey(file.getDbKey())) {
      return checkSourceHash(file.getDbKey(), rawSourceHashesByKey.get(file.getDbKey()));
    } else {
      String newSourceHash = computeRawSourceHash(file);
      rawSourceHashesByKey.put(file.getDbKey(), newSourceHash);
      return checkSourceHash(file.getDbKey(), newSourceHash);
    }
  }

  private static void checkComponentArgument(Component file) {
    requireNonNull(file, "Specified component can not be null");
    checkArgument(file.getType() == Component.Type.FILE, "File source information can only be retrieved from FILE components (got %s)", file.getType());
  }

  private String computeRawSourceHash(Component file) {
    SourceHashComputer sourceHashComputer = new SourceHashComputer();
    try (CloseableIterator<String> linesIterator = sourceLinesRepository.readLines(file)) {
      while (linesIterator.hasNext()) {
        sourceHashComputer.addLine(linesIterator.next(), linesIterator.hasNext());
      }
      return sourceHashComputer.getHash();
    }
  }

  private static String checkSourceHash(String fileKey, @Nullable String newSourceHash) {
    checkState(newSourceHash != null, SOURCE_OR_HASH_FAILURE_ERROR_MSG, fileKey);
    return newSourceHash;
  }

}
