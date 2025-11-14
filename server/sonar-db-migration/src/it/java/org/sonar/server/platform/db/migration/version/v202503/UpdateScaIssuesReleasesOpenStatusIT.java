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
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class UpdateScaIssuesReleasesOpenStatusIT {
  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(UpdateScaIssuesReleasesOpenStatus.class);
  private final DataChange underTest = new UpdateScaIssuesReleasesOpenStatus(db.database());

  @Test
  void execute_doesNotCreateRecords() throws SQLException {
    underTest.execute();
    assertThat(db.countSql("select count(*) from sca_issues_releases")).isZero();
  }

  @Test
  void execute_doesNotOverwriteOtherStatus() throws SQLException {
    insertScaIssuesReleases("FIXED");

    underTest.execute();

    assertAllStatusesEqual("FIXED");
  }

  @Test
  void execute_whenAlreadyExecuted_shouldBeIdempotent() throws SQLException {
    insertScaIssuesReleases("TO_REVIEW");
    underTest.execute();
    assertAllStatusesEqual("OPEN");
    underTest.execute();
    assertAllStatusesEqual("OPEN");
  }

  private void insertScaIssuesReleases(String status) {
    db.executeInsert("sca_issues_releases",
      "uuid", "uuid",
      "sca_issue_uuid", "issue_id",
      "sca_release_uuid", "release_id",
      "severity", "severity",
      "severity_sort_key", 1,
      "status", status,
      "created_at", new Date().getTime(),
      "updated_at", new Date().getTime()
    );
  }

  private void assertAllStatusesEqual(String status) {
    List<Map<String, Object>> rows = db.select("select status from sca_issues_releases");
    assertThat(rows).isNotEmpty()
      .allSatisfy(row -> assertThat(row).containsEntry("status", status));
  }
}
