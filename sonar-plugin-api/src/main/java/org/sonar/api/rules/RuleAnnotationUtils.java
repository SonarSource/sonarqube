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
package org.sonar.api.rules;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Check;

/**
 * @since 2.3
 */
public final class RuleAnnotationUtils {

  private RuleAnnotationUtils() {
    // only static methods
  }

  public static String getRuleKey(Class annotatedClass) {
    String key = null;
    org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getClassAnnotation(annotatedClass, org.sonar.check.Rule.class);
    if (ruleAnnotation != null) {
      key = ruleAnnotation.key();
    } else {
      Check checkAnnotation = AnnotationUtils.getClassAnnotation(annotatedClass, Check.class);
      if (checkAnnotation != null) {
        key = checkAnnotation.key();
      }
    }
    return StringUtils.defaultIfEmpty(key, annotatedClass.getCanonicalName());
  }
}
