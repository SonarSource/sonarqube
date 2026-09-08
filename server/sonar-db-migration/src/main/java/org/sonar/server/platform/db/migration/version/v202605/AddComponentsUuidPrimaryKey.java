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
import org.sonar.server.platform.db.migration.sql.AddPrimaryKeyBuilder;
import org.sonar.server.platform.db.migration.sql.DropPrimaryKeySqlGenerator;
import org.sonar.server.platform.db.migration.step.DdlChange;

/**
 * The 'components' table had no primary key, only the unique index dropped by {@link DropComponentsUuidIndex}.
 * A primary key improves performance on large instances, in particular by making the underlying index
 * clustered on SQL Server.
 */
public class AddComponentsUuidPrimaryKey extends DdlChange {
  static final String TABLE_NAME = "components";
  static final String COLUMN_UUID = "uuid";

  private final DropPrimaryKeySqlGenerator dropPrimaryKeySqlGenerator;

  public AddComponentsUuidPrimaryKey(Database db, DropPrimaryKeySqlGenerator dropPrimaryKeySqlGenerator) {
    super(db);
    this.dropPrimaryKeySqlGenerator = dropPrimaryKeySqlGenerator;
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(dropPrimaryKeySqlGenerator.generate(TABLE_NAME, COLUMN_UUID, false));
    context.execute(new AddPrimaryKeyBuilder(TABLE_NAME, COLUMN_UUID).build());
  }
}
