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
package org.sonar.core.extension;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.api.utils.AnnotationUtils;

public final class PlatformLevelPredicates {
  private PlatformLevelPredicates() {
    // prevents instantiation
  }

  public static Predicate<Object> hasPlatformLevel(int i) {
    checkSupportedLevel(i, null);
    return o -> {
      PlatformLevel platformLevel = AnnotationUtils.getAnnotation(o, PlatformLevel.class);
      return platformLevel != null && checkSupportedLevel(platformLevel.value(), o) == i;
    };
  }

  private static int checkSupportedLevel(int i, @Nullable Object annotatedObject) {
    boolean supported = i >= 1 && i <= 4;
    if (supported) {
      return i;
    }

    throw new IllegalArgumentException(buildErrorMsgFrom(annotatedObject));
  }

  private static String buildErrorMsgFrom(@Nullable Object annotatedObject) {
    String baseErrorMsg = "Only level 1, 2, 3 and 4 are supported";
    if (annotatedObject == null) {
      return baseErrorMsg;
    } else if (annotatedObject instanceof Class) {
      return String.format("Invalid value for annotation %s on class '%s'. %s",
        PlatformLevel.class.getName(), ((Class) annotatedObject).getName(),
        baseErrorMsg);
    } else {
      return String.format("Invalid value for annotation %s on object of type %s. %s",
        PlatformLevel.class.getName(), annotatedObject.getClass().getName(), baseErrorMsg);
    }
  }

  public static Predicate<Object> hasPlatformLevel4OrNone() {
    return o -> {
      PlatformLevel platformLevel = AnnotationUtils.getAnnotation(o, PlatformLevel.class);
      return platformLevel == null || checkSupportedLevel(platformLevel.value(), o) == 4;
    };
  }
}
