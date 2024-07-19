/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.issue.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition.StigVersion;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.view.index.ViewDoc;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.server.rule.RulesDefinition.OwaspAsvsVersion;
import static org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version.Y2017;
import static org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version.Y2021;
import static org.sonar.api.server.rule.RulesDefinition.PciDssVersion;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.issue.IssueDocTesting.newDocForProject;
import static org.sonar.server.security.SecurityStandards.StigSupportedRequirement.V222391;
import static org.sonar.server.security.SecurityStandards.StigSupportedRequirement.V222397;
import static org.sonar.server.security.SecurityStandards.UNKNOWN_STANDARD;

class IssueIndexSecurityReportsTest extends IssueIndexTestCommon {

  @Test
  void getOwaspTop10Report_dont_count_vulnerabilities_from_other_projects() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto another = newPrivateProjectDto();

    IssueDoc openVulDoc = newDocForProject("openvul1", project).setOwaspTop10(singletonList("a1")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.MAJOR);
    openVulDoc.setOwaspTop10For2021(singletonList("a2")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.MAJOR);

    IssueDoc otherProjectDoc = newDocForProject("anotherProject", another).setOwaspTop10(singletonList("a1")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL);
    otherProjectDoc.setOwaspTop10For2021(singletonList("a2")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL);

    indexIssues(openVulDoc, otherProjectDoc);

    List<SecurityStandardCategoryStatistics> owaspTop10Report = underTest.getOwaspTop10Report(project.uuid(), false, false, Y2017);
    assertThat(owaspTop10Report)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating)
      .contains(
        tuple("a1", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */));

    List<SecurityStandardCategoryStatistics> owaspTop10For2021Report = underTest.getOwaspTop10Report(project.uuid(), false, false, Y2021);
    assertThat(owaspTop10For2021Report)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating)
      .contains(
        tuple("a2", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */));

  }

  @Test
  void getOwaspTop10Report_dont_count_closed_vulnerabilities() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDocForProject("openvul1", project).setOwaspTop10(List.of("a1")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.MAJOR),
      newDocForProject("openvul12021", project).setOwaspTop10For2021(List.of("a2")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.MAJOR),
      newDocForProject("notopenvul", project).setOwaspTop10(List.of("a1")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED)
        .setSeverity(Severity.BLOCKER),
      newDocForProject("notopenvul2021", project).setOwaspTop10For2021(List.of("a2")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED)
        .setSeverity(Severity.BLOCKER));

    List<SecurityStandardCategoryStatistics> owaspTop10Report = underTest.getOwaspTop10Report(project.uuid(), false, false, Y2017);
    assertThat(owaspTop10Report)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating)
      .contains(
        tuple("a1", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */));

    List<SecurityStandardCategoryStatistics> owaspTop10For2021Report = underTest.getOwaspTop10Report(project.uuid(), false, false, Y2021);
    assertThat(owaspTop10For2021Report)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating)
      .contains(
        tuple("a2", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */));
  }

  @Test
  void getOwaspTop10Report_dont_count_old_vulnerabilities() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      // Previous vulnerabilities in projects that are not reanalyzed will have no owasp nor cwe attributes (not even 'unknown')
      newDocForProject("openvulNotReindexed", project).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.MAJOR));

    List<SecurityStandardCategoryStatistics> owaspTop10Report = underTest.getOwaspTop10Report(project.uuid(), false, false, Y2017);
    assertThat(owaspTop10Report)
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating)
      .containsOnly(
        tuple(0L, OptionalInt.empty()));

    List<SecurityStandardCategoryStatistics> owaspTop10For2021Report = underTest.getOwaspTop10Report(project.uuid(), false, false, Y2021);
    assertThat(owaspTop10For2021Report)
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating)
      .containsOnly(
        tuple(0L, OptionalInt.empty()));
  }

  @Test
  void getOwaspTop10Report_dont_count_hotspots_from_other_projects() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto another = newPrivateProjectDto();
    indexIssues(
      newDocForProject("openhotspot1", project).setOwaspTop10(List.of("a1")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("openhotspot2021", project).setOwaspTop10For2021(List.of("a2")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("anotherProject", another).setOwaspTop10(List.of("a1")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("anotherProject2021", another).setOwaspTop10For2021(List.of("a2")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

    List<SecurityStandardCategoryStatistics> owaspTop10Report = underTest.getOwaspTop10Report(project.uuid(), false, false, Y2017);
    assertThat(owaspTop10Report)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots)
      .contains(
        tuple("a1", 1L /* openhotspot1 */));

    List<SecurityStandardCategoryStatistics> owaspTop10For2021Report = underTest.getOwaspTop10Report(project.uuid(), false, false, Y2021);
    assertThat(owaspTop10For2021Report)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots)
      .contains(
        tuple("a2", 1L /* openhotspot1 */));
  }

  @Test
  void getOwaspTop10Report_dont_count_closed_hotspots() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDocForProject("openhotspot1", project).setOwaspTop10(List.of("a1")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("openhotspot2021", project).setOwaspTop10For2021(List.of("a2")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("closedHotspot", project).setOwaspTop10(List.of("a1")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_CLOSED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDocForProject("closedHotspot2021", project).setOwaspTop10For2021(List.of("a2")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_CLOSED)
        .setResolution(Issue.RESOLUTION_FIXED));

    List<SecurityStandardCategoryStatistics> owaspTop10Report = underTest.getOwaspTop10Report(project.uuid(), false, false, Y2017);
    assertThat(owaspTop10Report)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots)
      .contains(
        tuple("a1", 1L /* openhotspot1 */));

    List<SecurityStandardCategoryStatistics> owaspTop10For2021Report = underTest.getOwaspTop10Report(project.uuid(), false, false, Y2021);
    assertThat(owaspTop10For2021Report)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots)
      .contains(
        tuple("a2", 1L /* openhotspot1 */));
  }

  @Test
  void getOwaspTop10Report_aggregation_no_cwe() {
    List<SecurityStandardCategoryStatistics> owaspTop10Report = indexIssuesAndAssertOwaspReport(false);

    assertThat(owaspTop10Report)
      .isNotEmpty()
      .allMatch(category -> category.getChildren().isEmpty());
  }

  @Test
  void getPciDss32Report_aggregation() {
    List<SecurityStandardCategoryStatistics> pciDss32Report = indexIssuesAndAssertPciDss32Report();

    assertThat(pciDss32Report)
      .isNotEmpty();

    assertThat(pciDss32Report.get(0).getChildren()).hasSize(2);
    assertThat(pciDss32Report.get(1).getChildren()).isEmpty();
    assertThat(pciDss32Report.get(2).getChildren()).hasSize(4);
    assertThat(pciDss32Report.get(3).getChildren()).isEmpty();
    assertThat(pciDss32Report.get(4).getChildren()).isEmpty();
    assertThat(pciDss32Report.get(5).getChildren()).hasSize(2);
    assertThat(pciDss32Report.get(6).getChildren()).isEmpty();
    assertThat(pciDss32Report.get(7).getChildren()).hasSize(1);
    assertThat(pciDss32Report.get(8).getChildren()).isEmpty();
    assertThat(pciDss32Report.get(9).getChildren()).hasSize(1);
    assertThat(pciDss32Report.get(10).getChildren()).isEmpty();
    assertThat(pciDss32Report.get(11).getChildren()).isEmpty();
  }

  @Test
  void getOwaspAsvs40Report_aggregation() {
    List<SecurityStandardCategoryStatistics> owaspAsvsReport = indexIssuesAndAssertOwaspAsvsReport();

    assertThat(owaspAsvsReport)
      .isNotEmpty();

    assertThat(owaspAsvsReport.get(0).getChildren()).isEmpty();
    assertThat(owaspAsvsReport.get(1).getChildren()).hasSize(2);
    assertThat(owaspAsvsReport.get(2).getChildren()).hasSize(4);
    assertThat(owaspAsvsReport.get(3).getChildren()).isEmpty();
    assertThat(owaspAsvsReport.get(4).getChildren()).isEmpty();
    assertThat(owaspAsvsReport.get(5).getChildren()).hasSize(2);
    assertThat(owaspAsvsReport.get(6).getChildren()).hasSize(1);
    assertThat(owaspAsvsReport.get(7).getChildren()).hasSize(1);
    assertThat(owaspAsvsReport.get(8).getChildren()).isEmpty();
    assertThat(owaspAsvsReport.get(9).getChildren()).hasSize(1);
    assertThat(owaspAsvsReport.get(10).getChildren()).isEmpty();
    assertThat(owaspAsvsReport.get(11).getChildren()).isEmpty();
    assertThat(owaspAsvsReport.get(12).getChildren()).isEmpty();
    assertThat(owaspAsvsReport.get(13).getChildren()).isEmpty();
  }

  @Test
  void getOwaspAsvs40ReportGroupedByLevel_aggregation() {
    List<SecurityStandardCategoryStatistics> owaspAsvsReportGroupedByLevel = indexIssuesAndAssertOwaspAsvsReportGroupedByLevel();

    assertThat(owaspAsvsReportGroupedByLevel)
      .isNotEmpty();

    assertThat(owaspAsvsReportGroupedByLevel.get(0).getChildren()).hasSize(3);
    assertThat(owaspAsvsReportGroupedByLevel.get(1).getChildren()).hasSize(7);
    assertThat(owaspAsvsReportGroupedByLevel.get(2).getChildren()).hasSize(11);
  }

  @Test
  void getOwaspTop10Report_aggregation_with_cwe() {
    List<SecurityStandardCategoryStatistics> owaspTop10Report = indexIssuesAndAssertOwaspReport(true);

    Map<String, List<SecurityStandardCategoryStatistics>> cweByOwasp = owaspTop10Report.stream()
      .collect(Collectors.toMap(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getChildren));

    assertThat(cweByOwasp.get("a1")).extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple("123", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 0L, 0L, 1),
        tuple("456", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 0L, 0L, 1),
        tuple("unknown", 0L, OptionalInt.empty(), 1L /* openhotspot1 */, 0L, 5));
    assertThat(cweByOwasp.get("a3")).extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple("123", 2L /* openvul1, openvul2 */, OptionalInt.of(3)/* MAJOR = C */, 0L, 0L, 1),
        tuple("456", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 0L, 0L, 1),
        tuple("unknown", 0L, OptionalInt.empty(), 1L /* openhotspot1 */, 0L, 5));
  }

  @Test
  void getOwaspTop10For2021Report_aggregation_with_cwe() {
    List<SecurityStandardCategoryStatistics> owaspTop10Report = indexIssuesAndAssertOwasp2021Report(true);

    Map<String, List<SecurityStandardCategoryStatistics>> cweByOwasp = owaspTop10Report.stream()
      .collect(Collectors.toMap(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getChildren));

    assertThat(cweByOwasp.get("a1")).extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple("123", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 0L, 0L, 1),
        tuple("456", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 0L, 0L, 1),
        tuple("unknown", 0L, OptionalInt.empty(), 1L /* openhotspot1 */, 0L, 5));
    assertThat(cweByOwasp.get("a3")).extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple("123", 2L /* openvul1, openvul2 */, OptionalInt.of(3)/* MAJOR = C */, 0L, 0L, 1),
        tuple("456", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 0L, 0L, 1),
        tuple("unknown", 0L, OptionalInt.empty(), 1L /* openhotspot1 */, 0L, 5));
  }

  private List<SecurityStandardCategoryStatistics> indexIssuesAndAssertOwaspReport(boolean includeCwe) {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDocForProject("openvul1", project).setOwaspTop10(asList("a1", "a3")).setCwe(asList("123", "456")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setOwaspTop10(asList("a3", "a6")).setCwe(List.of("123")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("notowaspvul", project).setOwaspTop10(singletonList(UNKNOWN_STANDARD)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL),
      newDocForProject("toreviewhotspot1", project).setOwaspTop10(asList("a1", "a3")).setCwe(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("toreviewhotspot2", project).setOwaspTop10(asList("a3", "a6")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("reviewedHotspot", project).setOwaspTop10(asList("a3", "a8")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_REVIEWED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDocForProject("notowasphotspot", project).setOwaspTop10(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

    List<SecurityStandardCategoryStatistics> owaspTop10Report = underTest.getOwaspTop10Report(project.uuid(), false, includeCwe, Y2017);
    assertThat(owaspTop10Report)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple("a1", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 1L /* toreviewhotspot1 */, 0L, 5),
        tuple("a2", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("a3", 2L /* openvul1,openvul2 */, OptionalInt.of(3)/* MAJOR = C */, 2L/* toreviewhotspot1,toreviewhotspot2 */, 1L /* reviewedHotspot */, 4),
        tuple("a4", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("a5", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("a6", 1L /* openvul2 */, OptionalInt.of(2) /* MINOR = B */, 1L /* toreviewhotspot2 */, 0L, 5),
        tuple("a7", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("a8", 0L, OptionalInt.empty(), 0L, 1L /* reviewedHotspot */, 1),
        tuple("a9", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("a10", 0L, OptionalInt.empty(), 0L, 0L, 1));
    return owaspTop10Report;
  }

  private List<SecurityStandardCategoryStatistics> indexIssuesAndAssertPciDss32Report() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDocForProject("openvul1", project).setPciDss32(asList("1.2.0", "3.4.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setPciDss32(asList("3.3.2", "6.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("openvul3", project).setPciDss32(asList("10.1.2", "6.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("notpcidssvul", project).setPciDss32(singletonList(UNKNOWN_STANDARD)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL),
      newDocForProject("toreviewhotspot1", project).setPciDss32(asList("1.3.0", "3.3.2")).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("toreviewhotspot2", project).setPciDss32(asList("3.5.6", "6.4.5")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("reviewedHotspot", project).setPciDss32(asList("3.1.1", "8.6")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_REVIEWED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDocForProject("notpcidsshotspot", project).setPciDss32(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

    List<SecurityStandardCategoryStatistics> pciDssReport = underTest.getPciDssReport(project.uuid(), false, PciDssVersion.V3_2).stream()
      .sorted(comparing(s -> parseInt(s.getCategory())))
      .collect(toList());
    assertThat(pciDssReport)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple("1", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 1L /* toreviewhotspot1 */, 0L, 5),
        tuple("2", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("3", 2L /* openvul1,openvul2 */, OptionalInt.of(3)/* MAJOR = C */, 2L/* toreviewhotspot1,toreviewhotspot2 */, 1L /* reviewedHotspot */, 4),
        tuple("4", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("5", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("6", 2L /* openvul2 */, OptionalInt.of(2) /* MINOR = B */, 1L /* toreviewhotspot2 */, 0L, 5),
        tuple("7", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("8", 0L, OptionalInt.empty(), 0L, 1L /* reviewedHotspot */, 1),
        tuple("9", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("10", 1L, OptionalInt.of(2), 0L, 0L, 1),
        tuple("11", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("12", 0L, OptionalInt.empty(), 0L, 0L, 1));

    return pciDssReport;
  }

  private List<SecurityStandardCategoryStatistics> indexIssuesAndAssertOwaspAsvsReport() {
    ComponentDto project = getProjectWithOwaspAsvsIssuesIndexed();

    List<SecurityStandardCategoryStatistics> owaspAsvsReport = underTest.getOwaspAsvsReport(project.uuid(), false, OwaspAsvsVersion.V4_0, 3).stream()
      .sorted(comparing(s -> parseInt(s.getCategory())))
      .collect(toList());
    assertThat(owaspAsvsReport)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple("1", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("2", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 1L /* toreviewhotspot1 */, 0L, 5),
        tuple("3", 2L /* openvul1,openvul2 */, OptionalInt.of(3)/* MAJOR = C */, 2L/* toreviewhotspot1,toreviewhotspot2 */, 1L /* reviewedHotspot */, 4),
        tuple("4", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("5", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("6", 2L /* openvul2 */, OptionalInt.of(2) /* MINOR = B */, 0L, 0L, 1),
        tuple("7", 0L /* openvul2 */, OptionalInt.empty() /* MINOR = B */, 1L /* toreviewhotspot2 */, 0L, 5),
        tuple("8", 0L, OptionalInt.empty(), 0L, 1L /* reviewedHotspot */, 1),
        tuple("9", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("10", 1L, OptionalInt.of(2), 0L, 0L, 1),
        tuple("11", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("12", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("13", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("14", 0L, OptionalInt.empty(), 0L, 0L, 1));

    return owaspAsvsReport;
  }

  private List<SecurityStandardCategoryStatistics> indexIssuesAndAssertOwaspAsvsReportGroupedByLevel() {
    ComponentDto project = getProjectWithOwaspAsvsIssuesIndexed();

    List<SecurityStandardCategoryStatistics> owaspAsvsReportGroupedByLevel = new ArrayList<>();
    owaspAsvsReportGroupedByLevel.addAll(underTest.getOwaspAsvsReportGroupedByLevel(project.uuid(), false, OwaspAsvsVersion.V4_0, 1).stream()
      .sorted(comparing(s -> parseInt(s.getCategory())))
      .toList());
    owaspAsvsReportGroupedByLevel.addAll(underTest.getOwaspAsvsReportGroupedByLevel(project.uuid(), false, OwaspAsvsVersion.V4_0, 2).stream()
      .sorted(comparing(s -> parseInt(s.getCategory())))
      .toList());
    owaspAsvsReportGroupedByLevel.addAll(underTest.getOwaspAsvsReportGroupedByLevel(project.uuid(), false, OwaspAsvsVersion.V4_0, 3).stream()
      .sorted(comparing(s -> parseInt(s.getCategory())))
      .toList());

    assertThat(owaspAsvsReportGroupedByLevel)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple("l1", 1L /* openvul2 */, OptionalInt.of(2), 1L /* toreviewhotspot2 */, 0L, 5),
        tuple("l2", 2L /* openvul1, openvul2 */, OptionalInt.of(3)/* MAJOR = C */, 2L /* toreviewhotspot1, toreviewhotspot2 */, 1L /* reviewedHotspot */, 4),
        tuple("l3", 3L /* openvul1,openvul2,openvul3 */, OptionalInt.of(3)/* MAJOR = C */, 2L/* toreviewhotspot1,toreviewhotspot2 */, 1L /* reviewedHotspot */, 4));

    return owaspAsvsReportGroupedByLevel;
  }

  @NotNull
  private ComponentDto getProjectWithOwaspAsvsIssuesIndexed() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDocForProject("openvul1", project).setOwaspAsvs40(asList("2.4.1", "3.2.4")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setOwaspAsvs40(asList("3.4.5", "6.2.1")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("openvul3", project).setOwaspAsvs40(asList("10.2.4", "6.2.8")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("notowaspasvsvul", project).setOwaspAsvs40(singletonList(UNKNOWN_STANDARD)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL),
      newDocForProject("toreviewhotspot1", project).setOwaspAsvs40(asList("2.2.5", "3.2.4")).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("toreviewhotspot2", project).setOwaspAsvs40(asList("3.6.1", "7.1.1")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("reviewedHotspot", project).setOwaspAsvs40(asList("3.3.3", "8.3.7")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_REVIEWED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDocForProject("notowaspasvshotspot", project).setOwaspAsvs40(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));
    return project;
  }

  private List<SecurityStandardCategoryStatistics> indexIssuesAndAssertOwasp2021Report(boolean includeCwe) {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDocForProject("openvul1", project).setOwaspTop10For2021(asList("a1", "a3")).setCwe(asList("123", "456")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project).setOwaspTop10For2021(asList("a3", "a6")).setCwe(List.of("123")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("notowaspvul", project).setOwaspTop10For2021(singletonList(UNKNOWN_STANDARD)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL),
      newDocForProject("toreviewhotspot1", project).setOwaspTop10For2021(asList("a1", "a3")).setCwe(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("toreviewhotspot2", project).setOwaspTop10For2021(asList("a3", "a6")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("reviewedHotspot", project).setOwaspTop10For2021(asList("a3", "a8")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_REVIEWED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDocForProject("notowasphotspot", project).setOwaspTop10For2021(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

    List<SecurityStandardCategoryStatistics> owaspTop10Report = underTest.getOwaspTop10Report(project.uuid(), false, includeCwe, Y2021);
    assertThat(owaspTop10Report)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple("a1", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 1L /* toreviewhotspot1 */, 0L, 5),
        tuple("a2", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("a3", 2L /* openvul1,openvul2 */, OptionalInt.of(3)/* MAJOR = C */, 2L/* toreviewhotspot1,toreviewhotspot2 */, 1L /* reviewedHotspot */, 4),
        tuple("a4", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("a5", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("a6", 1L /* openvul2 */, OptionalInt.of(2) /* MINOR = B */, 1L /* toreviewhotspot2 */, 0L, 5),
        tuple("a7", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("a8", 0L, OptionalInt.empty(), 0L, 1L /* reviewedHotspot */, 1),
        tuple("a9", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("a10", 0L, OptionalInt.empty(), 0L, 0L, 1));
    return owaspTop10Report;
  }

  @Test
  void getPciDssReport_aggregation_on_portfolio() {
    ComponentDto portfolio1 = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto portfolio2 = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();

    indexIssues(
      newDocForProject("openvul1", project1).setPciDss32(asList("1.2.0", "3.4.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project2).setPciDss32(asList("3.3.2", "6.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("openvul3", project1).setPciDss32(asList("10.1.2", "6.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("notpcidssvul", project1).setPciDss32(singletonList(UNKNOWN_STANDARD)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL),
      newDocForProject("toreviewhotspot1", project2).setPciDss32(asList("1.3.0", "3.3.2")).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("toreviewhotspot2", project1).setPciDss32(asList("3.5.6", "6.4.5")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("reviewedHotspot", project2).setPciDss32(asList("3.1.1", "8.6")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_REVIEWED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDocForProject("notpcidsshotspot", project1).setPciDss32(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

    indexView(portfolio1.uuid(), singletonList(project1.uuid()));
    indexView(portfolio2.uuid(), singletonList(project2.uuid()));

    List<SecurityStandardCategoryStatistics> pciDssReport = underTest.getPciDssReport(portfolio1.uuid(), true, PciDssVersion.V3_2).stream()
      .sorted(comparing(s -> parseInt(s.getCategory())))
      .collect(toList());
    assertThat(pciDssReport)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple("1", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 0L, 0L, 1),
        tuple("2", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("3", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 1L/* toreviewhotspot2 */, 0L, 5),
        tuple("4", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("5", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("6", 1L /* openvul3 */, OptionalInt.of(2) /* MINOR = B */, 1L /* toreviewhotspot2 */, 0L, 5),
        tuple("7", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("8", 0L, OptionalInt.empty(), 0L, 0L /* reviewedHotspot */, 1),
        tuple("9", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("10", 1L /* openvul3 */, OptionalInt.of(2), 0L, 0L, 1),
        tuple("11", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("12", 0L, OptionalInt.empty(), 0L, 0L, 1));
  }

  @Test
  void getOwaspAsvsReport_aggregation_on_portfolio() {
    ComponentDto portfolio1 = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto portfolio2 = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();

    indexIssues(
      newDocForProject("openvul1", project1).setOwaspAsvs40(asList("2.1.1", "3.4.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project2).setOwaspAsvs40(asList("3.3.2", "6.2.1")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("openvul3", project1).setOwaspAsvs40(asList("10.3.2", "6.2.1")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("notowaspasvsvul", project1).setOwaspAsvs40(singletonList(UNKNOWN_STANDARD)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL),
      newDocForProject("toreviewhotspot1", project2).setOwaspAsvs40(asList("2.1.3", "3.3.2")).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("toreviewhotspot2", project1).setOwaspAsvs40(asList("3.4.4", "6.2.1")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("reviewedHotspot", project2).setOwaspAsvs40(asList("3.1.1", "8.3.1")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_REVIEWED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDocForProject("notowaspasvshotspot", project1).setOwaspAsvs40(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

    indexView(portfolio1.uuid(), singletonList(project1.uuid()));
    indexView(portfolio2.uuid(), singletonList(project2.uuid()));

    List<SecurityStandardCategoryStatistics> owaspAsvsReport = underTest.getOwaspAsvsReport(portfolio1.uuid(), true, OwaspAsvsVersion.V4_0, 1).stream()
      .sorted(comparing(s -> parseInt(s.getCategory())))
      .collect(toList());
    assertThat(owaspAsvsReport)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple("1", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("2", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 0L, 0L, 1),
        tuple("3", 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 1L/* toreviewhotspot2 */, 0L, 5),
        tuple("4", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("5", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("6", 1L /* openvul3 */, OptionalInt.of(2) /* MINOR = B */, 1L /* toreviewhotspot2 */, 0L, 5),
        tuple("7", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("8", 0L, OptionalInt.empty(), 0L, 0L /* reviewedHotspot */, 1),
        tuple("9", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("10", 1L /* openvul3 */, OptionalInt.of(2), 0L, 0L, 1),
        tuple("11", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("12", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("13", 0L, OptionalInt.empty(), 0L, 0L, 1),
        tuple("14", 0L, OptionalInt.empty(), 0L, 0L, 1));
  }

  @Test
  void getCWETop25Report_aggregation() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDocForProject("openvul", project).setCwe(List.of("119")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("notopenvul", project).setCwe(List.of("119")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_CLOSED)
        .setResolution(Issue.RESOLUTION_FIXED)
        .setSeverity(Severity.BLOCKER),
      newDocForProject("toreviewhotspot", project).setCwe(List.of("89")).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("only2020", project).setCwe(List.of("862")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("unknown", project).setCwe(List.of("999")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR));

    List<SecurityStandardCategoryStatistics> cweTop25Reports = underTest.getCweTop25Reports(project.uuid(), false);

    List<String> listOfYears = cweTop25Reports.stream()
      .map(SecurityStandardCategoryStatistics::getCategory)
      .collect(toList());

    assertThat(listOfYears).contains("2021", "2022", "2023");

    SecurityStandardCategoryStatistics cwe2021 = cweTop25Reports.stream()
      .filter(s -> s.getCategory().equals("2021"))
      .findAny().get();
    assertThat(cwe2021.getChildren()).hasSize(25);
    assertThat(findRuleInCweByYear(cwe2021, "119")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(1L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2021, "89")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(0L, 1L, 0L);
    assertThat(findRuleInCweByYear(cwe2021, "295")).isNull();
    assertThat(findRuleInCweByYear(cwe2021, "999")).isNull();

    SecurityStandardCategoryStatistics cwe2022 = cweTop25Reports.stream()
      .filter(s -> s.getCategory().equals("2022"))
      .findAny().get();
    assertThat(cwe2022.getChildren()).hasSize(25);
    assertThat(findRuleInCweByYear(cwe2022, "119")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(1L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2022, "89")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(0L, 1L, 0L);
    assertThat(findRuleInCweByYear(cwe2022, "950")).isNull();
    assertThat(findRuleInCweByYear(cwe2022, "999")).isNull();

    SecurityStandardCategoryStatistics cwe2023 = cweTop25Reports.stream()
      .filter(s -> s.getCategory().equals("2023"))
      .findAny().get();
    assertThat(cwe2023.getChildren()).hasSize(25);
    assertThat(findRuleInCweByYear(cwe2023, "119")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(1L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2023, "89")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(0L, 1L, 0L);
    assertThat(findRuleInCweByYear(cwe2023, "862")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(1L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2023, "999")).isNull();
  }

  @Test
  void getCWETop25Report_aggregation_on_portfolio() {
    ComponentDto application = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();

    indexIssues(
      newDocForProject("openvul1", project1).setCwe(List.of("119")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project2).setCwe(List.of("119")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("toreviewhotspot", project1).setCwe(List.of("89")).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("only2020", project2).setCwe(List.of("862")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("unknown", project2).setCwe(List.of("999")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR));

    indexView(application.uuid(), asList(project1.uuid(), project2.uuid()));

    List<SecurityStandardCategoryStatistics> cweTop25Reports = underTest.getCweTop25Reports(application.uuid(), true);

    List<String> listOfYears = cweTop25Reports.stream()
      .map(SecurityStandardCategoryStatistics::getCategory)
      .collect(toList());

    assertThat(listOfYears).contains("2021", "2022", "2023");

    SecurityStandardCategoryStatistics cwe2021 = cweTop25Reports.stream()
      .filter(s -> s.getCategory().equals("2021"))
      .findAny().get();
    assertThat(cwe2021.getChildren()).hasSize(25);
    assertThat(findRuleInCweByYear(cwe2021, "119")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(2L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2021, "89")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(0L, 1L, 0L);
    assertThat(findRuleInCweByYear(cwe2021, "295")).isNull();
    assertThat(findRuleInCweByYear(cwe2021, "999")).isNull();


    SecurityStandardCategoryStatistics cwe2022 = cweTop25Reports.stream()
      .filter(s -> s.getCategory().equals("2022"))
      .findAny().get();
    assertThat(cwe2022.getChildren()).hasSize(25);
    assertThat(findRuleInCweByYear(cwe2022, "119")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(2L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2022, "89")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(0L, 1L, 0L);
    assertThat(findRuleInCweByYear(cwe2022, "295")).isNull();
    assertThat(findRuleInCweByYear(cwe2022, "999")).isNull();

    SecurityStandardCategoryStatistics cwe2023 = cweTop25Reports.stream()
      .filter(s -> s.getCategory().equals("2023"))
      .findAny().get();
    assertThat(cwe2023.getChildren()).hasSize(25);
    assertThat(findRuleInCweByYear(cwe2023, "119")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(2L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2023, "89")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(0L, 1L, 0L);
    assertThat(findRuleInCweByYear(cwe2023, "862")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(1L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2023, "999")).isNull();
  }

  @Test
  void getStigAsdV5R3_whenRequestingReportOnApplication_ShouldAggregateBasedOnStigRequirement() {
    ComponentDto application = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();

    indexIssues(
      newDocForProject("openvul1", project1).setStigAsdV5R3(List.of(V222391.getRequirement())).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", project2).setStigAsdV5R3(List.of(V222391.getRequirement())).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDocForProject("toreviewhotspot", project1).setStigAsdV5R3(List.of(V222397.getRequirement())).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),

      newDocForProject("unknown", project2).setStigAsdV5R3(List.of("V-999999")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR));

    indexView(application.uuid(), asList(project1.uuid(), project2.uuid()));

    Map<String, SecurityStandardCategoryStatistics> statisticsToMap = underTest.getStig(application.uuid(), true, StigVersion.ASD_V5R3)
      .stream().collect(Collectors.toMap(SecurityStandardCategoryStatistics::getCategory, e -> e));

    assertThat(statisticsToMap).hasSize(41)
      .hasEntrySatisfying(V222391.getRequirement(), stat -> {
        assertThat(stat.getVulnerabilities()).isEqualTo(2);
        assertThat(stat.getToReviewSecurityHotspots()).isZero();
        assertThat(stat.getReviewedSecurityHotspots()).isZero();
      })
      .hasEntrySatisfying(V222397.getRequirement(), stat -> {
        assertThat(stat.getVulnerabilities()).isZero();
        assertThat(stat.getToReviewSecurityHotspots()).isEqualTo(1);
        assertThat(stat.getReviewedSecurityHotspots()).isZero();
      });
  }

  @Test
  void getStigAsdV5R3_whenRequestingReportOnProject_ShouldAggregateBasedOnStigRequirement() {
    ComponentDto branch = newPrivateProjectDto();
    indexIssues(
      newDocForProject("openvul", branch).setStigAsdV5R3(List.of(V222391.getRequirement())).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("openvul2", branch).setStigAsdV5R3(List.of(V222391.getRequirement())).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDocForProject("notopenvul", branch).setStigAsdV5R3(List.of(V222391.getRequirement())).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_CLOSED)
        .setResolution(Issue.RESOLUTION_FIXED)
        .setSeverity(Severity.BLOCKER),
      newDocForProject("toreviewhotspot", branch).setStigAsdV5R3(List.of(V222397.getRequirement())).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDocForProject("reviewedHostpot", branch).setStigAsdV5R3(List.of(V222397.getRequirement())).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_REVIEWED).setResolution(Issue.RESOLUTION_FIXED));

    Map<String, SecurityStandardCategoryStatistics> statisticsToMap = underTest.getStig(branch.uuid(), false, StigVersion.ASD_V5R3)
      .stream().collect(Collectors.toMap(SecurityStandardCategoryStatistics::getCategory, e -> e));

    assertThat(statisticsToMap).hasSize(41)
      .hasEntrySatisfying(V222391.getRequirement(), stat -> {
        assertThat(stat.getVulnerabilities()).isEqualTo(2);
        assertThat(stat.getToReviewSecurityHotspots()).isZero();
        assertThat(stat.getReviewedSecurityHotspots()).isZero();
        assertThat(stat.getVulnerabilityRating()).as("MAJOR = C").isEqualTo(OptionalInt.of(3));
      })
      .hasEntrySatisfying(V222397.getRequirement(), stat -> {
        assertThat(stat.getVulnerabilities()).isZero();
        assertThat(stat.getToReviewSecurityHotspots()).isEqualTo(1);
        assertThat(stat.getReviewedSecurityHotspots()).isEqualTo(1);
        assertThat(stat.getSecurityReviewRating()).as("50% of hotspots are reviewed, so rating is C").isEqualTo(3);
      });
  }

  private SecurityStandardCategoryStatistics findRuleInCweByYear(SecurityStandardCategoryStatistics statistics, String cweId) {
    return statistics.getChildren().stream().filter(stat -> stat.getCategory().equals(cweId)).findAny().orElse(null);
  }

  private void indexView(String viewUuid, List<String> projectBranchUuids) {
    viewIndexer.index(new ViewDoc().setUuid(viewUuid).setProjectBranchUuids(projectBranchUuids));
  }

}
