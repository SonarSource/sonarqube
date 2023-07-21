/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.Oracle;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class PopulateCreatedAtTempInComponents extends DataChange {

  private static final String SELECT_QUERY = """
    SELECT uuid, created_at
    FROM components
    WHERE created_at_temp is null
    """;

  private static final String UPDATE_QUERY = """
    UPDATE components
    SET created_at_temp=?
    WHERE uuid=?
    """;

  public PopulateCreatedAtTempInComponents(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    boolean columnAlreadyHasNewType = columnAlreadyHasNewType();

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update(UPDATE_QUERY);

    massUpdate.execute((row, update, index) -> {
      String componentUuid = row.getString(1);
      Long createdAt;
      if (columnAlreadyHasNewType) {
        createdAt = row.getNullableLong(2);
      } else {
        createdAt = row.getNullableDate(2) == null ? null : row.getNullableDate(2).getTime();
      }
      update.setLong(1, createdAt)
        .setString(2, componentUuid);
      return true;
    });
  }

  private boolean columnAlreadyHasNewType() throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      ColumnMetadata columnMetadata = DatabaseUtils.getColumnMetadata(connection, "components", "created_at");
      int newType = getDialect().getId().equals(Oracle.ID) ? Types.NUMERIC : Types.BIGINT;
      return columnMetadata != null && columnMetadata.sqlType() == newType;
    }
  }
}
