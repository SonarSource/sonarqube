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

import org.sonar.api.batch.fs.InputFile;

import javax.annotation.Nullable;

/**
 * Represents a single test in a test file.
 * @since 5.0
 */
public interface TestCase {

  /**
   * Test execution status.
   */
  enum Status {
    OK, FAILURE, ERROR, SKIPPED;

    public static Status of(@Nullable String s) {
      return s == null ? null : valueOf(s.toUpperCase());
    }
  }

  /**
   * Test type.
   */
  enum Type {
    UNIT, INTEGRATION;
  }

  /**
   * InputFile where this test is located.
   */
  InputFile testFile();

  /**
   * Set file where this test is located. Mandatory.
   */
  TestCase inTestFile(InputFile testFile);

  /**
   * Duration in milliseconds
   */
  Long durationInMs();

  /**
   * Duration in milliseconds
   */
  TestCase durationInMs(long duration);

  /**
   * Name of this test case.
   */
  String name();

  /**
   * Set name of this test. Name is mandatory.
   */
  TestCase name(String name);

  /**
   * Status of execution of the test.
   */
  Status status();

  /**
   * Status of execution of the test.
   */
  TestCase status(Status status);

  /**
   * Message (usually in case of {@link Status#ERROR} or {@link Status#FAILURE}).
   */
  String message();

  /**
   * Message (usually in case of {@link Status#ERROR} or {@link Status#FAILURE}).
   */
  TestCase message(String message);

  /**
   * Type of test.
   */
  Type type();

  /**
   * Type of test.
   */
  TestCase ofType(Type type);

  /**
   * Stacktrace (usually in case of {@link Status#ERROR}).
   */
  String stackTrace();

  /**
   * Set stacktrace (usually in case of {@link Status#ERROR}).
   */
  TestCase stackTrace(String stackTrace);

  /**
   * Call this method only once when your are done with defining the test case.
   */
  void save();

}
