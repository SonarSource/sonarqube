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

  LogTester sut = new LogTester();

  @Test
  public void debugLevel() throws Throwable {
    LoggerLevel initial = sut.getLevel();

    // when LogTester is used, then debug logs are enabled by default
    sut.before();
    assertThat(sut.getLevel()).isEqualTo(LoggerLevel.TRACE);
    assertThat(Loggers.getFactory().getLevel()).isEqualTo(LoggerLevel.TRACE);

    // change
    sut.setLevel(LoggerLevel.INFO);
    assertThat(sut.getLevel()).isEqualTo(LoggerLevel.INFO);
    assertThat(Loggers.getFactory().getLevel()).isEqualTo(LoggerLevel.INFO);

    // reset to initial level after execution of test
    sut.after();
    assertThat(sut.getLevel()).isEqualTo(initial);
    assertThat(Loggers.getFactory().getLevel()).isEqualTo(initial);
  }

  @Test
  public void intercept_logs() throws Throwable {
    sut.before();
    Loggers.get("logger1").info("an information");
    Loggers.get("logger2").warn("warning: {}", 42);

    assertThat(sut.logs()).containsExactly("an information", "warning: 42");
    assertThat(sut.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(sut.logs(LoggerLevel.INFO)).containsOnly("an information");
    assertThat(sut.logs(LoggerLevel.WARN)).containsOnly("warning: 42");

    sut.clear();
    assertThat(sut.logs()).isEmpty();
    assertThat(sut.logs(LoggerLevel.INFO)).isEmpty();

    sut.after();
    assertThat(LogInterceptors.get()).isSameAs(NullInterceptor.NULL_INSTANCE);
  }
}
