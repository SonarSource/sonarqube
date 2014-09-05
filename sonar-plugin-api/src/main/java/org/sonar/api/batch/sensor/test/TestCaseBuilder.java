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
package org.sonar.api.batch.sensor.test;

import org.sonar.api.batch.sensor.test.TestCase.Status;
import org.sonar.api.batch.sensor.test.TestCase.Type;

/**
 * Builder to create a new TestCase on a test file.
 * @since 5.0
 */
public interface TestCaseBuilder {

  /**
   * Duration in milliseconds
   */
  TestCaseBuilder durationInMs(long duration);

  /**
   * Status of execution of the test.
   */
  TestCaseBuilder status(Status status);

  /**
   * Message (usually in case of {@link Status#ERROR} or {@link Status#FAILURE}).
   */
  TestCaseBuilder message(String message);

  /**
   * Type of test.
   */
  TestCaseBuilder type(Type type);

  /**
   * Stacktrace (usually in case of {@link Status#ERROR}).
   */
  TestCaseBuilder stackTrace(String stackTrace);

  /**
   * Call this method only once when your are done with defining the test case.
   */
  TestCase build();

}
