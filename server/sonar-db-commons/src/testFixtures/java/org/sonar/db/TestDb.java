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

/**
 * Represents a database in test code, potentially used by {@link AbstractSqlDbTester} or {@link AbstractDbTester} implementations.
 * The test database could be an actual database process launched for testing, an in-memory database such as
 * H2, or a Java testcontainers database, as some examples.
 * Unlike the Database interface, a TestDb additionally handles migrations; the TestDb should have a schema installed.
 * Because it knows the schema, TestDb can implement {@link #truncateTables()}.
 */
public interface TestDb {
  void start();

  void stop();

  Database getDatabase();

  /**
   * Truncate all tables in the schema (except migration history tables).
   * Called between tests.
   */
  void truncateTables();
}
