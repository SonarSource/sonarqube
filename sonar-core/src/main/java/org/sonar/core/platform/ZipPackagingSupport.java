/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.core.platform;

import java.io.File;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.sonar.api.utils.MessageException;

import static java.lang.String.format;

/**
 * Locates the scanner-side jar nested inside a ZIP-packaged plugin, at {@code plugin/*.jar}.
 */
final class ZipPackagingSupport {

  private static final String EXECUTABLE_JAR_FOLDER = "plugin/";

  private ZipPackagingSupport() {
  }

  /**
   * @throws MessageException if the zip does not contain exactly one {@code plugin/*.jar} entry
   */
  static ZipEntry findExecutableJarEntry(ZipFile zip) {
    List<? extends ZipEntry> matches = zip.stream()
      .filter(ZipPackagingSupport::isExecutableJarEntry)
      .toList();
    if (matches.size() != 1) {
      throw MessageException.of(format(
        "File is not a valid ZIP-packaged plugin: expected exactly one %s*.jar entry, found %s. Please delete it and restart: %s",
        EXECUTABLE_JAR_FOLDER, matches.size(), new File(zip.getName()).getAbsolutePath()));
    }
    return matches.get(0);
  }

  private static boolean isExecutableJarEntry(ZipEntry entry) {
    String name = entry.getName();
    return !entry.isDirectory() && name.startsWith(EXECUTABLE_JAR_FOLDER) && name.endsWith(".jar");
  }
}
