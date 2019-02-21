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
import javax.annotation.Nullable;

public class SingleProjectRepository implements ProjectRepositories {
  private Map<String, FileData> fileDataByPath = new HashMap<>();

  public SingleProjectRepository addFileData(@Nullable String path, FileData fileData) {
    if (path == null || (fileData.hash() == null && fileData.revision() == null)) {
      return this;
    }

    fileDataByPath.put(path, fileData);
    return this;
  }

  public Map<String, FileData> fileData() {
    return fileDataByPath;
  }

  public FileData fileDataByPath(@Nullable String path) {
    return fileDataByPath.get(path);
  }
}
