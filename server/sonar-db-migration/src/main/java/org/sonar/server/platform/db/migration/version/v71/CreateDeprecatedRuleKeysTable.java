/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v71;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.IntegerColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class CreateDeprecatedRuleKeysTable extends DdlChange {

  private static final String DEPRECATED_RULE_KEYS = "deprecated_rule_keys";
  private static final VarcharColumnDef UUID_COLUMN = VarcharColumnDef.newVarcharColumnDefBuilder()
    .setColumnName("uuid")
    .setIsNullable(false)
    .setLimit(VarcharColumnDef.UUID_SIZE)
    .build();
  private static final IntegerColumnDef RULE_ID_COLUMN = IntegerColumnDef.newIntegerColumnDefBuilder()
    .setColumnName("rule_id")
    .setIsNullable(false)
    .build();
  private static final VarcharColumnDef OLD_REPOSITORY_KEY_COLUMN = VarcharColumnDef.newVarcharColumnDefBuilder()
    .setColumnName("old_repository_key")
    .setIsNullable(false)
    .setLimit(255)
    .build();
  private static final VarcharColumnDef OLD_RULE_KEY_COLUMN = VarcharColumnDef.newVarcharColumnDefBuilder()
    .setColumnName("old_rule_key")
    .setIsNullable(false)
    .setLimit(200)
    .build();
  private static final BigIntegerColumnDef CREATED_AT_COLUMN = BigIntegerColumnDef.newBigIntegerColumnDefBuilder()
    .setColumnName("created_at")
    .setIsNullable(false)
    .build();

  private Database db;

  public CreateDeprecatedRuleKeysTable(Database db) {
    super(db);
    this.db = db;
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (!tableExists()) {
      context.execute(new CreateTableBuilder(getDialect(), DEPRECATED_RULE_KEYS)
        .addPkColumn(UUID_COLUMN)
        .addColumn(RULE_ID_COLUMN)
        .addColumn(OLD_REPOSITORY_KEY_COLUMN)
        .addColumn(OLD_RULE_KEY_COLUMN)
        .addColumn(CREATED_AT_COLUMN)
        .build()
      );

      context.execute(new CreateIndexBuilder(getDialect())
        .setTable(DEPRECATED_RULE_KEYS)
        .addColumn(OLD_REPOSITORY_KEY_COLUMN)
        .addColumn(OLD_RULE_KEY_COLUMN)
        .setUnique(true)
        .setName("uniq_deprecated_rule_keys")
        .build()
      );

      context.execute(new CreateIndexBuilder(getDialect())
        .setTable(DEPRECATED_RULE_KEYS)
        .addColumn(RULE_ID_COLUMN)
        .setUnique(true)
        .setName("rule_id_deprecated_rule_keys")
        .build()
      );
    }
  }

  private boolean tableExists() throws SQLException {
    try (Connection connection = db.getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(DEPRECATED_RULE_KEYS, connection);
    }
  }
}
