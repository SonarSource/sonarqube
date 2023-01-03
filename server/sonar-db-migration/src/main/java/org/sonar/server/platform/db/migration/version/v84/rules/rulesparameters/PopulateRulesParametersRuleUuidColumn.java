/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.rules.rulesparameters;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateRulesParametersRuleUuidColumn extends DataChange {

  public PopulateRulesParametersRuleUuidColumn(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    populateUuids(context);
    removeOrphans(context);
  }

  private void populateUuids(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();

    massUpdate.select("select rp.rule_id, ru.uuid " +
      "from rules_parameters rp " +
      "join rules ru on rp.rule_id = ru.id " +
      "where rp.rule_uuid is null");
    massUpdate.update("update rules_parameters set rule_uuid = ? where rule_id = ?");

    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(2));
      update.setLong(2, row.getLong(1));
      return true;
    });
  }

  private void removeOrphans(DataChange.Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();

    massUpdate.select("select uuid from rules_parameters where rule_uuid is null");
    massUpdate.update("delete from rules_parameters where uuid = ?");

    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(1));
      return true;
    });
  }
}
