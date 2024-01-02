/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v96;

import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DropIndexChange;

import static org.sonar.server.platform.db.migration.version.v95.CreateIndexForRuleDescSections.INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v95.CreateRuleDescSectionsTable.RULE_DESCRIPTION_SECTIONS_TABLE;

public class DropIndexForRuleDescSection extends DropIndexChange {

  public DropIndexForRuleDescSection(Database db) {
    super(db, INDEX_NAME, RULE_DESCRIPTION_SECTIONS_TABLE);
  }

}
