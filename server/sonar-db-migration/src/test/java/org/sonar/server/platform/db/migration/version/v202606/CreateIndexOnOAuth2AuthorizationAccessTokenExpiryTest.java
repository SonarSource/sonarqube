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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnOAuth2AuthorizationAccessTokenExpiry.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnOAuth2AuthorizationAccessTokenExpiry.INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnOAuth2AuthorizationAccessTokenExpiry.TABLE_NAME;

class CreateIndexOnOAuth2AuthorizationAccessTokenExpiryTest {

  @RegisterExtension
  final MigrationDbTester db = MigrationDbTester.createEmpty();

  private final CreateIndexOnOAuth2AuthorizationAccessTokenExpiry underTest = new CreateIndexOnOAuth2AuthorizationAccessTokenExpiry(db.database());

  @BeforeEach
  void createTable() throws SQLException {
    new CreateOAuthRegisteredClientsTable(db.database()).execute();
    new CreateOAuthAuthorizationsTable(db.database()).execute();
  }

  @Test
  void creates_index_on_access_token_expiry() throws SQLException {
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);

    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);
  }

  @Test
  void is_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);
  }
}
