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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.Date;
import java.sql.SQLException;
import javax.annotation.CheckForNull;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

import static com.google.common.base.Preconditions.checkState;

public class PopulateColumnIsDefaultOfGroups extends DataChange {

  private static final String DEFAULT_GROUP_SETTING = "sonar.defaultGroup";
  private static final String DEFAULT_GROUP_DEFAULT_VALUE = "sonar-users";

  private final System2 system2;

  public PopulateColumnIsDefaultOfGroups(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Date now = new Date(system2.now());
    long defaultGroupId = searchDefaultGroupId(context);
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT id FROM groups WHERE is_default IS NULL");
    massUpdate.update("UPDATE groups SET is_default=?, updated_at=? WHERE id=?");
    massUpdate.rowPluralName("groups");
    massUpdate.execute((row, statement) -> {
      long groupId = row.getLong(1);
      statement.setBoolean(1, groupId == defaultGroupId);
      statement.setDate(2, now);
      statement.setLong(3, groupId);
      return true;
    });
  }

  private static long searchDefaultGroupId(Context context) throws SQLException {
    Long groupId = selectDefaultGroupIdFromProperties(context);
    if (groupId != null) {
      checkState(isGroupExist(context, groupId), "The default group defined in setting '%s' doesn't exist. Please set this setting to a valid group name and restart the migration",
        DEFAULT_GROUP_SETTING);
      return groupId;
    } else {
      groupId = selectSonarUsersGroup(context);
      checkState(groupId != null, "The default group '%s' doesn't exist. Please create it and restart the migration", DEFAULT_GROUP_DEFAULT_VALUE);
      return groupId;
    }
  }

  @CheckForNull
  private static Long selectDefaultGroupIdFromProperties(Context context) throws SQLException {
    return context.prepareSelect("SELECT prop_key,is_empty,text_value FROM properties WHERE prop_key=? AND is_empty=?")
      .setString(1, DEFAULT_GROUP_SETTING)
      .setBoolean(2, false)
      .get(row -> {
        boolean isEmpty = row.getBoolean(2);
        return isEmpty ? null : Long.parseLong(row.getString(3));
      });
  }

  private static boolean isGroupExist(Context context, long id) throws SQLException {
    long count = context.prepareSelect("SELECT count(id) FROM groups WHERE id=?")
      .setLong(1, id)
      .get(row -> row.getLong(1));
    return count == 1;
  }

  @CheckForNull
  private static Long selectSonarUsersGroup(Context context) throws SQLException {
    return context.prepareSelect("SELECT id FROM groups WHERE name=?")
      .setString(1, DEFAULT_GROUP_DEFAULT_VALUE)
      .get(row -> Long.parseLong(row.getString(1)));
  }

}
