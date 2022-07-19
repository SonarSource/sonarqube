/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.permission.index.IndexPermissions;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewDoc;
import org.sonar.server.view.index.ViewIndexer;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.TimeZone.getTimeZone;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version.Y2017;
import static org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version.Y2021;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.issue.IssueDocTesting.newDoc;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_INSECURE_INTERACTION;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_POROUS_DEFENSES;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_RISKY_RESOURCE;
import static org.sonar.server.security.SecurityStandards.UNKNOWN_STANDARD;

public class IssueIndexSecurityReportsTest {

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  private System2 system2 = new TestSystem2().setNow(1_500_000_000_000L).setDefaultTimeZone(getTimeZone("GMT-01:00"));
  @Rule
  public DbTester db = DbTester.create(system2);

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()), null);
  private ViewIndexer viewIndexer = new ViewIndexer(db.getDbClient(), es.client());
  private PermissionIndexerTester authorizationIndexer = new PermissionIndexerTester(es, issueIndexer);

  private IssueIndex underTest = new IssueIndex(es.client(), system2, userSessionRule, new WebAuthorizationTypeSupport(userSessionRule));

  @Test
  public void getOwaspTop10Report_dont_count_vulnerabilities_from_other_projects() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto another = newPrivateProjectDto();

    IssueDoc openVulDoc = newDoc("openvul1", project).setOwaspTop10(singletonList("a1")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.MAJOR);
    openVulDoc.setOwaspTop10For2021(singletonList("a2")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.MAJOR);

    IssueDoc otherProjectDoc = newDoc("anotherProject", another).setOwaspTop10(singletonList("a1")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL);
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
  public void getOwaspTop10Report_dont_count_closed_vulnerabilities() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDoc("openvul1", project).setOwaspTop10(asList("a1")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.MAJOR),
      newDoc("openvul12021", project).setOwaspTop10For2021(asList("a2")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.MAJOR),
      newDoc("notopenvul", project).setOwaspTop10(asList("a1")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED)
        .setSeverity(Severity.BLOCKER),
      newDoc("notopenvul2021", project).setOwaspTop10For2021(asList("a2")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED)
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
  public void getOwaspTop10Report_dont_count_old_vulnerabilities() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      // Previous vulnerabilities in projects that are not reanalyzed will have no owasp nor cwe attributes (not even 'unknown')
      newDoc("openvulNotReindexed", project).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.MAJOR));

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
  public void getOwaspTop10Report_dont_count_hotspots_from_other_projects() {
    ComponentDto project = newPrivateProjectDto();
    ComponentDto another = newPrivateProjectDto();
    indexIssues(
      newDoc("openhotspot1", project).setOwaspTop10(asList("a1")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("openhotspot2021", project).setOwaspTop10For2021(asList("a2")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("anotherProject", another).setOwaspTop10(asList("a1")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("anotherProject2021", another).setOwaspTop10For2021(asList("a2")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

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
  public void getOwaspTop10Report_dont_count_closed_hotspots() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDoc("openhotspot1", project).setOwaspTop10(asList("a1")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("openhotspot2021", project).setOwaspTop10For2021(asList("a2")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("closedHotspot", project).setOwaspTop10(asList("a1")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_CLOSED)
        .setResolution(Issue.RESOLUTION_FIXED),
    newDoc("closedHotspot2021", project).setOwaspTop10For2021(asList("a2")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_CLOSED)
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
  public void getOwaspTop10Report_aggregation_no_cwe() {
    List<SecurityStandardCategoryStatistics> owaspTop10Report = indexIssuesAndAssertOwaspReport(false);

    assertThat(owaspTop10Report)
      .isNotEmpty()
      .allMatch(category -> category.getChildren().isEmpty());
  }

  @Test
  public void getPciDss32Report_aggregation() {
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
  public void getOwaspTop10Report_aggregation_with_cwe() {
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
  public void getOwaspTop10For2021Report_aggregation_with_cwe() {
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
      newDoc("openvul1", project).setOwaspTop10(asList("a1", "a3")).setCwe(asList("123", "456")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDoc("openvul2", project).setOwaspTop10(asList("a3", "a6")).setCwe(asList("123")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDoc("notowaspvul", project).setOwaspTop10(singletonList(UNKNOWN_STANDARD)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL),
      newDoc("toreviewhotspot1", project).setOwaspTop10(asList("a1", "a3")).setCwe(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("toreviewhotspot2", project).setOwaspTop10(asList("a3", "a6")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("reviewedHotspot", project).setOwaspTop10(asList("a3", "a8")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_REVIEWED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDoc("notowasphotspot", project).setOwaspTop10(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

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
      newDoc("openvul1", project).setPciDss32(asList("1.2.0", "3.4.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDoc("openvul2", project).setPciDss32(asList("3.3.2", "6.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDoc("openvul3", project).setPciDss32(asList("10.1.2", "6.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDoc("notpcidssvul", project).setPciDss32(singletonList(UNKNOWN_STANDARD)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL),
      newDoc("toreviewhotspot1", project).setPciDss32(asList("1.3.0", "3.3.2")).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("toreviewhotspot2", project).setPciDss32(asList("3.5.6", "6.4.5")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("reviewedHotspot", project).setPciDss32(asList("3.1.1", "8.6")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_REVIEWED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDoc("notpcidsshotspot", project).setPciDss32(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

    List<SecurityStandardCategoryStatistics> pciDssReport = underTest.getPciDssReport(project.uuid(), false, RulesDefinition.PciDssVersion.V3_2).stream()
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

  private List<SecurityStandardCategoryStatistics> indexIssuesAndAssertOwasp2021Report(boolean includeCwe) {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDoc("openvul1", project).setOwaspTop10For2021(asList("a1", "a3")).setCwe(asList("123", "456")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDoc("openvul2", project).setOwaspTop10For2021(asList("a3", "a6")).setCwe(asList("123")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDoc("notowaspvul", project).setOwaspTop10For2021(singletonList(UNKNOWN_STANDARD)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL),
      newDoc("toreviewhotspot1", project).setOwaspTop10For2021(asList("a1", "a3")).setCwe(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("toreviewhotspot2", project).setOwaspTop10For2021(asList("a3", "a6")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("reviewedHotspot", project).setOwaspTop10For2021(asList("a3", "a8")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_REVIEWED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDoc("notowasphotspot", project).setOwaspTop10For2021(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

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
  public void getSansTop25Report_aggregation() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDoc("openvul1", project).setSansTop25(asList(SANS_TOP_25_INSECURE_INTERACTION, SANS_TOP_25_RISKY_RESOURCE)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDoc("openvul2", project).setSansTop25(asList(SANS_TOP_25_RISKY_RESOURCE, SANS_TOP_25_POROUS_DEFENSES)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDoc("notopenvul", project).setSansTop25(asList(SANS_TOP_25_RISKY_RESOURCE, SANS_TOP_25_POROUS_DEFENSES)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_CLOSED)
        .setResolution(Issue.RESOLUTION_FIXED)
        .setSeverity(Severity.BLOCKER),
      newDoc("notsansvul", project).setSansTop25(singletonList(UNKNOWN_STANDARD)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL),
      newDoc("toreviewhotspot1", project).setSansTop25(asList(SANS_TOP_25_INSECURE_INTERACTION, SANS_TOP_25_RISKY_RESOURCE)).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("toreviewhotspot2", project).setSansTop25(asList(SANS_TOP_25_RISKY_RESOURCE, SANS_TOP_25_POROUS_DEFENSES)).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("inReviewHotspot", project).setSansTop25(asList(SANS_TOP_25_RISKY_RESOURCE)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_IN_REVIEW),
      newDoc("reviewedHotspot", project).setSansTop25(asList(SANS_TOP_25_RISKY_RESOURCE)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_REVIEWED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDoc("notowasphotspot", project).setSansTop25(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

    List<SecurityStandardCategoryStatistics> sansTop25Report = underTest.getSansTop25Report(project.uuid(), false, false);
    assertThat(sansTop25Report)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple(SANS_TOP_25_INSECURE_INTERACTION, 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 1L /* toreviewhotspot1 */, 0L, 5),
        tuple(SANS_TOP_25_RISKY_RESOURCE, 2L /* openvul1,openvul2 */, OptionalInt.of(3)/* MAJOR = C */, 2L/* toreviewhotspot1,toreviewhotspot2 */,
          1L /* reviewedHotspot */, 4),
        tuple(SANS_TOP_25_POROUS_DEFENSES, 1L /* openvul2 */, OptionalInt.of(2)/* MINOR = B */, 1L/* openhotspot2 */, 0L, 5));

    assertThat(sansTop25Report).allMatch(category -> category.getChildren().isEmpty());
  }

  @Test
  public void getSansTop25Report_aggregation_on_portfolio() {
    ComponentDto portfolio1 = db.components().insertPrivateApplication();
    ComponentDto portfolio2 = db.components().insertPrivateApplication();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();

    indexIssues(
      newDoc("openvul1", project1).setSansTop25(asList(SANS_TOP_25_INSECURE_INTERACTION, SANS_TOP_25_RISKY_RESOURCE)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDoc("openvul2", project2).setSansTop25(asList(SANS_TOP_25_RISKY_RESOURCE, SANS_TOP_25_POROUS_DEFENSES)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDoc("notopenvul", project1).setSansTop25(asList(SANS_TOP_25_RISKY_RESOURCE, SANS_TOP_25_POROUS_DEFENSES)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_CLOSED)
        .setResolution(Issue.RESOLUTION_FIXED)
        .setSeverity(Severity.BLOCKER),
      newDoc("notsansvul", project2).setSansTop25(singletonList(UNKNOWN_STANDARD)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL),
      newDoc("toreviewhotspot1", project1).setSansTop25(asList(SANS_TOP_25_INSECURE_INTERACTION, SANS_TOP_25_RISKY_RESOURCE)).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("toreviewhotspot2", project2).setSansTop25(asList(SANS_TOP_25_RISKY_RESOURCE, SANS_TOP_25_POROUS_DEFENSES)).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("reviewedHotspot", project2).setSansTop25(asList(SANS_TOP_25_RISKY_RESOURCE)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_REVIEWED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDoc("notowasphotspot", project1).setSansTop25(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

    indexView(portfolio1.uuid(), singletonList(project1.uuid()));
    indexView(portfolio2.uuid(), singletonList(project2.uuid()));

    List<SecurityStandardCategoryStatistics> sansTop25Report = underTest.getSansTop25Report(portfolio1.uuid(), true, false);
    assertThat(sansTop25Report)
      .extracting(SecurityStandardCategoryStatistics::getCategory, SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getVulnerabilityRating, SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots, SecurityStandardCategoryStatistics::getSecurityReviewRating)
      .containsExactlyInAnyOrder(
        tuple(SANS_TOP_25_INSECURE_INTERACTION, 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 1L /* toreviewhotspot1 */, 0L, 5),
        tuple(SANS_TOP_25_RISKY_RESOURCE, 1L /* openvul1 */, OptionalInt.of(3)/* MAJOR = C */, 1L/* toreviewhotspot1 */, 0L, 5),
        tuple(SANS_TOP_25_POROUS_DEFENSES, 0L, OptionalInt.empty(), 0L, 0L, 1));

    assertThat(sansTop25Report).allMatch(category -> category.getChildren().isEmpty());
  }

  @Test
  public void getPciDssReport_aggregation_on_portfolio() {
    ComponentDto portfolio1 = db.components().insertPrivateApplication();
    ComponentDto portfolio2 = db.components().insertPrivateApplication();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();

    indexIssues(
      newDoc("openvul1", project1).setPciDss32(asList("1.2.0", "3.4.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDoc("openvul2", project2).setPciDss32(asList("3.3.2", "6.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDoc("openvul3", project1).setPciDss32(asList("10.1.2", "6.5")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDoc("notpcidssvul", project1).setPciDss32(singletonList(UNKNOWN_STANDARD)).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN).setSeverity(Severity.CRITICAL),
      newDoc("toreviewhotspot1", project2).setPciDss32(asList("1.3.0", "3.3.2")).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("toreviewhotspot2", project1).setPciDss32(asList("3.5.6", "6.4.5")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("reviewedHotspot", project2).setPciDss32(asList("3.1.1", "8.6")).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_REVIEWED)
        .setResolution(Issue.RESOLUTION_FIXED),
      newDoc("notpcidsshotspot", project1).setPciDss32(singletonList(UNKNOWN_STANDARD)).setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW));

    indexView(portfolio1.uuid(), singletonList(project1.uuid()));
    indexView(portfolio2.uuid(), singletonList(project2.uuid()));

    List<SecurityStandardCategoryStatistics> pciDssReport = underTest.getPciDssReport(portfolio1.uuid(), true, RulesDefinition.PciDssVersion.V3_2).stream()
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
  public void getCWETop25Report_aggregation() {
    ComponentDto project = newPrivateProjectDto();
    indexIssues(
      newDoc("openvul", project).setCwe(asList("119")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDoc("notopenvul", project).setCwe(asList("119")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_CLOSED)
        .setResolution(Issue.RESOLUTION_FIXED)
        .setSeverity(Severity.BLOCKER),
      newDoc("toreviewhotspot", project).setCwe(asList("89")).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("only2020", project).setCwe(asList("862")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDoc("unknown", project).setCwe(asList("999")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR));

    List<SecurityStandardCategoryStatistics> cweTop25Reports = underTest.getCweTop25Reports(project.uuid(), false);

    List<String> listOfYears = cweTop25Reports.stream()
      .map(SecurityStandardCategoryStatistics::getCategory)
      .collect(toList());

    assertThat(listOfYears).contains("2019", "2020", "2021");

    SecurityStandardCategoryStatistics cwe2019 = cweTop25Reports.stream()
      .filter(s -> s.getCategory().equals("2019"))
      .findAny().get();
    assertThat(cwe2019.getChildren()).hasSize(25);
    assertThat(findRuleInCweByYear(cwe2019, "119")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(1L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2019, "89")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(0L, 1L, 0L);
    assertThat(findRuleInCweByYear(cwe2019, "862")).isNull();
    assertThat(findRuleInCweByYear(cwe2019, "999")).isNull();

    SecurityStandardCategoryStatistics cwe2020 = cweTop25Reports.stream()
      .filter(s -> s.getCategory().equals("2020"))
      .findAny().get();
    assertThat(cwe2020.getChildren()).hasSize(25);
    assertThat(findRuleInCweByYear(cwe2020, "119")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(1L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2020, "89")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(0L, 1L, 0L);
    assertThat(findRuleInCweByYear(cwe2020, "862")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(1L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2020, "999")).isNull();

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
  }

  @Test
  public void getCWETop25Report_aggregation_on_portfolio() {
    ComponentDto application = db.components().insertPrivateApplication();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();

    indexIssues(
      newDoc("openvul1", project1).setCwe(asList("119")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_OPEN)
        .setSeverity(Severity.MAJOR),
      newDoc("openvul2", project2).setCwe(asList("119")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDoc("toreviewhotspot", project1).setCwe(asList("89")).setType(RuleType.SECURITY_HOTSPOT)
        .setStatus(Issue.STATUS_TO_REVIEW),
      newDoc("only2020", project2).setCwe(asList("862")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR),
      newDoc("unknown", project2).setCwe(asList("999")).setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_REOPENED)
        .setSeverity(Severity.MINOR));

    indexView(application.uuid(), asList(project1.uuid(), project2.uuid()));

    List<SecurityStandardCategoryStatistics> cweTop25Reports = underTest.getCweTop25Reports(application.uuid(), true);

    List<String> listOfYears = cweTop25Reports.stream()
      .map(SecurityStandardCategoryStatistics::getCategory)
      .collect(toList());

    assertThat(listOfYears).contains("2019", "2020", "2021");

    SecurityStandardCategoryStatistics cwe2019 = cweTop25Reports.stream()
      .filter(s -> s.getCategory().equals("2019"))
      .findAny().get();
    assertThat(cwe2019.getChildren()).hasSize(25);
    assertThat(findRuleInCweByYear(cwe2019, "119")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(2L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2019, "89")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(0L, 1L, 0L);
    assertThat(findRuleInCweByYear(cwe2019, "862")).isNull();
    assertThat(findRuleInCweByYear(cwe2019, "999")).isNull();

    SecurityStandardCategoryStatistics cwe2020 = cweTop25Reports.stream()
      .filter(s -> s.getCategory().equals("2020"))
      .findAny().get();
    assertThat(cwe2020.getChildren()).hasSize(25);
    assertThat(findRuleInCweByYear(cwe2020, "119")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(2L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2020, "89")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(0L, 1L, 0L);
    assertThat(findRuleInCweByYear(cwe2020, "862")).isNotNull()
      .extracting(SecurityStandardCategoryStatistics::getVulnerabilities,
        SecurityStandardCategoryStatistics::getToReviewSecurityHotspots,
        SecurityStandardCategoryStatistics::getReviewedSecurityHotspots)
      .containsExactlyInAnyOrder(1L, 0L, 0L);
    assertThat(findRuleInCweByYear(cwe2020, "999")).isNull();

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
  }

  private SecurityStandardCategoryStatistics findRuleInCweByYear(SecurityStandardCategoryStatistics statistics, String cweId) {
    return statistics.getChildren().stream().filter(stat -> stat.getCategory().equals(cweId)).findAny().orElse(null);
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(asList(issues).iterator());
    authorizationIndexer.allow(stream(issues).map(issue -> new IndexPermissions(issue.projectUuid(), PROJECT).allowAnyone()).collect(toList()));
  }

  private void indexView(String viewUuid, List<String> projects) {
    viewIndexer.index(new ViewDoc().setUuid(viewUuid).setProjects(projects));
  }

}
