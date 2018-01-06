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
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.DataChange;

public class RemoveUsersPasswordWhenNotLocal extends DataChange {

  private final System2 system2;

  public RemoveUsersPasswordWhenNotLocal(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT u.id FROM users u WHERE (u.crypted_password IS NOT NULL OR u.salt IS NOT NULL) AND u.user_local=?").setBoolean(1, false);
    massUpdate.update("UPDATE users SET crypted_password=null, salt=null, updated_at=? WHERE id=?");
    massUpdate.rowPluralName("none local users with password");
    massUpdate.execute((row, update) -> {
      update.setLong(1, now);
      update.setLong(2, row.getLong(1));
      return true;
    });
  }
}
