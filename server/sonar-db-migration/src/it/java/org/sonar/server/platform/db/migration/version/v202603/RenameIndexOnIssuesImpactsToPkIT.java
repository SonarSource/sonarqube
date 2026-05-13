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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class RenameIndexOnIssuesImpactsToPkIT {

  private static final String TABLE_NAME = "issues_impacts";
  private static final String OLD_INDEX_NAME = "uniq_iss_key_sof_qual";
  private static final String NEW_INDEX_NAME = "pk_issues_impacts";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(RenameIndexOnIssuesImpactsToPk.class);
  private final RenameIndexOnIssuesImpactsToPk underTest = new RenameIndexOnIssuesImpactsToPk(db.database());

  @Test
  void execute_shouldRenameIndexOnOracle() throws SQLException {
    assumeOracle();

    db.assertUniqueIndex(TABLE_NAME, OLD_INDEX_NAME, "issue_key", "software_quality");

    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, OLD_INDEX_NAME);
    db.assertUniqueIndex(TABLE_NAME, NEW_INDEX_NAME, "issue_key", "software_quality");
  }

  @Test
  void execute_shouldBeReentrantOnOracle() throws SQLException {
    assumeOracle();

    underTest.execute();
    underTest.execute();

    db.assertIndexDoesNotExist(TABLE_NAME, OLD_INDEX_NAME);
    db.assertUniqueIndex(TABLE_NAME, NEW_INDEX_NAME, "issue_key", "software_quality");
  }

  @Test
  void execute_shouldBeNoOpOnNonOracle() {
    assumeFalse(Oracle.ID.equals(db.database().getDialect().getId()));

    db.assertIndexDoesNotExist(TABLE_NAME, OLD_INDEX_NAME);

    assertThatCode(underTest::execute).doesNotThrowAnyException();

    db.assertIndexDoesNotExist(TABLE_NAME, OLD_INDEX_NAME);
  }

  private void assumeOracle() {
    assumeTrue(Oracle.ID.equals(db.database().getDialect().getId()));
  }
}
