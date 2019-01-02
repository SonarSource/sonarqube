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
package org.sonar.server.platform.db.migration.version.v60;

import com.google.common.base.Throwables;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.step.DataChange;

public class PopulateUserUpdatedAtOfRulesProfiles extends DataChange {

  private static final String SQL_SELECT_PROFILES_NOT_UPDATED = "select kee from rules_profiles where user_updated_at is null";

  public PopulateUserUpdatedAtOfRulesProfiles(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    Map<String, Long> userUpdatedAtByProfileKeys = buildUserUpdatedAtMap(context);
    populateUserUpdatedAtColumn(context, userUpdatedAtByProfileKeys);
  }

  private static Map<String, Long> buildUserUpdatedAtMap(Context context) throws SQLException {
    Map<String, Long> lastAnalysisDatesByQPKeys = new HashMap<>();
    List<String> profileKeys = context.prepareSelect(SQL_SELECT_PROFILES_NOT_UPDATED).list(row -> row.getString(1));
    profileKeys.forEach(profileKey -> lastAnalysisDatesByQPKeys.put(profileKey, getUserUpdateAt(context, profileKey)));

    return lastAnalysisDatesByQPKeys;
  }

  @CheckForNull
  private static Long getUserUpdateAt(Context context, String profileKey) {
    try {
      return context.prepareSelect("select created_at as \"createdAt\" " +
        "from activities " +
        "where user_login is not null " +
        "  and profile_key=? " +
        "order by created_at DESC ")
        .setString(1, profileKey)
        .get(row -> {
          Date userUpdatedAt = row.getNullableDate(1);
          return userUpdatedAt == null ? null : userUpdatedAt.getTime();
        });
    } catch (SQLException e) {
      throw Throwables.propagate(e);
    }
  }

  private static void populateUserUpdatedAtColumn(Context context, Map<String, Long> userUpdatedAdByProfileKey) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SQL_SELECT_PROFILES_NOT_UPDATED);
    massUpdate.update("update rules_profiles set user_updated_at=? where kee=?");
    massUpdate.rowPluralName("quality profiles");
    massUpdate.execute((row, update) -> handle(userUpdatedAdByProfileKey, row, update));
  }

  private static boolean handle(Map<String, Long> userUpdatedAtByProfileKey, Select.Row row, SqlStatement update) throws SQLException {
    String profileKey = row.getString(1);

    update.setLong(1, userUpdatedAtByProfileKey.get(profileKey));
    update.setString(2, profileKey);

    return true;
  }
}
