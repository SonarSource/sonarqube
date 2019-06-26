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
package org.sonar.server.platform.db.migration.version.v79;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

@SupportsBlueGreen
public class MigrateVstsProviderToAzureDevOps extends DataChange {

  private static final String VSTS = "VSTS";
  private static final String VSTS_TFS = "VSTS / TFS";

  public MigrateVstsProviderToAzureDevOps(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id, text_value from properties " +
      " where prop_key = 'sonar.pullrequest.provider' and text_value in ('" + VSTS + "', '" + VSTS_TFS + "')");
    massUpdate.update("update properties " +
      " set text_value= ?, " +
      " clob_value = null " +
      " where id = ?");
    massUpdate.rowPluralName("PR provider properties");
    massUpdate.execute((row, update) -> {
      update.setString(1, convert(row.getString(2)));
      update.setLong(2, row.getLong(1));
      return true;
    });
  }

  private static String convert(String oldValue) {
    switch (oldValue) {
      case VSTS:
        return "Azure DevOps Services";
      case VSTS_TFS:
        return "Azure DevOps";
      default:
        throw new IllegalStateException("Unexpected value: " + oldValue);
    }
  }
}
