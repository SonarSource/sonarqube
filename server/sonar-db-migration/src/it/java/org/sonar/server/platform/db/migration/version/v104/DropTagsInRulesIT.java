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
package org.sonar.server.platform.db.migration.version.v104;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

class DropTagsInRulesIT {
  static final String TABLE_NAME = "rules";
  static final String COLUMN_NAME = "tags";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DropTagsInRules.class);
  private final DdlChange underTest = new DropTagsInRules(db.database());

  @Test
  void executed_whenRun_shouldDropTagsColumn() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, 4000, true);
    underTest.execute();
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);
  }

  @Test
  void execute_whenExecutedMoreThanOnce_shouldBeReentrant() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, 4000, true);
    underTest.execute();
    underTest.execute();
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);
  }
}
