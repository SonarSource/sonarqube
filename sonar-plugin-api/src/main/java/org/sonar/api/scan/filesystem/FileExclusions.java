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
package org.sonar.api.scan.filesystem;

import com.google.common.collect.ObjectArrays;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchSide;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration of file inclusions and exclusions.
 * <p>Plugins must not extend nor instantiate this class. An instance is injected at
 * runtime.</p>
 *
 * @since 3.5
 */
@BatchSide
public class FileExclusions {
  private final Settings settings;

  public FileExclusions(Settings settings) {
    this.settings = settings;
  }

  public String[] sourceInclusions() {
    return inclusions(CoreProperties.PROJECT_INCLUSIONS_PROPERTY);
  }

  public String[] testInclusions() {
    return inclusions(CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY);
  }

  private String[] inclusions(String propertyKey) {
    String[] patterns = sanitize(settings.getStringArray(propertyKey));
    List<String> list = new ArrayList<>();
    for (String pattern : patterns) {
      if (!"**/*".equals(pattern) && !"file:**/*".equals(pattern)) {
        list.add(pattern);
      }
    }
    return list.toArray(new String[list.size()]);
  }

  public String[] sourceExclusions() {
    return exclusions(CoreProperties.GLOBAL_EXCLUSIONS_PROPERTY, CoreProperties.PROJECT_EXCLUSIONS_PROPERTY);
  }

  public String[] testExclusions() {
    return exclusions(CoreProperties.GLOBAL_TEST_EXCLUSIONS_PROPERTY, CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY);
  }

  private String[] exclusions(String globalExclusionsProperty, String exclusionsProperty) {
    String[] globalExclusions = settings.getStringArray(globalExclusionsProperty);
    String[] exclusions = settings.getStringArray(exclusionsProperty);
    return sanitize(ObjectArrays.concat(globalExclusions, exclusions, String.class));
  }

  private static String[] sanitize(String[] patterns) {
    for (int i = 0; i < patterns.length; i++) {
      patterns[i] = StringUtils.trim(patterns[i]);
    }
    return patterns;
  }
}
