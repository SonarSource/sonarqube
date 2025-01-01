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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.sql.Types.BIGINT;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class AlterCveColumnsToNullableIT {

  private static final String TABLE_NAME = "cves";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(AlterCveColumnsToNullable.class);

  private final DdlChange underTest = new AlterCveColumnsToNullable(db.database());

  @Test
  void execute_shouldUpdateConstraints() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, "published_at", BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, "last_modified_at", BIGINT, null, false);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, "published_at", BIGINT, null, true);
    db.assertColumnDefinition(TABLE_NAME, "last_modified_at", BIGINT, null, true);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, "published_at", BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, "last_modified_at", BIGINT, null, false);
    underTest.execute();

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, "published_at", BIGINT, null, true);
    db.assertColumnDefinition(TABLE_NAME, "last_modified_at", BIGINT, null, true);
  }

}
