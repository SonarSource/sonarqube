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
package org.sonar.server.platform.db.migration.version.v202605;

import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.RenameTableChange;

/**
 * {@code arch_intended} (created by {@code CreateArchIntendedTable} in v202604) has the exact same
 * columns as {@code arch_models}, so renaming it in place preserves any rows that were already
 * persisted before this upgrade, unlike a create-fresh-table-then-drop-old-table approach.
 */
public class RenameArchIntendedToArchModels extends RenameTableChange {

  static final String OLD_TABLE_NAME = "arch_intended";
  static final String NEW_TABLE_NAME = "arch_models";

  public RenameArchIntendedToArchModels(Database db) {
    super(db, OLD_TABLE_NAME, NEW_TABLE_NAME);
  }
}
