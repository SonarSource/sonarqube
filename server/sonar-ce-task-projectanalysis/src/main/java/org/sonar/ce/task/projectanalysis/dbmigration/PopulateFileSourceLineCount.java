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
package org.sonar.ce.task.projectanalysis.dbmigration;

import com.google.common.collect.Iterables;
import java.sql.SQLException;
import org.sonar.ce.task.CeTask;
import org.sonar.db.Database;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;

import static org.sonar.db.source.FileSourceDto.LINE_COUNT_NOT_POPULATED;

public class PopulateFileSourceLineCount extends DataChange implements ProjectAnalysisDataChange {
  private final CeTask ceTask;

  public PopulateFileSourceLineCount(Database database, CeTask ceTask) {
    super(database);
    this.ceTask = ceTask;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String componentUuid = ceTask.getComponent().get().getUuid();
    Long unInitializedFileSources = context.prepareSelect("select count(1) from file_sources where line_count = ? and project_uuid = ?")
      .setInt(1, LINE_COUNT_NOT_POPULATED)
      .setString(2, componentUuid)
      .get(row -> row.getLong(1));

    if (unInitializedFileSources != null && unInitializedFileSources > 0) {
      MassUpdate massUpdate = context.prepareMassUpdate();
      massUpdate.select("select id,line_hashes from file_sources where line_count = ? and project_uuid = ?")
        .setInt(1, LINE_COUNT_NOT_POPULATED)
        .setString(2, componentUuid);
      massUpdate.update("update file_sources set line_count = ? where id = ?");
      massUpdate.rowPluralName("line counts of sources of project " + componentUuid);
      massUpdate.execute(PopulateFileSourceLineCount::handle);
    }
  }

  private static boolean handle(Select.Row row, SqlStatement update) throws SQLException {
    int rowId = row.getInt(1);
    String rawData = row.getNullableString(2);

    int lineCount = rawData == null ? 0 : Iterables.size(FileSourceDto.LINES_HASHES_SPLITTER.split(rawData));
    update.setInt(1, lineCount);
    update.setInt(2, rowId);
    return true;
  }
}
