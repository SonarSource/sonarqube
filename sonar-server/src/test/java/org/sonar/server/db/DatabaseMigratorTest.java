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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.persistence.dialect.MySql;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DatabaseMigratorTest extends AbstractDaoTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  MyBatis mybatis = mock(MyBatis.class);
  Database database = mock(Database.class);

  @Test
  public void should_support_only_creation_of_h2_database() throws Exception {
    when(database.getDialect()).thenReturn(new MySql());

    DatabaseMigrator migrator = new DatabaseMigrator(mybatis, database);

    assertThat(migrator.createDatabase()).isFalse();
    verifyZeroInteractions(mybatis);
  }

  @Test
  public void fail_if_execute_unknown_migration() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to execute database migration: org.xxx.UnknownMigration");

    DatabaseMigrator migrator = new DatabaseMigrator(mybatis, database);
    migrator.executeMigration("org.xxx.UnknownMigration");
  }

  @Test
  public void fail_if_execute_not_a_migration() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to execute database migration: java.lang.String");

    DatabaseMigrator migrator = new DatabaseMigrator(mybatis, database);
    migrator.executeMigration("java.lang.String");
  }

  @Test
  public void execute_migration() throws Exception {
    DatabaseMigrator migrator = new DatabaseMigrator(mybatis, database);
    assertThat(FakeMigration.executed).isFalse();
    migrator.executeMigration(FakeMigration.class.getName());
    assertThat(FakeMigration.executed).isTrue();
  }

  public static class FakeMigration implements DatabaseMigration {
    static boolean executed = false;
    @Override
    public void execute(Database db) {
      executed = true;
    }
  }
}
