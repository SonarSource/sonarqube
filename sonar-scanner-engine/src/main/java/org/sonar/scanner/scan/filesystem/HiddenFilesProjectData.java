/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.scan.filesystem;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.SystemUtils;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.bootstrap.SonarUserHome;

public class HiddenFilesProjectData {

  final Map<DefaultInputModule, Set<Path>> hiddenFilesByModule = new HashMap<>();
  private final SonarUserHome sonarUserHome;
  private Path cachedSonarUserHomePath;

  public HiddenFilesProjectData(SonarUserHome sonarUserHome) {
    this.sonarUserHome = sonarUserHome;
  }

  public void markAsHiddenFile(Path file, DefaultInputModule module) {
    hiddenFilesByModule.computeIfAbsent(module, k -> new HashSet<>()).add(file);
  }

  /**
   * To alleviate additional strain on the memory, we remove the visibility information for <code>hiddenFilesByModule</code> mapdirectly after querying,
   * as we don't need it afterward.
   */
  public boolean getIsMarkedAsHiddenFileAndRemoveVisibilityInformation(Path file, DefaultInputModule module) {
    Set<Path> hiddenFilesPerModule = hiddenFilesByModule.get(module);
    if (hiddenFilesPerModule != null) {
      return hiddenFilesPerModule.remove(file);
    }
    return false;
  }

  public Path getCachedSonarUserHomePath() throws IOException {
    if (cachedSonarUserHomePath == null) {
      cachedSonarUserHomePath = resolveRealPath(sonarUserHome.getPath());
    }
    return cachedSonarUserHomePath;
  }

  public void clearHiddenFilesData() {
    // Allowing the GC to collect the map, should only be done after all indexing is complete
    hiddenFilesByModule.clear();
  }

  public Path resolveRealPath(Path path) throws IOException {
    if (SystemUtils.IS_OS_WINDOWS) {
      return path.toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath().normalize();
    }
    return path;
  }
}
