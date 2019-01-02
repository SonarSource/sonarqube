/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;

/**
 * The migration drops duplicated rows from some rules-related tables.
 * The root cause of these duplications is not known. It could even
 * be already fixed. Assuming that the duplications are not created frequently,
 * nor recently, then executing the migration when server is up
 * is safe. Blue/green deployment is supported.
 */
@SupportsBlueGreen
public class PurgeDuplicateRulesParameters extends DataChange {

  private static final String REMOVE_DUPLICATE_RULES_PARAMS_SQL_FOR_GENERIC =
    "DELETE FROM rules_parameters p1 WHERE id NOT IN (SELECT * FROM (SELECT MIN(id) FROM rules_parameters GROUP BY rule_id, name) temp)";

  private static final String REMOVE_DUPLICATE_ACTIVE_RULE_PARAMS_SQL_FOR_GENERIC =
    "DELETE FROM active_rule_parameters arp WHERE arp.rules_parameter_id NOT IN (SELECT * FROM (SELECT MIN(id) FROM rules_parameters GROUP BY rule_id, name) temp)";

  private static final String REMOVE_DUPLICATE_RULES_PARAMS_SQL_FOR_MYSQL_MSSQL =
    "DELETE p1 FROM rules_parameters as p1 WHERE id NOT IN (SELECT id FROM (SELECT MIN(id) as id FROM rules_parameters GROUP BY rule_id, name) temp)";

  private static final String REMOVE_DUPLICATE_ACTIVE_RULE_PARAMS_SQL_FOR_MYSQL_MSSQL =
    "DELETE arp FROM active_rule_parameters as arp WHERE arp.rules_parameter_id NOT IN (SELECT id FROM (SELECT MIN(id) as id FROM rules_parameters GROUP BY rule_id, name) temp)";

  public PurgeDuplicateRulesParameters(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    String removeDuplicateRulesParamsSql;
    String removeDuplicateActiveRuleParamsSql;
    switch (getDialect().getId()) {
      case "mssql":
      case "mysql":
        removeDuplicateRulesParamsSql = REMOVE_DUPLICATE_RULES_PARAMS_SQL_FOR_MYSQL_MSSQL;
        removeDuplicateActiveRuleParamsSql = REMOVE_DUPLICATE_ACTIVE_RULE_PARAMS_SQL_FOR_MYSQL_MSSQL;
        break;
      default:
        removeDuplicateRulesParamsSql = REMOVE_DUPLICATE_RULES_PARAMS_SQL_FOR_GENERIC;
        removeDuplicateActiveRuleParamsSql = REMOVE_DUPLICATE_ACTIVE_RULE_PARAMS_SQL_FOR_GENERIC;
        break;
    }

    context.prepareUpsert(removeDuplicateActiveRuleParamsSql)
      .execute()
      .commit();
    context.prepareUpsert(removeDuplicateRulesParamsSql)
      .execute()
      .commit();
  }
}
