/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class FixDuplicationInExternalLoginOnUsers extends DataChange {

  private final System2 system2;

  public FixDuplicationInExternalLoginOnUsers(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("users having duplicated values in EXTERNAL_LOGIN");
    massUpdate.select("SELECT u1.id, u1.login FROM users u1 " +
      "WHERE EXISTS (SELECT 1 FROM users u2 WHERE u2.external_login = u1.external_login AND u2.id != u1.id)");
    // EXTERNAL_ID is also updated because the column was added previously and content was copied from EXTERNAL_LOGIN
    massUpdate.update("UPDATE users SET external_login=?, external_id=?, updated_at=? WHERE id=?");

    long now = system2.now();
    massUpdate.execute((row, update) -> {
      long id = row.getLong(1);
      String login = row.getString(2);
      update.setString(1, login);
      update.setString(2, login);
      update.setLong(3, now);
      update.setLong(4, id);
      return true;
    });
  }
}
