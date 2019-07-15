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
package org.sonar.scanner.protocol.input;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class MultiModuleProjectRepository implements ProjectRepositories {
  private Map<String, SingleProjectRepository> repositoryPerModule = new HashMap<>();

  public MultiModuleProjectRepository addFileDataToModule(String moduleKey, @Nullable String path, FileData fileData) {
    if (path == null || (fileData.hash() == null && fileData.revision() == null)) {
      return this;
    }

    SingleProjectRepository repository = repositoryPerModule.computeIfAbsent(moduleKey, k -> new SingleProjectRepository());
    repository.addFileData(path, fileData);
    return this;
  }

  public Map<String, SingleProjectRepository> repositoriesByModule() {
    return repositoryPerModule;
  }

  @CheckForNull
  public FileData fileData(String moduleKeyWithBranch, @Nullable String path) {
    Optional<SingleProjectRepository> moduleRepository = Optional.ofNullable(repositoryPerModule.get(moduleKeyWithBranch));
    return moduleRepository
      .map(singleProjectRepository -> singleProjectRepository.fileDataByPath(path))
      .orElse(null);
  }
}
