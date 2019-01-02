/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.application.es;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;

public class EsLoggingTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private EsLogging underTest = new EsLogging();

  @Test
  public void createProperties_with_empty_props() throws IOException {
    File logDir = temporaryFolder.newFolder();
    Properties properties = underTest.createProperties(newProps(), logDir);

    verifyProperties(properties,
      "status", "ERROR",
      "appender.file_es.type", "RollingFile",
      "appender.file_es.name", "file_es",
      "appender.file_es.filePattern", new File(logDir, "es.%d{yyyy-MM-dd}.log").getAbsolutePath(),
      "appender.file_es.fileName", new File(logDir, "es.log").getAbsolutePath(),
      "appender.file_es.layout.type", "PatternLayout",
      "appender.file_es.layout.pattern", "%d{yyyy.MM.dd HH:mm:ss} %-5level es[][%logger{1.}] %msg%n",
      "appender.file_es.policies.type", "Policies",
      "appender.file_es.policies.time.type", "TimeBasedTriggeringPolicy",
      "appender.file_es.policies.time.interval", "1",
      "appender.file_es.policies.time.modulate", "true",
      "appender.file_es.strategy.type", "DefaultRolloverStrategy",
      "appender.file_es.strategy.fileIndex", "nomax",
      "appender.file_es.strategy.action.type", "Delete",
      "appender.file_es.strategy.action.basepath", logDir.getAbsolutePath(),
      "appender.file_es.strategy.action.maxDepth", "1",
      "appender.file_es.strategy.action.condition.type", "IfFileName",
      "appender.file_es.strategy.action.condition.glob", "es*",
      "appender.file_es.strategy.action.condition.nested_condition.type", "IfAccumulatedFileCount",
      "appender.file_es.strategy.action.condition.nested_condition.exceeds", "7",
      "rootLogger.level", "INFO",
      "rootLogger.appenderRef.file_es.ref", "file_es");
  }

  @Test
  public void createProperties_sets_root_logger_to_INFO_if_no_property_is_set() throws IOException {
    File logDir = temporaryFolder.newFolder();
    Properties properties = underTest.createProperties(newProps(), logDir);

    assertThat(properties.getProperty("rootLogger.level")).isEqualTo("INFO");
  }

  @Test
  public void createProperties_sets_root_logger_to_global_property_if_set() throws IOException {
    File logDir = temporaryFolder.newFolder();
    Properties properties = underTest.createProperties(newProps("sonar.log.level", "TRACE"), logDir);

    assertThat(properties.getProperty("rootLogger.level")).isEqualTo("TRACE");
  }

  @Test
  public void createProperties_sets_root_logger_to_process_property_if_set() throws IOException {
    File logDir = temporaryFolder.newFolder();
    Properties properties = underTest.createProperties(newProps("sonar.log.level.es", "DEBUG"), logDir);

    assertThat(properties.getProperty("rootLogger.level")).isEqualTo("DEBUG");
  }

  @Test
  public void createProperties_sets_root_logger_to_process_property_over_global_property_if_both_set() throws IOException {
    File logDir = temporaryFolder.newFolder();
    Properties properties = underTest.createProperties(
      newProps(
        "sonar.log.level", "DEBUG",
        "sonar.log.level.es", "TRACE"),
      logDir);

    assertThat(properties.getProperty("rootLogger.level")).isEqualTo("TRACE");
  }

  private static Props newProps(String... propertyKeysAndValues) {
    assertThat(propertyKeysAndValues.length % 2).describedAs("Number of parameters must be even").isEqualTo(0);
    Properties properties = new Properties();
    for (int i = 0; i < propertyKeysAndValues.length; i++) {
      properties.put(propertyKeysAndValues[i++], propertyKeysAndValues[i]);
    }
    return new Props(properties);
  }

  private void verifyProperties(Properties properties, String... expectedPropertyKeysAndValuesOrdered) {
    if (expectedPropertyKeysAndValuesOrdered.length == 0) {
      assertThat(properties.size()).isEqualTo(0);
    } else {
      assertThat(expectedPropertyKeysAndValuesOrdered.length % 2).describedAs("Number of parameters must be even").isEqualTo(0);
      Set<String> keys = new HashSet<>(expectedPropertyKeysAndValuesOrdered.length / 2 + 1);
      keys.add("status");
      for (int i = 0; i < expectedPropertyKeysAndValuesOrdered.length; i++) {
        String key = expectedPropertyKeysAndValuesOrdered[i++];
        String value = expectedPropertyKeysAndValuesOrdered[i];
        assertThat(properties.get(key)).describedAs("Unexpected value for property " + key).isEqualTo(value);
        keys.add(key);
      }
      assertThat(properties.keySet()).containsOnly(keys.toArray());
    }
  }
}
