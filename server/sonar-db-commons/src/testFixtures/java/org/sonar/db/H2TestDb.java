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

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * H2-specific test database implementation. Typically accessed via {@link H2DbTester} which provides
 * table truncation between tests for proper test isolation.
 * <p>
 * <strong>Schema creation is optimized:</strong> The schema is created once when the H2TestDb instance
 * is constructed, and is automatically reused across multiple test methods. Subsequent calls to
 * {@link #start()} are no-ops.
 * <p>
 * <strong>Tests which rely on this class can only be run on H2</strong> because:
 * <ul>
 *   <li>when a specific schema is provided, this schema uses H2-specific SQL syntax</li>
 *   <li>the implementation assumes H2's in-memory database semantics</li>
 * </ul>
 */
public class H2TestDb extends AbstractTestDb {
  private static final Logger LOG = LoggerFactory.getLogger(H2TestDb.class);

  protected H2TestDb(Database db) {
    super(db);
  }

  protected H2TestDb(String schemaPath) {
    this(createDatabase(schemaPath, true));
  }
  
  public static H2TestDb create(String schemaPath) {
    return create(schemaPath, true);
  }

  public static H2TestDb create(String schemaPath, boolean databaseToUpper) {
    requireNonNull(schemaPath, "schemaPath can't be null");
    return new H2TestDb(createDatabase(schemaPath, databaseToUpper));
  }

  public static H2TestDb createEmpty() {
    return new H2TestDb(createDatabase(null, true));
  }

  private static Database createDatabase(@Nullable String schemaPath, boolean databaseToUpper) {
    // Check if test should only run on H2
    String dialect = System.getProperty("sonar.jdbc.dialect");
    if (dialect != null && !"h2".equals(dialect)) {
      throw new AssumptionViolatedException("This test is intended to be run on H2 only");
    }

    // Create H2 database name
    String name = "h2Tests-" + (schemaPath == null ? "empty" : DigestUtils.md5Hex(schemaPath));
    if (!databaseToUpper) {
      name = name + ";DATABASE_TO_UPPER=FALSE";
    }
    name = name + ";NON_KEYWORDS=VALUE";

    // Create and start database
    H2Database database = new H2Database(name);
    database.start();

    // Execute schema script if provided
    if (schemaPath != null) {
      database.executeScript(schemaPath);
    }

    LOG.debug("Test Database: {}", database);
    return database;
  }

  @Override
  public void truncateTables() {
    try {
      DatabaseTestUtils.truncateTables(getDatabase().getDataSource(), DatabaseTestUtils.loadTableNames(getDatabase().getDataSource()));
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to truncate db tables", e);
    }
  }

  @Override
  public void start() {
    // everything is done in constructor
  }

  @Override
  public void stop() {
    getDatabase().stop();
  }

}
