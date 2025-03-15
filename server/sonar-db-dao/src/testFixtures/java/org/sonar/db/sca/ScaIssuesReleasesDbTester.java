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

import org.sonar.db.DbClient;
import org.sonar.db.DbTester;

public class ScaIssuesReleasesDbTester {
  private final DbTester db;
  private final DbClient dbClient;

  public ScaIssuesReleasesDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  public static ScaIssueReleaseDto fromDtos(ScaIssueDto issueDto, ScaReleaseDto releaseDto, String suffix) {
    return new ScaIssueReleaseDto("issueReleaseUuid" + suffix, issueDto.uuid(), releaseDto.uuid(), ScaSeverity.INFO, 89L, 90L);
  }

  public static IssueReleaseData newProhibitedLicenseIssueReleaseDto(String suffix) {
    var issueDto = ScaIssuesDbTester.newProhibitedLicenseScaIssueDto(suffix);
    var releaseDto = ScaReleasesDbTester.newScaReleaseDto(suffix);
    var issueReleaseDto = fromDtos(issueDto, releaseDto, suffix);
    return new IssueReleaseData(issueReleaseDto, issueDto, releaseDto, null);
  }

  public static IssueReleaseData newVulnerabilityIssueReleaseDto(String suffix) {
    var issueDto = ScaIssuesDbTester.newVulnerabilityScaIssueDto(suffix);
    var vulnerabiiltyIssueDto = ScaIssuesDbTester.newVulnerabilityIssueDto(suffix);
    var releaseDto = ScaReleasesDbTester.newScaReleaseDto(suffix);
    var issueReleaseDto = fromDtos(issueDto, releaseDto, suffix);
    return new IssueReleaseData(issueReleaseDto, issueDto, releaseDto, vulnerabiiltyIssueDto);
  }

  public IssueReleaseData insertProhibitedLicenseIssueReleaseDto(String suffix) {
    var data = newProhibitedLicenseIssueReleaseDto(suffix);
    dbClient.scaIssuesDao().insert(db.getSession(), data.issueDto);
    dbClient.scaReleasesDao().insert(db.getSession(), data.releaseDto);
    dbClient.scaIssuesReleasesDao().insert(db.getSession(), data.issueReleaseDto);
    return data;
  }

  public IssueReleaseData insertVulnerabilityIssueReleaseDto(String suffix) {
    var data = newVulnerabilityIssueReleaseDto(suffix);
    dbClient.scaIssuesDao().insert(db.getSession(), data.issueDto);
    dbClient.scaReleasesDao().insert(db.getSession(), data.releaseDto);
    dbClient.scaIssuesReleasesDao().insert(db.getSession(), data.issueReleaseDto);
    dbClient.scaVulnerabilityIssuesDao().insert(db.getSession(), data.vulnerabilityIssueDto);
    return data;
  }

  public record IssueReleaseData(
    ScaIssueReleaseDto issueReleaseDto,
    ScaIssueDto issueDto,
    ScaReleaseDto releaseDto,
    ScaVulnerabilityIssueDto vulnerabilityIssueDto) {
  }
}
