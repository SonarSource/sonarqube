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
package org.sonar.server.platform.db.migration.version.v202603;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202603.MakeScaIssuesReleasesComponentUuidNotNullable.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v202603.MakeScaIssuesReleasesComponentUuidNotNullable.INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v202603.MakeScaIssuesReleasesComponentUuidNotNullable.TABLE_NAME;

class MakeScaIssuesReleasesComponentUuidNotNullableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MakeScaIssuesReleasesComponentUuidNotNullable.class);

  private final MakeScaIssuesReleasesComponentUuidNotNullable underTest = new MakeScaIssuesReleasesComponentUuidNotNullable(db.database());

  @Test
  void execute_shouldMakeColumnNotNullableAndCreateIndex() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, 40, true);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, 40, false);
    try (Connection connection = db.database().getDataSource().getConnection()) {
      assertThat(DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)).isTrue();
    }
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, 40, false);
    try (Connection connection = db.database().getDataSource().getConnection()) {
      assertThat(DatabaseUtils.indexExistsIgnoreCase(TABLE_NAME, INDEX_NAME, connection)).isTrue();
    }
  }
}
