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

import org.junit.rules.ExternalResource;

import java.util.List;

/**
 * <b>For tests only</b>
 * <p/>
 * This JUnit rule allows to configure and access logs in tests. By default
 * trace level is enabled.
 * <p/>
 * Warning - not compatible with parallel execution of tests in the same JVM fork.
 * <p/>
 * Example:
 * <pre>
 * public class MyClass {
 *   private final Logger logger = Loggers.get("logger_name");
 *
 *   public void doSomething() {
 *     logger.info("foo");
 *   }
 * }
 *
 * public class MyTest {
 *   &#064;org.junit.Rule
 *   public LogTester logTester = new LogTester();
 *
 *   &#064;org.junit.Test
 *   public void test_log() {
 *     new MyClass().doSomething();
 *
 *     assertThat(logTester.logs()).containsOnly("foo");
 *   }
 * }
 * </pre>
 *
 * @since 5.1
 */
public class LogTester extends ExternalResource {

  private LoggerLevel initialLevel;

  @Override
  protected void before() throws Throwable {
    initialLevel = Loggers.getFactory().getLevel();

    // this shared instance breaks compatibility with parallel execution of tests
    LogInterceptors.set(new ListInterceptor());
    setLevel(LoggerLevel.TRACE);
  }

  @Override
  protected void after() {
    LogInterceptors.set(NullInterceptor.NULL_INSTANCE);
    setLevel(initialLevel);
  }

  LoggerLevel getLevel() {
    return Loggers.getFactory().getLevel();
  }

  /**
   * Enable/disable debug logs. Info, warn and error logs are always enabled.
   * By default debug logs are enabled when LogTester is started.
   */
  public LogTester setLevel(LoggerLevel level) {
    Loggers.getFactory().setLevel(level);
    return this;
  }

  /**
   * Logs in chronological order (item at index 0 is the oldest one)
   */
  public List<String> logs() {
    return ((ListInterceptor) LogInterceptors.get()).logs();
  }

  /**
   * Logs in chronological order (item at index 0 is the oldest one) for
   * a given level
   */
  public List<String> logs(LoggerLevel level) {
    return ((ListInterceptor) LogInterceptors.get()).logs(level);
  }

  public LogTester clear() {
    ((ListInterceptor) LogInterceptors.get()).clear();
    return this;
  }
}
