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
package org.sonar.process.logging;

import ch.qos.logback.classic.Level;
import java.util.Collections;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;
import static org.sonar.process.logging.LogLevelConfig.newBuilder;

public class LogLevelConfigTest {

  private final String rootLoggerName = RandomStringUtils.secure().nextAlphabetic(20);
  private LogLevelConfig.Builder underTest = newBuilder(rootLoggerName);

  @Test
  public void newBuilder_throws_NPE_if_rootLoggerName_is_null() {
    assertThatThrownBy(() -> newBuilder(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("rootLoggerName can't be null");
  }

  @Test
  public void getLoggerName_returns_name_passed_to_builder() {
    String loggerName = RandomStringUtils.secure().nextAlphabetic(32);

    LogLevelConfig logLevelConfig = newBuilder(loggerName).build();

    assertThat(logLevelConfig.getRootLoggerName()).isEqualTo(loggerName);
  }

  @Test
  public void build_can_create_empty_config_and_returned_maps_are_unmodifiable() {
    LogLevelConfig underTest = newBuilder(rootLoggerName).build();

    expectUnsupportedOperationException(() -> underTest.getConfiguredByProperties().put("1", Collections.emptyList()));
    expectUnsupportedOperationException(() -> underTest.getConfiguredByHardcodedLevel().put("1", Level.ERROR));
  }

  @Test
  public void builder_rootLevelFor_add_global_and_process_property_in_order_for_root_logger() {
    LogLevelConfig underTest = newBuilder(rootLoggerName).rootLevelFor(ProcessId.ELASTICSEARCH).build();

    assertThat(underTest.getConfiguredByProperties()).hasSize(1);
    assertThat(underTest.getConfiguredByProperties().get(rootLoggerName))
      .containsExactly("sonar.log.level", "sonar.log.level.es");
    assertThat(underTest.getConfiguredByHardcodedLevel()).isEmpty();
  }

  @Test
  public void builder_rootLevelFor_fails_with_ProcessId_if_loggerName_is_null() {
    assertThatThrownBy(() -> underTest.rootLevelFor(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("ProcessId can't be null");
  }

  @Test
  public void builder_rootLevelFor_fails_with_ISE_if_called_twice() {
    underTest.rootLevelFor(ProcessId.ELASTICSEARCH);

    assertThatThrownBy(() -> underTest.rootLevelFor(ProcessId.WEB_SERVER))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Configuration by property already registered for " + rootLoggerName);
  }

  @Test
  public void builder_levelByDomain_fails_with_NPE_if_loggerName_is_null() {
    assertThatThrownBy(() -> underTest.levelByDomain(null, ProcessId.WEB_SERVER, LogDomain.JMX))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("loggerName can't be null");
  }

  @Test
  public void builder_levelByDomain_fails_with_IAE_if_loggerName_is_empty() {
    assertThatThrownBy(() -> underTest.levelByDomain("", ProcessId.WEB_SERVER, LogDomain.JMX))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("loggerName can't be empty");
  }

  @Test
  public void builder_levelByDomain_fails_with_NPE_if_ProcessId_is_null() {
    assertThatThrownBy(() -> underTest.levelByDomain("bar", null, LogDomain.JMX))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("ProcessId can't be null");
  }

  @Test
  public void builder_levelByDomain_fails_with_NPE_if_LogDomain_is_null() {
    assertThatThrownBy(() -> underTest.levelByDomain("bar", ProcessId.WEB_SERVER, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("LogDomain can't be null");
  }

  @Test
  public void builder_levelByDomain_adds_global_process_and_domain_properties_in_order_for_specified_logger() {
    LogLevelConfig underTest = newBuilder(rootLoggerName)
      .levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.SQL)
      .build();

    assertThat(underTest.getConfiguredByProperties()).hasSize(1);
    assertThat(underTest.getConfiguredByProperties().get("foo"))
      .containsExactly("sonar.log.level", "sonar.log.level.web", "sonar.log.level.web.sql");
    assertThat(underTest.getConfiguredByHardcodedLevel()).isEmpty();
  }

  @Test
  public void builder_levelByDomain_fails_with_ISE_if_loggerName_has_immutableLevel() {
    underTest.immutableLevel("bar", Level.INFO);

    assertThatThrownBy(() ->  underTest.levelByDomain("bar", ProcessId.WEB_SERVER, LogDomain.JMX))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Configuration hardcoded level already registered for bar");
  }

  @Test
  public void builder_immutableLevel_fails_with_NPE_if_logger_name_is_null() {
    assertThatThrownBy(() -> underTest.immutableLevel(null, Level.ERROR))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("loggerName can't be null");
  }

  @Test
  public void builder_immutableLevel_fails_with_IAE_if_logger_name_is_empty() {
    assertThatThrownBy(() ->  underTest.immutableLevel("", Level.ERROR))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("loggerName can't be empty");
  }

  @Test
  public void builder_immutableLevel_fails_with_NPE_if_level_is_null() {
    assertThatThrownBy(() -> underTest.immutableLevel("foo", null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("level can't be null");
  }

  @Test
  public void builder_immutableLevel_set_specified_level_for_specified_logger() {
    LogLevelConfig config = underTest.immutableLevel("bar", Level.INFO).build();

    assertThat(config.getConfiguredByProperties()).isEmpty();
    assertThat(config.getConfiguredByHardcodedLevel()).hasSize(1);
    assertThat(config.getConfiguredByHardcodedLevel()).containsEntry("bar", Level.INFO);
  }

  @Test
  public void builder_fails_with_ISE_if_immutableLevel_called_twice_for_same_logger() {
    underTest.immutableLevel("foo", Level.INFO);

    assertThatThrownBy(() -> underTest.immutableLevel("foo", Level.DEBUG))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Configuration hardcoded level already registered for foo");
  }

  @Test
  public void builder_fails_with_ISE_if_logger_has_domain_config() {
    underTest.levelByDomain("pop", ProcessId.WEB_SERVER, LogDomain.JMX);

    assertThatThrownBy(() -> underTest.immutableLevel("pop", Level.DEBUG))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Configuration by property already registered for pop");
  }

  private static void expectUnsupportedOperationException(Runnable runnable) {
    try {
      runnable.run();
      fail("a UnsupportedOperationException should have been raised");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
