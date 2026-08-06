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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code organization_configs} table holding the unified Hunter agent's per-organization
 * enablement configuration. There is at most one row per organization, so {@code organization_id} is
 * backed by a unique index.
 *
 * <p>Boolean columns are rendered per dialect by the framework ({@code BOOLEAN} on PostgreSQL/H2,
 * {@code NUMBER(1)} on Oracle, {@code BIT} on MS SQL Server); uuids are stored as {@code VARCHAR(40)},
 * consistent with the other unified capability tables.
 */
public class CreateOrganizationConfigsTable extends CreateTableChange {

  static final String TABLE_NAME = "organization_configs";

  static final String COLUMN_ID = "id";
  static final String COLUMN_ORGANIZATION_ID = "organization_id";
  static final String COLUMN_IS_ENABLED = "is_enabled";
  static final String COLUMN_IS_BETA_ENROLLED = "is_beta_enrolled";

  static final int ID_SIZE = 40;
  static final int ORGANIZATION_ID_SIZE = 40;

  static final String INDEX_ORGANIZATION_ID = "uq_org_configs_org_id";

  protected CreateOrganizationConfigsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ORGANIZATION_ID).setIsNullable(false).setLimit(ORGANIZATION_ID_SIZE).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_IS_ENABLED).setIsNullable(false).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_IS_BETA_ENROLLED).setIsNullable(false).setDefaultValue(false).build())
      .build());

    // Unique index: at most one config row per organization. Also serves lookups by organization_id.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_ORGANIZATION_ID)
      .setUnique(true)
      .addColumn(COLUMN_ORGANIZATION_ID, false)
      .build());
  }
}
