/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.db.version.v60;

import java.sql.SQLException;
import java.util.Map;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class PopulateProfileKeyOfActivities extends BaseDataChange {

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
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    int id = row.getInt(1);
    String data = row.getString(2);
    Map<String, String> fields = KeyValueFormat.parse(data);
    String profileKey = fields.remove("profileKey");
    checkState(isNotBlank(profileKey), "No profile key found in db row of activities.data_field", id);

    update.setString(1, profileKey);
    update.setString(2, KeyValueFormat.format(fields));
    update.setInt(3, id);

    return true;
  }
}
