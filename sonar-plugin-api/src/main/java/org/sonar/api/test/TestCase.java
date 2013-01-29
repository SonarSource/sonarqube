/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.test;

import java.util.Collection;

public interface TestCase {
  String TYPE_UNIT = "unit";
  String TYPE_INTEGRATION = "integration";

  String STATUS_PASS = "pass";
  String STATUS_FAIL = "fail";

  // unit test/integration test/...
  String type();

  /**
   * Duration in milliseconds
   */
  Long durationInMs();

  // pass/fail/...
  String status();

  /**
   * The key is not null and unique among the test plan.
   */
  String key();

  String name();

  String message();

  String stackTrace();

  TestPlan testPlan();

  boolean hasCoveredLines();

  int countCoveredLines();

  Collection<CoveredTestable> coveredTestable();
}
