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

import ch.qos.logback.classic.Level;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class LogbackLoggerTest {

  LogbackLogger sut = new LogbackLogger((ch.qos.logback.classic.Logger)LoggerFactory.getLogger(getClass()));

  @Rule
  public LogTester tester = new LogTester();

  @Test
  public void debug_enabling() throws Exception {
    tester.enableDebug(true);
    assertThat(sut.isDebugEnabled()).isTrue();

    tester.enableDebug(false);
    assertThat(sut.isDebugEnabled()).isFalse();
  }

  @Test
  public void log() throws Exception {
    // no assertions. Simply verify that calls do not fail.
    sut.debug("message");
    sut.debug("message {}", "foo");
    sut.debug("message {} {}", "foo", "bar");
    sut.debug("message {} {} {}", "foo", "bar", "baz");

    sut.info("message");
    sut.info("message {}", "foo");
    sut.info("message {} {}", "foo", "bar");
    sut.info("message {} {} {}", "foo", "bar", "baz");

    sut.warn("message");
    sut.warn("message {}", "foo");
    sut.warn("message {} {}", "foo", "bar");
    sut.warn("message {} {} {}", "foo", "bar", "baz");

    sut.error("message");
    sut.error("message {}", "foo");
    sut.error("message {} {}", "foo", "bar");
    sut.error("message {} {} {}", "foo", "bar", "baz");
    sut.error("message", new IllegalArgumentException(""));
  }

  @Test
  public void change_level() throws Exception {
    assertThat(sut.setLevel(LoggerLevel.ERROR)).isTrue();
    assertThat(sut.logbackLogger().getLevel()).isEqualTo(Level.ERROR);

    assertThat(sut.setLevel(LoggerLevel.WARN)).isTrue();
    assertThat(sut.logbackLogger().getLevel()).isEqualTo(Level.WARN);

    assertThat(sut.setLevel(LoggerLevel.INFO)).isTrue();
    assertThat(sut.logbackLogger().getLevel()).isEqualTo(Level.INFO);

    assertThat(sut.setLevel(LoggerLevel.DEBUG)).isTrue();
    assertThat(sut.logbackLogger().getLevel()).isEqualTo(Level.DEBUG);
    assertThat(sut.isDebugEnabled()).isTrue();
  }
}
