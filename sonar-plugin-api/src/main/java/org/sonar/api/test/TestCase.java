/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.test;

import javax.annotation.Nullable;

public interface TestCase {
  enum Status {
    OK, FAILURE, ERROR, SKIPPED;

    public static Status of(@Nullable String s) {
      return s == null ? null : valueOf(s.toUpperCase());
    }
  }

  String TYPE_UNIT = "UNIT";
  String TYPE_INTEGRATION = "INTEGRATION";

  /**
   * Duration in milliseconds
   */
  Long durationInMs();

  String type();

  Status status();

  String name();

  String message();

  String stackTrace();

  TestPlan testPlan();

  boolean doesCover();

  int countCoveredLines();

  Iterable<CoverageBlock> coverageBlocks();

  CoverageBlock coverageBlock(Testable testable);
}
