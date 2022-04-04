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
package org.sonar.server.platform.db.migration.version.v84.rules.issues;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableAsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BlobColumnDef.newBlobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.TinyIntColumnDef.newTinyIntColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CopyIssuesTable extends DdlChange {
  public CopyIssuesTable(Database db) {
    super(db);
  }

  @Override public void execute(Context context) throws SQLException {
    CreateTableAsBuilder builder = new CreateTableAsBuilder(getDialect(), "issues_copy", "issues")
      // this will cause the following changes:
      // * Drop ID and RULE_ID
      // * Add RULE_UUID with values in RULE_ID casted to varchar
      .addColumn(newVarcharColumnDefBuilder().setColumnName("kee").setLimit(50).setIsNullable(false).build())
      .addColumnWithCast(newVarcharColumnDefBuilder().setColumnName("rule_uuid").setLimit(40).build(), "rule_id")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("severity").setLimit(10).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName("manual_severity").setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("message").setLimit(4000).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName("line").build())
      .addColumn(newDecimalColumnDefBuilder().setColumnName("gap").build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("status").setLimit(20).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("resolution").setLimit(20).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("checksum").setLimit(1000).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("reporter").setLimit(255).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("assignee").setLimit(255).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("author_login").setLimit(255).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("action_plan_key").setLimit(50).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("issue_attributes").setLimit(4000).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName("effort").build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_creation_date").build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_update_date").build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_close_date").build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("tags").setLimit(4000).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("component_uuid").setLimit(50).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("project_uuid").setLimit(50).build())
      .addColumn(newBlobColumnDefBuilder().setColumnName("locations").build())
      .addColumn(newTinyIntColumnDefBuilder().setColumnName("issue_type").build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName("from_hotspot").build());

    context.execute(builder.build());
        /*
         "KEE VARCHAR(50) NOT NULL",
         "RULE_UUID VARCHAR(40)",
         "SEVERITY VARCHAR(10)",
         "MANUAL_SEVERITY BOOLEAN NOT NULL",
         "MESSAGE VARCHAR(4000)",
         "LINE INTEGER",
         "GAP DOUBLE",
         "STATUS VARCHAR(20)",
         "RESOLUTION VARCHAR(20)",
         "CHECKSUM VARCHAR(1000)",
         "REPORTER VARCHAR(255)",
         "ASSIGNEE VARCHAR(255)",
         "AUTHOR_LOGIN VARCHAR(255)",
         "ACTION_PLAN_KEY VARCHAR(50)",
         "ISSUE_ATTRIBUTES VARCHAR(4000)",
         "EFFORT INTEGER",
         "CREATED_AT BIGINT",
         "UPDATED_AT BIGINT",
         "ISSUE_CREATION_DATE BIGINT",
         "ISSUE_UPDATE_DATE BIGINT",
         "ISSUE_CLOSE_DATE BIGINT",
         "TAGS VARCHAR(4000)",
         "COMPONENT_UUID VARCHAR(50)",
         "PROJECT_UUID VARCHAR(50)",
         "LOCATIONS BLOB",
         "ISSUE_TYPE TINYINT",
         "FROM_HOTSPOT BOOLEAN"
         */
  }
}
