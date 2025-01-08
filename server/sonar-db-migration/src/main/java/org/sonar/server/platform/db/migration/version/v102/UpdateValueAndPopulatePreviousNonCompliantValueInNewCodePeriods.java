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
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class UpdateValueAndPopulatePreviousNonCompliantValueInNewCodePeriods extends DataChange {

  private static final String SELECT_QUERY = """
    SELECT uuid, value
    FROM new_code_periods
    WHERE type = 'NUMBER_OF_DAYS'
    """;
  private static final String UPDATE_QUERY = """
    UPDATE new_code_periods
    SET previous_non_compliant_value=?, value='90', updated_at=?
    where uuid=?
    """;

  private static final String COLUMN_NAME= "previous_non_compliant_value";

  private static final String TABLE_NAME = "new_code_periods";

  public UpdateValueAndPopulatePreviousNonCompliantValueInNewCodePeriods(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (!checkIfColumnExists()) {
      return;
    }
    Long updatedAt = System.currentTimeMillis();

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update(UPDATE_QUERY);

    massUpdate.execute((row, update, index) -> {
      String newCodeDefinitionId = row.getString(1);
      String previousNewCodeDefinitionValue = row.getString(2);
      if(Integer.parseInt(previousNewCodeDefinitionValue) > 90) {
        update.setString(1, previousNewCodeDefinitionValue)
          .setLong(2, updatedAt)
          .setString(3, newCodeDefinitionId);
        return true;
      }
      return false;
    });
  }

  public boolean checkIfColumnExists() throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      if (DatabaseUtils.tableColumnExists(connection, TABLE_NAME, COLUMN_NAME)) {
        return true;
      }
    }
    return false;
  }
}
