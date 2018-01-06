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
package org.sonar.server.platform.db.migration.version.v61;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

public class PopulateTableProperties2 extends DataChange {
  private final System2 system2;

  public PopulateTableProperties2(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT" +
      " p.prop_key, p.resource_id, p.text_value, p.user_id" +
      " from properties p" +
      " left outer join properties2 p2" +
      " on p2.prop_key=p.prop_key" +
      "    and (p2.resource_id=p.resource_id or (p2.resource_id is null and p.resource_id is null))" +
      "    and (p2.user_id=p.user_id or (p2.user_id is null and p.user_id is null))" +
      " where" +
      " p2.id is null" +
      " order by p.id");
    massUpdate.update("insert into properties2" +
      " (prop_key, resource_id, user_id, is_empty, text_value, clob_value, created_at)" +
      " values " +
      " (?, ?, ?, ?, ?, ?, ?)");
    massUpdate.rowPluralName("copy data from table properties into table properties2");
    massUpdate.execute(this::handle);
  }

  private boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    String key = row.getString(1);
    Integer resourceId = row.getNullableInt(2);
    String value = row.getNullableString(3);
    Integer userId = row.getNullableInt(4);

    update.setString(1, key);
    update.setInt(2, resourceId);
    update.setInt(3, userId);
    if (value == null || value.isEmpty()) {
      update.setBoolean(4, true);
      update.setString(5, null);
      update.setString(6, null);
    } else if (value.length() > 4000) {
      update.setBoolean(4, false);
      update.setString(5, null);
      update.setString(6, value);
    } else {
      update.setBoolean(4, false);
      update.setString(5, value);
      update.setString(6, null);
    }
    update.setLong(7, system2.now());
    return true;
  }
}
