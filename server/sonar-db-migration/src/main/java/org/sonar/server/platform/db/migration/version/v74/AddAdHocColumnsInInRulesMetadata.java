/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.TinyIntColumnDef.newTinyIntColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

@SupportsBlueGreen
public class AddAdHocColumnsInInRulesMetadata extends DdlChange {

  public AddAdHocColumnsInInRulesMetadata(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new AddColumnsBuilder(getDialect(), "rules_metadata")
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("ad_hoc_name")
        .setLimit(200)
        .setIsNullable(true)
        .build())
      .addColumn(newClobColumnDefBuilder()
        .setColumnName("ad_hoc_description")
        .setIsNullable(true)
        .build())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("ad_hoc_severity")
        .setIsNullable(true)
        .setLimit(10)
        .build())
      .addColumn(newTinyIntColumnDefBuilder()
        .setColumnName("ad_hoc_type")
        .setIsNullable(true)
        .build())
      .build());
  }
}
