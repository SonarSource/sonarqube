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

import java.util.Map;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.scanner.scan.SonarGlobalPropertiesFilter.SONAR_GLOBAL_PROPERTIES_PREFIX;

public class SonarGlobalPropertiesFilterTest {

  private static final String SONAR_GLOBAL_KEY_1 = SONAR_GLOBAL_PROPERTIES_PREFIX + "key1";
  private static final String SONAR_GLOBAL_VALUE_1 = "value for " + SONAR_GLOBAL_KEY_1;
  private static final String SONAR_GLOBAL_KEY_2 = SONAR_GLOBAL_PROPERTIES_PREFIX + "key2";
  private static final String SONAR_GLOBAL_KEY_3 = SONAR_GLOBAL_PROPERTIES_PREFIX + "key3";
  private static final String SONAR_GLOBAL_VALUE_3 = "value for " + SONAR_GLOBAL_KEY_3;

  private static final String SONAR_NON_GLOBAL_KEY_4 = "sonar.key4";
  private static final String SONAR_NON_GLOBAL_VALUE_4 = "value for " + SONAR_NON_GLOBAL_KEY_4;
  private static final String ANOTHER_KEY = "another key";
  private static final String ANOTHER_VALUE = "another value";

  private final SonarGlobalPropertiesFilter sonarGlobalPropertiesFilter = new SonarGlobalPropertiesFilter();

  @Test
  public void should_filterSonarGlobalProperties() {
    Map<String, String> settingProperties = Map.of(
      SONAR_GLOBAL_KEY_1, "shouldBeOverride",
      SONAR_GLOBAL_KEY_2, "shouldBeRemove",
      SONAR_NON_GLOBAL_KEY_4, SONAR_NON_GLOBAL_VALUE_4,
      ANOTHER_KEY, ANOTHER_VALUE);

    Map<String, String> globalServerSettingsProperties = Map.of(
      SONAR_GLOBAL_KEY_1, SONAR_GLOBAL_VALUE_1,
      SONAR_GLOBAL_KEY_3, SONAR_GLOBAL_VALUE_3,
      SONAR_NON_GLOBAL_KEY_4, "shouldBeIgnored"
    );

    Map<String, String> properties = sonarGlobalPropertiesFilter.enforceOnlyServerSideSonarGlobalPropertiesAreUsed(settingProperties, globalServerSettingsProperties);

    assertThat(properties).hasSize(4)
      .containsEntry(SONAR_GLOBAL_KEY_1, SONAR_GLOBAL_VALUE_1)
      .containsEntry(SONAR_GLOBAL_KEY_3, SONAR_GLOBAL_VALUE_3)
      .containsEntry(SONAR_NON_GLOBAL_KEY_4, SONAR_NON_GLOBAL_VALUE_4)
      .containsEntry(ANOTHER_KEY, ANOTHER_VALUE);


  }
}