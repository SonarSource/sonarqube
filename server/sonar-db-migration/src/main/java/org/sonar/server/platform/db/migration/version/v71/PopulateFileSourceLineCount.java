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
package org.sonar.server.platform.db.migration.version.v71;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

import static com.google.common.base.Splitter.on;

public class PopulateFileSourceLineCount extends DataChange {
  private static final Splitter LINES_HASHES_SPLITTER = on('\n');
  public PopulateFileSourceLineCount(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id,line_hashes from file_sources where line_count is null");
    massUpdate.update("update file_sources set line_count = ? where id = ?");
    massUpdate.rowPluralName("line counts");
    massUpdate.execute(PopulateFileSourceLineCount::handle);
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    int rowId = row.getInt(1);
    String rawData = row.getNullableString(2);

    int lineCount = rawData == null ? 0 : Iterables.size(LINES_HASHES_SPLITTER.split(rawData));
    update.setInt(1, lineCount);
    update.setInt(2, rowId);
    return true;
  }
}
