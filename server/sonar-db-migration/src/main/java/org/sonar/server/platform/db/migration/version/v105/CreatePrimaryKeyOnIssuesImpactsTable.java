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
package org.sonar.server.platform.db.migration.version.v105;

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.AddPrimaryKeyBuilder;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class CreatePrimaryKeyOnIssuesImpactsTable extends DdlChange {
  @VisibleForTesting
  static final String PK_NAME = "pk_issues_impacts";
  @VisibleForTesting
  static final String TABLE_NAME = "issues_impacts";
  @VisibleForTesting
  static final String ISSUE_KEY_COLUMN_NAME = "issue_key";
  @VisibleForTesting
  static final String SOFTWARE_QUALITY_COLUMN = "software_quality";


  public CreatePrimaryKeyOnIssuesImpactsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(DdlChange.Context context) throws SQLException {
    createPrimaryKey(context);
  }

  private void createPrimaryKey(DdlChange.Context context) throws SQLException {
    boolean pkExists = new DbPrimaryKeyConstraintFinder(getDatabase()).findConstraintName(TABLE_NAME).isPresent();
    if (!pkExists) {
      context.execute(new AddPrimaryKeyBuilder(TABLE_NAME, ISSUE_KEY_COLUMN_NAME, SOFTWARE_QUALITY_COLUMN).build());
    }
  }
}
