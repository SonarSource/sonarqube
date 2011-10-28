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
import org.mockito.Mockito;
import org.sonar.jpa.session.MemoryDatabaseConnector;
import org.sonar.persistence.HsqlDatabase;

import java.sql.Connection;

import static org.junit.Assert.assertEquals;

public class SchemaMigrationTest {

  @Test
  public void currentVersionShouldBeUnknownWhenSchemaIsEmpty() throws Exception {
    HsqlDatabase hsqlDatabase = new HsqlDatabase();
    hsqlDatabase.start();

    MemoryDatabaseConnector connector = new MemoryDatabaseConnector(hsqlDatabase, SchemaMigration.VERSION_UNKNOWN);
    connector.start();

    Connection connection = Mockito.mock(Connection.class);
    try {
      connection = connector.getConnection();
      assertEquals(SchemaMigration.VERSION_UNKNOWN, SchemaMigration.getCurrentVersion(connection));
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
    connector.stop();
    hsqlDatabase.stop();
  }

  @Test
  public void versionShouldBeLoadedFromSchemaMigrationsTable() throws Exception {
    HsqlDatabase hsqlDatabase = new HsqlDatabase();
    hsqlDatabase.start();
    MemoryDatabaseConnector connector = new MemoryDatabaseConnector(hsqlDatabase, 30);
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
    hsqlDatabase.stop();
  }
}
