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

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.scanner.config.DefaultConfiguration;

public class ScaProperties {
  private static final Pattern sonarScaPropertyRegex = Pattern.compile("^sonar\\.sca\\.([a-zA-Z]+)$");
  private static final String SONAR_SCA_PREFIX = "sonar.sca.";
  private static final Set<String> IGNORED_PROPERTIES = Set.of(
    // sonar.sca.exclusions is a special case which we handle when building --exclude
    "sonar.sca.exclusions",
    // excludedManifests is a special case which we handle when building --exclude
    "sonar.sca.excludedManifests",
    // keep recursive enabled to better match sonar-scanner behavior
    "sonar.sca.recursiveManifestSearch");

  private ScaProperties() {
  }

  /**
   * Build a map of environment variables from the sonar.sca.* properties in the configuration.
   * The environment variable names are derived from the property names by removing the sonar.sca. prefix
   * and converting to upper snake case to be used with the Tidelift CLI with the value from the configuration.
   * <p>
   * Examples:
   * <br>
   * { "sonar.sca.propertyName" : "value" } becomes { "TIDELIFT_PROPERTY_NAME" : "value" }
   * <br>
   * { "sonar.someOtherProperty" : "value" } returns an empty map
   *
   * @param configuration the scanner configuration possibly containing sonar.sca.* properties
   * @return a map of Tidelift CLI compatible environment variable names to their configuration values
   */
  public static Map<String, String> buildFromScannerProperties(DefaultConfiguration configuration) {
    HashMap<String, String> props = new HashMap<>(configuration.getProperties());

    return props
      .entrySet()
      .stream()
      .filter(entry -> entry.getKey().startsWith(SONAR_SCA_PREFIX))
      .filter(entry -> !IGNORED_PROPERTIES.contains(entry.getKey()))
      .collect(Collectors.toMap(entry -> convertPropToEnvVariable(entry.getKey()), Map.Entry::getValue));
  }

  // convert sonar.sca.* to TIDELIFT_* and convert from camelCase to UPPER_SNAKE_CASE
  private static String convertPropToEnvVariable(String propertyName) {
    var regexMatcher = sonarScaPropertyRegex.matcher(propertyName);

    if (regexMatcher.matches() && regexMatcher.groupCount() == 1) {
      var tideliftNamespace = "TIDELIFT_";
      var convertedPropertyName = PropertyNamingStrategies.UpperSnakeCaseStrategy.INSTANCE.translate(regexMatcher.group(1));

      return tideliftNamespace + convertedPropertyName;
    }

    return propertyName;
  }
}
