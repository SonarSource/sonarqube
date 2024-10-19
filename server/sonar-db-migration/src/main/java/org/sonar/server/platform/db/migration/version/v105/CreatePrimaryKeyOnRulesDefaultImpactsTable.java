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
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.AddPrimaryKeyBuilder;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class CreatePrimaryKeyOnRulesDefaultImpactsTable extends DdlChange {
  @VisibleForTesting
  static final String PK_NAME = "pk_rules_default_impacts";
  @VisibleForTesting
  static final String TABLE_NAME = "rules_default_impacts";
  @VisibleForTesting
  static final String RULE_UUID_COLUMN_NAME = "rule_uuid";
  @VisibleForTesting
  static final String SOFTWARE_QUALITY_COLUMN = "software_quality";


  public CreatePrimaryKeyOnRulesDefaultImpactsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    createPrimaryKey(context);
  }

  private void createPrimaryKey(Context context) throws SQLException {
    boolean pkExists = new DbPrimaryKeyConstraintFinder(getDatabase()).findConstraintName(TABLE_NAME).isPresent();
    if (!pkExists) {
      context.execute(new AddPrimaryKeyBuilder(TABLE_NAME, RULE_UUID_COLUMN_NAME, SOFTWARE_QUALITY_COLUMN).build());
    }
  }
}
