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
package org.sonar.server.db.migrations;

import java.sql.Connection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MySql;
import org.sonar.db.version.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatabaseMigratorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
  MigrationStep[] migrations = new MigrationStep[] {new FakeMigrationStep()};
  ServerUpgradeStatus serverUpgradeStatus = mock(ServerUpgradeStatus.class);
  DatabaseMigrator migrator;

  @Before
  public void setUp() {
    migrator = new DatabaseMigrator(dbClient, migrations, serverUpgradeStatus, null);
  }

  @Test
  public void should_support_only_creation_of_h2_database() {
    when(dbClient.getDatabase().getDialect()).thenReturn(new MySql());

    assertThat(migrator.createDatabase()).isFalse();
    verify(dbClient, never()).openSession(anyBoolean());
  }

  @Test
  public void fail_if_execute_unknown_migration() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Database migration not found: org.xxx.UnknownMigration");

    migrator.executeMigration("org.xxx.UnknownMigration");
  }

  @Test
  public void execute_migration() {
    assertThat(FakeMigrationStep.executed).isFalse();
    migrator.executeMigration(FakeMigrationStep.class.getName());
    assertThat(FakeMigrationStep.executed).isTrue();
  }

  @Test
  public void should_create_schema_on_h2() {
    Dialect supportedDialect = new H2();
    when(dbClient.getDatabase().getDialect()).thenReturn(supportedDialect);
    Connection connection = mock(Connection.class);
    DbSession session = mock(DbSession.class);
    when(session.getConnection()).thenReturn(connection);
    when(dbClient.openSession(false)).thenReturn(session);
    when(serverUpgradeStatus.isFreshInstall()).thenReturn(true);

    DatabaseMigrator databaseMigrator = new DatabaseMigrator(dbClient, migrations, serverUpgradeStatus, null) {
      @Override
      protected void createSchema(Connection connection, String dialectId) {
      }
    };

    assertThat(databaseMigrator.createDatabase()).isTrue();
  }

  public static class FakeMigrationStep implements MigrationStep {
    static boolean executed = false;

    @Override
    public void execute() {
      executed = true;
    }
  }
}
