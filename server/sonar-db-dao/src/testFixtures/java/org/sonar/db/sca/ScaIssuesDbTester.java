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
import java.util.Map;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;

public class ScaIssuesDbTester {
  private static final Map<String, String> SAMPLE_LICENSES_BY_SUFFIX = Map.of(
    "1", "MIT",
    "2", "0BSD",
    "3", "GPL-3.0",
    "4", "Apache-2.0",
    "5", "BSD-3-Clause");

  private final DbTester db;
  private final DbClient dbClient;

  public ScaIssuesDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
  }

  public static ScaVulnerabilityIssueDto newVulnerabilityIssueDto(String suffix) {
    return new ScaVulnerabilityIssueDto("sca-issue-uuid" + suffix, ScaSeverity.INFO, List.of("cwe" + suffix), new BigDecimal("7.1"), 1L, 2L);
  }

  public static ScaIssueDto newVulnerabilityScaIssueDto(String suffix) {
    return new ScaIssueDto("sca-issue-uuid" + suffix, ScaIssueType.VULNERABILITY, "fakePackageUrl" + suffix, "fakeVulnerabilityId" + suffix, ScaIssueDto.NULL_VALUE, 1L,
      2L);
  }

  public static ScaIssueDto newProhibitedLicenseScaIssueDto(String suffix) {
    return new ScaIssueDto("sca-issue-uuid" + suffix, ScaIssueType.PROHIBITED_LICENSE, ScaIssueDto.NULL_VALUE, ScaIssueDto.NULL_VALUE,
      SAMPLE_LICENSES_BY_SUFFIX.getOrDefault(suffix, "GPL-3.0-only"), 1L, 2L);
  }

  Map.Entry<ScaIssueDto, ScaVulnerabilityIssueDto> insertVulnerabilityIssue(String suffix) {
    var scaIssueDto = newVulnerabilityScaIssueDto(suffix);
    var vulnerabilityIssueDto = newVulnerabilityIssueDto(suffix);

    dbClient.scaIssuesDao().insert(db.getSession(), scaIssueDto);
    dbClient.scaVulnerabilityIssuesDao().insert(db.getSession(), vulnerabilityIssueDto);
    return Map.entry(scaIssueDto, vulnerabilityIssueDto);
  }

  ScaIssueDto insertProhibitedLicenseIssue(String suffix) {
    var scaIssueDto = newProhibitedLicenseScaIssueDto(suffix);
    dbClient.scaIssuesDao().insert(db.getSession(), scaIssueDto);
    return scaIssueDto;
  }
}
