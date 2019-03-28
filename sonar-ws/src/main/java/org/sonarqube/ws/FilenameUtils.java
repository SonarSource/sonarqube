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
package org.sonarqube.ws;

import static java.util.Objects.requireNonNull;

/**
 * Extracted from org.apache.commons.io.FilenameUtils
 */
public class FilenameUtils {

  private static final int NOT_FOUND = -1;

  private static final char EXTENSION_SEPARATOR = '.';
  /**
   * The Unix separator character.
   */
  private static final char UNIX_SEPARATOR = '/';

  /**
   * The Windows separator character.
   */
  private static final char WINDOWS_SEPARATOR = '\\';

  private FilenameUtils() {
    // Only static methods here
  }

  public static String getExtension(String filename) {
    requireNonNull(filename);
    final int index = indexOfExtension(filename);
    if (index == NOT_FOUND) {
      return "";
    } else {
      return filename.substring(index + 1);
    }
  }

  private static int indexOfExtension(String filename) {
    final int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
    final int lastSeparator = indexOfLastSeparator(filename);
    return lastSeparator > extensionPos ? NOT_FOUND : extensionPos;
  }

  private static int indexOfLastSeparator(String filename) {
    final int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
    final int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
    return Math.max(lastUnixPos, lastWindowsPos);
  }
}
