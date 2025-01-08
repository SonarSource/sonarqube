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
package org.sonar.server.platform.db.migration.version.v202501;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class RenameOldSonarWayToLegacy extends DataChange {

  private static final String SELECT_SONAR_WAY = "select uuid from quality_gates where name = 'Sonar way'";
  private static final String SELECT_SONAR_WAY_CONDITIONS = "select * from quality_gate_conditions where qgate_uuid = ?";
  private static final String SELECT_0_NEW_ISSUES_CONDITION = """
    select * from quality_gate_conditions where qgate_uuid = ?
    and metric_uuid = (select uuid from metrics where name = 'new_violations')
    and value_error = '0' and operator = 'GT'
    """;
  private static final String UPDATE_SONAR_WAY_NAME = """
    update quality_gates set name = ?, is_built_in=? where uuid = ?
    """;

  public RenameOldSonarWayToLegacy(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String sonarWayUuid = context.prepareSelect(SELECT_SONAR_WAY).get(row -> row.getString(1));
    if (sonarWayUuid == null) {
      //Sonar way has been already renamed, and the new one hasn't been created yet
      return;
    }
    String sonarWayConditions = context.prepareSelect(SELECT_SONAR_WAY_CONDITIONS)
      .setString(1, sonarWayUuid)
      .get(row -> row.getString(1));
    if (sonarWayConditions == null) {
      //Fresh instance, Sonar way created, but the conditions weren't populated yet
      return;
    }
    String sonarWayWith0NewIssues = context.prepareSelect(SELECT_0_NEW_ISSUES_CONDITION)
      .setString(1, sonarWayUuid)
      .get(row -> row.getString(1));
    if (sonarWayWith0NewIssues != null) {
      //Migrating from version 10.3 or newer, Sonar way with 0 new issues condition exists
      return;
    }
    context.prepareUpsert(UPDATE_SONAR_WAY_NAME)
      .setString(1, "Sonar way (legacy)")
      .setBoolean(2, false)
      .setString(3, sonarWayUuid)
      .execute()
      .commit();
  }
}
