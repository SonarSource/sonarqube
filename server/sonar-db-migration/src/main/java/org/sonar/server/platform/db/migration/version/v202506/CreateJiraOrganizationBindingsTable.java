/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateJiraOrganizationBindingsTable extends CreateTableChange {

  static final String TABLE_NAME = "jira_org_bindings";

  static final String COLUMN_ID = "id";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";
  static final String COLUMN_SONAR_ORGANIZATION_UUID = "sonar_organization_uuid";
  static final String COLUMN_JIRA_INSTANCE_URL = "jira_instance_url";
  static final String COLUMN_JIRA_CLOUD_ID = "jira_cloud_id";
  static final String COLUMN_JIRA_ACCESS_TOKEN = "jira_access_token";
  static final String COLUMN_JIRA_ACCESS_TOKEN_EXPIRES_AT = "jira_access_token_expires_at";
  static final String COLUMN_JIRA_REFRESH_TOKEN = "jira_refresh_token";
  static final String COLUMN_JIRA_REFRESH_TOKEN_CREATED_AT = "jira_refresh_token_created_at";
  static final String COLUMN_JIRA_REFRESH_TOKEN_UPDATED_AT = "jira_refresh_token_updated_at";
  static final String COLUMN_UPDATED_BY = "updated_by";

  protected CreateJiraOrganizationBindingsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(40).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SONAR_ORGANIZATION_UUID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_JIRA_INSTANCE_URL).setIsNullable(true).setLimit(2048).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_JIRA_CLOUD_ID).setIsNullable(true).setLimit(100).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_JIRA_ACCESS_TOKEN).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_JIRA_ACCESS_TOKEN_EXPIRES_AT).setIsNullable(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_JIRA_REFRESH_TOKEN).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_JIRA_REFRESH_TOKEN_CREATED_AT).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_JIRA_REFRESH_TOKEN_UPDATED_AT).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UPDATED_BY).setIsNullable(true).setLimit(40).build())
      .build());
  }
}
