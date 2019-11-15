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

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * <b>For tests only</b>
 * <br>
 * This JUnit 5 extension allows to configure and access logs in tests. By default
 * trace level is enabled.
 * <br>
 * Warning - not compatible with parallel execution of tests in the same JVM fork.
 * <br>
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
 * class MyClassTests {
 *   &#064;org.junit.jupiter.api.extension.RegisterExtension
 *   LogTesterJUnit5 logTester = new LogTesterJUnit5();
 *
 *   &#064;org.junit.jupiter.api.Test
 *   public void test_log() {
 *     new MyClass().doSomething();
 *
 *     assertThat(logTester.logs()).containsOnly("foo");
 *   }
 * }
 * </pre>
 *
 * @since 8.1
 */
public class LogTesterJUnit5 extends AbstractLogTester<LogTesterJUnit5> implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    before();
  }

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    after();
  }
}
