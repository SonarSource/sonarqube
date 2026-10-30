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
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;

class CreateOAuthTablesTest {
  @RegisterExtension
  final MigrationDbTester db = MigrationDbTester.createEmpty();

  @Test
  void creates_spring_jdbc_tables_with_keys_token_columns_and_lookup_indexes() throws SQLException {
    migrate();

    db.assertPrimaryKey("oauth2_registered_client", "pk_oauth2_registered_client", "id");
    db.assertUniqueIndex("oauth2_registered_client", "oauth2_client_id_uniq", "client_id");
    db.assertColumnDefinition("oauth2_registered_client", "client_id", Types.VARCHAR, 512, false);
    db.assertColumnDefinition("oauth2_registered_client", "client_id_issued_at", Types.BIGINT, null, false);
    db.assertColumnDefinition("oauth2_registered_client", "client_secret_expires_at", Types.BIGINT, null, true);
    db.assertPrimaryKey("oauth2_authorization", "pk_oauth2_authorization", "id");
    db.assertIndex("oauth2_authorization", "oauth2_auth_access_token", "access_token_value");
    db.assertIndex("oauth2_authorization", "oauth2_auth_code", "authorization_code_value");
    db.assertIndex("oauth2_authorization", "oauth2_auth_refresh_token", "refresh_token_value");
    db.assertColumnDefinition("oauth2_authorization", "registered_client_id", Types.VARCHAR, 100, false);
    db.assertColumnDefinition("oauth2_authorization", "principal_name", Types.VARCHAR, 200, false);
    db.assertColumnDefinition("oauth2_authorization", "attributes", Types.CLOB, null, true);
    db.assertColumnDefinition("oauth2_authorization", "state", Types.VARCHAR, 500, true);
    for (String token : new String[] {"authorization_code", "access_token", "oidc_id_token", "refresh_token", "user_code", "device_code"}) {
      db.assertColumnDefinition("oauth2_authorization", token + "_value", Types.VARCHAR, 512, true);
      db.assertColumnDefinition("oauth2_authorization", token + "_metadata", Types.CLOB, null, true);
      db.assertColumnDefinition("oauth2_authorization", token + "_issued_at", Types.BIGINT, null, true);
      db.assertColumnDefinition("oauth2_authorization", token + "_expires_at", Types.BIGINT, null, true);
    }
    db.assertPrimaryKey("oauth2_authorization_consent", "pk_oauth2_auth_consent", "registered_client_id", "principal_name");
    db.assertColumnDefinition("oauth2_authorization_consent", "authorities", Types.VARCHAR, 1000, false);
  }

  @Test
  void rerunning_migrations_preserves_existing_data() throws SQLException {
    migrate();
    try (var connection = db.database().getDataSource().getConnection(); var statement = connection.createStatement()) {
      connection.setAutoCommit(false);
      statement.executeUpdate("INSERT INTO oauth2_authorization_consent VALUES ('client', 'user', 'SCOPE_mcp:access')");
      connection.commit();
    }
    migrate();
    db.assertIndex("oauth2_authorization", "oauth2_auth_refresh_token", "refresh_token_value");
    try (var connection = db.database().getDataSource().getConnection(); var statement = connection.createStatement();
      var result = statement.executeQuery("SELECT authorities FROM oauth2_authorization_consent")) {
      assertThat(result.next()).isTrue();
      assertThat(result.getString(1)).isEqualTo("SCOPE_mcp:access");
      assertThat(result.next()).isFalse();
    }
  }

  @Test
  void datetime_columns_use_epoch_milliseconds_across_dialects() {
    var column = OAuthJdbcColumnDefs.epochMillis("expires_at", true);
    assertThat(column.generateSqlType(new H2())).isEqualTo("BIGINT");
    assertThat(column.generateSqlType(new PostgreSql())).isEqualTo("BIGINT");
    assertThat(column.generateSqlType(new Oracle())).isEqualTo("NUMBER (38)");
    assertThat(column.generateSqlType(new MsSql())).isEqualTo("BIGINT");
  }

  private void migrate() throws SQLException {
    new CreateOAuthRegisteredClientsTable(db.database()).execute();
    new CreateOAuthAuthorizationsTable(db.database()).execute();
    new CreateOAuthAuthorizationConsentsTable(db.database()).execute();
  }
}

