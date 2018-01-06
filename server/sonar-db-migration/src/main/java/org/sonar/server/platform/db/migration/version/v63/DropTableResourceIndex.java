/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.DropIndexBuilder;
import org.sonar.server.platform.db.migration.sql.DropTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class DropTableResourceIndex extends DdlChange {

  private static final String TABLE_RESOURCE_INDEX = "resource_index";

  public DropTableResourceIndex(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_RESOURCE_INDEX)
      .setName("resource_index_key")
      .build());

    try {
      context.execute(new DropIndexBuilder(getDialect())
        .setTable(TABLE_RESOURCE_INDEX)
        .setName("resource_index_component")
        .build());
    } catch (Exception e) {
      // migrating from 5.6. The migration 1204 MakeUuidColumnsNotNullOnResourceIndex,
      // introduced in 6.0, has been dropped in 6.7 for performance reasons. There was no need to
      // alter the table resource_index while it's dropped later in 6.3.
      // As a consequence this index may not exist when upgrading from 6.1+.
      // Note that the "delete index if exists" is still not supported by MySQL, Oracle and MSSQL < 2016,
      // that's why an exception is raised if the index does not exist.
    }

    context.execute(new DropTableBuilder(getDialect(), TABLE_RESOURCE_INDEX).build());
  }

}
