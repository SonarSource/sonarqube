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
package org.sonar.server.platform.db.migration.version.v105;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.sql.DropPrimaryKeySqlGenerator;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class DropPrimaryKeyConstraintInRulesDefaultImpactsTable extends DdlChange {
  @VisibleForTesting
  static final String TABLE_NAME = "rules_default_impacts";
  @VisibleForTesting
  static final String COLUMN_NAME = "uuid";
  @VisibleForTesting
  static final String CONSTRAINT_NAME = "pk_rules_default_impacts";
  private final DropPrimaryKeySqlGenerator dropPrimaryKeySqlGenerator;
  private final DbPrimaryKeyConstraintFinder dbConstraintFinder;

  public DropPrimaryKeyConstraintInRulesDefaultImpactsTable(Database db, DropPrimaryKeySqlGenerator dropPrimaryKeySqlGenerator, DbPrimaryKeyConstraintFinder dbConstraintFinder) {
    super(db);
    this.dropPrimaryKeySqlGenerator = dropPrimaryKeySqlGenerator;
    this.dbConstraintFinder = dbConstraintFinder;
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (DatabaseUtils.tableColumnExists(connection, TABLE_NAME, COLUMN_NAME)) {
        Optional<String> constraintName = dbConstraintFinder.findConstraintName(TABLE_NAME);
        if (constraintName.isPresent() && constraintName.get().equalsIgnoreCase(CONSTRAINT_NAME)) {
          List<String> statements = dropPrimaryKeySqlGenerator.generate(TABLE_NAME, COLUMN_NAME, false);
          context.execute(statements);
        }
      }
    }
  }
}
