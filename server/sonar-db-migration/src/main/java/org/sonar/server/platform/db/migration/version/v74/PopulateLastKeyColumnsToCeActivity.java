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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import org.sonar.api.config.Configuration;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

@SupportsBlueGreen
public class PopulateLastKeyColumnsToCeActivity extends DataChange {
  private static final String TABLE_NAME = "ce_activity";

  private final Configuration configuration;

  public PopulateLastKeyColumnsToCeActivity(Database db, Configuration configuration) {
    super(db);
    this.configuration = configuration;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (configuration.getBoolean("sonar.sonarcloud.enabled").orElse(false)) {
      // data migration will be done in background so that interruption of service
      // is reduced during upgrade
      return;
    }

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select" +
      "  ca.uuid, ca.tmp_is_last, ca.tmp_is_last_key, ca.tmp_main_is_last, ca.tmp_main_is_last_key" +
      " from ce_activity ca" +
      " where" +
      "  ca.is_last is null" +
      "  or ca.is_last_key is null" +
      "  or ca.main_is_last is null" +
      "  or ca.main_is_last_key is null");
    massUpdate.rowPluralName("rows of " + TABLE_NAME);
    massUpdate.update("update " + TABLE_NAME + " set is_last=?, is_last_key=?, main_is_last=?, main_is_last_key=? where uuid=?");
    massUpdate.execute(PopulateLastKeyColumnsToCeActivity::handleUpdate);
  }

  private static boolean handleUpdate(Select.Row row, SqlStatement update) throws SQLException {
    String uuid = row.getString(1);
    boolean isLast = row.getBoolean(2);
    String isLastKey = row.getString(3);
    boolean mainIsLast = row.getBoolean(2);
    String mainIsLastKey = row.getString(3);

    update.setBoolean(1, isLast);
    update.setString(2, isLastKey);
    update.setBoolean(3, mainIsLast);
    update.setString(4, mainIsLastKey);
    update.setString(5, uuid);

    return true;
  }
}
