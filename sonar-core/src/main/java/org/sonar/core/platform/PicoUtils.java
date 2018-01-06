/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.base.Throwables;
import org.picocontainer.PicoLifecycleException;

class PicoUtils {

  private PicoUtils() {
    // only static methods
  }

  static Throwable sanitize(Throwable t) {
    Throwable result = t;
    Throwable cause = t.getCause();
    if (t instanceof PicoLifecycleException && cause != null) {
      if ("wrapper".equals(cause.getMessage()) && cause.getCause() != null) {
        result = cause.getCause();
      } else {
        result = cause;
      }
    }
    return result;
  }

  static RuntimeException propagate(Throwable t) {
    throw Throwables.propagate(sanitize(t));
  }
}
