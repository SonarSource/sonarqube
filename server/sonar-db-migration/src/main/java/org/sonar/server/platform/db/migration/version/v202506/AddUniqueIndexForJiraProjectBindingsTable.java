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
package org.sonar.server.platform.db.migration.version.v202506;

import java.util.Map;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.CreateUniqueIndexOnColumns;

public class AddUniqueIndexForJiraProjectBindingsTable extends CreateUniqueIndexOnColumns {

  private static final String TABLE_NAME = "jira_project_bindings";
  private static final String INDEX_NAME = "idx_jira_proj_bindings_unique";
  static final String JIRA_ORGANIZATION_BINDING_ID = "jira_organization_binding_id";
  static final String SONAR_PROJECT_ID = "sonar_project_id";

  protected AddUniqueIndexForJiraProjectBindingsTable(Database db) {
    super(db, TABLE_NAME, INDEX_NAME, Map.of(JIRA_ORGANIZATION_BINDING_ID, false, SONAR_PROJECT_ID, false));
  }

}
