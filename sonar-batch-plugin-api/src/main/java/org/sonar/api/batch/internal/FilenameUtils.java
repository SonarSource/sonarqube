/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.api.internal;

import java.io.File;

/**
 * Copied from commons io
 *
 */
public class FilenameUtils {

  public static final char EXTENSION_SEPARATOR = '.';

  private static final char UNIX_SEPARATOR = '/';

  /**
   * The Windows separator character.
   */
  private static final char WINDOWS_SEPARATOR = '\\';

  /**
   * The system separator character.
   */
  private static final char SYSTEM_SEPARATOR = File.separatorChar;

  /**
   * The separator character that is the opposite of the system separator.
   */
  private static final char OTHER_SEPARATOR;
  static {
    if (isSystemWindows()) {
      OTHER_SEPARATOR = UNIX_SEPARATOR;
    } else {
      OTHER_SEPARATOR = WINDOWS_SEPARATOR;
    }
  }

  static boolean isSystemWindows() {
    return SYSTEM_SEPARATOR == WINDOWS_SEPARATOR;
  }

  public static String normalize(String filename) {
    return doNormalize(filename, UNIX_SEPARATOR, true);
  }

  private static String doNormalize(String filename, char separator, boolean keepSeparator) {
    if (filename == null) {
      return null;
    }
    int size = filename.length();
    if (size == 0) {
      return filename;
    }
    int prefix = getPrefixLength(filename);
    if (prefix < 0) {
      return null;
    }

    char[] array = new char[size + 2]; // +1 for possible extra slash, +2 for arraycopy
    filename.getChars(0, filename.length(), array, 0);

    // fix separators throughout
    char otherSeparator = separator == SYSTEM_SEPARATOR ? OTHER_SEPARATOR : SYSTEM_SEPARATOR;
    for (int i = 0; i < array.length; i++) {
      if (array[i] == otherSeparator) {
        array[i] = separator;
      }
    }

    // add extra separator on the end to simplify code below
    boolean lastIsDirectory = true;
    if (array[size - 1] != separator) {
      array[size++] = separator;
      lastIsDirectory = false;
    }

    // adjoining slashes
    for (int i = prefix + 1; i < size; i++) {
      if (array[i] == separator && array[i - 1] == separator) {
        System.arraycopy(array, i, array, i - 1, size - i);
        size--;
        i--;
      }
    }

    // dot slash
    for (int i = prefix + 1; i < size; i++) {
      if (array[i] == separator && array[i - 1] == '.' &&
        (i == prefix + 1 || array[i - 2] == separator)) {
        if (i == size - 1) {
          lastIsDirectory = true;
        }
        System.arraycopy(array, i + 1, array, i - 1, size - i);
        size -= 2;
        i--;
      }
    }

    // double dot slash
    outer: for (int i = prefix + 2; i < size; i++) {
      if (array[i] == separator && array[i - 1] == '.' && array[i - 2] == '.' &&
        (i == prefix + 2 || array[i - 3] == separator)) {
        if (i == prefix + 2) {
          return null;
        }
        if (i == size - 1) {
          lastIsDirectory = true;
        }
        int j;
        for (j = i - 4; j >= prefix; j--) {
          if (array[j] == separator) {
            // remove b/../ from a/b/../c
            System.arraycopy(array, i + 1, array, j + 1, size - i);
            size -= i - j;
            i = j + 1;
            continue outer;
          }
        }
        // remove a/../ from a/../c
        System.arraycopy(array, i + 1, array, prefix, size - i);
        size -= i + 1 - prefix;
        i = prefix + 1;
      }
    }

    if (size <= 0) { // should never be less than 0
      return "";
    }
    if (size <= prefix) { // should never be less than prefix
      return new String(array, 0, size);
    }
    if (lastIsDirectory && keepSeparator) {
      return new String(array, 0, size); // keep trailing separator
    }
    return new String(array, 0, size - 1); // lose trailing separator
  }

  public static int getPrefixLength(String filename) {
    if (filename == null) {
      return -1;
    }
    int len = filename.length();
    if (len == 0) {
      return 0;
    }
    char ch0 = filename.charAt(0);
    if (ch0 == ':') {
      return -1;
    }
    if (len == 1) {
      if (ch0 == '~') {
        return 2; // return a length greater than the input
      }
      return isSeparator(ch0) ? 1 : 0;
    } else {
      if (ch0 == '~') {
        int posUnix = filename.indexOf(UNIX_SEPARATOR, 1);
        int posWin = filename.indexOf(WINDOWS_SEPARATOR, 1);
        if (posUnix == -1 && posWin == -1) {
          return len + 1; // return a length greater than the input
        }
        posUnix = posUnix == -1 ? posWin : posUnix;
        posWin = posWin == -1 ? posUnix : posWin;
        return Math.min(posUnix, posWin) + 1;
      }
      char ch1 = filename.charAt(1);
      if (ch1 == ':') {
        ch0 = Character.toUpperCase(ch0);
        if (ch0 >= 'A' && ch0 <= 'Z') {
          if (len == 2 || isSeparator(filename.charAt(2)) == false) {
            return 2;
          }
          return 3;
        }
        return -1;

      } else if (isSeparator(ch0) && isSeparator(ch1)) {
        int posUnix = filename.indexOf(UNIX_SEPARATOR, 2);
        int posWin = filename.indexOf(WINDOWS_SEPARATOR, 2);
        if (posUnix == -1 && posWin == -1 || posUnix == 2 || posWin == 2) {
          return -1;
        }
        posUnix = posUnix == -1 ? posWin : posUnix;
        posWin = posWin == -1 ? posUnix : posWin;
        return Math.min(posUnix, posWin) + 1;
      } else {
        return isSeparator(ch0) ? 1 : 0;
      }
    }
  }

  private static boolean isSeparator(char ch) {
    return ch == UNIX_SEPARATOR || ch == WINDOWS_SEPARATOR;
  }

  public static String getExtension(String filename) {
    if (filename == null) {
      return null;
    }
    int index = indexOfExtension(filename);
    if (index == -1) {
      return "";
    } else {
      return filename.substring(index + 1);
    }
  }

  public static int indexOfExtension(String filename) {
    if (filename == null) {
      return -1;
    }
    int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
    int lastSeparator = indexOfLastSeparator(filename);
    return lastSeparator > extensionPos ? -1 : extensionPos;
  }

  public static int indexOfLastSeparator(String filename) {
    if (filename == null) {
      return -1;
    }
    int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
    int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
    return Math.max(lastUnixPos, lastWindowsPos);
  }

}
