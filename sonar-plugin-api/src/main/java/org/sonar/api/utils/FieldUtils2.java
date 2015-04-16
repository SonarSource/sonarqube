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
package org.sonar.api.utils;

import org.apache.commons.lang.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Add features missing in org.apache.commons.lang.reflect.FieldUtils
 *
 * @since 2.14
 */
public final class FieldUtils2 {
  private FieldUtils2() {
  }

  /**
   * Get accessible <code>Field</code> breaking scope if requested. Superclasses/interfaces are considered.
   *
   * @param clazz       the class to reflect, must not be null
   * @param forceAccess whether to break scope restrictions using the <code>setAccessible</code> method.
   *                    <code>False</code> only matches public fields.
   */
  public static List<Field> getFields(Class clazz, boolean forceAccess) {
    List<Field> result = new ArrayList<>();
    Class c = clazz;
    while (c != null) {
      for (Field declaredField : c.getDeclaredFields()) {
        if (!Modifier.isPublic(declaredField.getModifiers())) {
          if (forceAccess) {
            declaredField.setAccessible(true);// NOSONAR only works from sufficiently privileged code
          } else {
            continue;
          }
        }
        result.add(declaredField);
      }
      c = c.getSuperclass();
    }

    for (Object anInterface : ClassUtils.getAllInterfaces(clazz)) {
      Collections.addAll(result, ((Class) anInterface).getDeclaredFields());
    }

    return result;
  }
}
