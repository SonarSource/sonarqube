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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class MakeDefaultOrganizationGuarded extends DataChange {
  private final DefaultOrganizationUuidProvider defaultOrganizationUuid;

  public MakeDefaultOrganizationGuarded(Database db, DefaultOrganizationUuidProvider defaultOrganizationUuid) {
    super(db);
    this.defaultOrganizationUuid = defaultOrganizationUuid;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String uuid = this.defaultOrganizationUuid.getAndCheck(context);
    context.prepareUpsert("update organizations set guarded=? where uuid=?")
      .setBoolean(1, true)
      .setString(2, uuid)
      .execute()
      .commit();
  }
}
