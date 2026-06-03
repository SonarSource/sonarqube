/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class DecommissionSonarWayForAiCodeQualityGate extends DataChange {

  static final String GATE_NAME = "Sonar way for AI Code";
  static final String LEGACY_GATE_NAME = "Sonar way for AI code (legacy)";

  public DecommissionSonarWayForAiCodeQualityGate(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String gateUuid = context.prepareSelect("SELECT uuid FROM quality_gates WHERE name = ?")
      .setString(1, GATE_NAME)
      .get(row -> row.getString(1));

    if (gateUuid == null) {
      return;
    }

    if (isGateInUse(context, gateUuid)) {
      context.prepareUpsert("UPDATE quality_gates SET name = ?, is_built_in = ? WHERE uuid = ?")
        .setString(1, LEGACY_GATE_NAME)
        .setBoolean(2, false)
        .setString(3, gateUuid)
        .execute()
        .commit();
    } else {
      context.prepareUpsert("DELETE FROM qgate_user_permissions WHERE quality_gate_uuid = ?")
        .setString(1, gateUuid)
        .execute();
      context.prepareUpsert("DELETE FROM qgate_group_permissions WHERE quality_gate_uuid = ?")
        .setString(1, gateUuid)
        .execute();
      context.prepareUpsert("DELETE FROM quality_gate_conditions WHERE qgate_uuid = ?")
        .setString(1, gateUuid)
        .execute();
      context.prepareUpsert("DELETE FROM quality_gates WHERE uuid = ?")
        .setString(1, gateUuid)
        .execute()
        .commit();
    }
  }

  private static boolean isGateInUse(Context context, String gateUuid) throws SQLException {
    long instanceDefaultCount = context.prepareSelect(
      "SELECT COUNT(*) FROM properties WHERE prop_key = 'qualitygate.default' AND entity_uuid IS NULL AND text_value = ?")
      .setString(1, gateUuid)
      .get(row -> row.getLong(1));

    if (instanceDefaultCount > 0) {
      return true;
    }

    long projectAssignmentCount = context.prepareSelect(
      "SELECT COUNT(*) FROM project_qgates WHERE quality_gate_uuid = ?")
      .setString(1, gateUuid)
      .get(row -> row.getLong(1));

    return projectAssignmentCount > 0;
  }
}
