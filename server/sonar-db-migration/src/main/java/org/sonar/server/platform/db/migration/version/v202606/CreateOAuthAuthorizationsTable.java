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

import static org.sonar.server.platform.db.migration.version.v202606.OAuthJdbcColumnDefs.clob;
import static org.sonar.server.platform.db.migration.version.v202606.OAuthJdbcColumnDefs.epochMillis;
import static org.sonar.server.platform.db.migration.version.v202606.OAuthJdbcColumnDefs.text;

public class CreateOAuthAuthorizationsTable extends CreateTableChange {
  public CreateOAuthAuthorizationsTable(Database database) {
    super(database, "oauth2_authorization");
  }

  @Override
  public void execute(Context context, String tableName) {
    var table = new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(text("id", 100, false))
      .addColumn(text("registered_client_id", 100, false))
      .addColumn(text("principal_name", 200, false))
      .addColumn(text("authorization_grant_type", 100, false))
      .addColumn(text("authorized_scopes", 1000, true))
      .addColumn(clob("attributes"))
      .addColumn(text("state", 500, true))
      .addColumn(text("access_token_type", 100, true))
      .addColumn(text("access_token_scopes", 1000, true));
    for (String token : new String[] {"authorization_code", "access_token", "oidc_id_token", "refresh_token", "user_code", "device_code"}) {
      table.addColumn(text(token + "_value", 512, true))
        .addColumn(epochMillis(token + "_issued_at", true))
        .addColumn(epochMillis(token + "_expires_at", true))
        .addColumn(clob(token + "_metadata"));
    }
    context.execute(table.build());
    context.execute(new CreateIndexBuilder(getDialect()).setTable(tableName).setName("oauth2_auth_access_token")
      .addColumn("access_token_value", true).build());
    context.execute(new CreateIndexBuilder(getDialect()).setTable(tableName).setName("oauth2_auth_code")
      .addColumn("authorization_code_value", true).build());
    context.execute(new CreateIndexBuilder(getDialect()).setTable(tableName).setName("oauth2_auth_refresh_token")
      .addColumn("refresh_token_value", true).build());
  }
}

