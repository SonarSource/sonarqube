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

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;

import static org.assertj.core.api.Assertions.assertThat;

class ScaIssuesReleasesDetailsDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final ScaIssuesReleasesDetailsDao scaIssuesReleasesDetailsDao = db.getDbClient().scaIssuesReleasesDetailsDao();

  @Test
  void selectByBranchUuid_shouldReturnIssues() {
    var projectData = db.components().insertPrivateProject();
    var componentDto = projectData.getMainBranchComponent();
    var issue1 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.VULNERABILITY, "1", componentDto.uuid(), projectData.projectUuid());
    var issue2 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.PROHIBITED_LICENSE, "2", componentDto.uuid(), projectData.projectUuid());

    var foundPage = scaIssuesReleasesDetailsDao.selectByBranchUuid(db.getSession(), componentDto.branchUuid(), Pagination.forPage(1).andSize(1));

    ScaIssueReleaseDetailsDto expected1 = new ScaIssueReleaseDetailsDto(
      issue1.scaIssueReleaseUuid(),
      issue1.severity(),
      issue1.scaIssueUuid(),
      issue1.scaReleaseUuid(),
      ScaIssueType.VULNERABILITY,
      "fakePackageUrl1",
      "fakeVulnerabilityId1",
      ScaIssueDto.NULL_VALUE,
      ScaSeverity.INFO,
      List.of("cwe1"),
      new BigDecimal("7.1"));
    ScaIssueReleaseDetailsDto expected2 = new ScaIssueReleaseDetailsDto(
      issue2.scaIssueReleaseUuid(),
      issue2.severity(),
      issue2.scaIssueUuid(),
      issue2.scaReleaseUuid(),
      ScaIssueType.PROHIBITED_LICENSE,
      ScaIssueDto.NULL_VALUE,
      ScaIssueDto.NULL_VALUE,
      "0BSD",
      null,
      null,
      null);

    assertThat(foundPage).hasSize(1).isSubsetOf(expected1, expected2);
    var foundAllIssues = scaIssuesReleasesDetailsDao.selectByBranchUuid(db.getSession(), componentDto.branchUuid(), Pagination.forPage(1).andSize(10));
    assertThat(foundAllIssues).hasSize(2).containsExactlyInAnyOrder(expected1, expected2);
  }

  @Test
  void countByBranchUuid_shouldCountIssues() {
    var componentDto = db.components().insertPrivateProject().getMainBranchComponent();
    db.getScaIssuesReleasesDetailsDbTester().insertVulnerabilityIssue("1", componentDto.uuid());
    db.getScaIssuesReleasesDetailsDbTester().insertVulnerabilityIssue("2", componentDto.uuid());
    db.getScaIssuesReleasesDetailsDbTester().insertVulnerabilityIssue("3", componentDto.uuid());

    var count1 = scaIssuesReleasesDetailsDao.countByBranchUuid(db.getSession(), componentDto.branchUuid());
    assertThat(count1).isEqualTo(3);

    assertThat(scaIssuesReleasesDetailsDao.countByBranchUuid(db.getSession(), "bogus-branch-uuid")).isZero();
  }
}
