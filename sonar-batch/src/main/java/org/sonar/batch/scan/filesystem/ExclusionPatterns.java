/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch.scan.filesystem;

import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.WildcardPattern;

import java.util.List;

/**
 * Get configuration of file inclusions and exclusions
 *
 * @since 3.5
 */
class ExclusionPatterns {
  static WildcardPattern[] sourceInclusions(Settings settings) {
    return inclusions(settings, CoreProperties.PROJECT_INCLUSIONS_PROPERTY);
  }

  static WildcardPattern[] testInclusions(Settings settings) {
    return inclusions(settings, CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY);
  }

  private static WildcardPattern[] inclusions(Settings settings, String propertyKey) {
    String[] patterns = sanitize(settings.getStringArray(propertyKey));
    List<WildcardPattern> list = Lists.newArrayList();
    for (String pattern : patterns) {
      if (!"**/*".equals(pattern)) {
        list.add(WildcardPattern.create(pattern));
      }
    }
    return list.toArray(new WildcardPattern[list.size()]);
  }

  static WildcardPattern[] sourceExclusions(Settings settings) {
    return exclusions(settings, CoreProperties.GLOBAL_EXCLUSIONS_PROPERTY, CoreProperties.PROJECT_EXCLUSIONS_PROPERTY);
  }

  static WildcardPattern[] testExclusions(Settings settings) {
    return exclusions(settings, CoreProperties.GLOBAL_TEST_EXCLUSIONS_PROPERTY, CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY);
  }

  private static WildcardPattern[] exclusions(Settings settings, String globalExclusionsProperty, String exclusionsProperty) {
    String[] globalExclusions = settings.getStringArray(globalExclusionsProperty);
    String[] exclusions = settings.getStringArray(exclusionsProperty);
    String[] patterns = sanitize(ObjectArrays.concat(globalExclusions, exclusions, String.class));
    return WildcardPattern.create(patterns);
  }

  private static String[] sanitize(String[] patterns) {
    for (int i = 0; i < patterns.length; i++) {
      patterns[i] = StringUtils.trim(patterns[i]);
    }
    return patterns;
  }
}
