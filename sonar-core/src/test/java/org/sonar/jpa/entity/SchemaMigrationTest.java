/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.jpa.entity;

import org.junit.Test;
import org.sonar.jpa.session.MemoryDatabaseConnector;
import org.sonar.persistence.Database;
import org.sonar.persistence.InMemoryDatabase;

import java.sql.Connection;

import static org.junit.Assert.assertEquals;

public class SchemaMigrationTest {

  @Test
  public void currentVersionShouldBeUnknownWhenSchemaIsEmpty() throws Exception {
    Database database = new InMemoryDatabase();
    database.start();

    MemoryDatabaseConnector connector = new MemoryDatabaseConnector(database, SchemaMigration.VERSION_UNKNOWN);
    connector.start();

    Connection connection = null;
    try {
      connection = connector.getConnection();
      assertEquals(SchemaMigration.VERSION_UNKNOWN, SchemaMigration.getCurrentVersion(connection));
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
    connector.stop();
    database.stop();
  }

  @Test
  public void versionShouldBeLoadedFromSchemaMigrationsTable() throws Exception {
    Database database = new InMemoryDatabase();
    database.start();
    MemoryDatabaseConnector connector = new MemoryDatabaseConnector(database, 30);
    connector.start();

    Connection connection = null;
    try {
      connection = connector.getConnection();
      assertEquals(30, SchemaMigration.getCurrentVersion(connection));

    } finally {
      if (connection != null) {
        connection.close();
      }
    }
    connector.stop();
    database.stop();
  }
}
