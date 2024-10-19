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
package org.sonar.server.platform.db.migration.version.v104;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class RemoveCleanCodeAttributeFromCustomHotspotRules extends DataChange {

  private static final String SELECT = """
    SELECT uuid
    FROM rules
    WHERE rule_type = 4
    AND clean_code_attribute IS NOT NULL
    """;
  private static final String UPDATE = """
    UPDATE rules SET clean_code_attribute = NULL, updated_at = ? WHERE uuid = ?
     """;

  public RemoveCleanCodeAttributeFromCustomHotspotRules(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    Long updatedAt = System.currentTimeMillis();
    massUpdate.select(SELECT);
    massUpdate.update(UPDATE);
    massUpdate.execute((row, update) -> {
      String uuid = row.getString(1);
      update.setLong(1, updatedAt);
      update.setString(2, uuid);
      return true;
    });
  }
}
