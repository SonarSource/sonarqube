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

import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.version.v202606.OAuthJdbcColumnDefs.epochMillis;
import static org.sonar.server.platform.db.migration.version.v202606.OAuthJdbcColumnDefs.text;

public class CreateOAuthRegisteredClientsTable extends CreateTableChange {
  public CreateOAuthRegisteredClientsTable(Database database) {
    super(database, "oauth2_registered_client");
  }

  @Override
  public void execute(Context context, String tableName) {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(text("id", 100, false))
      .addColumn(text("client_id", 512, false))
      .addColumn(epochMillis("client_id_issued_at", false))
      .addColumn(text("client_secret", 200, true))
      .addColumn(epochMillis("client_secret_expires_at", true))
      .addColumn(text("client_name", 200, false))
      .addColumn(text("client_authentication_methods", 1000, false))
      .addColumn(text("authorization_grant_types", 1000, false))
      .addColumn(text("redirect_uris", 1000, true))
      .addColumn(text("post_logout_redirect_uris", 1000, true))
      .addColumn(text("scopes", 1000, false))
      .addColumn(text("client_settings", 2000, false))
      .addColumn(text("token_settings", 2000, false))
      .build());
    context.execute(new CreateIndexBuilder(getDialect()).setTable(tableName).setName("oauth2_client_id_uniq")
      .setUnique(true).addColumn("client_id", false).build());
  }
}

