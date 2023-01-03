/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.users.fk.qprofileeditusers;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.version.v84.util.OrphanData;

public class PopulateQProfileEditUsersUserUuid extends DataChange {

  public PopulateQProfileEditUsersUserUuid(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();

    massUpdate.select("select qeu.uuid, u.uuid " +
      "from qprofile_edit_users qeu " +
      "join users u on qeu.user_id = u.id where qeu.user_uuid is null");

    massUpdate.update("update qprofile_edit_users set user_uuid = ? where uuid = ?");

    massUpdate.execute((row, update, index) -> {
      String permTemplatesUuid = row.getString(1);
      String userUuid = row.getString(2);

      update.setString(1, userUuid);
      update.setString(2, permTemplatesUuid);
      return true;
    });

    OrphanData.delete(context, "qprofile_edit_users", "user_uuid");
  }
}
