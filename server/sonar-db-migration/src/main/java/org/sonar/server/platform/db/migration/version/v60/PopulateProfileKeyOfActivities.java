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
import java.util.Map;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.apache.commons.lang.StringUtils.isBlank;

public class PopulateProfileKeyOfActivities extends DataChange {

  public PopulateProfileKeyOfActivities(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id, data_field from activities where profile_key is null");
    massUpdate.update("update activities set profile_key=?, data_field=? where id=?");
    massUpdate.rowPluralName("activities");
    massUpdate.execute(PopulateProfileKeyOfActivities::handle);

    // SONAR-8534 delete orphans
    context.prepareUpsert("delete from activities where profile_key is null")
      .execute()
      .commit();
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    int id = row.getInt(1);
    String data = row.getString(2);
    Map<String, String> fields = KeyValueFormat.parse(data);
    String profileKey = fields.remove("profileKey");
    if (isBlank(profileKey)) {
      return false;
    }
    update.setString(1, profileKey);
    update.setString(2, KeyValueFormat.format(fields));
    update.setInt(3, id);
    return true;
  }
}
