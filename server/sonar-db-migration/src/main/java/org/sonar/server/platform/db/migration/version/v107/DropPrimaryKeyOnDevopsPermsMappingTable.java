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

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.sql.DropPrimaryKeySqlGenerator;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.version.v107.RenameGithubPermsMappingTable.DEVOPS_PERMS_MAPPING_TABLE_NAME;

public class DropPrimaryKeyOnDevopsPermsMappingTable extends DdlChange {

  @VisibleForTesting
  static final String UUID_COLUMN_NAME = "uuid";
  @VisibleForTesting
  static final String CONSTRAINT_NAME = "pk_github_perms_mapping";

  private final DropPrimaryKeySqlGenerator dropPrimaryKeySqlGenerator;
  private final DbPrimaryKeyConstraintFinder dbConstraintFinder;

  public DropPrimaryKeyOnDevopsPermsMappingTable(Database db, DropPrimaryKeySqlGenerator dropPrimaryKeySqlGenerator, DbPrimaryKeyConstraintFinder dbConstraintFinder) {
    super(db);
    this.dropPrimaryKeySqlGenerator = dropPrimaryKeySqlGenerator;
    this.dbConstraintFinder = dbConstraintFinder;
  }

  @Override
  public void execute(Context context) throws SQLException {
    Optional<String> constraintName = dbConstraintFinder.findConstraintName(DEVOPS_PERMS_MAPPING_TABLE_NAME);
    if (constraintName.isPresent() && constraintName.get().equalsIgnoreCase(CONSTRAINT_NAME)) {
      List<String> statements = dropPrimaryKeySqlGenerator.generate(DEVOPS_PERMS_MAPPING_TABLE_NAME, UUID_COLUMN_NAME, false);
      context.execute(statements);
    }
  }

}
