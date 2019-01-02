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

public class PopulateExternalIdOnUsers extends DataChange {

  private final System2 system2;

  public PopulateExternalIdOnUsers(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("users");
    massUpdate.select("SELECT id, external_login FROM users WHERE external_id IS NULL");
    massUpdate.update("UPDATE users SET external_id=?, updated_at=? WHERE id=?");

    long now = system2.now();
    massUpdate.execute((row, update) -> {
      long id = row.getLong(1);
      String externalLogin = row.getString(2);
      update.setString(1, externalLogin);
      update.setLong(2, now);
      update.setLong(3, id);
      return true;
    });
  }
}
