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
package org.sonar.scanner.sensor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.utils.System2;
import org.sonar.scanner.bootstrap.GlobalConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractSensorOptimizerTest {

  private ListAppender<ILoggingEvent> appender;
  private final Logger logger = (Logger) LoggerFactory.getLogger(AbstractSensorOptimizer.class);

  private

  @BeforeEach
  void setup() {
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.DEBUG);
  }

  @Test
  void whenFailedHasKey_thenLogsAMessageAndReturnsFalse() {
    SensorDescriptor sensorDescriptor = new DefaultSensorDescriptor()
      .name("XML Sensor")
      .onlyWhenConfiguration(c -> c.hasKey("aKey") & c.hasKey("anotherKey"));

    PropertyDefinitions propertyDefinitions = new PropertyDefinitions(System2.INSTANCE);
    Configuration configuration = new GlobalConfiguration(propertyDefinitions, new Encryption(null), Map.of("anotherKey", "some value"));
    MySensorOptimizer sensorOptimizer = new MySensorOptimizer(configuration);

    boolean result = sensorOptimizer.shouldExecute((DefaultSensorDescriptor) sensorDescriptor);

    assertThat(result).isFalse();

    assertThat(appender.list)
      .extracting(ILoggingEvent::getFormattedMessage)
      .containsExactly("""
        'XML Sensor' skipped because of missing configuration requirements.
        Accessed configuration:
        - anotherKey: some value
        - aKey: <empty>""");
  }

  @Test
  void whenFailedGet_thenLogsAMessageAndReturnsFalse() {
    SensorDescriptor sensorDescriptor = new DefaultSensorDescriptor()
      .name("XML Sensor")
      .onlyWhenConfiguration(c -> c.get("aKey").orElse("").equals("another value") & c.get("anotherKey").orElse("").equals("another value"));

    PropertyDefinitions propertyDefinitions = new PropertyDefinitions(System2.INSTANCE);
    Configuration configuration = new GlobalConfiguration(propertyDefinitions, new Encryption(null), Map.of("aKey", "some value"));
    MySensorOptimizer sensorOptimizer = new MySensorOptimizer(configuration);

    boolean result = sensorOptimizer.shouldExecute((DefaultSensorDescriptor) sensorDescriptor);

    assertThat(result).isFalse();

    assertThat(appender.list)
      .extracting(ILoggingEvent::getFormattedMessage)
      .containsExactly("""
        'XML Sensor' skipped because of missing configuration requirements.
        Accessed configuration:
        - anotherKey: <empty>
        - aKey: some value""");
  }

  @Test
  void whenFailedGetStringArray_thenLogsAMessageAndReturnsFalse() {
    SensorDescriptor sensorDescriptor = new DefaultSensorDescriptor()
      .name("XML Sensor")
      .onlyWhenConfiguration(c -> Arrays.equals(c.getStringArray("aKey"), new String[]{"another value 1", "another value 2"}) &
        Arrays.equals(c.getStringArray("anotherKey"), new String[]{"another value 3", "another value 4"}));

    PropertyDefinitions propertyDefinitions = new PropertyDefinitions(System2.INSTANCE);
    Configuration configuration = new GlobalConfiguration(propertyDefinitions, new Encryption(null), Map.of("aKey", "some value 1, some value 2"));
    MySensorOptimizer sensorOptimizer = new MySensorOptimizer(configuration);

    boolean result = sensorOptimizer.shouldExecute((DefaultSensorDescriptor) sensorDescriptor);

    assertThat(result).isFalse();

    assertThat(appender.list)
      .extracting(ILoggingEvent::getFormattedMessage)
      .containsExactly("""
        'XML Sensor' skipped because of missing configuration requirements.
        Accessed configuration:
        - anotherKey: <empty>
        - aKey: some value 1, some value 2""");
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
  }

  static class MySensorOptimizer extends AbstractSensorOptimizer {
    public MySensorOptimizer(Configuration config) {
      super(null, null, config);
    }
  }
}
