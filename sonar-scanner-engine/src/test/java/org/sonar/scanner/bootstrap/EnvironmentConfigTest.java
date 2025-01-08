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
package org.sonar.scanner.bootstrap;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

class EnvironmentConfigTest {

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void shouldProcessGenericEnvVariables() {
    var inputProperties = new HashMap<String, String>();
    EnvironmentConfig.processEnvVariables(inputProperties,
      Map.of("SONAR_SCANNER", "ignored",
        "SONAR_SCANNER_", "ignored as well",
        "SONAR_SCANNER_FOO", "bar",
        "SONAR_SCANNER_FOO_BAZ", "bar",
        "SONAR_SCANNER_fuZz_bAz", "env vars are case insensitive"));

    assertThat(inputProperties).containsOnly(
      entry("sonar.scanner.foo", "bar"),
      entry("sonar.scanner.fooBaz", "bar"),
      entry("sonar.scanner.fuzzBaz", "env vars are case insensitive"));
  }

  @Test
  void genericEnvVarShouldNotOverrideInputProperties() {
    var inputProperties = new HashMap<String, String>(Map.of("sonar.scanner.foo", "foo", "sonar.scanner.bar", "same value"));
    EnvironmentConfig.processEnvVariables(inputProperties,
      Map.of(
        "SONAR_SCANNER_FOO", "should not override",
        "SONAR_SCANNER_BAR", "same value",
        "SONAR_SCANNER_BAZ", "baz"));

    assertThat(inputProperties).containsOnly(
      entry("sonar.scanner.foo", "foo"),
      entry("sonar.scanner.bar", "same value"),
      entry("sonar.scanner.baz", "baz"));

    assertThat(logTester.logs(Level.WARN)).containsOnly("Ignoring environment variable 'SONAR_SCANNER_FOO' because it is already defined in the properties");
  }

  @Test
  void shouldProcessJsonEnvVariables() {
    var inputProperties = new HashMap<String, String>();
    EnvironmentConfig.processEnvVariables(inputProperties,
      Map.of("SONAR_SCANNER_JSON_PARAMS",
        "{\"key1\":\"value1\", \"key2\":\"value2\"}"));

    assertThat(inputProperties).containsOnly(
      entry("key1", "value1"),
      entry("key2", "value2"));
  }

  @Test
  void ignoreEmptyValueForJsonEnv() {
    var inputProperties = new HashMap<String, String>();
    EnvironmentConfig.processEnvVariables(inputProperties,
      Map.of("SONAR_SCANNER_JSON_PARAMS", ""));

    assertThat(inputProperties).isEmpty();
  }

  @Test
  void throwIfInvalidFormat() {
    var inputProperties = new HashMap<String, String>();
    var env = Map.of("SONAR_SCANNER_JSON_PARAMS", "{garbage");
    var thrown = assertThrows(IllegalArgumentException.class, () -> EnvironmentConfig.processEnvVariables(inputProperties, env));

    assertThat(thrown).hasMessage("Failed to parse JSON properties from environment variable 'SONAR_SCANNER_JSON_PARAMS'");
  }

  @Test
  void jsonEnvVariablesShouldNotOverrideInputProperties() {
    var inputProperties = new HashMap<String, String>(Map.of("key1", "value1", "key3", "value3"));
    EnvironmentConfig.processEnvVariables(inputProperties,
      Map.of("SONAR_SCANNER_JSON_PARAMS",
        "{\"key1\":\"should not override\", \"key2\":\"value2\"}"));

    assertThat(inputProperties).containsOnly(
      entry("key1", "value1"),
      entry("key2", "value2"),
      entry("key3", "value3"));

    assertThat(logTester.logs(Level.WARN)).containsOnly("Ignoring property 'key1' from env variable 'SONAR_SCANNER_JSON_PARAMS' because it is already defined");
  }

  @Test
  void jsonEnvVariablesShouldNotOverrideGenericEnv() {
    var inputProperties = new HashMap<String, String>();
    EnvironmentConfig.processEnvVariables(inputProperties,
      Map.of("SONAR_SCANNER_FOO", "value1",
        "SONAR_SCANNER_JSON_PARAMS", "{\"sonar.scanner.foo\":\"should not override\", \"key2\":\"value2\"}"));

    assertThat(inputProperties).containsOnly(
      entry("sonar.scanner.foo", "value1"),
      entry("key2", "value2"));

    assertThat(logTester.logs(Level.WARN)).containsOnly("Ignoring property 'sonar.scanner.foo' from env variable 'SONAR_SCANNER_JSON_PARAMS' because it is already defined");
  }

  @Test
  void shouldProcessOldJsonEnvVariables() {
    var inputProperties = new HashMap<String, String>();
    EnvironmentConfig.processEnvVariables(inputProperties,
      Map.of("SONARQUBE_SCANNER_PARAMS",
        "{\"key1\":\"value1\", \"key2\":\"value2\"}"));

    assertThat(inputProperties).containsOnly(
      entry("key1", "value1"),
      entry("key2", "value2"));
  }

  @Test
  void oldJsonEnvVariablesIsIgnoredIfNewIsDefinedAndLogAWarning() {
    var inputProperties = new HashMap<String, String>();
    EnvironmentConfig.processEnvVariables(inputProperties,
      Map.of("SONARQUBE_SCANNER_PARAMS", "{\"key1\":\"should not override\", \"key3\":\"value3\"}",
        "SONAR_SCANNER_JSON_PARAMS", "{\"key1\":\"value1\", \"key2\":\"value2\"}"));

    assertThat(inputProperties).containsOnly(
      entry("key1", "value1"),
      entry("key2", "value2"));

    assertThat(logTester.logs(Level.WARN)).containsOnly("Ignoring environment variable 'SONARQUBE_SCANNER_PARAMS' because 'SONAR_SCANNER_JSON_PARAMS' is set");
  }

  @Test
  void oldJsonEnvVariablesIsIgnoredIfNewIsDefinedButDontLogIfSameValue() {
    var inputProperties = new HashMap<String, String>();
    EnvironmentConfig.processEnvVariables(inputProperties,
      Map.of("SONARQUBE_SCANNER_PARAMS", "{\"key1\":\"value1\", \"key2\":\"value2\"}",
        "SONAR_SCANNER_JSON_PARAMS", "{\"key1\":\"value1\", \"key2\":\"value2\"}"));

    assertThat(inputProperties).containsOnly(
      entry("key1", "value1"),
      entry("key2", "value2"));

    assertThat(logTester.logs()).isEmpty();
  }

}
