/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.sca;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.scanner.config.DefaultConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScaPropertiesTest {
  private final DefaultConfiguration configuration = mock(DefaultConfiguration.class);

  @Test
  void buildFromScannerProperties_withNoProperties_returnsEmptyMap() {
    when(configuration.get(anyString())).thenReturn(Optional.empty());

    var result = ScaProperties.buildFromScannerProperties(configuration);

    assertThat(result).isEqualTo(Map.of());
  }

  @Test
  void buildFromScannerProperties_withUnmappedProperties_ignoresProperties() {
    var inputProperties = new HashMap<String, String>();
    inputProperties.put("sonar.sca.pythonBinary", "/usr/bin/python3");
    inputProperties.put("sonar.sca.unknownProperty", "value");
    inputProperties.put("sonar.somethingElse", "dont-include-non-sca");
    inputProperties.put("sonar.sca.recursiveManifestSearch", "ignore-me");
    when(configuration.getProperties()).thenReturn(inputProperties);
    when(configuration.get(anyString())).thenAnswer(i -> Optional.ofNullable(inputProperties.get(i.getArgument(0, String.class))));

    var result = ScaProperties.buildFromScannerProperties(configuration);

    assertThat(result).containsExactly(
      Map.entry("TIDELIFT_PYTHON_BINARY", "/usr/bin/python3"),
      Map.entry("TIDELIFT_UNKNOWN_PROPERTY", "value"));
  }

  @Test
  void buildFromScannerProperties_withLotsOfProperties_mapsAllProperties() {
    var inputProperties = new HashMap<String, String>();
    inputProperties.put("sonar.sca.goNoResolve", "true");
    inputProperties.put("sonar.sca.gradleConfigurationPattern", "pattern");
    inputProperties.put("sonar.sca.gradleNoResolve", "false");
    inputProperties.put("sonar.sca.mavenForceDepPlugin", "plugin");
    inputProperties.put("sonar.sca.mavenNoResolve", "true");
    inputProperties.put("sonar.sca.mavenIgnoreWrapper", "false");
    inputProperties.put("sonar.sca.mavenOptions", "-DskipTests");
    inputProperties.put("sonar.sca.npmEnableScripts", "true");
    inputProperties.put("sonar.sca.npmNoResolve", "true");
    inputProperties.put("sonar.sca.nugetNoResolve", "false");
    inputProperties.put("sonar.sca.pythonBinary", "/usr/bin/python3");
    inputProperties.put("sonar.sca.pythonNoResolve", "true");
    inputProperties.put("sonar.sca.pythonResolveLocal", "false");
    when(configuration.getProperties()).thenReturn(inputProperties);
    when(configuration.get(anyString())).thenAnswer(i -> Optional.ofNullable(inputProperties.get(i.getArgument(0, String.class))));

    var expectedProperties = new HashMap<String, String>();
    expectedProperties.put("TIDELIFT_GO_NO_RESOLVE", "true");
    expectedProperties.put("TIDELIFT_GRADLE_CONFIGURATION_PATTERN", "pattern");
    expectedProperties.put("TIDELIFT_GRADLE_NO_RESOLVE", "false");
    expectedProperties.put("TIDELIFT_MAVEN_FORCE_DEP_PLUGIN", "plugin");
    expectedProperties.put("TIDELIFT_MAVEN_NO_RESOLVE", "true");
    expectedProperties.put("TIDELIFT_MAVEN_IGNORE_WRAPPER", "false");
    expectedProperties.put("TIDELIFT_MAVEN_OPTIONS", "-DskipTests");
    expectedProperties.put("TIDELIFT_NPM_ENABLE_SCRIPTS", "true");
    expectedProperties.put("TIDELIFT_NPM_NO_RESOLVE", "true");
    expectedProperties.put("TIDELIFT_NUGET_NO_RESOLVE", "false");
    expectedProperties.put("TIDELIFT_PYTHON_BINARY", "/usr/bin/python3");
    expectedProperties.put("TIDELIFT_PYTHON_NO_RESOLVE", "true");
    expectedProperties.put("TIDELIFT_PYTHON_RESOLVE_LOCAL", "false");

    var result = ScaProperties.buildFromScannerProperties(configuration);

    assertThat(result).containsExactlyInAnyOrderEntriesOf(expectedProperties);
  }
}
