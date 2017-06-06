/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class DeleteOrphansFromRulesProfiles extends DataChange {

  public DeleteOrphansFromRulesProfiles(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    deleteOrphansFromRulesProfiles(context);
    deleteOrphansFromActiveRules(context);
    deleteOrphansFromActiveRuleParameters(context);
    deleteOrphansFromQProfileChanges(context);
  }

  private static void deleteOrphansFromRulesProfiles(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate()
      .rowPluralName("rules profiles");

    massUpdate.select("select rp.kee " +
      " from rules_profiles rp" +
      " where not exists " +
      "    ( select 1 from org_qprofiles oqp where oqp.rules_profile_uuid = rp.kee )");

    massUpdate.update("delete from rules_profiles where kee = ?")
      .execute((row, update) -> {
        String kee = row.getString(1);
        update.setString(1, kee);
        return true;
      });
  }

  private static void deleteOrphansFromActiveRules(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate()
      .rowPluralName("active rules");

    massUpdate.select("select ar.id " +
      " from active_rules ar " +
      " where not exists " +
      "    ( select 1 from rules_profiles rp where ar.profile_id = rp.id )");

    massUpdate.update("delete from active_rules where id = ?")
      .execute((row, update) -> {
        int id = row.getInt(1);
        update.setInt(1, id);
        return true;
      });
  }

  private static void deleteOrphansFromActiveRuleParameters(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate()
      .rowPluralName("active rule parameters");

    massUpdate.select("select arp.id " +
      " from active_rule_parameters arp " +
      " where not exists " +
      "    ( select 1 from active_rules ar where ar.id = arp.active_rule_id )");

    massUpdate.update("delete from active_rule_parameters where id = ?")
      .execute((row, update) -> {
        int id = row.getInt(1);
        update.setInt(1, id);
        return true;
      });
  }

  private static void deleteOrphansFromQProfileChanges(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate()
      .rowPluralName("qprofile changes");

    massUpdate.select("select qpc.kee " +
      " from qprofile_changes qpc" +
      " where not exists " +
      "    ( select 1 from rules_profiles rp where qpc.qprofile_key = rp.kee )");

    massUpdate.update("delete from qprofile_changes where kee = ?")
      .execute((row, update) -> {
        String kee = row.getString(1);
        update.setString(1, kee);
        return true;
      });
  }

}
