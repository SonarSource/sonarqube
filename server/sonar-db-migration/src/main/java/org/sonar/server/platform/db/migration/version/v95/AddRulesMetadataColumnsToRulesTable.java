/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v95;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.TinyIntColumnDef.newTinyIntColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.USER_UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v00.CreateInitialSchema.newVarcharColumnBuilder;

public class AddRulesMetadataColumnsToRulesTable extends DdlChange {

  private static final String TABLE_NAME = "rules";


  public AddRulesMetadataColumnsToRulesTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      createNoteDataAt(context, connection);
      createNoteUserUuid(context, connection);
      createNoteCreatedAt(context, connection);
      createNoteUpdatedAt(context, connection);
      createRemediationFunction(context, connection);
      createRemediationGapMult(context, connection);
      createRemediationBaseEffort(context, connection);
      createTags(context, connection);
      createAdHocName(context, connection);
      createAdHocDescription(context, connection);
      createAdHocSeverity(context, connection);
      createAdHocType(context, connection);
    }
  }

  private void createAdHocType(Context context, Connection connection) {
    if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, "ad_hoc_type")) {
      context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
        .addColumn(newTinyIntColumnDefBuilder().setColumnName("ad_hoc_type").setIsNullable(true).build())
        .build());
    }
  }

  private void createAdHocSeverity(Context context, Connection connection) {
    if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, "ad_hoc_severity")) {
      context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
        .addColumn(newVarcharColumnBuilder("ad_hoc_severity").setLimit(10).setIsNullable(true).build())
        .build());
    }
  }

  private void createAdHocDescription(Context context, Connection connection) {
    if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, "ad_hoc_description")) {
      context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
        .addColumn(newClobColumnDefBuilder().setColumnName("ad_hoc_description").setIsNullable(true).build())
        .build());
    }
  }

  private void createAdHocName(Context context, Connection connection) {
    if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, "ad_hoc_name")) {
      context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
        .addColumn(newVarcharColumnBuilder("ad_hoc_name").setLimit(200).setIsNullable(true).build())
        .build());
    }
  }

  private void createTags(Context context, Connection connection) {
    if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, "tags")) {
      context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
        .addColumn(newVarcharColumnBuilder("tags").setLimit(4_000).setIsNullable(true).build())
        .build());
    }
  }

  private void createRemediationBaseEffort(Context context, Connection connection) {
    if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, "remediation_base_effort")) {
      context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
        .addColumn(newVarcharColumnBuilder("remediation_base_effort").setLimit(20).setIsNullable(true).build())
        .build());
    }
  }

  private void createRemediationGapMult(Context context, Connection connection) {
    if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, "remediation_gap_mult")) {
      context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
        .addColumn(newVarcharColumnBuilder("remediation_gap_mult").setLimit(20).setIsNullable(true).build())
        .build());
    }
  }

  private void createRemediationFunction(Context context, Connection connection) {
    if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, "remediation_function")) {
      context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
        .addColumn(newVarcharColumnBuilder("remediation_function").setLimit(20).setIsNullable(true).build())
        .build());
    }
  }

  private void createNoteUpdatedAt(Context context, Connection connection) {
    if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, "note_updated_at")) {
      context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("note_updated_at").setIsNullable(true).build())
        .build());
    }
  }

  private void createNoteCreatedAt(Context context, Connection connection) {
    if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, "note_created_at")) {
      context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("note_created_at").setIsNullable(true).build())
        .build());
    }
  }

  private void createNoteUserUuid(Context context, Connection connection) {
    if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, "note_user_uuid")) {
      context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
        .addColumn(newVarcharColumnBuilder("note_user_uuid").setLimit(USER_UUID_SIZE).setIsNullable(true).build())
        .build());
    }
  }

  private void createNoteDataAt(Context context, Connection connection) {
    if (!DatabaseUtils.tableColumnExists(connection, TABLE_NAME, "note_data")) {
      context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
        .addColumn(newClobColumnDefBuilder().setColumnName("note_data").setIsNullable(true).build())
        .build());
    }
  }
}
