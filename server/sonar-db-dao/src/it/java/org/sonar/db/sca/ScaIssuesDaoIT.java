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

class ScaIssuesDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final ScaIssuesDao scaIssuesDao = db.getDbClient().scaIssuesDao();

  private static final ScaIssueDto newScaIssueDto(String suffix) {
    return new ScaIssueDto("uuid" + suffix, ScaIssueType.PROHIBITED_LICENSE, "fakePackageUrl" + suffix, "fakeVulnerabilityId" + suffix, "fakeSpdxId" + suffix, 1L, 2L);
  }

  @Test
  void insert_shouldPersistScaIssues() {
    ScaIssueDto issueDto = newScaIssueDto("1");
    scaIssuesDao.insert(db.getSession(), issueDto);

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from sca_issues");
    assertThat(select).hasSize(1);
    Map<String, Object> stringObjectMap = select.get(0);
    assertThat(stringObjectMap).containsExactlyInAnyOrderEntriesOf(
      Map.ofEntries(
        Map.entry("uuid", issueDto.uuid()),
        Map.entry("sca_issue_type", issueDto.scaIssueType().name()),
        Map.entry("package_url", issueDto.packageUrl()),
        Map.entry("vulnerability_id", issueDto.vulnerabilityId()),
        Map.entry("spdx_license_id", issueDto.spdxLicenseId()),
        Map.entry("created_at", issueDto.createdAt()),
        Map.entry("updated_at", issueDto.updatedAt())));
  }

  // Postgresql apparently doesn't support doing anything else in the session
  // after a statement with an error (constraint violation), while other
  // databases do. So we use a dedicated session here.
  private void insertAndCommit(ScaIssueDto issueDto) {
    try (var dbSession = db.getDbClient().openSession(false)) {
      scaIssuesDao.insert(dbSession, issueDto);
      dbSession.commit(true);
    }
  }

  @Test
  void insert_shouldFailOnDuplicateInsert() {
    // we are avoiding db.getSession() in here because the constraint violation
    // can invalidate the session and there's some chance of deadlock problems
    // with multiple sessions so let's just juggle dedicated sessions manually
    // for each query and not use the global session from DbTester

    ScaIssueDto issueDto = newScaIssueDto("1");
    try (var dbSession = db.getDbClient().openSession(false)) {
      scaIssuesDao.insert(dbSession, issueDto);
      dbSession.commit();
    }

    ScaIssueDto issueDtoDifferentUuid = new ScaIssueDto("uuid-different", issueDto.scaIssueType(), issueDto.packageUrl(),
      issueDto.vulnerabilityId(), issueDto.spdxLicenseId(), 10L, 11L);

    assertThrows(PersistenceException.class, () -> insertAndCommit(issueDtoDifferentUuid));

    try (var dbSession = db.getDbClient().openSession(false)) {
      List<Map<String, Object>> select = db.select(dbSession, "select * from sca_issues");
      assertThat(select).hasSize(1);
      Map<String, Object> stringObjectMap = select.get(0);
      assertThat(stringObjectMap).containsEntry("uuid", issueDto.uuid());
    }
  }

  @Test
  void selectByUuid_shouldLoadScaIssue() {
    ScaIssueDto issueDto = newScaIssueDto("1");
    scaIssuesDao.insert(db.getSession(), issueDto);

    var loadedOptional = scaIssuesDao.selectByUuid(db.getSession(), issueDto.uuid());

    assertThat(loadedOptional).contains(issueDto);
  }

  @Test
  void selectByUuids_shouldLoadScaIssues() {
    List<ScaIssueDto> issueDtos = List.of(newScaIssueDto("1"), newScaIssueDto("2"), newScaIssueDto("3"));
    for (var issueDto : issueDtos) {
      scaIssuesDao.insert(db.getSession(), issueDto);
    }

    List<String> uuidsToLoad = List.of(issueDtos.get(0).uuid(), issueDtos.get(2).uuid());
    var loaded = scaIssuesDao.selectByUuids(db.getSession(), uuidsToLoad);

    assertThat(loaded).containsExactlyInAnyOrder(issueDtos.get(0), issueDtos.get(2));
  }

  @Test
  void selectUuidByValue_shouldLoadScaIssue() {
    List<ScaIssueDto> issueDtos = List.of(newScaIssueDto("1"), newScaIssueDto("2"), newScaIssueDto("3"));
    for (var issueDto : issueDtos) {
      scaIssuesDao.insert(db.getSession(), issueDto);
    }

    ScaIssueDto toLoad = issueDtos.get(1);
    var loadedOptionalUuid = scaIssuesDao.selectUuidByValue(db.getSession(), toLoad);

    assertThat(loadedOptionalUuid).contains(toLoad.uuid());
  }
}
