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
package org.sonar.process;

import java.util.Map;
import org.apache.commons.lang.SystemUtils;

/**
 * An interface allowing to wrap around static call to {@link System} class.
 */
public interface System2 {
  System2 INSTANCE = new System2() {
    @Override
    public Map<String, String> getenv() {
      return System.getenv();
    }

    @Override
    public String getenv(String name) {
      return System.getenv(name);
    }

    @Override
    public boolean isOsWindows() {
      return SystemUtils.IS_OS_WINDOWS;
    }
  };

  /**
   * Proxy to {@link System#getenv()}
   */
  Map<String, String> getenv();

  /**
   * Proxy to {@link System#getenv(String)}.
   */
  String getenv(String name);

  /**
   * True if this is MS Windows.
   */
  boolean isOsWindows();
}
