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
import java.util.List;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.AddPrimaryKeyBuilder;
import org.sonar.server.platform.db.migration.sql.DropPrimaryKeySqlGenerator;
import org.sonar.server.platform.db.migration.step.DdlChange;

/**
 * {@link RenameArchIntendedToArchModels} renames the table but, unlike columns and indexes, a primary key
 * constraint keeps whatever name it had before the rename ({@code pk_arch_intended}). Dropping and
 * re-adding it is the only way to get it renamed to the {@code pk_} + table-name convention used elsewhere.
 */
public class RenameArchModelsPrimaryKeyConstraint extends DdlChange {

  static final String TABLE_NAME = "arch_models";
  static final String COLUMN_PROJECT_ID = "project_id";
  static final String COLUMN_UUID = "uuid";
  static final String NEW_CONSTRAINT_NAME = "pk_" + TABLE_NAME;

  private final DropPrimaryKeySqlGenerator dropPrimaryKeySqlGenerator;

  public RenameArchModelsPrimaryKeyConstraint(Database db, DropPrimaryKeySqlGenerator dropPrimaryKeySqlGenerator) {
    super(db);
    this.dropPrimaryKeySqlGenerator = dropPrimaryKeySqlGenerator;
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(dropPrimaryKeySqlGenerator.generate(TABLE_NAME, List.of(COLUMN_PROJECT_ID, COLUMN_UUID), false));
    context.execute(new AddPrimaryKeyBuilder(TABLE_NAME, COLUMN_PROJECT_ID, COLUMN_UUID).build());
  }
}
