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
package org.sonar.db.sca;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScaIssuesReleasesDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final ScaIssuesReleasesDao scaIssuesReleasesDao = db.getDbClient().scaIssuesReleasesDao();

  private static ScaIssueReleaseDto newScaIssueReleaseDto(String suffix) {
    return new ScaIssueReleaseDto("uuid" + suffix, "sca-issue-uuid" + suffix, "sca-release-uuid" + suffix, ScaSeverity.INFO, 1L, 2L);
  }

  @Test
  void insert_shouldPersistScaIssuesReleases() {
    ScaIssueReleaseDto issueReleaseDto = newScaIssueReleaseDto("1");

    scaIssuesReleasesDao.insert(db.getSession(), issueReleaseDto);

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from sca_issues_releases");
    assertThat(select).hasSize(1);
    Map<String, Object> stringObjectMap = select.get(0);
    assertThat(stringObjectMap).usingRecursiveComparison().isEqualTo(
      Map.ofEntries(
        Map.entry("uuid", issueReleaseDto.uuid()),
        Map.entry("sca_issue_uuid", issueReleaseDto.scaIssueUuid()),
        Map.entry("sca_release_uuid", issueReleaseDto.scaReleaseUuid()),
        Map.entry("severity", issueReleaseDto.severity().name()),
        Map.entry("severity_sort_key", (long) issueReleaseDto.severitySortKey()),
        Map.entry("created_at", issueReleaseDto.createdAt()),
        Map.entry("updated_at", issueReleaseDto.updatedAt())));
  }

  // Postgresql apparently doesn't support doing anything else in the session
  // after a statement with an error (constraint violation), while other
  // databases do. So we use a dedicated session here.
  private void insertAndCommit(ScaIssueReleaseDto issueReleaseDto) {
    try (var dbSession = db.getDbClient().openSession(false)) {
      scaIssuesReleasesDao.insert(dbSession, issueReleaseDto);
      dbSession.commit(true);
    }
  }

  @Test
  void insert_shouldFailOnDuplicateInsert() {
    ScaIssueReleaseDto issueReleaseDto = newScaIssueReleaseDto("1");
    try (var dbSession = db.getDbClient().openSession(false)) {
      scaIssuesReleasesDao.insert(dbSession, issueReleaseDto);
      dbSession.commit();
    }

    ScaIssueReleaseDto issueReleaseDtoDifferentUuid = new ScaIssueReleaseDto("uuid-different",
      issueReleaseDto.scaIssueUuid(), issueReleaseDto.scaReleaseUuid(), ScaSeverity.INFO,
      10L, 11L);

    assertThrows(PersistenceException.class, () -> insertAndCommit(issueReleaseDtoDifferentUuid));

    try (var dbSession = db.getDbClient().openSession(false)) {
      List<Map<String, Object>> select = db.select(dbSession, "select * from sca_issues_releases");
      assertThat(select).hasSize(1);
      Map<String, Object> stringObjectMap = select.get(0);
      assertThat(stringObjectMap).containsEntry("uuid", issueReleaseDto.uuid());
    }
  }

  @Test
  void deleteByUuid_shouldDelete() {
    var dbSession = db.getSession();
    ScaIssueReleaseDto issueReleaseDto = newScaIssueReleaseDto("1");
    scaIssuesReleasesDao.insert(dbSession, issueReleaseDto);
    dbSession.commit();

    assertThat(db.countRowsOfTable(dbSession, "sca_issues_releases")).isOne();

    scaIssuesReleasesDao.deleteByUuid(dbSession, issueReleaseDto.uuid());

    assertThat(db.countRowsOfTable(dbSession, "sca_issues_releases")).isZero();
  }
}
