/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.rulesparameters.fk;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateActiveRuleParametersRulesParameterUuid extends DataChange {

  public PopulateActiveRuleParametersRulesParameterUuid(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();

    massUpdate.select("select arp.uuid, rp.uuid " +
      "from active_rule_parameters arp " +
      "join rules_parameters rp on arp.rules_parameter_id = rp.id " +
      "where arp.rules_parameter_uuid is null");
    massUpdate.update("update active_rule_parameters set rules_parameter_uuid = ? where uuid = ?");

    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(2));
      update.setString(2, row.getString(1));
      return true;
    });

    massUpdate = context.prepareMassUpdate();

    massUpdate.select("select uuid from active_rule_parameters where rules_parameter_uuid is null");
    massUpdate.update("delete from active_rule_parameters where uuid = ?");

    massUpdate.execute((row, update) -> {
      update.setString(1, row.getString(1));
      return true;
    });
  }
}
