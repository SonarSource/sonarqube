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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;

public class CreateTemporaryIndicesFor1211 extends DdlChange {

  static final String INDEX_ON_CE_ACTIVITY = "ce_activity_snapshot_id";
  static final String INDEX_ON_DUPLICATIONS_INDEX = "dup_index_psid";

  public CreateTemporaryIndicesFor1211(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new CreateIndexBuilder(getDialect())
      .setTable("ce_activity")
      .setName(INDEX_ON_CE_ACTIVITY)
      .addColumn(newIntegerColumnDefBuilder().setColumnName("snapshot_id").build())
      .build());
    context.execute(new CreateIndexBuilder(getDialect())
      .setTable("duplications_index")
      .setName(INDEX_ON_DUPLICATIONS_INDEX)
      .addColumn(newIntegerColumnDefBuilder().setColumnName("project_snapshot_id").build())
      .build());
  }
}
