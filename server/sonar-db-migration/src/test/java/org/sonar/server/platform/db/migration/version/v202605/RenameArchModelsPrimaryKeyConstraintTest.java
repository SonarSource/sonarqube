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

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.sql.DropPrimaryKeySqlGenerator;

import static org.sonar.server.platform.db.migration.version.v202605.RenameArchModelsPrimaryKeyConstraint.COLUMN_PROJECT_ID;
import static org.sonar.server.platform.db.migration.version.v202605.RenameArchModelsPrimaryKeyConstraint.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.RenameArchModelsPrimaryKeyConstraint.NEW_CONSTRAINT_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.RenameArchModelsPrimaryKeyConstraint.TABLE_NAME;

class RenameArchModelsPrimaryKeyConstraintTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RenameArchModelsPrimaryKeyConstraint.class);

  private final DropPrimaryKeySqlGenerator sqlGenerator = new DropPrimaryKeySqlGenerator(db.database(), new DbPrimaryKeyConstraintFinder(db.database()));

  private final RenameArchModelsPrimaryKeyConstraint underTest = new RenameArchModelsPrimaryKeyConstraint(db.database(), sqlGenerator);

  @Test
  void execute_shouldRenamePrimaryKeyConstraint() throws SQLException {
    db.assertPrimaryKey(TABLE_NAME, "PK_ARCH_INTENDED", COLUMN_PROJECT_ID, COLUMN_UUID);

    underTest.execute();

    db.assertPrimaryKey(TABLE_NAME, NEW_CONSTRAINT_NAME.toUpperCase(), COLUMN_PROJECT_ID, COLUMN_UUID);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertPrimaryKey(TABLE_NAME, NEW_CONSTRAINT_NAME.toUpperCase(), COLUMN_PROJECT_ID, COLUMN_UUID);
  }
}
