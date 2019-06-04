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
package org.sonar.api.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Utilities for unit tests
 *
 * @since 2.2
 */
public final class TestUtils {

  private TestUtils() {
  }

  /**
   * Asserts that all constructors are private, usually for helper classes with
   * only static methods. If a constructor does not have any parameters, then
   * it's instantiated.
   */
  public static boolean hasOnlyPrivateConstructors(Class clazz) {
    boolean ok = true;
    for (Constructor constructor : clazz.getDeclaredConstructors()) {
      ok &= Modifier.isPrivate(constructor.getModifiers());
      if (constructor.getParameterTypes().length == 0) {
        constructor.setAccessible(true);
        try {
          constructor.newInstance();
        } catch (Exception e) {
          throw new IllegalStateException(String.format("Fail to instantiate %s", clazz), e);
        }
      }
    }
    return ok;
  }
}
