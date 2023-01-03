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
package org.sonar.server.platform.db.migration.version.v84.rules.issues;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class AddIndexesToIssuesTable extends DdlChange {
  private static final String TABLE = "issues";

  public AddIndexesToIssuesTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (!indexExists("issues_assignee")) {
      context.execute(new CreateIndexBuilder()
        .setTable(TABLE)
        .setName("issues_assignee")
        .addColumn("assignee")
        .build());
    }
    if (!indexExists("issues_component_uuid")) {

      context.execute(new CreateIndexBuilder()
        .setTable(TABLE)
        .setName("issues_component_uuid")
        .addColumn("component_uuid")
        .build());
    }
    if (!indexExists("issues_creation_date")) {

      context.execute(new CreateIndexBuilder()
        .setTable(TABLE)
        .setName("issues_creation_date")
        .addColumn("issue_creation_date")
        .build());
    }
    if (!indexExists("issues_kee")) {

      context.execute(new CreateIndexBuilder()
        .setTable(TABLE)
        .setName("issues_kee")
        .setUnique(true)
        .addColumn("kee")
        .build());
    }
    if (!indexExists("issues_project_uuid")) {
      context.execute(new CreateIndexBuilder()
        .setTable(TABLE)
        .setName("issues_project_uuid")
        .addColumn("project_uuid")
        .build());
    }
    if (!indexExists("issues_resolution")) {
      context.execute(new CreateIndexBuilder()
        .setTable(TABLE)
        .setName("issues_resolution")
        .addColumn("resolution")
        .build());
    }
    if (!indexExists("issues_updated_at")) {
      context.execute(new CreateIndexBuilder()
        .setTable(TABLE)
        .setName("issues_updated_at")
        .addColumn("updated_at")
        .build());
    }
    if (!indexExists("issues_rule_uuid")) {
      context.execute(new CreateIndexBuilder()
        .setTable(TABLE)
        .setName("issues_rule_uuid")
        .addColumn("rule_uuid")
        .build());
    }
  }

  private boolean indexExists(String name) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.indexExistsIgnoreCase(TABLE, name, connection);
    }
  }
}
