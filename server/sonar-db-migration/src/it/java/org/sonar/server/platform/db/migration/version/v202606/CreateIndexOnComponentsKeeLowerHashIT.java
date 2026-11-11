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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnComponentsKeeLowerHash.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnComponentsKeeLowerHash.INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnComponentsKeeLowerHash.TABLE_NAME;

class CreateIndexOnComponentsKeeLowerHashIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateIndexOnComponentsKeeLowerHash.class);

  private final DdlChange underTest = new CreateIndexOnComponentsKeeLowerHash(db.database());

  @Test
  void execute_shouldCreateHashIndexOnMsSql() throws SQLException {
    assumeTrue(MsSql.ID.equals(db.database().getDialect().getId()));
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);

    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);
  }

  @Test
  void execute_shouldBeReentrantOnMsSql() throws SQLException {
    assumeTrue(MsSql.ID.equals(db.database().getDialect().getId()));

    underTest.execute();
    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME);
  }

  @Test
  void execute_shouldLeaveOtherDialectsUnchanged() throws SQLException {
    assumeTrue(!MsSql.ID.equals(db.database().getDialect().getId()));

    underTest.execute();
    underTest.execute();

    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);
  }
}
