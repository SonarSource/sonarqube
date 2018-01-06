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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.step.DataChange;

public class FixProjectUuidOfDeveloperProjects extends DataChange {

  public FixProjectUuidOfDeveloperProjects(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select distinct p.person_id,d.uuid from projects p, projects d where p.qualifier = 'DEV_PRJ' and p.project_uuid != d.uuid and d.id = p.person_id");
    massUpdate.update("update projects set project_uuid = ? where person_id = ? and qualifier = 'DEV_PRJ' and project_uuid != ?");
    massUpdate.rowPluralName("developers with incorrect project_uuid");
    massUpdate.execute(FixProjectUuidOfDeveloperProjects::handleComponent);
  }

  private static boolean handleComponent(Select.Row row, SqlStatement update) throws SQLException {
    long personId = row.getLong(1);
    String developerUuid = row.getString(2);
    update.setString(1, developerUuid);
    update.setLong(2, personId);
    update.setString(3, developerUuid);

    return true;
  }
}
