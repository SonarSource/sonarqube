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

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.sonar.api.utils.TempFolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.Component;

public class SourceLinesHashCache {
  private static final String FILE_NAME_PREFIX = "hashes-";

  private final Path cacheDirectoryPath;
  private final Set<Integer> cacheFileIds = new HashSet<>();

  public SourceLinesHashCache(TempFolder tempFolder) {
    this.cacheDirectoryPath = tempFolder.newDir().toPath();
  }

  public List<String> computeIfAbsent(Component component, Function<Component, List<String>> hashesComputer) {
    int ref = getId(component);

    if (cacheFileIds.add(ref)) {
      List<String> hashes = hashesComputer.apply(component);
      save(ref, hashes);
      return hashes;
    } else {
      return load(ref);
    }
  }

  /**
   * @throws IllegalStateException if the requested value is not cached
   */
  public List<String> get(Component component) {
    Preconditions.checkState(contains(component), "Source line hashes for component %s not cached", component);
    return load(getId(component));
  }

  public boolean contains(Component component) {
    return cacheFileIds.contains(getId(component));
  }

  private static int getId(Component component) {
    return component.getReportAttributes().getRef();
  }

  private void save(int fileId, List<String> hashes) {
    Path filePath = getFilePath(fileId);
    try {
      Files.write(filePath, hashes, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Failed to write to '%s'", filePath), e);
    }
  }

  private List<String> load(int fileId) {
    Path filePath = getFilePath(fileId);
    try {
      return Files.readAllLines(filePath, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Failed to read '%s'", filePath), e);
    }
  }

  private Path getFilePath(int fileId) {
    return cacheDirectoryPath.resolve(FILE_NAME_PREFIX + fileId);
  }
}
