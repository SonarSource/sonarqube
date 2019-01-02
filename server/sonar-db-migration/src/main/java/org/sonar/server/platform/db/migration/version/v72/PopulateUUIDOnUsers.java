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
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateUUIDOnUsers extends DataChange {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public PopulateUUIDOnUsers(Database db, System2 system2, UuidFactory uuidFactory) {
    super(db);
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id, login from users where uuid is null");
    massUpdate.rowPluralName("users");
    massUpdate.update("update users set uuid=?, updated_at=? where id=?");
    massUpdate.execute((row, update) -> {
      String login = row.getString(2);
      if (login == null) {
        login = uuidFactory.create();
      }
      // login -> uuid
      update.setString(1, login);
      update.setLong(2, system2.now());
      update.setLong(3, row.getLong(1));
      return true;
    });
  }
}
