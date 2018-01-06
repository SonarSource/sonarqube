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
package org.sonar.server.platform.db.migration.version.v61;

import java.sql.SQLException;
import java.util.Date;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class CopyActivitiesToQprofileChanges extends DataChange {

  public CopyActivitiesToQprofileChanges(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.rowPluralName("activities");
    massUpdate
      .select("select a.log_key, a.profile_key, a.created_at, a.user_login, a.log_action, a.data_field " +
        "from activities a " +
        "left join qprofile_changes qc on qc.kee = a.log_key " +
        "where a.log_type=? " +
        "and a.log_action is not null " +
        "and a.profile_key is not null " +
        "and a.created_at is not null " +
        "and qc.kee is null")
      .setString(1, "QPROFILE");

    massUpdate.update("insert into qprofile_changes (kee, qprofile_key, created_at, user_login, change_type, change_data) values (?,?,?,?,?,?)");
    massUpdate.execute((row, update) -> {
      String key = row.getString(1);
      String profileKey = row.getString(2);
      Date createdAt = row.getDate(3);
      String login = row.getNullableString(4);
      String type = row.getString(5);
      String data = row.getNullableString(6);

      update.setString(1, key);
      update.setString(2, profileKey);
      update.setLong(3, createdAt.getTime());
      update.setString(4, login);
      update.setString(5, type);
      update.setString(6, data);
      return true;
    });
  }

}
