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
package org.sonar.api.scan.filesystem;

import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.config.Configuration;

/**
 * Configuration of file inclusions and exclusions.
 * <p>Plugins must not extend nor instantiate this class. An instance is injected at
 * runtime.
 *
 * @since 3.5
 */
@ScannerSide
public class FileExclusions {
  private final Configuration settings;

  public FileExclusions(Configuration settings) {
    this.settings = settings;
  }

  public String[] sourceInclusions() {
    return inclusions(CoreProperties.PROJECT_INCLUSIONS_PROPERTY);
  }

  public String[] testInclusions() {
    return inclusions(CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY);
  }

  private String[] inclusions(String propertyKey) {
    return Arrays.stream(settings.getStringArray(propertyKey))
      .map(StringUtils::trim)
      .filter(s -> !"**/*".equals(s))
      .filter(s -> !"file:**/*".equals(s))
      .toArray(String[]::new);
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
    return Stream.concat(Arrays.stream(globalExclusions), Arrays.stream(exclusions))
      .map(StringUtils::trim)
      .toArray(String[]::new);
  }
}
