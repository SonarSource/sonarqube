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
package org.sonar.api.utils.log;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogTesterTest {

  LogTester underTest = new LogTester();

  @Test
  public void info_level_by_default() throws Throwable {
    // when LogTester is used, then info logs are enabled by default
    underTest.before();
    assertThat(underTest.getLevel()).isEqualTo(LoggerLevel.INFO);
    assertThat(Loggers.getFactory().getLevel()).isEqualTo(LoggerLevel.INFO);

    // change
    underTest.setLevel(LoggerLevel.DEBUG);
    assertThat(underTest.getLevel()).isEqualTo(LoggerLevel.DEBUG);
    assertThat(Loggers.getFactory().getLevel()).isEqualTo(LoggerLevel.DEBUG);

    // reset to initial level after execution of test
    underTest.after();
    assertThat(underTest.getLevel()).isEqualTo(LoggerLevel.INFO);
    assertThat(Loggers.getFactory().getLevel()).isEqualTo(LoggerLevel.INFO);
  }

  @Test
  public void intercept_logs() throws Throwable {
    underTest.before();
    Loggers.get("logger1").info("an information");
    Loggers.get("logger2").warn("warning: {}", 42);

    assertThat(underTest.logs()).containsExactly("an information", "warning: 42");
    assertThat(underTest.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(underTest.logs(LoggerLevel.INFO)).containsOnly("an information");
    assertThat(underTest.logs(LoggerLevel.WARN)).containsOnly("warning: 42");

    underTest.clear();
    assertThat(underTest.logs()).isEmpty();
    assertThat(underTest.logs(LoggerLevel.INFO)).isEmpty();

    underTest.after();
    assertThat(LogInterceptors.get()).isSameAs(NullInterceptor.NULL_INSTANCE);
  }

  @Test
  public void use_suppliers() throws Throwable {
    // when LogTester is used, then info logs are enabled by default
    underTest.before();
    AtomicBoolean touchedTrace = new AtomicBoolean();
    AtomicBoolean touchedDebug = new AtomicBoolean();
    Loggers.get("logger1").trace(() -> {
      touchedTrace.set(true);
      return "a trace information";
    });
    Loggers.get("logger1").debug(() -> {
      touchedDebug.set(true);
      return "a debug information";
    });

    assertThat(underTest.logs()).isEmpty();
    assertThat(touchedTrace.get()).isFalse();
    assertThat(touchedDebug.get()).isFalse();

    // change level to DEBUG
    underTest.setLevel(LoggerLevel.DEBUG);
    Loggers.get("logger1").trace(() -> {
      touchedTrace.set(true);
      return "a trace information";
    });
    Loggers.get("logger1").debug(() -> {
      touchedDebug.set(true);
      return "a debug information";
    });

    assertThat(underTest.logs()).containsOnly("a debug information");
    assertThat(touchedTrace.get()).isFalse();
    assertThat(touchedDebug.get()).isTrue();
    touchedDebug.set(false);
    underTest.clear();

    // change level to TRACE
    underTest.setLevel(LoggerLevel.TRACE);
    Loggers.get("logger1").trace(() -> {
      touchedTrace.set(true);
      return "a trace information";
    });
    Loggers.get("logger1").debug(() -> {
      touchedDebug.set(true);
      return "a debug information";
    });

    assertThat(underTest.logs()).containsExactly("a trace information", "a debug information");
    assertThat(touchedTrace.get()).isTrue();
    assertThat(touchedDebug.get()).isTrue();
  }
}
