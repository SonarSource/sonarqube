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
package org.sonar.server.platform.db.migration.version.v202502;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.db.MigrationDbTester.createForMigrationStep;
import static org.sonar.server.platform.db.migration.version.v202502.CreateUniqueIndexOnScaIssues.COLUMN_NAME_PACKAGE_URL;
import static org.sonar.server.platform.db.migration.version.v202502.CreateUniqueIndexOnScaIssues.COLUMN_NAME_SCA_ISSUE_TYPE;
import static org.sonar.server.platform.db.migration.version.v202502.CreateUniqueIndexOnScaIssues.COLUMN_NAME_SPDX_LICENSE_ID;
import static org.sonar.server.platform.db.migration.version.v202502.CreateUniqueIndexOnScaIssues.COLUMN_NAME_VULNERABILITY_ID;
import static org.sonar.server.platform.db.migration.version.v202502.CreateUniqueIndexOnScaIssues.INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v202502.CreateUniqueIndexOnScaIssues.TABLE_NAME;

class CreateUniqueIndexOnScaIssuesIT {
  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(CreateUniqueIndexOnScaIssues.class);
  private final DdlChange underTest = new CreateUniqueIndexOnScaIssues(db.database());

  @Test
  void execute_shouldCreateIndex() throws SQLException {
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
    underTest.execute();
    db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME_SCA_ISSUE_TYPE, COLUMN_NAME_VULNERABILITY_ID, COLUMN_NAME_PACKAGE_URL, COLUMN_NAME_SPDX_LICENSE_ID);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
    underTest.execute();
    underTest.execute();
    db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, COLUMN_NAME_SCA_ISSUE_TYPE, COLUMN_NAME_VULNERABILITY_ID, COLUMN_NAME_PACKAGE_URL, COLUMN_NAME_SPDX_LICENSE_ID);
  }
}
