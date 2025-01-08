/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v107;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.DecimalColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.DESCRIPTION_SECTION_KEY_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.MAX_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateCvesTable extends CreateTableChange {

  private static final String TABLE_NAME = "cves";

  private static final VarcharColumnDef UUID_COLUMN = newVarcharColumnDefBuilder().setColumnName("uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
  private static final VarcharColumnDef ID_COLUMN = newVarcharColumnDefBuilder().setColumnName("id").setIsNullable(false).setLimit(DESCRIPTION_SECTION_KEY_SIZE).build();
  private static final VarcharColumnDef DESCRIPTION_COLUMN = newVarcharColumnDefBuilder().setColumnName("description").setIsNullable(false).setLimit(MAX_SIZE).build();
  public static final BigIntegerColumnDef UPDATED_AT_COLUMN = newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build();
  public static final BigIntegerColumnDef CREATED_AT_COLUMN = newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build();
  public static final BigIntegerColumnDef LAST_MODIFIED_COLUMN = newBigIntegerColumnDefBuilder().setColumnName("last_modified_at").setIsNullable(false).build();
  public static final BigIntegerColumnDef PUBLISHED_COLUMN = newBigIntegerColumnDefBuilder().setColumnName("published_at").setIsNullable(false).build();
  public static final DecimalColumnDef CVSS_SCORE_COLUMN = newDecimalColumnDefBuilder().setColumnName("cvss_score").setIsNullable(true).build();
  public static final DecimalColumnDef EPSS_SCORE_COLUMN = newDecimalColumnDefBuilder().setColumnName("epss_score").setIsNullable(true).build();
  public static final DecimalColumnDef EPSS_PERCENTILE_COLUMN = newDecimalColumnDefBuilder().setColumnName("epss_percentile").setIsNullable(true).build();

  protected CreateCvesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(UUID_COLUMN)
      .addColumn(ID_COLUMN)
      .addColumn(DESCRIPTION_COLUMN)
      .addColumn(CVSS_SCORE_COLUMN)
      .addColumn(EPSS_SCORE_COLUMN)
      .addColumn(EPSS_PERCENTILE_COLUMN)
      .addColumn(PUBLISHED_COLUMN)
      .addColumn(LAST_MODIFIED_COLUMN)
      .addColumn(CREATED_AT_COLUMN)
      .addColumn(UPDATED_AT_COLUMN)
      .build());
  }
}
