/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import org.apache.commons.lang.StringUtils;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

public class PopulateFileSourceLineCount extends DataChange {
  static final int LINE_COUNT_NOT_POPULATED = -1;
  private static final String NEW_LINE = "\n";

  public PopulateFileSourceLineCount(Database database) {
    super(database);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid, line_hashes from file_sources where line_count = ?").setInt(1, LINE_COUNT_NOT_POPULATED);
    massUpdate.update("update file_sources set line_count = ? where uuid = ?");
    massUpdate.rowPluralName("line counts of file sources");
    massUpdate.execute(PopulateFileSourceLineCount::handle);
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    String rowUuid = row.getString(1);
    String rawData = row.getNullableString(2);

    int lineCount = rawData == null ? 0 : (StringUtils.countMatches(rawData, NEW_LINE) + 1);
    update.setInt(1, lineCount);
    update.setString(2, rowUuid);
    return true;
  }
}
