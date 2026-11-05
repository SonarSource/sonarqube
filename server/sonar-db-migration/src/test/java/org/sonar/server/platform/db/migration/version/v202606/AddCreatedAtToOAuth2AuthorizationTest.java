/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202606.AddCreatedAtToOAuth2Authorization.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.AddCreatedAtToOAuth2Authorization.TABLE_NAME;

class AddCreatedAtToOAuth2AuthorizationTest {

  @RegisterExtension
  final MigrationDbTester db = MigrationDbTester.createEmpty();

  private final AddCreatedAtToOAuth2Authorization underTest = new AddCreatedAtToOAuth2Authorization(db.database());

  @BeforeEach
  void createTable() throws SQLException {
    new CreateOAuthRegisteredClientsTable(db.database()).execute();
    new CreateOAuthAuthorizationsTable(db.database()).execute();
  }

  @Test
  void adds_nullable_timestamp_column() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.TIMESTAMP, null, true);
  }

  @Test
  void rows_inserted_without_the_column_get_the_current_time() throws SQLException {
    underTest.execute();
    Instant before = Instant.now().minus(Duration.ofMinutes(1));

    insertAuthorization("pending");

    assertThat(createdAt("pending")).isAfterOrEqualTo(Timestamp.from(before)).isBeforeOrEqualTo(Timestamp.from(Instant.now().plus(Duration.ofMinutes(1))));
  }

  @Test
  void is_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.TIMESTAMP, null, true);
    insertAuthorization("pending");
    assertThat(createdAt("pending")).isNotNull();
  }

  private void insertAuthorization(String id) throws SQLException {
    try (var connection = db.database().getDataSource().getConnection(); var statement = connection.createStatement()) {
      connection.setAutoCommit(false);
      statement.executeUpdate("INSERT INTO oauth2_authorization (id, registered_client_id, principal_name, authorization_grant_type) "
        + "VALUES ('" + id + "', 'client', 'user', 'authorization_code')");
      connection.commit();
    }
  }

  private Timestamp createdAt(String id) throws SQLException {
    try (var connection = db.database().getDataSource().getConnection(); var statement = connection.createStatement();
      var result = statement.executeQuery("SELECT created_at FROM oauth2_authorization WHERE id = '" + id + "'")) {
      assertThat(result.next()).isTrue();
      return result.getTimestamp(1);
    }
  }
}
