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
package org.sonar.server.platform.db.migration.version.v67;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

public class PopulateAnalysisUuidColumnOnWebhookDeliveries extends DataChange {

  public PopulateAnalysisUuidColumnOnWebhookDeliveries(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT wd.uuid, ca.analysis_uuid " +
      " FROM webhook_deliveries wd " +
      " INNER JOIN ce_activity ca ON ca.uuid=wd.ce_task_uuid " +
      " WHERE wd.analysis_uuid IS NULL AND ca.analysis_uuid IS NOT NULL");
    massUpdate.update("UPDATE webhook_deliveries SET analysis_uuid=? WHERE uuid=?");
    massUpdate.rowPluralName("webhook_deliveries");
    massUpdate.execute(PopulateAnalysisUuidColumnOnWebhookDeliveries::handle);
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    String uuid = row.getString(1);
    String analysisUuid = row.getString(2);

    update.setString(1, analysisUuid);
    update.setString(2, uuid);

    return true;
  }
}
