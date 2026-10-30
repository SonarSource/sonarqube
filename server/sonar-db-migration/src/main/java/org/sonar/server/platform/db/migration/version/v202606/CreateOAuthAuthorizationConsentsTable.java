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
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.version.v202606.OAuthJdbcColumnDefs.text;

public class CreateOAuthAuthorizationConsentsTable extends CreateTableChange {
  public CreateOAuthAuthorizationConsentsTable(Database database) {
    super(database, "oauth2_authorization_consent");
  }

  @Override
  public void execute(Context context, String tableName) {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(text("registered_client_id", 100, false))
      .addPkColumn(text("principal_name", 200, false))
      .addColumn(text("authorities", 1000, false))
      .withPkConstraintName("pk_oauth2_auth_consent")
      .build());
  }
}

