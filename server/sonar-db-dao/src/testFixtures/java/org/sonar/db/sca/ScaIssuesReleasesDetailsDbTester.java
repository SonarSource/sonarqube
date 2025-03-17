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
import java.util.function.Function;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class ScaIssuesReleasesDetailsDbTester {
  private final DbTester db;
  private final DbClient dbClient;

  public ScaIssuesReleasesDetailsDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  public static ScaIssueReleaseDetailsDto fromIssueReleaseData(ScaIssuesReleasesDbTester.IssueReleaseData issueReleaseData) {
    return new ScaIssueReleaseDetailsDto(
      issueReleaseData.issueReleaseDto().uuid(),
      issueReleaseData.issueReleaseDto(),
      issueReleaseData.issueDto(),
      issueReleaseData.releaseDto(),
      issueReleaseData.vulnerabilityIssueDto());
  }

  public static ScaIssueReleaseDetailsDto fromDtos(ScaIssueReleaseDto issueReleaseDto, ScaIssueDto issueDto,
    Optional<ScaVulnerabilityIssueDto> vulnerabilityIssueDtoOptional, ScaReleaseDto releaseDto) {
    return new ScaIssueReleaseDetailsDto(issueReleaseDto.uuid(), issueReleaseDto,
      issueDto, releaseDto, vulnerabilityIssueDtoOptional.orElse(null));
  }

  private ScaIssueReleaseDetailsDto insertIssue(ScaIssueDto scaIssue, Optional<ScaVulnerabilityIssueDto> scaVulnerabilityIssueDtoOptional,
    String suffix, String componentUuid) {
    // insertScaRelease has suffix and componentUuid swapped vs. our own method...
    var scaRelease = db.getScaReleasesDbTester().insertScaRelease(componentUuid, suffix);
    var scaIssueRelease = new ScaIssueReleaseDto("sca-issue-release-uuid-" + suffix, scaIssue, scaRelease, ScaSeverity.INFO, 1L, 2L);
    dbClient.scaIssuesReleasesDao().insert(db.getSession(), scaIssueRelease);
    return fromDtos(scaIssueRelease, scaIssue, scaVulnerabilityIssueDtoOptional, scaRelease);
  }

  public ScaIssueReleaseDetailsDto insertVulnerabilityIssue(String suffix, String componentUuid) {
    var entry = db.getScaIssuesDbTester().insertVulnerabilityIssue(suffix);
    return insertIssue(entry.getKey(), Optional.of(entry.getValue()), suffix, componentUuid);
  }

  public ScaIssueReleaseDetailsDto insertProhibitedLicenseIssue(String suffix, String componentUuid) {
    var scaIssue = db.getScaIssuesDbTester().insertProhibitedLicenseIssue(suffix);
    return insertIssue(scaIssue, Optional.empty(), suffix, componentUuid);
  }

  public ScaIssueReleaseDetailsDto insertIssue(ScaIssueType scaIssueType, String suffix, String componentUuid) {
    return insertIssue(scaIssueType, suffix, componentUuid,
      null, null, null, null);
  }

  public ScaIssueReleaseDetailsDto insertIssue(ScaIssueType scaIssueType, String suffix, String componentUuid,
    @Nullable Function<ScaIssueDto, ScaIssueDto> scaIssueModifier,
    @Nullable Function<ScaVulnerabilityIssueDto, ScaVulnerabilityIssueDto> scaVulnerabilityIssueModifier,
    @Nullable Function<ScaReleaseDto, ScaReleaseDto> scaReleaseModifier,
    @Nullable Function<ScaIssueReleaseDto, ScaIssueReleaseDto> scaIssueReleaseModifier) {
    var scaRelease = ScaReleasesDbTester.newScaReleaseDto(componentUuid, suffix, PackageManager.MAVEN, "packageName" + suffix);
    if (scaReleaseModifier != null) {
      scaRelease = scaReleaseModifier.apply(scaRelease);
    }
    // little hack here because it's useful to allow providing a release that already exists
    var existing = dbClient.scaReleasesDao().selectByUuid(db.getSession(), scaRelease.uuid());
    if (existing.isEmpty()) {
      dbClient.scaReleasesDao().insert(db.getSession(), scaRelease);
    } else {
      assertThat(existing).contains(scaRelease);
    }
    var scaIssue = switch (scaIssueType) {
      case PROHIBITED_LICENSE -> ScaIssuesDbTester.newProhibitedLicenseScaIssueDto(suffix);
      case VULNERABILITY -> ScaIssuesDbTester.newVulnerabilityScaIssueDto(suffix);
    };
    if (scaIssueModifier != null) {
      scaIssue = scaIssueModifier.apply(scaIssue);
    }
    dbClient.scaIssuesDao().insert(db.getSession(), scaIssue);
    ScaVulnerabilityIssueDto scaVulnerabilityIssue = null;
    if (scaIssue.scaIssueType() == ScaIssueType.VULNERABILITY) {
      scaVulnerabilityIssue = ScaIssuesDbTester.newVulnerabilityIssueDto(suffix);
      if (!scaVulnerabilityIssue.uuid().equals(scaIssue.uuid())) {
        throw new IllegalStateException("ScaVulnerabilityIssueDto.uuid must match ScaIssueDto.uuid or we won't find the ScaVueberabilityIssueDto");
      }
      if (scaVulnerabilityIssueModifier != null) {
        scaVulnerabilityIssue = scaVulnerabilityIssueModifier.apply(scaVulnerabilityIssue);
      }
      dbClient.scaVulnerabilityIssuesDao().insert(db.getSession(), scaVulnerabilityIssue);
    }
    var scaIssueRelease = new ScaIssueReleaseDto("sca-issue-release-uuid-" + suffix, scaIssue, scaRelease, ScaSeverity.INFO, 1L, 2L);
    if (scaIssueReleaseModifier != null) {
      scaIssueRelease = scaIssueReleaseModifier.apply(scaIssueRelease);
    }
    dbClient.scaIssuesReleasesDao().insert(db.getSession(), scaIssueRelease);
    return fromDtos(scaIssueRelease, scaIssue, Optional.ofNullable(scaVulnerabilityIssue), scaRelease);
  }
}
