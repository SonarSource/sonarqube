/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v202504;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class PopulateOriginalSeverityForScaIssuesReleasesTableIT {
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateOriginalSeverityForScaIssuesReleasesTable.class);
  private final PopulateOriginalSeverityForScaIssuesReleasesTable underTest = new PopulateOriginalSeverityForScaIssuesReleasesTable(db.database());

  @Test
  void execute_shouldPopulateOriginaldSeverity() throws SQLException {
    insertScaIssuesReleases(1, "HIGH", null);
    insertScaIssuesReleases(2, "INFO", null);

    underTest.execute();

    assertThatOriginalSeverityIs(1, "HIGH");
    assertThatOriginalSeverityIs(2, "INFO");
  }

  @Test
  void execute_whenAlreadyExecuted_shouldBeIdempotent() throws SQLException {
    insertScaIssuesReleases(1, "HIGH", "INFO");

    underTest.execute();
    underTest.execute();

    assertThatOriginalSeverityIs(1, "INFO");
  }

  private void insertScaIssuesReleases(Integer index, String severity, @Nullable String originalSeverity) {
    db.executeInsert("sca_issues_releases",
      "uuid", "uuid-" + index,
      "sca_issue_uuid", "issue_id" + index,
      "sca_release_uuid", "release_id",
      "status", "TO_REVIEW",
      "severity", severity,
      "original_severity", originalSeverity,
      "manual_severity", null,
      "severity_sort_key", 1,
      "created_at", new Date().getTime(),
      "updated_at", new Date().getTime()
    );
  }

  private void assertThatOriginalSeverityIs(Integer index, String expectedSeverity) {
    String uuid = "uuid-" + index;
    List<Map<String, Object>> rows = db.select("select original_severity from sca_issues_releases where uuid = '%s'".formatted(uuid));
    assertThat(rows).isNotEmpty()
      .allSatisfy(row -> assertThat(row).containsEntry("original_severity", expectedSeverity));
  }
}
