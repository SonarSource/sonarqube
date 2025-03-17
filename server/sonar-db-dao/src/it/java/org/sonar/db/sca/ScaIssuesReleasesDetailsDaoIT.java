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

import com.google.common.collect.Lists;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;

import static org.assertj.core.api.Assertions.assertThat;

class ScaIssuesReleasesDetailsDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final ScaIssuesReleasesDetailsDao scaIssuesReleasesDetailsDao = db.getDbClient().scaIssuesReleasesDetailsDao();

  private static Comparator<ScaIssueReleaseDetailsDto> identityComparator() {
    Function<ScaIssueReleaseDetailsDto, String> typeString = dto -> dto.scaIssueType().name();
    return Comparator.comparing(typeString)
      .thenComparing(ScaIssueReleaseDetailsDto::vulnerabilityId)
      .thenComparing(ScaIssueReleaseDetailsDto::issuePackageUrl)
      .thenComparing(ScaIssueReleaseDetailsDto::spdxLicenseId)
      .thenComparing(ScaIssueReleaseDetailsDto::issueReleaseUuid);
  }

  private static Comparator<ScaIssueReleaseDetailsDto> severityComparator() {
    return Comparator.comparing(dto -> dto.severity().databaseSortKey());
  }

  private static Comparator<ScaIssueReleaseDetailsDto> cvssScoreComparator() {
    return Comparator.comparing(ScaIssueReleaseDetailsDto::cvssScore,
      // we treat null cvss as a score of 0.0
      Comparator.nullsFirst(Comparator.naturalOrder()));
  }

  private static Comparator<ScaIssueReleaseDetailsDto> comparator(ScaIssuesReleasesDetailsQuery.Sort sort) {
    return switch (sort) {
      case IDENTITY_ASC -> identityComparator();
      case IDENTITY_DESC -> identityComparator().reversed();
      case SEVERITY_ASC -> severityComparator()
        .thenComparing(cvssScoreComparator())
        .thenComparing(identityComparator());
      case SEVERITY_DESC -> severityComparator().reversed()
        .thenComparing(cvssScoreComparator().reversed())
        .thenComparing(identityComparator());
      case CVSS_SCORE_ASC -> cvssScoreComparator()
        .thenComparing(ScaIssueReleaseDetailsDto::severity)
        .thenComparing(identityComparator());
      case CVSS_SCORE_DESC -> cvssScoreComparator().reversed()
        .thenComparing(Comparator.comparing(ScaIssueReleaseDetailsDto::severity).reversed())
        .thenComparing(identityComparator());
    };
  }

  @Test
  void selectByBranchUuid_shouldReturnIssues() {
    var projectData = db.components().insertPrivateProject();
    var componentDto = projectData.getMainBranchComponent();
    var issue1 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.VULNERABILITY, "1", componentDto.uuid());
    var issue2 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.PROHIBITED_LICENSE, "2", componentDto.uuid());

    var foundPage = scaIssuesReleasesDetailsDao.selectByBranchUuid(db.getSession(), componentDto.branchUuid(), Pagination.forPage(1).andSize(1));

    assertThat(foundPage).hasSize(1).isSubsetOf(issue1, issue2);
    var foundAllIssues = scaIssuesReleasesDetailsDao.selectByBranchUuid(db.getSession(), componentDto.branchUuid(), Pagination.forPage(1).andSize(10));
    assertThat(foundAllIssues).hasSize(2).containsExactlyElementsOf(Stream.of(issue1, issue2).sorted(comparator(ScaIssuesReleasesDetailsQuery.Sort.SEVERITY_DESC)).toList());
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

  @Test
  void selectByReleaseUuid_shouldReturnIssues() {
    var projectData = db.components().insertPrivateProject();
    var componentDto = projectData.getMainBranchComponent();
    var issue1 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.VULNERABILITY, "1", componentDto.uuid());
    var release1 = issue1.releaseDto();
    // make these other issues use the same release and have a variety of CVSS
    var issue2 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.VULNERABILITY, "2", componentDto.uuid(),
      null, vi -> vi.toBuilder().setCvssScore(new BigDecimal("1.1")).build(),
      releaseDto -> release1,
      issueReleaseDto -> issueReleaseDto.toBuilder().setScaReleaseUuid(release1.uuid()).build());
    var issue3 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.VULNERABILITY, "3", componentDto.uuid(),
      null, vi -> vi.toBuilder().setCvssScore(new BigDecimal("9.9")).build(),
      releaseDto -> release1,
      issueReleaseDto -> issueReleaseDto.toBuilder().setScaReleaseUuid(release1.uuid()).build());
    var issue4 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.PROHIBITED_LICENSE, "4", componentDto.uuid(),
      null, null,
      releaseDto -> release1,
      issueReleaseDto -> issueReleaseDto.toBuilder().setScaReleaseUuid(release1.uuid()).build());

    var foundPage = scaIssuesReleasesDetailsDao.selectByBranchUuid(db.getSession(), componentDto.branchUuid(), Pagination.forPage(1).andSize(1));

    assertThat(foundPage).hasSize(1).isSubsetOf(issue1, issue2, issue3, issue4);
    var foundAllIssues = scaIssuesReleasesDetailsDao.selectByBranchUuid(db.getSession(), componentDto.branchUuid(), Pagination.forPage(1).andSize(10));
    assertThat(foundAllIssues).hasSize(4)
      .containsExactlyElementsOf(Stream.of(issue1, issue2, issue3, issue4).sorted(comparator(ScaIssuesReleasesDetailsQuery.Sort.SEVERITY_DESC)).toList());
  }

  @Test
  void withNoQueryFilters_shouldReturnAllIssues() {
    setupAndExecuteQueryTest(Function.identity(), QueryTestData::expectedIssuesSortedByIdentityAsc, "All issues should be returned");
  }

  @Test
  void withNoQueryFilters_shouldCountAllIssues() {
    setupAndExecuteQueryCountTest(Function.identity(), 6);
  }

  @Test
  void withNoQueryFilters_shouldSort() {
    QueryTestData testData = createQueryTestData();
    var expectedLists = new EnumMap<ScaIssuesReleasesDetailsQuery.Sort, List<ScaIssueReleaseDetailsDto>>(ScaIssuesReleasesDetailsQuery.Sort.class);
    for (var sort : ScaIssuesReleasesDetailsQuery.Sort.values()) {
      var expectedIssues = testData.expectedIssuesSorted(sort);
      executeQueryTest(testData, queryBuilder -> queryBuilder.setSort(sort), expectedIssues,
        "Sort %s should return expected issues".formatted(sort));
      expectedLists.put(sort, expectedIssues);
    }

    // The assertions below here are actually about the expectations, but above
    // we've just established that the actual matches the expectations.

    // The point of this is to assert that the test data contains a distinct ordering for each
    // ordering in ScaIssuesReleasesDetailsQuery.Sort, because if it doesn't we could get
    // false negatives in our tests.
    assertThat(expectedLists.values().stream().distinct().toList())
      .as("Expected issues should have distinct orderings for each sort")
      .containsExactlyInAnyOrderElementsOf(expectedLists.values());

    // for identity, assert that our ASC and DESC actually invert each other.
    // for severity and cvss score, this isn't supposed to be true because the
    // secondary sort is IDENTITY_ASC even when we sort by DESC severity; but the
    // severity and score values ignoring the other attributes should still be
    // reversed.
    assertThat(Lists.reverse(expectedLists.get(ScaIssuesReleasesDetailsQuery.Sort.IDENTITY_ASC)))
      .as("IDENTITY sort should be reversed when sorted by DESC")
      .containsExactlyElementsOf(expectedLists.get(ScaIssuesReleasesDetailsQuery.Sort.IDENTITY_DESC));
    assertThat(
      Lists.reverse(expectedLists.get(ScaIssuesReleasesDetailsQuery.Sort.SEVERITY_ASC)).stream()
        .map(ScaIssueReleaseDetailsDto::severity)
        .toList())
          .as("SEVERITY sort should be reversed when sorted by DESC")
          .containsExactlyElementsOf(expectedLists.get(ScaIssuesReleasesDetailsQuery.Sort.SEVERITY_DESC).stream()
            .map(ScaIssueReleaseDetailsDto::severity)
            .toList());
    assertThat(Lists.reverse(expectedLists.get(ScaIssuesReleasesDetailsQuery.Sort.CVSS_SCORE_ASC).stream()
      .map(ScaIssueReleaseDetailsDto::cvssScore)
      .toList()))
        .as("CVSS_SCORE sort should be reversed when sorted by DESC")
        .containsExactlyElementsOf(expectedLists.get(ScaIssuesReleasesDetailsQuery.Sort.CVSS_SCORE_DESC).stream()
          .map(ScaIssueReleaseDetailsDto::cvssScore)
          .toList());
  }

  @Test
  void withQueryFilteredByIssueType_shouldReturnExpectedTypes() {
    QueryTestData testData = createQueryTestData();
    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setTypes(List.of(ScaIssueType.VULNERABILITY)),
      testData.expectedIssuesSortedByIdentityAsc().stream()
        .filter(expected -> expected.scaIssueType() == ScaIssueType.VULNERABILITY)
        .toList(),
      "Only vulnerability issues should be returned");
    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setTypes(List.of(ScaIssueType.PROHIBITED_LICENSE)),
      testData.expectedIssuesSortedByIdentityAsc().stream()
        .filter(expected -> expected.scaIssueType() == ScaIssueType.PROHIBITED_LICENSE)
        .toList(),
      "Only vulnerability issues should be returned");
    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setTypes(List.of(ScaIssueType.values())),
      testData.expectedIssuesSortedByIdentityAsc(),
      "All issues should be returned");
    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setTypes(Collections.emptyList()),
      Collections.emptyList(),
      "No issues should be returned when searching for zero types");
  }

  @Test
  void withQueryFilteredByIssueType_shouldCountSelectedIssues() {
    QueryTestData testData = createQueryTestData();
    executeQueryCountTest(testData,
      queryBuilder -> queryBuilder.setTypes(List.of(ScaIssueType.VULNERABILITY)),
      4);
    executeQueryCountTest(testData,
      queryBuilder -> queryBuilder.setTypes(List.of(ScaIssueType.PROHIBITED_LICENSE)),
      2);
    executeQueryCountTest(testData,
      queryBuilder -> queryBuilder.setTypes(List.of(ScaIssueType.values())),
      6);
    executeQueryCountTest(testData,
      queryBuilder -> queryBuilder.setTypes(Collections.emptyList()),
      0);
  }

  @Test
  void withQueryFilteredByVulnerabilityId_shouldReturnExpectedItems() {
    QueryTestData testData = createQueryTestData();
    var expectedEndsInId1 = testData.expectedIssues().stream()
      .filter(issue -> issue.vulnerabilityId() != null && issue.vulnerabilityId().endsWith("Id1"))
      .toList();
    assertThat(expectedEndsInId1).hasSize(1);
    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setVulnerabilityIdSubstring("Id1"),
      expectedEndsInId1,
      "Only the vulnerability ending in Id1 should be returned");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setVulnerabilityIdSubstring("NotInThere"),
      Collections.emptyList(),
      "No issues should be returned when searching for the substring 'NotInThere'");
    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setVulnerabilityIdSubstring("Escape% NULL AS!%"),
      Collections.emptyList(),
      "No issues should be returned when searching for a string that needs escaping");
    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setVulnerabilityIdSubstring(ScaIssueDto.NULL_VALUE),
      Collections.emptyList(),
      "No vulnerabilities should be returned when searching for ScaIssueDto.NULL_VALUE");

    var allVulnerabilityIssues = testData.expectedIssuesSortedByIdentityAsc().stream()
      .filter(issue -> issue.scaIssueType() == ScaIssueType.VULNERABILITY)
      .toList();
    assertThat(allVulnerabilityIssues).hasSize(4);

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setVulnerabilityIdSubstring("Vulnerability"),
      allVulnerabilityIssues,
      "All vulnerabilities should be returned when searching for the substring 'Vulnerability'");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setVulnerabilityIdSubstring(""),
      allVulnerabilityIssues,
      "All vulnerabilities should be returned when searching for empty vulnerabilityId");
  }

  @Test
  void withQueryFilteredByPackageName_shouldReturnExpectedItems() {
    QueryTestData testData = createQueryTestData();
    var expectedEndsInName1 = testData.expectedIssues().subList(0, 1);

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setPackageNameSubstring("Name1"),
      expectedEndsInName1,
      "Only the packages containing Name1 should be returned");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setPackageNameSubstring("NotInThere"),
      Collections.emptyList(),
      "No issues should be returned when searching for the substring 'NotInThere'");
    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setPackageNameSubstring("Escape% NULL AS!%"),
      Collections.emptyList(),
      "No issues should be returned when searching for a string that needs escaping");
    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setPackageNameSubstring(ScaIssueDto.NULL_VALUE),
      Collections.emptyList(),
      "No vulnerabilities should be returned when searching for ScaIssueDto.NULL_VALUE");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setPackageNameSubstring("Package"),
      testData.expectedIssuesSortedByIdentityAsc(),
      "All issues should be returned when searching for the substring 'Package'");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setPackageNameSubstring(""),
      testData.expectedIssuesSortedByIdentityAsc(),
      "All issues should be returned when searching for empty package name");
  }

  @Test
  void withQueryFilteredByNewInPullRequest_shouldReturnExpectedItems() {
    QueryTestData testData = createQueryTestData();

    var expectedNew = testData.expectedIssuesSortedByIdentityAsc().stream()
      .filter(issue -> issue.newInPullRequest())
      .toList();
    var expectedNotNew = testData.expectedIssuesSortedByIdentityAsc().stream()
      .filter(issue -> !issue.newInPullRequest())
      .toList();

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setNewInPullRequest(true),
      expectedNew,
      "Only the releases marked newInPullRequest should be returned");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setNewInPullRequest(false),
      expectedNotNew,
      "Only the releases marked not newInPullRequest should be returned");
  }

  @Test
  void withQueryFilteredBySeverity_shouldReturnExpectedItems() {
    QueryTestData testData = createQueryTestData();
    var expectedSeverityInfo = testData.expectedIssuesSortedByIdentityAsc().stream()
      .filter(issue -> issue.severity() == ScaSeverity.INFO)
      .toList();
    var expectedSeverityBlocker = testData.expectedIssuesSortedByIdentityAsc().stream()
      .filter(issue -> issue.severity() == ScaSeverity.BLOCKER)
      .toList();
    assertThat(expectedSeverityInfo).hasSize(5);
    assertThat(expectedSeverityBlocker).hasSize(1);

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setSeverities(List.of(ScaSeverity.INFO)),
      expectedSeverityInfo,
      "Only the issues of severity INFO should be returned");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setSeverities(List.of(ScaSeverity.BLOCKER)),
      expectedSeverityBlocker,
      "Only the issues of severity BLOCKER should be returned");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setSeverities(List.of(ScaSeverity.LOW, ScaSeverity.HIGH)),
      Collections.emptyList(),
      "Should not match any severities of LOW or HIGH");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setSeverities(List.of(ScaSeverity.BLOCKER, ScaSeverity.INFO, ScaSeverity.LOW)),
      testData.expectedIssuesSortedByIdentityAsc(),
      "All issues should be returned when searching for a list that contains them all");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setSeverities(Collections.emptyList()),
      Collections.emptyList(),
      "No issues should be returned when searching for zero severities");
  }

  @Test
  void withQueryFilteredByPackageManager_shouldReturnExpectedItems() {
    QueryTestData testData = createQueryTestData();
    var expectedPackageManagerNpm = testData.expectedIssuesWithPackageManager(PackageManager.NPM).stream()
      .sorted(identityComparator()).toList();
    var expectedPackageManagerMaven = testData.expectedIssuesWithPackageManager(PackageManager.MAVEN).stream()
      .sorted(identityComparator()).toList();

    assertThat(expectedPackageManagerNpm).hasSize(2);
    assertThat(expectedPackageManagerMaven).hasSize(4);

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setPackageManagers(List.of(PackageManager.NPM)),
      expectedPackageManagerNpm,
      "Only the npm issues should be returned");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setPackageManagers(List.of(PackageManager.MAVEN)),
      expectedPackageManagerMaven,
      "Only the Maven issues should be returned");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setPackageManagers(List.of(PackageManager.NPM, PackageManager.MAVEN)),
      testData.expectedIssuesSortedByIdentityAsc(),
      "All issues should be returned when searching for two package managers");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setPackageManagers(Collections.emptyList()),
      Collections.emptyList(),
      "No issues should be returned when searching for zero package managers");
  }

  @Test
  void withQueryFilteredByDirect_shouldReturnExpectedItems() {
    QueryTestData testData = createQueryTestData();
    var expectedDirect = testData.directIssues();
    var expectedTransitive = testData.transitiveIssues();

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setDirect(true),
      expectedDirect,
      "Only direct issues should be returned");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setDirect(false),
      expectedTransitive,
      "Only the transitive issues should be returned");
  }

  @Test
  void withQueryFilteredByProductionScope_shouldReturnExpectedItems() {
    QueryTestData testData = createQueryTestData();
    var expectedProduction = testData.productionIssues();
    var expectedNotProduction = testData.notProductionIssues();

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setProductionScope(true),
      expectedProduction,
      "Only production issues should be returned");

    executeQueryTest(testData,
      queryBuilder -> queryBuilder.setProductionScope(false),
      expectedNotProduction,
      "Only the non-production issues should be returned");
  }

  @Test
  void withQueryMultipleFiltersNonDefaultSort_shouldReturnExpectedItems() {
    QueryTestData testData = createQueryTestData();
    var expectedPackageManagerMaven = testData.expectedIssuesWithPackageManager(PackageManager.MAVEN);
    var expectedTypeVulnerability = testData.expectedIssuesSortedByIdentityAsc().stream()
      .filter(issue -> issue.scaIssueType() == ScaIssueType.VULNERABILITY)
      .toList();
    var sortedByCvssDesc = testData.expectedIssuesSortedByCvssDesc();
    var expectedResults = sortedByCvssDesc.stream()
      .filter(expectedPackageManagerMaven::contains)
      .filter(expectedTypeVulnerability::contains)
      .toList();
    assertThat(expectedResults).hasSize(3);

    executeQueryTest(testData,
      queryBuilder -> queryBuilder
        .setSort(ScaIssuesReleasesDetailsQuery.Sort.CVSS_SCORE_DESC)
        .setPackageManagers(List.of(PackageManager.MAVEN))
        .setTypes(List.of(ScaIssueType.VULNERABILITY)),
      expectedResults,
      "Maven vulnerabilities returned in cvss score desc order");
  }

  private void setupAndExecuteQueryTest(Function<ScaIssuesReleasesDetailsQuery.Builder, ScaIssuesReleasesDetailsQuery.Builder> builderFunction,
    Function<QueryTestData, List<ScaIssueReleaseDetailsDto>> expectedIssuesFunction, String assertAs) {
    QueryTestData testData = createQueryTestData();
    executeQueryTest(testData, builderFunction, expectedIssuesFunction.apply(testData), assertAs);
  }

  private void executeQueryTest(QueryTestData testData,
    Function<ScaIssuesReleasesDetailsQuery.Builder, ScaIssuesReleasesDetailsQuery.Builder> builderFunction,
    List<ScaIssueReleaseDetailsDto> expectedIssues,
    String assertAs) {
    var query = builderFunction.apply(
      new ScaIssuesReleasesDetailsQuery.Builder()
        .setBranchUuid(testData.branchUuid())
        .setSort(ScaIssuesReleasesDetailsQuery.Sort.IDENTITY_ASC))
      .build();
    var foundPage = scaIssuesReleasesDetailsDao.selectByQuery(db.getSession(), query, Pagination.forPage(1).andSize(10));

    assertThat(foundPage).as(assertAs).containsExactlyElementsOf(expectedIssues);
  }

  private void setupAndExecuteQueryCountTest(Function<ScaIssuesReleasesDetailsQuery.Builder, ScaIssuesReleasesDetailsQuery.Builder> builderFunction,
    int expectedCount) {
    QueryTestData testData = createQueryTestData();
    executeQueryCountTest(testData, builderFunction, expectedCount);
  }

  private void executeQueryCountTest(QueryTestData testData,
    Function<ScaIssuesReleasesDetailsQuery.Builder, ScaIssuesReleasesDetailsQuery.Builder> builderFunction,
    int expectedCount) {
    var query = builderFunction.apply(
      new ScaIssuesReleasesDetailsQuery.Builder()
        .setBranchUuid(testData.branchUuid())
        .setSort(ScaIssuesReleasesDetailsQuery.Sort.IDENTITY_ASC))
      .build();
    var count = scaIssuesReleasesDetailsDao.countByQuery(db.getSession(), query);

    assertThat(count).isEqualTo(expectedCount);
  }

  private QueryTestData createQueryTestData() {
    var projectData = db.components().insertPrivateProject();
    var componentDto = projectData.getMainBranchComponent();
    // the first two are set to NPM, the others default to MAVEN
    var issue1 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.VULNERABILITY, "1", componentDto.uuid(),
      scaIssueDto -> scaIssueDto,
      scaVulnerabilityIssueDto -> scaVulnerabilityIssueDto,
      scaReleaseDto -> scaReleaseDto.toBuilder().setPackageManager(PackageManager.NPM).build(),
      scaIssueReleaseDto -> scaIssueReleaseDto);
    var issue2 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.PROHIBITED_LICENSE, "2", componentDto.uuid(),
      scaIssueDto -> scaIssueDto,
      scaVulnerabilityIssueDto -> scaVulnerabilityIssueDto,
      scaReleaseDto -> scaReleaseDto.toBuilder().setPackageManager(PackageManager.NPM).build(),
      scaIssueReleaseDto -> scaIssueReleaseDto);
    var issue3 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.VULNERABILITY, "3", componentDto.uuid());
    var issue4 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.PROHIBITED_LICENSE, "4", componentDto.uuid());
    // low cvss but high severity
    var issue5 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.VULNERABILITY, "5", componentDto.uuid(),
      scaIssueDto -> scaIssueDto,
      scaVulnerabilityIssueDto -> scaVulnerabilityIssueDto.toBuilder()
        .setCvssScore(new BigDecimal("2.1"))
        .setBaseSeverity(ScaSeverity.BLOCKER)
        .build(),
      scaReleaseDto -> scaReleaseDto.toBuilder().setNewInPullRequest(true).build(),
      scaIssueReleaseDto -> scaIssueReleaseDto.toBuilder().setSeverity(ScaSeverity.BLOCKER).build());
    // high cvss but low severity
    var issue6 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.VULNERABILITY, "6", componentDto.uuid(),
      scaIssueDto -> scaIssueDto,
      scaVulnerabilityIssueDto -> scaVulnerabilityIssueDto.toBuilder()
        .setCvssScore(new BigDecimal("9.1"))
        .setBaseSeverity(ScaSeverity.INFO)
        .build(),
      scaReleaseDto -> scaReleaseDto.toBuilder().setNewInPullRequest(true).build(),
      scaIssueReleaseDto -> scaIssueReleaseDto.toBuilder().setSeverity(ScaSeverity.INFO).build());

    // issue 1 weirdly has no dependency, issues 2-3 are direct, issues 4-6 are transitive
    // issues 2 and 4 are production, 3,5,6 are not production
    db.getScaDependenciesDbTester().insertScaDependency(issue2.releaseUuid(), "2",
      builder -> builder.setDirect(true).setProductionScope(true));
    var dep3 = db.getScaDependenciesDbTester().insertScaDependency(issue3.releaseUuid(), "3",
      builder -> builder.setDirect(true).setProductionScope(false));
    var dep4 = db.getScaDependenciesDbTester().insertScaDependency(issue4.releaseUuid(), "4",
      builder -> builder.setDirect(false).setProductionScope(true));
    var dep5 = db.getScaDependenciesDbTester().insertScaDependency(issue5.releaseUuid(), "5",
      builder -> builder.setDirect(false).setProductionScope(false));
    db.getScaDependenciesDbTester().insertScaDependency(issue6.releaseUuid(), "6",
      builder -> builder.setDirect(false).setProductionScope(false));

    // make issue3 and issue4 BOTH direct and transitive,
    // issue4 and issue5 are BOTH production and not
    db.getScaDependenciesDbTester().insertScaDependency(issue3.releaseUuid(), "7",
      builder -> builder.setDirect(!dep3.direct()).setProductionScope(dep3.productionScope()));
    db.getScaDependenciesDbTester().insertScaDependency(issue4.releaseUuid(), "8",
      builder -> builder.setDirect(!dep4.direct()).setProductionScope(!dep4.productionScope()));
    db.getScaDependenciesDbTester().insertScaDependency(issue5.releaseUuid(), "9",
      builder -> builder.setDirect(dep5.direct()).setProductionScope(!dep5.productionScope()));

    var directIssues = List.of(issue2, issue3, issue4).stream().sorted(identityComparator()).toList();
    var transitiveIssues = List.of(issue3, issue4, issue5, issue6).stream().sorted(identityComparator()).toList();
    var productionIssues = List.of(issue2, issue4, issue5).stream().sorted(identityComparator()).toList();
    var notProductionIssues = List.of(issue3, issue4, issue5, issue6).stream().sorted(identityComparator()).toList();

    return new QueryTestData(projectData, componentDto,
      List.of(issue1, issue2, issue3, issue4, issue5, issue6),
      directIssues, transitiveIssues, productionIssues, notProductionIssues);
  }

  @Test
  void selectByScaIssueReleaseUuid_shouldReturnAnIssue() {
    var projectData = db.components().insertPrivateProject();
    var componentDto = projectData.getMainBranchComponent();
    var issue1 = db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.VULNERABILITY, "1", componentDto.uuid());

    // insert another issue to assert that it's not selected
    db.getScaIssuesReleasesDetailsDbTester().insertIssue(ScaIssueType.PROHIBITED_LICENSE, "2", componentDto.uuid());

    var foundIssue = scaIssuesReleasesDetailsDao.selectByScaIssueReleaseUuid(db.getSession(), issue1.issueReleaseUuid());
    assertThat(foundIssue).isEqualTo(issue1);

    var notFoundIssue = scaIssuesReleasesDetailsDao.selectByScaIssueReleaseUuid(db.getSession(), "00000");
    assertThat(notFoundIssue).isNull();
  }

  private record QueryTestData(ProjectData projectData,
    ComponentDto componentDto,
    List<ScaIssueReleaseDetailsDto> expectedIssues,
    List<ScaIssueReleaseDetailsDto> directIssues,
    List<ScaIssueReleaseDetailsDto> transitiveIssues,
    List<ScaIssueReleaseDetailsDto> productionIssues,
    List<ScaIssueReleaseDetailsDto> notProductionIssues) {

    public String branchUuid() {
      return componentDto.branchUuid();
    }

    public List<ScaIssueReleaseDetailsDto> expectedIssuesSorted(ScaIssuesReleasesDetailsQuery.Sort sort) {
      return expectedIssues.stream().sorted(comparator(sort)).toList();
    }

    public List<ScaIssueReleaseDetailsDto> expectedIssuesSortedByIdentityAsc() {
      return expectedIssuesSorted(ScaIssuesReleasesDetailsQuery.Sort.IDENTITY_ASC);
    }

    public List<ScaIssueReleaseDetailsDto> expectedIssuesSortedByCvssDesc() {
      return expectedIssuesSorted(ScaIssuesReleasesDetailsQuery.Sort.CVSS_SCORE_DESC);
    }

    public List<ScaIssueReleaseDetailsDto> expectedIssuesWithPackageManager(PackageManager packageManager) {
      // we just have hardcoded knowledge of how we set them up, because ScaIssueReleaseDetailsDto doesn't
      // contain the ScaReleaseDto to look at this
      return switch (packageManager) {
        case NPM -> expectedIssues.subList(0, 2);
        case MAVEN -> expectedIssues.subList(2, expectedIssues.size());
        default -> Collections.emptyList();
      };
    }
  }
}
