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
package org.sonar.server.platform.db.migration.version.v202503;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class PopulateStatusColumnForScaIssuesReleasesTableTestIT {
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateStatusColumnForScaIssuesReleasesTable.class);

  private final PopulateStatusColumnForScaIssuesReleasesTable underTest = new PopulateStatusColumnForScaIssuesReleasesTable(db.database());

  @Test
  void execute_shouldPopulateStatusWithToReview() throws SQLException {
    insertScaIssuesReleases(1);
    insertScaIssuesReleases(2);

    underTest.execute();

    assertThatStatusIsPopulated();
  }

  @Test
  void execute_whenAlreadyExecuted_shouldBeIdempotent() throws SQLException {
    insertScaIssuesReleases(1);

    underTest.execute();
    underTest.execute();

    assertThatStatusIsPopulated();
  }

  private void insertScaIssuesReleases(Integer index) {
    db.executeInsert("sca_issues_releases",
      "uuid", "uuid-" + index,
      "sca_issue_uuid", "issue_id" + index,
      "sca_release_uuid", "release_id",
      "severity", "severity",
      "severity_sort_key", 1,
      "created_at", new Date().getTime(),
      "updated_at", new Date().getTime()
    );
  }

  private void assertThatStatusIsPopulated() {
    List<Map<String, Object>> rows = db.select("select status from sca_issues_releases");
    assertThat(rows).isNotEmpty()
      .allSatisfy(row -> assertThat(row).containsEntry("status", "TO_REVIEW"));
  }
}
