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
package org.sonar.server.platform.db.migration.version.v67;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;

public class CopyDeprecatedServerId extends DataChange {

  private static final String DEPRECATED_KEY = "sonar.server_id";
  private static final String NEW_KEY = "sonar.core.id";

  public CopyDeprecatedServerId(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String deprecatedValue = context
      .prepareSelect("select text_value from properties where prop_key = '" + DEPRECATED_KEY + "'")
      .get(new Select.StringReader());
    if (deprecatedValue != null && !deprecatedValue.isEmpty()) {
      deleteProperty(context, NEW_KEY);
      context.prepareUpsert("insert into properties" +
        " (prop_key, is_empty, text_value, created_at)" +
        " values " +
        " (?, ?, ?, ?)")
        .setString(1, NEW_KEY)
        .setBoolean(2, false)
        .setString(3, deprecatedValue)
        .setLong(4, System.currentTimeMillis())
        .execute()
        .commit();
    }
    deleteProperty(context, DEPRECATED_KEY);
  }

  private static void deleteProperty(Context context, String key) throws SQLException {
    context.prepareUpsert("delete from properties where prop_key = '" + key + "'")
      .execute()
      .commit();
  }

}
