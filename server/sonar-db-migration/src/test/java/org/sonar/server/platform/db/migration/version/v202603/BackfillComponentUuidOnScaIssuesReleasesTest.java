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

import static org.assertj.core.api.Assertions.assertThat;

class BackfillComponentUuidOnScaIssuesReleasesTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(BackfillComponentUuidOnScaIssuesReleases.class);

  private final BackfillComponentUuidOnScaIssuesReleases underTest = new BackfillComponentUuidOnScaIssuesReleases(db.database());

  @Test
  void execute_shouldBackfillComponentUuidFromRelease() throws SQLException {
    insertRelease("release-uuid-1", "component-uuid-1");
    insertIssueRelease("ir-uuid-1", "issue-uuid-1", "release-uuid-1");

    underTest.execute();

    var rows = db.select("SELECT component_uuid FROM sca_issues_releases WHERE uuid = 'ir-uuid-1'");
    assertThat(rows).hasSize(1);
    assertThat(rows.getFirst()).containsEntry("COMPONENT_UUID", "component-uuid-1");
  }

  @Test
  void execute_shouldDeleteOrphanedRows() throws SQLException {
    // Valid row — must survive
    insertRelease("release-uuid-valid", "component-uuid-valid");
    insertIssueRelease("ir-valid", "issue-uuid-valid", "release-uuid-valid");
    
    // Row with no matching sca_releases entry
    insertIssueRelease("ir-orphan", "issue-uuid-orphan", "non-existent-release-uuid");

    underTest.execute();

    var orphanRows = db.select("SELECT uuid FROM sca_issues_releases WHERE uuid = 'ir-orphan'");
    assertThat(orphanRows).isEmpty();
    
    var validRows = db.select("SELECT component_uuid FROM sca_issues_releases WHERE uuid = 'ir-valid'");
    assertThat(validRows).hasSize(1);
    assertThat(validRows.getFirst()).containsEntry("COMPONENT_UUID", "component-uuid-valid");
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    insertRelease("release-uuid-2", "component-uuid-2");
    insertIssueRelease("ir-uuid-2", "issue-uuid-2", "release-uuid-2");

    underTest.execute();
    underTest.execute();

    var rows = db.select("SELECT component_uuid FROM sca_issues_releases WHERE uuid = 'ir-uuid-2'");
    assertThat(rows).hasSize(1);
    assertThat(rows.getFirst()).containsEntry("COMPONENT_UUID", "component-uuid-2");
  }

  private void insertRelease(String uuid, String componentUuid) {
    db.executeInsert("sca_releases",
      "uuid", uuid,
      "component_uuid", componentUuid,
      "package_url", "pkg:maven/com.example/test@1.0.0",
      "package_manager", "MAVEN",
      "package_name", "com.example:test",
      "version", "1.0.0",
      "license_expression", "MIT",
      "declared_license_expression", "MIT",
      "known", true,
      "known_package", true,
      "is_new", false,
      "created_at", 1_000_000L,
      "updated_at", 1_000_000L);
  }

  private void insertIssueRelease(String uuid, String issueUuid, String releaseUuid) {
    db.executeInsert("sca_issues_releases",
      "uuid", uuid,
      "sca_issue_uuid", issueUuid,
      "sca_release_uuid", releaseUuid,
      "severity", "HIGH",
      "severity_sort_key", 3,
      "status", "OPEN",
      "original_severity", "HIGH",
      "show_increased_severity_warning", false,
      "created_at", 1_000_000L,
      "updated_at", 1_000_000L);
  }
}
