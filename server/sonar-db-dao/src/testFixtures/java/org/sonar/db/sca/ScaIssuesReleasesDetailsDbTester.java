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

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;

public class ScaIssuesReleasesDetailsDbTester {
  private final DbTester db;
  private final DbClient dbClient;

  public ScaIssuesReleasesDetailsDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  public ScaIssueReleaseDetailsDto fromDtos(@Nullable String projectUuid, ScaIssueReleaseDto scaIssueReleaseDto, ScaIssueDto scaIssueDto,
    Optional<ScaVulnerabilityIssueDto> scaVulnerabilityIssueDto) {
    // this should emulate what the mapper does when joining these tables
    return new ScaIssueReleaseDetailsDto(scaIssueReleaseDto.uuid(), ScaSeverity.INFO,
      scaIssueReleaseDto.scaIssueUuid(), scaIssueReleaseDto.scaReleaseUuid(), scaIssueDto.scaIssueType(),
      scaIssueDto.packageUrl(), scaIssueDto.vulnerabilityId(), scaIssueDto.spdxLicenseId(),
      scaVulnerabilityIssueDto.map(ScaVulnerabilityIssueDto::baseSeverity).orElse(null),
      scaVulnerabilityIssueDto.map(ScaVulnerabilityIssueDto::cweIds).orElse(null),
      scaVulnerabilityIssueDto.map(ScaVulnerabilityIssueDto::cvssScore).orElse(null));
  }

  private ScaIssueReleaseDetailsDto insertIssue(ScaIssueDto scaIssue, String suffix, String componentUuid, @Nullable String projectUuid) {
    // insertScaRelease has suffix and componentUuid swapped vs. our own method...
    var scaRelease = db.getScaReleasesDbTester().insertScaRelease(componentUuid, suffix);
    var scaIssueRelease = new ScaIssueReleaseDto("sca-issue-release-uuid-" + suffix, scaIssue, scaRelease, ScaSeverity.INFO, 1L, 2L);
    dbClient.scaIssuesReleasesDao().insert(db.getSession(), scaIssueRelease);
    return fromDtos(projectUuid, scaIssueRelease, scaIssue, Optional.empty());
  }

  public ScaIssueReleaseDetailsDto insertVulnerabilityIssue(String suffix, String componentUuid) {
    var scaIssue = db.getScaIssuesDbTester().insertVulnerabilityIssue(suffix).getKey();
    return insertIssue(scaIssue, suffix, componentUuid, null);
  }

  public ScaIssueReleaseDetailsDto insertProhibitedLicenseIssue(String suffix, String componentUuid) {
    var scaIssue = db.getScaIssuesDbTester().insertProhibitedLicenseIssue(suffix);
    return insertIssue(scaIssue, suffix, componentUuid, null);
  }

  public ScaIssueReleaseDetailsDto insertIssue(ScaIssueType scaIssueType, String suffix, String componentUuid, @Nullable String projectUuid) {
    var scaIssue = switch (scaIssueType) {
      case VULNERABILITY -> db.getScaIssuesDbTester().insertVulnerabilityIssue(suffix).getKey();
      case PROHIBITED_LICENSE -> db.getScaIssuesDbTester().insertProhibitedLicenseIssue(suffix);
    };
    return insertIssue(scaIssue, suffix, componentUuid, projectUuid);
  }
}
