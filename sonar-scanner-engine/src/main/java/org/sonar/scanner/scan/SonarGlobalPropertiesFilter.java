/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.scan;

import com.google.common.annotations.VisibleForTesting;
import java.util.Map;
import java.util.stream.Collectors;

public class SonarGlobalPropertiesFilter {

  @VisibleForTesting
  static final String SONAR_GLOBAL_PROPERTIES_PREFIX = "sonar.global.";

  public Map<String, String> enforceOnlyServerSideSonarGlobalPropertiesAreUsed(Map<String, String> settingProperties, Map<String, String> globalServerSettingsProperties) {
    Map<String, String> settings = getNonSonarGlobalProperties(settingProperties);
    settings.putAll(getSonarGlobalProperties(globalServerSettingsProperties));
    return settings;
  }


  private static Map<String, String> getNonSonarGlobalProperties(Map<String, String> settingProperties) {
    return settingProperties.entrySet()
      .stream()
      .filter(entry -> !isSonarGlobalProperty(entry.getKey()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Map<String, String> getSonarGlobalProperties(Map<String, String> properties) {
    return properties
      .entrySet()
      .stream()
      .filter(entry -> isSonarGlobalProperty(entry.getKey()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static boolean isSonarGlobalProperty(String propertiesCode) {
    return propertiesCode.startsWith(SONAR_GLOBAL_PROPERTIES_PREFIX);
  }

}
