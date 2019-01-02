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
package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

public class UseRuleIdInQPChangesData extends DataChange {

  private static final String RULE_KEY_DATA_FIELD = "ruleKey";
  private static final String RULE_ID_DATA_FIELD = "ruleId";

  public UseRuleIdInQPChangesData(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Map<String, Integer> ruleKeysById = context.prepareSelect("select id, plugin_name, plugin_rule_key from rules")
      .list(row -> new Rule(row.getInt(1), RuleKey.of(row.getString(2), row.getString(3)).toString()))
      .stream()
      .collect(Collectors.toMap(r -> r.ruleKey, r -> r.ruleId));

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select kee,change_data from qprofile_changes");
    massUpdate.update("update qprofile_changes set change_data=? where kee=?");
    massUpdate.execute(((row, update) -> handle(row, update, ruleKeysById)));
  }

  private static boolean handle(Select.Row row, SqlStatement update, Map<String, Integer> ruleKeysById) throws SQLException {
    String key = row.getString(1);
    String data = row.getString(2);

    Map<String, String> map = KeyValueFormat.parse(data);
    String ruleKey = map.get(RULE_KEY_DATA_FIELD);
    if (ruleKey == null) {
      return false;
    }

    Integer ruleId = ruleKeysById.get(ruleKey);
    if (ruleId != null) {
      map.put(RULE_ID_DATA_FIELD, String.valueOf(ruleId));
    }
    map.remove(RULE_KEY_DATA_FIELD);

    update.setString(1, KeyValueFormat.format(map));
    update.setString(2, key);
    return true;
  }

  private static final class Rule {
    private final int ruleId;
    private final String ruleKey;

    private Rule(int ruleId, String ruleKey) {
      this.ruleId = ruleId;
      this.ruleKey = ruleKey;
    }
  }
}
