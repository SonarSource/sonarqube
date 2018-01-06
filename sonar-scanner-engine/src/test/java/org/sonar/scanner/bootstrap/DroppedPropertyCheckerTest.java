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
package org.sonar.scanner.bootstrap;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;

public class DroppedPropertyCheckerTest {
  private static final String SOME_VALUE = "some value";
  private static final String DROPPED_PROPERTY_1 = "I'm dropped";
  private static final String DROPPED_PROPERTY_MSG_1 = "blablabla!";

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void no_log_if_no_dropped_property() {
    new DroppedPropertyChecker(ImmutableMap.of(DROPPED_PROPERTY_1, SOME_VALUE), ImmutableMap.<String, String>of()).checkDroppedProperties();

    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void no_log_if_settings_does_not_contain_any_dropped_property() {
    new DroppedPropertyChecker(ImmutableMap.<String, String>of(), ImmutableMap.of(DROPPED_PROPERTY_1, DROPPED_PROPERTY_MSG_1)).checkDroppedProperties();

    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void warn_log_if_settings_contains_any_dropped_property() {
    new DroppedPropertyChecker(ImmutableMap.of(DROPPED_PROPERTY_1, SOME_VALUE), ImmutableMap.of(DROPPED_PROPERTY_1, DROPPED_PROPERTY_MSG_1)).checkDroppedProperties();

    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly("Property '" + DROPPED_PROPERTY_1 + "' is not supported any more. " + DROPPED_PROPERTY_MSG_1);
    assertThat(logTester.logs(LoggerLevel.INFO)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.TRACE)).isEmpty();
  }
}
