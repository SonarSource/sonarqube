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
package org.sonar.server.db;

import org.apache.ibatis.session.SqlSession;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.persistence.dialect.Dialect;
import org.sonar.core.persistence.dialect.H2;
import org.sonar.core.persistence.dialect.MySql;

import java.sql.Connection;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DatabaseMigratorTest extends AbstractDaoTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  MyBatis mybatis = mock(MyBatis.class);
  Database database = mock(Database.class);
  DatabaseMigration[] migrations = new DatabaseMigration[]{new FakeMigration()};

  @Test
  public void should_support_only_creation_of_h2_database() throws Exception {
    when(database.getDialect()).thenReturn(new MySql());

    DatabaseMigrator migrator = new DatabaseMigrator(mybatis, database, migrations);

    assertThat(migrator.createDatabase()).isFalse();
    verifyZeroInteractions(mybatis);
  }

  @Test
  public void fail_if_execute_unknown_migration() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Database migration not found: org.xxx.UnknownMigration");

    DatabaseMigrator migrator = new DatabaseMigrator(mybatis, database, migrations);
    migrator.executeMigration("org.xxx.UnknownMigration");
  }

  @Test
  public void execute_migration() throws Exception {
    DatabaseMigrator migrator = new DatabaseMigrator(mybatis, database, migrations);
    assertThat(FakeMigration.executed).isFalse();
    migrator.executeMigration(FakeMigration.class.getName());
    assertThat(FakeMigration.executed).isTrue();
  }

  @Test
  public void should_create_schema_on_h2() throws Exception {

    Dialect supportedDialect = new H2();
    when(database.getDialect()).thenReturn(supportedDialect);
    Connection connection = mock(Connection.class);
    SqlSession session = mock(SqlSession.class);
    when(session.getConnection()).thenReturn(connection);
    when(mybatis.openSession()).thenReturn(session);

    DatabaseMigrator databaseMigrator = new DatabaseMigrator(mybatis, database, migrations) {
      @Override
      protected void createSchema(Connection connection, String dialectId) {
      }
    };

    assertThat(databaseMigrator.createDatabase()).isTrue();
  }

  public static class FakeMigration implements DatabaseMigration {
    static boolean executed = false;
    @Override
    public void execute() {
      executed = true;
    }
  }
}
