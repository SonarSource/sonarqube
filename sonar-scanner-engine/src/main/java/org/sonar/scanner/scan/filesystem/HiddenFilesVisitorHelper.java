/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributes;
import org.apache.commons.lang3.SystemUtils;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.scan.ModuleConfiguration;

public class HiddenFilesVisitorHelper {

  private static final String EXCLUDE_HIDDEN_FILES_PROPERTY = "sonar.scanner.excludeHiddenFiles";
  private final HiddenFilesProjectData hiddenFilesProjectData;
  private final DefaultInputModule module;
  final boolean excludeHiddenFiles;
  private Path moduleWorkDir;
  Path rootHiddenDir;

  public HiddenFilesVisitorHelper(HiddenFilesProjectData hiddenFilesProjectData, DefaultInputModule module, ModuleConfiguration moduleConfig) {
    this.hiddenFilesProjectData = hiddenFilesProjectData;
    this.module = module;
    this.excludeHiddenFiles = moduleConfig.getBoolean(EXCLUDE_HIDDEN_FILES_PROPERTY).orElse(false);
  }

  public boolean shouldVisitDir(Path path) throws IOException {
    boolean isHidden = isHiddenDir(path);

    if (isHidden && (excludeHiddenFiles || isExcludedHiddenDirectory(path))) {
      return false;
    }
    if (isHidden) {
      enterHiddenDirectory(path);
    }
    return true;
  }

  private boolean isExcludedHiddenDirectory(Path path) throws IOException {
    return getCachedModuleWorkDir().equals(path) || hiddenFilesProjectData.getCachedSonarUserHomePath().equals(path);
  }

  void enterHiddenDirectory(Path dir) {
    if (!insideHiddenDirectory()) {
      rootHiddenDir = dir;
    }
  }

  public void exitDirectory(Path path) {
    if (insideHiddenDirectory() && rootHiddenDir.equals(path)) {
      resetRootHiddenDir();
    }
  }

  void resetRootHiddenDir() {
    this.rootHiddenDir = null;
  }

  public boolean shouldVisitFile(Path path) throws IOException {
    boolean isHidden = insideHiddenDirectory() || Files.isHidden(path);

    if (!excludeHiddenFiles && isHidden) {
      hiddenFilesProjectData.markAsHiddenFile(path, module);
    }

    return !excludeHiddenFiles || !isHidden;
  }

  private Path getCachedModuleWorkDir() throws IOException {
    if (moduleWorkDir == null) {
      moduleWorkDir = hiddenFilesProjectData.resolveRealPath(module.getWorkDir());
    }
    return moduleWorkDir;
  }

  // visible for testing
  boolean insideHiddenDirectory() {
    return rootHiddenDir != null;
  }

  protected static boolean isHiddenDir(Path path) throws IOException {
    if (SystemUtils.IS_OS_WINDOWS) {
      try {
        DosFileAttributes dosFileAttributes = Files.readAttributes(path, DosFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return dosFileAttributes.isHidden();
      } catch (UnsupportedOperationException e) {
        return path.toFile().isHidden();
      }
    } else {
      return Files.isHidden(path);
    }
  }
}
