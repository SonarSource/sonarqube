/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import com.google.gson.Gson;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

public class EnvironmentConfig {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EnvironmentConfig.class);

  private static final String SONAR_SCANNER_JSON_PARAMS = "SONAR_SCANNER_JSON_PARAMS";
  private static final String SONARQUBE_SCANNER_PARAMS = "SONARQUBE_SCANNER_PARAMS";
  private static final String GENERIC_ENV_PREFIX = "SONAR_SCANNER_";

  private EnvironmentConfig() {
    // only static methods
  }

  public static void processEnvVariables(Map<String, String> inputProperties) {
    processEnvVariables(inputProperties, System.getenv());
  }

  static void processEnvVariables(Map<String, String> inputProperties, Map<String, String> env) {
    env.forEach((key, value) -> {
      if (!key.equals(SONAR_SCANNER_JSON_PARAMS) && key.startsWith(GENERIC_ENV_PREFIX)) {
        processEnvVariable(key, value, inputProperties);
      }
    });
    var jsonParams = env.get(SONAR_SCANNER_JSON_PARAMS);
    var oldJsonParams = env.get(SONARQUBE_SCANNER_PARAMS);
    if (jsonParams != null) {
      if (oldJsonParams != null && !oldJsonParams.equals(jsonParams)) {
        LOG.warn("Ignoring environment variable '{}' because '{}' is set", SONARQUBE_SCANNER_PARAMS, SONAR_SCANNER_JSON_PARAMS);
      }
      parseJsonPropertiesFromEnv(jsonParams, inputProperties, SONAR_SCANNER_JSON_PARAMS);
    } else if (oldJsonParams != null) {
      parseJsonPropertiesFromEnv(oldJsonParams, inputProperties, SONARQUBE_SCANNER_PARAMS);
    }
  }

  private static void parseJsonPropertiesFromEnv(String jsonParams, Map<String, String> inputProperties, String envVariableName) {
    try {
      var jsonProperties = new Gson().<Map<String, String>>fromJson(jsonParams, Map.class);
      if (jsonProperties != null) {
        jsonProperties.forEach((key, value) -> {
          if (inputProperties.containsKey(key)) {
            if (!inputProperties.get(key).equals(value)) {
              LOG.warn("Ignoring property '{}' from env variable '{}' because it is already defined", key, envVariableName);
            }
          } else {
            inputProperties.put(key, value);
          }
        });
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse JSON properties from environment variable '" + envVariableName + "'", e);
    }
  }

  private static void processEnvVariable(String key, String value, Map<String, String> inputProperties) {
    var suffix = key.substring(GENERIC_ENV_PREFIX.length());
    if (suffix.isEmpty()) {
      return;
    }
    var toCamelCase = Stream.of(suffix.split("_"))
      .map(String::toLowerCase)
      .reduce((a, b) -> a + StringUtils.capitalize(b)).orElseThrow();
    var propKey = "sonar.scanner." + toCamelCase;
    if (inputProperties.containsKey(propKey)) {
      if (!inputProperties.get(propKey).equals(value)) {
        LOG.warn("Ignoring environment variable '{}' because it is already defined in the properties", key);
      }
    } else {
      inputProperties.put(propKey, value);
    }
  }

}
