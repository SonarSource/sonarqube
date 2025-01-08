/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.SQLException;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateReportSubscriptions extends DataChange {

  private static final String SELECT_QUERY = """
    SELECT port.uuid as portfolioUuid, pb.uuid as branchUuid, p.user_uuid as userUuid
    FROM properties p
    LEFT JOIN portfolios port ON p.entity_uuid = port.uuid
    LEFT JOIN project_branches pb ON p.entity_uuid = pb.uuid
    WHERE p.prop_key = 'sonar.governance.report.userNotification' or p.prop_key = 'sonar.governance.report.project.branch.userNotification'
    AND NOT EXISTS (
      SELECT * FROM report_subscriptions rs
      WHERE (rs.branch_uuid = p.entity_uuid or rs.portfolio_uuid = p.entity_uuid) and rs.user_uuid = p.user_uuid
    )
    """;

  public PopulateReportSubscriptions(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update("insert into report_subscriptions (uuid, branch_uuid, portfolio_uuid, user_uuid) values (?, ?, ?, ?)");
    massUpdate.execute((row, update) -> {
      String portfolioUuid = row.getString(1);
      String branchUuid = row.getString(2);

      // one and only one needs to be null
      if ((portfolioUuid == null) == (branchUuid == null)) {
        return false;
      }

      String userUuid = row.getString(3);
      update.setString(1, UuidFactoryImpl.INSTANCE.create());
      update.setString(2, branchUuid);
      update.setString(3, portfolioUuid);
      update.setString(4, userUuid);
      return true;
    });
  }
}
