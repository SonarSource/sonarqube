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
package org.sonar.server.platform.db.migration.version.v67;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class CleanupDisabledUsers extends DataChange {

  private final System2 system2;
  private final String falseValue;

  public CleanupDisabledUsers(Database db, System2 system2) {
    super(db);
    this.falseValue = db.getDialect().getFalseSqlValue();
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    long now = system2.now();

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select u.id from users u " +
      "where u.active = " + falseValue + " and " +
      "( email is not null or " +
      "  crypted_password is not null or " +
      "  salt is not null or " +
      "  external_identity is not null or " +
      "  external_identity_provider is not null ) ");
    massUpdate.update("update users set " +
      "  email = null, " +
      "  crypted_password = null, " +
      "  salt = null, " +
      "  external_identity = null, " +
      "  external_identity_provider = null, " +
      "  updated_at = ? " +
      " where id = ?");
    massUpdate.rowPluralName("deactivated users");
    massUpdate.execute((row, update) -> {
      update.setLong(1, now);
      update.setLong(2, row.getLong(1));
      return true;
    });
  }
}
