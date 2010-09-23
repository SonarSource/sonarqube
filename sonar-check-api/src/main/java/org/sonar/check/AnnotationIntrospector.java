/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.check;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated
public final class AnnotationIntrospector {

  private AnnotationIntrospector() {
    // only static methods
  }

  public static String getCheckKey(Class annotatedClass) {
    Check checkAnnotation = getCheckAnnotation(annotatedClass);
    if (checkAnnotation == null) {
      return null;
    }

    String key = checkAnnotation.key();
    if (key == null || "".equals(key.trim())) {
      key = annotatedClass.getCanonicalName();
    }
    return key;
  }

  public static Check getCheckAnnotation(Class annotatedClass) {
    return (Check) annotatedClass.getAnnotation(Check.class);
  }

  public static List<Field> getPropertyFields(Class annotatedClass) {
    List<Field> fields = new ArrayList<Field>();
    for (Field field : annotatedClass.getDeclaredFields()) {
      org.sonar.check.CheckProperty propertyAnnotation = field.getAnnotation(org.sonar.check.CheckProperty.class);
      if (propertyAnnotation != null) {
        fields.add(field);
      }
    }
    return fields;
  }

  public static String getPropertyFieldKey(Field field) {
    String key = null;
    org.sonar.check.CheckProperty propertyAnnotation = field.getAnnotation(org.sonar.check.CheckProperty.class);
    if (propertyAnnotation != null) {
      key = propertyAnnotation.key();
      if (key == null || "".equals(key)) {
        key = field.getName();
      }
    }
    return key;
  }
}
