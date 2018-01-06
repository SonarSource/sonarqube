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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;

public class PopulateUsersOnboarded extends DataChange {

  private final System2 system2;

  public PopulateUsersOnboarded(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.prepareUpsert("update users set onboarded=?, updated_at=?")
      .setBoolean(1, true)
      .setLong(2, system2.now())
      .execute()
      .commit();
    long users = context.prepareSelect("select count(u.id) from users u").get(Select.LONG_READER);
    if (users == 1) {
      context.prepareUpsert("update users set onboarded=?, updated_at=? where login=?")
        .setBoolean(1, false)
        .setLong(2, system2.now())
        .setString(3, "admin")
        .execute()
        .commit();
    }
  }
}
