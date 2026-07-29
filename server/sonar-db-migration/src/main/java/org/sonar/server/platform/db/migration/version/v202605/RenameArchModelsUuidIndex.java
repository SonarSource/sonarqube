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

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DropIndexChange;

/**
 * {@link RenameArchIntendedToArchModels} renames the table but leaves its index carrying the old
 * {@code arch_intended_uuid} name. Indexes hold no data of their own, so unlike the table rename this
 * is safely rebuilt as drop-old/create-new rather than renamed in place.
 */
public class RenameArchModelsUuidIndex extends DropIndexChange {

  static final String TABLE_NAME = "arch_models";
  static final String OLD_INDEX_NAME = "arch_intended_uuid";
  static final String NEW_INDEX_NAME = "arch_models_uuid";
  static final String COLUMN_UUID = "uuid";

  public RenameArchModelsUuidIndex(Database db) {
    super(db, OLD_INDEX_NAME, TABLE_NAME);
  }

  @Override
  public void execute(Context context) throws SQLException {
    super.execute(context);
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, NEW_INDEX_NAME, connection)) {
        context.execute(new CreateIndexBuilder(getDialect())
          .setTable(TABLE_NAME)
          .setName(NEW_INDEX_NAME)
          .addColumn(COLUMN_UUID, false)
          .build());
      }
    }
  }
}
