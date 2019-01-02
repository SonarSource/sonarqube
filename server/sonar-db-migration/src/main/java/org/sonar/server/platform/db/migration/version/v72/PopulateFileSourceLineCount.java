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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

/**
 * New column is populated with value {@code -1} rather than with the number of lines computed from the value of column
 * {@code line_hashes} (which would be the correct value for this column).
 * <p>
 * Column will be populated with the correct value by the new "DB migration step" of the analysis report processing the
 * first time a project is analyzed after SonarQube's upgrade.
 * <p>
 * This innovative approach to DB migration is used because populating the column from {@code line_hashes} will take
 * a very long time on large DBs.
 */
public class PopulateFileSourceLineCount extends DataChange {
  private static final int LINE_COUNT_NOT_POPULATED = -1;

  public PopulateFileSourceLineCount(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select distinct project_uuid from file_sources where line_count is null");
    massUpdate.update("update file_sources set line_count = ? where project_uuid = ?")
      // Having transactions involving many rows can be very slow and should be avoided.
      // A project can have many file_sources, so transaction is committed after each project.
      .setBatchSize(1);
    massUpdate.rowPluralName("file sources");
    massUpdate.execute(PopulateFileSourceLineCount::handle);
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    String projectUuid = row.getString(1);

    update.setInt(1, LINE_COUNT_NOT_POPULATED);
    update.setString(2, projectUuid);
    return true;
  }
}
