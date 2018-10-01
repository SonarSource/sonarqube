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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

@SupportsBlueGreen
public class DropTmpColumnsToCeActivity extends DdlChange {
  private static final String TABLE_NAME = "ce_activity";

  public DropTmpColumnsToCeActivity(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("ce_activity_t_islast_key")
      .build());
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("ce_activity_t_islast")
      .build());
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("ce_activity_t_main_islast_key")
      .build());
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("ce_activity_t_main_islast")
      .build());
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("ce_activity_tmp_cpnt_uuid")
      .build());
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("ce_activity_tmp_main_cpnt_uuid")
      .build());

    context.execute(new DropColumnsBuilder(getDialect(), TABLE_NAME,
      "tmp_is_last", "tmp_is_last_key", "tmp_main_is_last", "tmp_main_is_last_key",
      "tmp_component_uuid", "tmp_main_component_uuid")
        .build());
  }
}
