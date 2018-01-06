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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

/**
 * Organizations and root users were not production-ready
 * in versions prior to 6.3. For this reason they should not
 * be enabled. This migration unsets the root flag from
 * users.
 */
public class UnsetUserRootFlags extends DataChange {

  private final System2 system2;

  public UnsetUserRootFlags(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    context.prepareUpsert("update users set is_root=?, updated_at=? where is_root=?")
      .setBoolean(1, false)
      .setLong(2, system2.now())
      .setBoolean(3, true)
      .execute()
      .commit();
  }
}
