/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;

public class AlterCveColumnsToNullable extends DdlChange {

  public static final BigIntegerColumnDef LAST_MODIFIED_COLUMN = newBigIntegerColumnDefBuilder().setColumnName("last_modified_at").setIsNullable(true).build();
  public static final BigIntegerColumnDef PUBLISHED_COLUMN = newBigIntegerColumnDefBuilder().setColumnName("published_at").setIsNullable(true).build();


  public AlterCveColumnsToNullable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new AlterColumnsBuilder(getDialect(), "cves")
      .updateColumn(LAST_MODIFIED_COLUMN)
      .build());
    context.execute(new AlterColumnsBuilder(getDialect(), "cves")
      .updateColumn(PUBLISHED_COLUMN)
      .build());
  }
}
