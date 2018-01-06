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
package util.selenium;

import java.util.ArrayList;
import java.util.List;

class Failure {
  private static final String PREFIX = Failure.class.getPackage().getName() + ".";

  private Failure() {
    // Static class
  }

  public static AssertionError create(String message) {
    AssertionError error = new AssertionError(message);
    removeSimpleleniumFromStackTrace(error);
    return error;
  }

  private static void removeSimpleleniumFromStackTrace(Throwable throwable) {
    List<StackTraceElement> filtered = new ArrayList<>();

    for (StackTraceElement element : throwable.getStackTrace()) {
      if (!element.getClassName().contains(PREFIX)) {
        filtered.add(element);
      }
    }

    throwable.setStackTrace(filtered.toArray(new StackTraceElement[filtered.size()]));
  }
}
