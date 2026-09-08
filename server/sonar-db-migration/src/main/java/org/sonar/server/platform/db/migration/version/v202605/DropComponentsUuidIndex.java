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
import org.sonar.server.platform.db.migration.step.DropIndexChange;

/**
 * This unique index is replaced by a primary key on the same column, see {@link AddComponentsUuidPrimaryKey}.
 */
public class DropComponentsUuidIndex extends DropIndexChange {
  static final String TABLE_NAME = "components";
  static final String INDEX_NAME = "components_uuid";

  public DropComponentsUuidIndex(Database db) {
    super(db, INDEX_NAME, TABLE_NAME);
  }
}
