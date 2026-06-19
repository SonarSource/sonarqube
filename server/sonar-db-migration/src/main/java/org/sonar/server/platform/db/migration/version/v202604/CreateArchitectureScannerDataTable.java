/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.S3StyleTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

/**
 * Creates the {@code arch_scanner_data} table holding the binary payloads uploaded by scanner clients. It
 * is the relational replacement for the SonarQube Cloud S3 scanner-data bucket: each object is keyed by a
 * composite name ({@code projectId/branchId/ceTaskId}), carries the payload bytes ({@code data}), and is
 * accessed via the host MyBatis mapper registered by the architecture-server module. Community/global.
 */
public class CreateArchitectureScannerDataTable extends CreateTableChange {

  static final String TABLE_NAME = "arch_scanner_data";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_NAME = "name";
  static final String COLUMN_DATA = "data";
  static final String COLUMN_METADATA = "metadata";
  static final String INDEX_NAME = "arch_scanner_data_name";

  protected CreateArchitectureScannerDataTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new S3StyleTableBuilder(getDialect(), tableName)
      .withUuidColumn(COLUMN_UUID)
      .withNameColumn(COLUMN_NAME, 255, INDEX_NAME)
      .withDataColumn(COLUMN_DATA)
      .build());
  }
}
