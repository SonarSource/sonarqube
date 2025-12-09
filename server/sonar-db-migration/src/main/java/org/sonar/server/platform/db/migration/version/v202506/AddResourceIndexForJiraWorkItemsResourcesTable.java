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

import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.CreateNonUniqueIndexOnColumns;

public class AddResourceIndexForJiraWorkItemsResourcesTable extends CreateNonUniqueIndexOnColumns {

  private static final String TABLE_NAME = "jira_work_items_resources";
  private static final String INDEX_PREFIX = "ix";
  private static final String COLUMN_RESOURCE_ID = "resource_id";
  private static final String COLUMN_RESOURCE_TYPE = "resource_type";

  protected AddResourceIndexForJiraWorkItemsResourcesTable(Database db) {
    super(db, TABLE_NAME, INDEX_PREFIX, COLUMN_RESOURCE_ID, COLUMN_RESOURCE_TYPE);
  }

}
