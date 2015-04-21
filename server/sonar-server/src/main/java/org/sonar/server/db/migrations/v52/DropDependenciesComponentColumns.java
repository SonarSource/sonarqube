/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.db.migrations.v52;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DdlChange;
import org.sonar.server.db.migrations.DropColumnsBuilder;

import java.sql.SQLException;

/**
 * Remove the following columns from the dependencies table :
 * - from_resource_id
 * - to_resource_id
 */
public class DropDependenciesComponentColumns extends DdlChange {

  private final Database db;

  public DropDependenciesComponentColumns(Database db) {
    super(db);
    this.db = db;
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(generateSql());
  }

  @VisibleForTesting
  String generateSql() {
    return new DropColumnsBuilder(db.getDialect(), "dependencies", "from_resource_id", "to_resource_id")
      .build();
  }

}
