/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db;

import org.apache.commons.lang3.StringUtils;

/**
 * H2-specific test fixture for JUnit tests. Provides an H2 in-memory database with automatic
 * table truncation between tests to ensure test isolation.
 * <p>
 * <strong>Usage:</strong>
 * <ul>
 *   <li><strong>JUnit 4:</strong> Use {@code @Rule} (per-test instance field)</li>
 *   <li><strong>JUnit 5:</strong> Use {@code @RegisterExtension} (per-test instance field)</li>
 * </ul>
 * <p>
 * <strong>Example (JUnit 4):</strong>
 * <pre>{@code
 * @Rule
 * public H2DbTester db = H2DbTester.createForSchema(MyTest.class, "schema.sql");
 * }</pre>
 * <p>
 * <strong>Example (JUnit 5):</strong>
 * <pre>{@code
 * @RegisterExtension
 * H2DbTester db = H2DbTester.createForSchema(MyTest.class, "schema.sql");
 * }</pre>
 */
public class H2DbTester extends AbstractSqlDbTester<H2TestDb> {

  private H2DbTester(H2TestDb testDb) {
    super(testDb);
  }

  public static H2DbTester createForSchema(Class testClass, String filename) {
    return createForSchema(testClass, filename, true);
  }

  public static H2DbTester createForSchema(Class testClass, String filename, boolean databaseToUpper) {
    String path = StringUtils.replaceChars(testClass.getCanonicalName(), '.', '/');
    String schemaPath = path + "/" + filename;
    return new H2DbTester(H2TestDb.create(schemaPath, databaseToUpper));
  }

  public static H2DbTester createEmpty() {
    return new H2DbTester(H2TestDb.createEmpty());
  }
}
