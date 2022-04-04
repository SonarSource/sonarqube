/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.users.fk.properties;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulatePropertiesUserUuid extends DataChange {

  public PopulatePropertiesUserUuid(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    populateUserUuid(context);
    removeRowWithNonExistentUser(context);
  }

  private static void populateUserUuid(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();

    massUpdate.select("select p.uuid, u.uuid " +
      "from properties p " +
      "join users u on p.user_id = u.id where p.user_uuid is null");

    massUpdate.update("update properties set user_uuid = ? where uuid = ?");

    massUpdate.execute((row, update, index) -> {
      String propertiesUuid = row.getString(1);
      String userUuid = row.getString(2);

      update.setString(1, userUuid);
      update.setString(2, propertiesUuid);
      return true;
    });
  }

  private static void removeRowWithNonExistentUser(Context context) throws SQLException {
    context.prepareUpsert("delete from properties where user_uuid is null and user_id is not null")
      .execute()
      .commit();
  }
}
