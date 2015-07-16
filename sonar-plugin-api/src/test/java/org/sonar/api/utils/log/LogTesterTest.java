/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils.log;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogTesterTest {

  LogTester underTest = new LogTester();

  @Test
  public void debugLevel() throws Throwable {
    LoggerLevel initial = underTest.getLevel();

    // when LogTester is used, then debug logs are enabled by default
    underTest.before();
    assertThat(underTest.getLevel()).isEqualTo(LoggerLevel.TRACE);
    assertThat(Loggers.getFactory().getLevel()).isEqualTo(LoggerLevel.TRACE);

    // change
    underTest.setLevel(LoggerLevel.INFO);
    assertThat(underTest.getLevel()).isEqualTo(LoggerLevel.INFO);
    assertThat(Loggers.getFactory().getLevel()).isEqualTo(LoggerLevel.INFO);

    // reset to initial level after execution of test
    underTest.after();
    assertThat(underTest.getLevel()).isEqualTo(initial);
    assertThat(Loggers.getFactory().getLevel()).isEqualTo(initial);
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
}
