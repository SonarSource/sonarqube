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

import java.util.Arrays;

/**
 * The on-disk packaging of a plugin: a plain {@code .jar}, or a {@code .zip} containing
 * {@code resources/} (arbitrary static resources) and {@code plugin/*.jar} (the scanner-side jar).
 */
public enum PluginPackaging {
  JAR("jar"),
  ZIP("zip");

  private static final String[] FILE_EXTENSIONS = Arrays.stream(values())
    .map(PluginPackaging::getFileExtension)
    .toArray(String[]::new);

  private final String fileExtension;

  PluginPackaging(String fileExtension) {
    this.fileExtension = fileExtension;
  }

  public String getFileExtension() {
    return fileExtension;
  }

  /**
   * The file extensions (without the leading dot) of all recognized plugin packagings, e.g. for
   * {@link org.apache.commons.io.FileUtils#listFiles(java.io.File, String[], boolean)}.
   */
  public static String[] fileExtensions() {
    return FILE_EXTENSIONS.clone();
  }
}
