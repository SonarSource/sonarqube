/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v97;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class RemoveBranchInformationFromComponentsKey extends DataChange {

  private static final String BRANCH_IDENTIFIER = ":BRANCH:";
  private static final String PULL_REQUEST_IDENTIFIER = ":PULL_REQUEST:";
  private static final String SELECT_QUERY = "select kee, uuid from components where main_branch_project_uuid is not null";
  private static final String UPDATE_QUERY = "update components set kee = ? where uuid= ?";

  private final MigrationEsClient migrationEsClient;

  public RemoveBranchInformationFromComponentsKey(Database db, MigrationEsClient migrationEsClient) {
    super(db);
    this.migrationEsClient = migrationEsClient;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update(UPDATE_QUERY);
    massUpdate.execute((row, update) -> {
      boolean toUpdate = false;
      String componentKey = row.getString(1);
      String componentUuid = row.getString(2);

      int branchIndex = componentKey.indexOf(BRANCH_IDENTIFIER);
      if (branchIndex > -1) {
        toUpdate = true;
        componentKey = componentKey.substring(0, branchIndex);

      } else {
        int pullRequestIndex = componentKey.indexOf(PULL_REQUEST_IDENTIFIER);
        if (pullRequestIndex > -1) {
          toUpdate = true;
          componentKey = componentKey.substring(0, pullRequestIndex);
        }
      }
      update.setString(1, componentKey)
        .setString(2, componentUuid);
      return toUpdate;
    });
    migrationEsClient.deleteIndexes("components");
  }
}
