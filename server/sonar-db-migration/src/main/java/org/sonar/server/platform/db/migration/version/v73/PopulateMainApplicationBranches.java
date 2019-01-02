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
package org.sonar.server.platform.db.migration.version.v73;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

@SupportsBlueGreen
public class PopulateMainApplicationBranches extends DataChange {

  private static final String MAIN_BRANCH_NAME = "master";

  private final System2 system2;

  public PopulateMainApplicationBranches(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT uuid FROM projects p "
      + "WHERE p.scope='PRJ' AND p.qualifier='APP' AND p.main_branch_project_uuid IS NULL "
      + "AND NOT EXISTS (SELECT uuid FROM project_branches b WHERE b.uuid = p.uuid)");
    massUpdate.update("INSERT INTO project_branches (uuid, project_uuid, kee, branch_type, key_type, "
      + "merge_branch_uuid, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
    massUpdate.rowPluralName("applications");
    massUpdate.execute((row, update) -> {
      String uuid = row.getString(1);
      update.setString(1, uuid);
      update.setString(2, uuid);
      update.setString(3, MAIN_BRANCH_NAME);
      update.setString(4, "LONG");
      update.setString(5, "BRANCH");
      update.setString(6, null);
      update.setLong(7, now);
      update.setLong(8, now);
      return true;
    });
  }

}
