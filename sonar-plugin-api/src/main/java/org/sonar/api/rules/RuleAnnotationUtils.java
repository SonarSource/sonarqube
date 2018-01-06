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
package org.sonar.api.rules;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.AnnotationUtils;

/**
 * @since 2.3
 */
public final class RuleAnnotationUtils {

  private RuleAnnotationUtils() {
    // only static methods
  }

  public static String getRuleKey(Class annotatedClass) {
    String key = null;
    org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(annotatedClass, org.sonar.check.Rule.class);
    if (ruleAnnotation != null) {
      key = ruleAnnotation.key();
    }
    return StringUtils.defaultIfEmpty(key, annotatedClass.getCanonicalName());
  }
}
