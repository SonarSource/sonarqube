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
package org.sonar.server.platform.db.migration.version.v66;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class FixEmptyIdentityProviderInUsers extends DataChange {

  private static final String SQ_AUTHORITY = "sonarqube";

  private final System2 system2;

  public FixEmptyIdentityProviderInUsers(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select u.id, u.login from users u " +
      "where u.external_identity is null or u.external_identity_provider is null");
    massUpdate.update("update users set external_identity = ?, external_identity_provider = ?, updated_at = ? where id = ?");
    massUpdate.rowPluralName("users without external identity information");
    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(2));
      update.setString(2, SQ_AUTHORITY);
      update.setLong(3, now);
      update.setLong(4, row.getLong(1));
      return true;
    });
  }
}
