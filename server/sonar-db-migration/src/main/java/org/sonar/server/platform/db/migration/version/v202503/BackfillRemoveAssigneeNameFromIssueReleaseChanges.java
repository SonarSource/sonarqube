/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202503;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class BackfillRemoveAssigneeNameFromIssueReleaseChanges extends DataChange {
  // Select records that might contain assigneeName in their change_data
  private static final String SELECT_QUERY = "select uuid, change_data from sca_issue_rels_changes where change_data like '%assigneeName%'";
  private static final String UPDATE_QUERY = "update sca_issue_rels_changes set change_data = ? where uuid = ?";
  private static final Gson GSON = new Gson();

  public BackfillRemoveAssigneeNameFromIssueReleaseChanges(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update(UPDATE_QUERY);

    massUpdate.execute((row, update, index) -> {
      String uuid = row.getString(1);
      String changeData = row.getString(2);

      if (changeData == null) {
        return false;
      }

      String updatedChangeData = removeAssigneeNameFromChangeData(changeData);

      if (updatedChangeData.equals(changeData)) {
        return false;
      }
      update.setString(1, updatedChangeData);
      update.setString(2, uuid);
      return true;
    });
  }

  private static String removeAssigneeNameFromChangeData(String changeData) {
    var obj = JsonParser.parseString(changeData).getAsJsonObject();
    obj.remove("assigneeName");
    return GSON.toJson(obj);
  }
}
