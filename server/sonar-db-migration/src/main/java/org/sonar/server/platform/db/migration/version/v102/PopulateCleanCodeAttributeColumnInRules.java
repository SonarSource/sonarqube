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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;

public class PopulateCleanCodeAttributeColumnInRules extends DataChange {

  private static final String SELECT_QUERY = """
    SELECT uuid, clean_code_attribute
    FROM rules
    WHERE clean_code_attribute is null and (rule_type <> %1$s or ad_hoc_type <> %1$s)
    """.formatted(SECURITY_HOTSPOT.getDbConstant());

  private static final String UPDATE_QUERY = """
    UPDATE rules
    SET clean_code_attribute=?
    WHERE uuid=?
    """;

  public PopulateCleanCodeAttributeColumnInRules(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update(UPDATE_QUERY);

    massUpdate.execute((row, update, index) -> {
      String ruleUuid = row.getString(1);
      update.setString(1, CleanCodeAttribute.CONVENTIONAL.name())
        .setString(2, ruleUuid);
      return true;
    });
  }
}
