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

import javax.annotation.Nullable;

/**
 * Represents a single test in a test plan.
 * @since 5.0
 *
 */
public interface TestCase {
  enum Status {
    OK, FAILURE, ERROR, SKIPPED;

    public static Status of(@Nullable String s) {
      return s == null ? null : valueOf(s.toUpperCase());
    }
  }

  enum Type {
    UNIT, INTEGRATION;
  }

  /**
   * Duration in milliseconds
   */
  Long durationInMs();

  Type type();

  /**
   * Status of execution of the test.
   */
  Status status();

  /**
   * Name of this test case.
   */
  String name();

  /**
   * Message (usually in case of {@link Status#ERROR} or {@link Status#FAILURE}).
   */
  String message();

  /**
   * Stacktrace (usually in case of {@link Status#ERROR}).
   */
  String stackTrace();

}
