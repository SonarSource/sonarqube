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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class MigrateRemoveDuplicateScaReleasesIT {
  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(MigrateRemoveDuplicateScaReleases.class);
  private final MigrationStep underTest = new MigrateRemoveDuplicateScaReleases(db.database());

  @Test
  void test_removesDuplicates() throws SQLException {
    // we should keep this one
    insertRelease("0", "componentUuid1", "packageUrlNotDuplicated", 1L);
    // we should keep these rows associated with release 0
    insertDependency("0", "scaReleaseUuid0");
    insertIssueRelease("0", "scaReleaseUuid0");
    insertIssueReleaseChange("0");
    // we should keep the first (oldest) packageUrl1 entry on componentUuid1
    insertRelease("1", "componentUuid1", "packageUrl1", 2L);
    insertRelease("2", "componentUuid1", "packageUrl1", 3L);
    insertRelease("3", "componentUuid1", "packageUrl1", 4L);
    // we should delete these rows associated with release 3 that we delete
    insertDependency("3", "scaReleaseUuid3");
    insertIssueRelease("3", "scaReleaseUuid3");
    insertIssueReleaseChange("3");
    // we should keep the first (oldest) packageUrl2 entry on componentUuid1
    insertRelease("4", "componentUuid1", "packageUrl2", 5L);
    insertRelease("5", "componentUuid1", "packageUrl2", 6L);
    // we should keep the first (oldest) packageUrl1 entry on componentUuid2
    insertRelease("6", "componentUuid2", "packageUrl1", 7L);
    insertRelease("7", "componentUuid2", "packageUrl1", 8L);
    // we should keep these rows associated with release 6
    insertDependency("6", "scaReleaseUuid6");
    insertIssueRelease("6", "scaReleaseUuid6");
    insertIssueReleaseChange("6");
    // we should delete these rows associated with release 7 that we delete
    insertDependency("7", "scaReleaseUuid7");
    insertIssueRelease("7", "scaReleaseUuid7");
    insertIssueReleaseChange("7");

    assertThat(db.countSql("select count(*) from sca_releases")).isEqualTo(8);
    assertThat(db.countSql("select count(*) from sca_dependencies")).isEqualTo(4);
    assertThat(db.countSql("select count(*) from sca_issues_releases")).isEqualTo(4);
    assertThat(db.countSql("select count(*) from sca_issue_rels_changes")).isEqualTo(4);
    underTest.execute();

    assertThat(db.select("select uuid from sca_releases")).map(row -> row.get("uuid"))
      .containsExactlyInAnyOrder("scaReleaseUuid0", "scaReleaseUuid1", "scaReleaseUuid4", "scaReleaseUuid6");
    assertThat(db.select("select uuid from sca_dependencies")).map(row -> row.get("uuid"))
      .containsExactlyInAnyOrder("scaDependencyUuid0", "scaDependencyUuid6");
    assertThat(db.select("select uuid from sca_issues_releases")).map(row -> row.get("uuid"))
      .containsExactlyInAnyOrder("scaIssueReleaseUuid0", "scaIssueReleaseUuid6");
    assertThat(db.select("select uuid from sca_issue_rels_changes")).map(row -> row.get("uuid"))
      .containsExactlyInAnyOrder("scaIssueReleaseChangeUuid0", "scaIssueReleaseChangeUuid6");
  }

  @Test
  void test_canRunMultipleTimesOnEmptyTable() throws SQLException {
    assertThat(db.countSql("select count(*) from sca_releases")).isZero();
    underTest.execute();
    underTest.execute();
    assertThat(db.countSql("select count(*) from sca_releases")).isZero();
  }

  private void insertRelease(String suffix, String componentUuid, String packageUrl, long createdAt) {
    db.executeInsert("sca_releases",
      "uuid", "scaReleaseUuid" + suffix,
      "component_uuid", componentUuid,
      "package_url", packageUrl,
      "package_manager", "MAVEN",
      "package_name", "packageName",
      "version", "1.0.0",
      "license_expression", "MIT",
      "declared_license_expression", "MIT",
      "is_new", false,
      "known", true,
      "known_package", true,
      "updated_at", 1L,
      "created_at", createdAt);
  }

  private void insertDependency(String suffix, String releaseUuid) {
    db.executeInsert("sca_dependencies",
      "uuid", "scaDependencyUuid" + suffix,
      "sca_release_uuid", releaseUuid,
      "direct", true,
      "scope", "compile",
      "is_new", false,
      "updated_at", 1L,
      "created_at", 2L);
  }

  private void insertIssueRelease(String suffix, String releaseUuid) {
    db.executeInsert("sca_issues_releases",
      "uuid", "scaIssueReleaseUuid" + suffix,
      "sca_release_uuid", releaseUuid,
      "sca_issue_uuid", "scaIssueUuid" + suffix,
      "severity", "LOW",
      "severity_sort_key", 10,
      "status", "OPEN",
      "updated_at", 1L,
      "created_at", 2L);
  }

  private void insertIssueReleaseChange(String suffix) {
    db.executeInsert("sca_issue_rels_changes",
      "uuid", "scaIssueReleaseChangeUuid" + suffix,
      "sca_issues_releases_uuid", "scaIssueReleaseUuid" + suffix,
      "updated_at", 1L,
      "created_at", 2L);
  }
}
