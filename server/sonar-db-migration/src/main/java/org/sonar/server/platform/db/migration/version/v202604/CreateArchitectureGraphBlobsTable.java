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

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

/**
 * Creates the {@code arch_graph_blobs} table holding the binary payloads of the unified architecture
 * capability's graphs. It is the relational form of the SonarQube Cloud S3 graph bucket: each object is keyed
 * by the graph UUID ({@code name}), carries the payload bytes ({@code data}) and an optional metadata map
 * ({@code metadata}) — expressed through {@link S3StyleTableBuilder}. The capability reads/writes it via the
 * host MyBatis mapper it registers (see the architecture-server module); this table is community/global, not
 * edition-gated.
 */
public class CreateArchitectureGraphBlobsTable extends CreateTableChange {

  static final String TABLE_NAME = "arch_graph_blobs";
  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_NAME = "name";
  static final String COLUMN_DATA = "data";
  static final String COLUMN_METADATA = "metadata";
  static final String INDEX_NAME = "arch_graph_blobs_name";

  protected CreateArchitectureGraphBlobsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new S3StyleTableBuilder(getDialect(), tableName)
      .withUuidColumn(COLUMN_UUID)
      .withNameColumn(COLUMN_NAME, UUID_SIZE, INDEX_NAME)
      .withDataColumn(COLUMN_DATA)
      .build());
  }
}
