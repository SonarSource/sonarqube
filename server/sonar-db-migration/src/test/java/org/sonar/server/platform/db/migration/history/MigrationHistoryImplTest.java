/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.history;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MigrationHistoryImplTest {
  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(MigrationHistoryImplTest.class, "schema_migration.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MigrationHistoryMeddler migrationHistoryMeddler = mock(MigrationHistoryMeddler.class);
  private MigrationHistoryImpl underTest = new MigrationHistoryImpl(dbTester.database(), migrationHistoryMeddler);

  @Test
  public void start_does_not_fail_if_table_history_exists_and_calls_meddler() {
    underTest.start();

    verify(migrationHistoryMeddler).meddle(underTest);
  }

  @Test
  public void getLastMigrationNumber_returns_empty_if_history_table_is_empty() {
    assertThat(underTest.getLastMigrationNumber()).isEmpty();
  }

  @Test
  public void getLastMigrationNumber_returns_last_version_assuming_version_are_only_number() throws SQLException {
    insert(12, 5, 30, 8);

    assertThat(underTest.getLastMigrationNumber()).contains(30L);
  }

  @Test
  public void done_fails_with_NPE_if_argument_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.done(null);
  }

  @Test
  public void done_adds_migration_number_to_table() {
    underTest.done(new RegisteredMigrationStep(12, "aa", MigrationStep.class));

    assertThat(underTest.getLastMigrationNumber()).contains(12L);
  }

  private void insert(int... versions) throws SQLException {
    try (Connection connection = dbTester.database().getDataSource().getConnection()) {
      Arrays.stream(versions).forEach(version -> insert(connection, version));
    }
  }

  private void insert(Connection connection, long version) {
    try (PreparedStatement statement = connection.prepareStatement("insert into schema_migrations(version) values (?)")) {
      statement.setString(1, String.valueOf(version));
      statement.execute();
      connection.commit();
    } catch (SQLException e) {
      throw new IllegalStateException(String.format("Failed to insert row with value %s", version), e);
    }
  }
}
