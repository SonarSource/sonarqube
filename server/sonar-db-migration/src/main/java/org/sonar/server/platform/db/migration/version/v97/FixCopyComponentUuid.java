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
package org.sonar.server.platform.db.migration.version.v97;

import java.sql.SQLException;
import org.apache.commons.lang.StringUtils;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class FixCopyComponentUuid extends DataChange {
  private static final String BRANCH_IDENTIFIER = ":BRANCH:";
  private static final String SELECT_QUERY = "select kee, uuid, copy_component_uuid from components where copy_component_uuid is not null";
  private static final String UPDATE_QUERY = "update components set copy_component_uuid = ? where uuid = ?";

  public FixCopyComponentUuid(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update(UPDATE_QUERY);
    massUpdate.execute((row, update) -> {
      String componentKey = row.getString(1);
      String componentUuid = row.getString(2);
      String copyComponentUuid = row.getString(3);

      String branchKey = StringUtils.substringAfterLast(componentKey, BRANCH_IDENTIFIER);

      if (StringUtils.isEmpty(branchKey)) {
        return false;
      }

      String branchUuid = context.prepareSelect("select uuid from project_branches where project_uuid = ? and branch_type = 'BRANCH' and kee = ?")
        .setString(1, copyComponentUuid)
        .setString(2, branchKey)
        .get(t -> t.getString(1));

      if (branchUuid != null && !branchUuid.equals(copyComponentUuid)) {
        update.setString(1, branchUuid)
          .setString(2, componentUuid);
        return true;
      }

      return false;
    });
  }
}
