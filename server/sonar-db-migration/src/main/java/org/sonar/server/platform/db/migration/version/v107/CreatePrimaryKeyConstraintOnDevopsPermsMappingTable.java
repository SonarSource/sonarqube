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
package org.sonar.server.platform.db.migration.version.v107;

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.AddPrimaryKeyBuilder;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.version.v107.RenameGithubPermsMappingTable.DEVOPS_PERMS_MAPPING_TABLE_NAME;

public class CreatePrimaryKeyConstraintOnDevopsPermsMappingTable extends DdlChange {

  @VisibleForTesting
  static final String UUID_COLUMN_NAME = "uuid";

  public CreatePrimaryKeyConstraintOnDevopsPermsMappingTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    createPrimaryKey(context);
  }

  private void createPrimaryKey(Context context) throws SQLException {
    boolean pkExists = new DbPrimaryKeyConstraintFinder(getDatabase()).findConstraintName(DEVOPS_PERMS_MAPPING_TABLE_NAME).isPresent();
    if (!pkExists) {
      context.execute(new AddPrimaryKeyBuilder(DEVOPS_PERMS_MAPPING_TABLE_NAME, UUID_COLUMN_NAME).build());
    }
  }
}
