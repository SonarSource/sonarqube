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
package org.sonar.server.platform.db.migration.version.v92;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class DeprecateSWRecommendedQProfile extends DataChange {
  private final static String NEW_NAME = "Sonar way Recommended (deprecated)";

  public DeprecateSWRecommendedQProfile(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    migrate(context, "Sonar way Recommended", "js");
    migrate(context, "Sonar way recommended", "ts");
  }

  private static void migrate(Context context, String oldName, String language) throws SQLException {
    Integer count = context.prepareSelect("select count(*) from rules_profiles "
      + "where name = ? and language = ?")
      .setString(1, NEW_NAME)
      .setString(2, language)
      .get(t -> t.getInt(1));

    if (count != null && count > 0) {
      // keep the same name to not create a duplicated entry
      context
        .prepareUpsert("update rules_profiles "
          + "set is_built_in = ? "
          + "where name = ? and is_built_in = ? and language = ?")
        .setBoolean(1, false)
        .setString(2, oldName)
        .setBoolean(3, true)
        .setString(4, language)
        .execute()
        .commit();
    } else {
      context
        .prepareUpsert("update rules_profiles "
          + "set is_built_in = ?, name = ? "
          + "where name = ? and is_built_in = ? and language = ?")
        .setBoolean(1, false)
        .setString(2, NEW_NAME)
        .setString(3, oldName)
        .setBoolean(4, true)
        .setString(5, language)
        .execute()
        .commit();
    }
  }
}
