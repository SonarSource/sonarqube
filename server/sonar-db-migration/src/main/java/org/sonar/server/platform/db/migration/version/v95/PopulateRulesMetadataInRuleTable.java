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
package org.sonar.server.platform.db.migration.version.v95;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateRulesMetadataInRuleTable extends DataChange {

  private static final String RULES_METADATA_TABLE_NAME = "rules_metadata";

  public PopulateRulesMetadataInRuleTable(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (ruleMetadataTableExists()) {
      MassUpdate massUpdate = context.prepareMassUpdate();
      massUpdate.select("select note_data, note_user_uuid, note_created_at, note_updated_at, remediation_function," +
        " remediation_gap_mult, remediation_base_effort, tags, ad_hoc_name, ad_hoc_description, ad_hoc_severity, ad_hoc_type, rule_uuid from rules_metadata");

      massUpdate.update("update rules set note_data = ?, note_user_uuid = ?, note_created_at = ?," +
        " note_updated_at = ?, remediation_function = ?, remediation_gap_mult = ?, remediation_base_effort = ?," +
        " tags = ?, ad_hoc_name = ?, ad_hoc_description = ?, ad_hoc_severity = ?, ad_hoc_type = ? where uuid = ?");

      massUpdate.execute((row, update) -> {
        update.setString(1, row.getString(1)); // note_data
        update.setString(2, row.getString(2)); // note_user_uuid
        update.setLong(3, row.getLong(3)); // note_created_at
        update.setLong(4, row.getLong(4)); // note_updated_at
        update.setString(5, row.getString(5)); // remediation_function
        update.setString(6, row.getString(6)); // remediation_gap_mult
        update.setString(7, row.getString(7)); // remediation_base_effort
        update.setString(8, row.getString(8)); // tags
        update.setString(9, row.getString(9)); // ad_hoc_name
        update.setString(10, row.getString(10)); // ad_hoc_description
        update.setString(11, row.getString(11)); // ad_hoc_severity
        update.setInt(12, row.getInt(12)); // ad_hoc_type
        update.setString(13, row.getString(13)); // where clause (rule uuid)

        return true;
      });
    }
  }

  private boolean ruleMetadataTableExists() throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(RULES_METADATA_TABLE_NAME, connection);
    }
  }
}
