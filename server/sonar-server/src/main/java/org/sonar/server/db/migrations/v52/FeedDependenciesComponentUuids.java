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

import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

import java.sql.SQLException;

/**
 * Add the following columns to the dependencies table :
 * - from_component_uuid
 * - to_component_uuid
 */
public class FeedDependenciesComponentUuids extends BaseDataChange {


  public FeedDependenciesComponentUuids(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("dependencies");
    update.select(
      "SELECT from_component.uuid, to_component.uuid, dependency.id " +
        "FROM dependencies dependency " +
        "INNER JOIN projects from_component ON from_component.id=dependency.from_resource_id " +
        "INNER JOIN projects to_component ON to_component.id=dependency.to_resource_id " +
        "WHERE dependency.from_component_uuid IS NULL");
    update.update("UPDATE dependencies SET from_component_uuid=?, to_component_uuid=? WHERE id=?");
    update.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        update.setString(1, row.getString(1));
        update.setString(2, row.getString(2));
        update.setLong(3, row.getLong(3));
        return true;
      }
    });
  }

}
