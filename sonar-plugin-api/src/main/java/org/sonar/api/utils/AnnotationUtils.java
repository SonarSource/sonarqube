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

import org.apache.commons.lang.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * A utility class for annotations
 *
 * @since 1.11
 */
public final class AnnotationUtils {

  private AnnotationUtils() {
  }

  /**
   * Searches for a class annotation. All inheritance tree is analysed.
   * 
   * @since 3.1
   */
  public static <A extends Annotation> A getAnnotation(Object objectOrClass, Class<A> annotationClass) {
    Class<?> initialClass = objectOrClass instanceof Class<?> ? (Class<?>) objectOrClass : objectOrClass.getClass();
    
    for (Class<?> aClass = initialClass; aClass != null; aClass = aClass.getSuperclass()) {
      A result = aClass.getAnnotation(annotationClass);
      if (result != null) {
        return result;
      }
    }

    for (Class<?> anInterface : (List<Class<?>>) ClassUtils.getAllInterfaces(initialClass)) {
      A result = anInterface.getAnnotation(annotationClass);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  /**
   * Searches for a class annotation. All inheritance tree is analysed.
   * 
   * @deprecated  As of 3.1, replaced by {@link #getAnnotation(Object,Class)}
   */
  @Deprecated
  public static <A> A getClassAnnotation(Object object, Class<A> annotationClass) {
    return (A) getAnnotation(object, (Class<? extends Annotation>) annotationClass);
  }
}
