/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202502;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class CreateUniqueIndexOnScaIssues extends DdlChange {

  static final String TABLE_NAME = "sca_issues";
  static final String INDEX_NAME = "sca_issues_unique";
  static final String COLUMN_NAME_SCA_ISSUE_TYPE = "sca_issue_type";
  static final String COLUMN_NAME_PACKAGE_URL = "package_url";
  static final String COLUMN_NAME_VULNERABILITY_ID = "vulnerability_id";
  static final String COLUMN_NAME_SPDX_LICENSE_ID = "spdx_license_id";

  public CreateUniqueIndexOnScaIssues(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      createIndex(context, connection);
    }
  }

  private void createIndex(Context context, Connection connection) {
    if (!DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)) {
      context.execute(new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName(INDEX_NAME)
        .setUnique(true)
        .addColumn(COLUMN_NAME_SCA_ISSUE_TYPE, false)
        .addColumn(COLUMN_NAME_VULNERABILITY_ID, false)
        // we specifically want this to be after vulnerability ID, so we can
        // do an indexed lookup by vulnerability ID without having to know the
        // package URL
        .addColumn(COLUMN_NAME_PACKAGE_URL, false)
        .addColumn(COLUMN_NAME_SPDX_LICENSE_ID, false)
        .build());
    }
  }
}
