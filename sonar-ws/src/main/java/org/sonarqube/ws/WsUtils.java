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

import javax.annotation.Nullable;

import static java.lang.String.format;

public class WsUtils {

  private WsUtils() {
    // Only static methods here
  }

  public static void checkArgument(boolean expression) {
    if (!expression) {
      throw new IllegalArgumentException();
    }
  }

  public static void checkArgument(boolean expression, String message, Object... args) {
    if (!expression) {
      throw new IllegalArgumentException(format(message, args));
    }
  }

  public static boolean isNullOrEmpty(@Nullable String string) {
    return string == null || string.length() == 0;
  }

  public static String nullToEmpty(@Nullable String string) {
    return string == null ? "" : string;
  }

}
