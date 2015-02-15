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
    boolean initial = sut.isDebugEnabled();

    // when LogTester is used, then debug logs are enabled by default
    sut.before();
    assertThat(sut.isDebugEnabled()).isTrue();
    assertThat(Loggers.getFactory().isDebugEnabled()).isTrue();

    // change
    sut.enableDebug(false);
    assertThat(sut.isDebugEnabled()).isFalse();
    assertThat(Loggers.getFactory().isDebugEnabled()).isFalse();

    // reset to initial level
    sut.after();
    assertThat(sut.isDebugEnabled()).isEqualTo(initial);
    assertThat(Loggers.getFactory().isDebugEnabled()).isEqualTo(initial);
  }

  @Test
  public void intercept_logs() throws Throwable {
    sut.before();
    Loggers.get("logger1").info("an information");
    Loggers.get("logger2").warn("warning: {}", 42);

    assertThat(sut.logs()).containsExactly("an information", "warning: 42");

    sut.after();
    assertThat(LogInterceptor.instance).isSameAs(NullInterceptor.NULL_INSTANCE);
  }
}
