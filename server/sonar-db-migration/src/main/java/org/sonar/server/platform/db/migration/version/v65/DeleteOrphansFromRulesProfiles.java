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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class DeleteOrphansFromRulesProfiles extends DataChange {

  private static final Logger LOG = Loggers.get(DeleteOrphansFromRulesProfiles.class);

  public DeleteOrphansFromRulesProfiles(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    execute(context, "rules_profiles", "delete from rules_profiles where not exists( select 1 from org_qprofiles oqp where oqp.rules_profile_uuid = kee)");
    execute(context, "active_rules", "delete from active_rules where not exists ( select 1 from rules_profiles rp where rp.id = profile_id)");
    execute(context, "active_rule_parameters", "delete from active_rule_parameters where not exists ( select 1 from active_rules ar where ar.id = active_rule_id)");
    execute(context, "qprofile_changes", "delete from qprofile_changes where not exists ( select 1 from rules_profiles rp where rp.kee = qprofile_key)");
  }

  private static void execute(Context context, String tableName, String sql) throws SQLException {
    LOG.info("Deleting orphans from " + tableName);
    context
      .prepareUpsert(sql)
      .execute()
      .commit();
  }
}
