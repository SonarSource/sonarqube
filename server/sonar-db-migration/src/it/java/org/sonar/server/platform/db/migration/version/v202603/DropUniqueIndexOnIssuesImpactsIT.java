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

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Oracle;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class DropUniqueIndexOnIssuesImpactsIT {

  private static final String TABLE_NAME = "issues_impacts";
  private static final String INDEX_NAME = "uniq_iss_key_sof_qual";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(DropUniqueIndexOnIssuesImpacts.class);
  private final DropUniqueIndexOnIssuesImpacts underTest = new DropUniqueIndexOnIssuesImpacts(db.database());

  @Test
  void execute_shouldDropIndex() throws SQLException {
    assumeNotOracle();

    db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, "issue_key", "software_quality");

    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    assumeNotOracle();

    db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, "issue_key", "software_quality");

    underTest.execute();
    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, INDEX_NAME);
  }

  @Test
  void execute_shouldNotDropIndexOnOracle() throws SQLException {
    assumeOracle();

    db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, "issue_key", "software_quality");

    underTest.execute();

    db.assertUniqueIndex(TABLE_NAME, INDEX_NAME, "issue_key", "software_quality");
  }

  private void assumeNotOracle() {
    assumeFalse(Oracle.ID.equals(db.database().getDialect().getId()),
      "Oracle reuses this unique index as the backing index for the PK, so the drop is skipped");
  }

  private void assumeOracle() {
    assumeTrue(Oracle.ID.equals(db.database().getDialect().getId()));
  }
}
