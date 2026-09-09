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

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateNotificationGroupSubscriptionsTable extends CreateTableChange {

  static final String TABLE_NAME = "notif_group_subscriptions";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_GROUP_UUID = "group_uuid";
  static final String COLUMN_NOTIFICATION_TYPE = "notification_type";
  static final String COLUMN_CHANNEL_KEY = "channel_key";
  static final String COLUMN_CREATED_AT = "created_at";

  static final int UUID_SIZE = 40;
  static final int NOTIFICATION_TYPE_SIZE = 100;
  static final int CHANNEL_KEY_SIZE = 100;

  protected CreateNotificationGroupSubscriptionsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_GROUP_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_NOTIFICATION_TYPE).setIsNullable(false).setLimit(NOTIFICATION_TYPE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_CHANNEL_KEY).setIsNullable(false).setLimit(CHANNEL_KEY_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName("notif_grp_sub_uniq")
      .setUnique(true)
      .addColumn(COLUMN_GROUP_UUID, false)
      .addColumn(COLUMN_NOTIFICATION_TYPE, false)
      .addColumn(COLUMN_CHANNEL_KEY, false)
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName("notif_grp_sub_type")
      .setUnique(false)
      .addColumn(COLUMN_NOTIFICATION_TYPE, false)
      .addColumn(COLUMN_CHANNEL_KEY, false)
      .build());
  }
}
