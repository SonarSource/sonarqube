/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.history;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.BaseMyBatisConfExtension;
import org.sonar.db.DBSessionsImpl;
import org.sonar.db.DbTester;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.model.IssueCountDimension;
import org.sonarsource.history.model.IssueCountDimensionKey;
import org.sonarsource.history.model.IssueCountHistoryFilters;
import org.sonarsource.history.model.IssueCountHistoryQuery;
import org.sonarsource.history.model.IssueCountHistoryQueryRow;
import org.sonarsource.history.model.IssueCountHistoryRow;
import org.sonarsource.history.model.IssueDensityDistribution;
import org.sonarsource.history.model.IssueDensityHistoryPoint;
import org.sonarsource.history.model.IssueDensityHistoryResponse;
import org.sonarsource.history.model.IssueResolutionHistoryQuery;
import org.sonarsource.history.model.IssueResolutionHistoryQueryRow;
import org.sonarsource.history.model.IssueTtrHistory;
import org.sonarsource.history.model.IssueType;
import org.sonarsource.history.model.MeasureHistoryQueryRow;
import org.sonarsource.history.model.MeasureHistoryRow;
import org.sonarsource.history.model.MeasureKeyMapping;
import org.sonarsource.history.model.ProjectIssueResolutionQuery;
import org.sonarsource.history.model.ProjectIssueResolutionValue;
import org.sonarsource.history.model.ProjectMeasureValue;
import org.sonarsource.history.server.db.HistoryDbClient;
import org.sonarsource.history.server.db.HistoryMyBatisConfExtension;
import org.sonarsource.history.server.db.mapper.IssueTtrHistoryMapperFragments;
import org.sonarsource.history.server.db.mapper.IssueTtrHistoryMapperFragmentsTestMapper;
import org.sonarsource.history.server.db.repository.IssueCountDimensionsRepository;
import org.sonarsource.history.server.db.repository.IssueCountHistoryRepository;
import org.sonarsource.history.server.db.repository.IssueTtrHistoryRepository;
import org.sonarsource.history.server.db.repository.MeasureHistoryRepository;
import org.sonarsource.history.server.db.repository.MeasureKeyMappingRepository;
import org.sonarsource.history.server.service.IssueCountHistoryService;
import org.sonarsource.history.server.service.MeasuresHistoryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newSubPortfolio;

/**
 * Exercises the SQL used by the dashboard history and project-breakdown endpoints against the SQS schema.
 *
 * <p>This class deliberately lives in the DAO module because the database CI matrix runs it against each
 * configured vendor, while the unification repository tests use feature-local schemas.
 */
class HistoryRepositoriesIT {

  private static final Instant FIRST_DAY = Instant.parse("2026-07-08T00:00:00Z");
  private static final Instant SECOND_DAY = FIRST_DAY.plusSeconds(86_400);
  private static final String ENTITY_ID = "branch-uuid";

  @RegisterExtension
  private final DbTester db = DbTester.createWithConfExtensions(
    System2.INSTANCE,
    List.of(
      new HistoryMyBatisConfExtension(IssueTtrHistoryMapperFragments.class),
      new AggregationBranchesMyBatisConfExtension()));

  private final IssueCountDimensionsRepository dimensions = new IssueCountDimensionsRepository();
  private final IssueCountHistoryRepository issueCountHistory = new IssueCountHistoryRepository();
  private final IssueTtrHistoryRepository issueTtrHistory = new IssueTtrHistoryRepository();
  private final MeasureHistoryRepository measureHistory = new MeasureHistoryRepository();
  private final MeasureKeyMappingRepository measureKeys = new MeasureKeyMappingRepository();
  private final HistoryDbClient historyDbClient = new HistoryDbClient(
    db.getDbClient().getMyBatis(),
    new DBSessionsImpl(db.getDbClient().getMyBatis()),
    List.of(dimensions, issueCountHistory, issueTtrHistory, measureHistory, measureKeys));

  @Test
  void measureHistory_andProjectMeasures_shouldReturnCurrentAndHistoricalValues() {
    MeasureKeyMapping coverage = metric("coverage", "PERCENT");
    MeasureKeyMapping bugs = metric("bugs", "INT");

    measureHistory.upsert(db.getSession(), new MeasureHistoryRow(coverage.id(), ENTITY_ID, EntityType.PROJECT_BRANCH, FIRST_DAY, "80.0"));
    measureHistory.upsert(db.getSession(), new MeasureHistoryRow(coverage.id(), ENTITY_ID, EntityType.PROJECT_BRANCH, SECOND_DAY, "90.0"));
    measureHistory.upsert(db.getSession(), new MeasureHistoryRow(bugs.id(), ENTITY_ID, EntityType.PROJECT_BRANCH, SECOND_DAY, "7"));

    List<MeasureHistoryQueryRow> history = measureHistory.findMeasureHistoryByMetricNames(
      db.getSession(), ENTITY_ID, EntityType.PROJECT_BRANCH, List.of("coverage", "bugs"), FIRST_DAY, SECOND_DAY);

    assertThat(history)
      .extracting(MeasureHistoryQueryRow::metricName, MeasureHistoryQueryRow::recordedAt, MeasureHistoryQueryRow::textValue)
      .containsExactly(
        tuple("coverage", FIRST_DAY, "80.0"),
        tuple("coverage", SECOND_DAY, "90.0"),
        tuple("bugs", SECOND_DAY, "7"));
    assertThat(measureHistory.findLatestMeasureValuesForEntity(db.getSession(), ENTITY_ID, EntityType.PROJECT_BRANCH))
      .containsExactlyInAnyOrderEntriesOf(Map.of(coverage.id(), "90.0", bugs.id(), "7"));

    measureHistory.upsert(db.getSession(), new MeasureHistoryRow(coverage.id(), "branch-1", EntityType.PROJECT_BRANCH, FIRST_DAY, "70.0"));
    measureHistory.upsert(db.getSession(), new MeasureHistoryRow(coverage.id(), "branch-1", EntityType.PROJECT_BRANCH, SECOND_DAY, "75.0"));
    measureHistory.upsert(db.getSession(), new MeasureHistoryRow(coverage.id(), "branch-2", EntityType.PROJECT_BRANCH, SECOND_DAY, null));
    measureHistory.upsert(db.getSession(), new MeasureHistoryRow(coverage.id(), "branch-3", EntityType.PROJECT_BRANCH, SECOND_DAY, "60.0"));
    measureHistory.upsert(db.getSession(), new MeasureHistoryRow(coverage.id(), "branch-1", EntityType.PORTFOLIO, SECOND_DAY, "100.0"));

    assertThat(measureHistory.findLatestProjectMeasureValues(
      db.getSession(), List.of("branch-1", "branch-2"), coverage.id(), null))
      .extracting(ProjectMeasureValue::branchId, ProjectMeasureValue::textValue)
      .containsExactlyInAnyOrder(tuple("branch-1", "75.0"), tuple("branch-2", null));
    assertThat(measureHistory.findLatestProjectMeasureValues(
      db.getSession(), List.of("branch-1"), coverage.id(), FIRST_DAY))
      .containsExactly(new ProjectMeasureValue("branch-1", "70.0"));
    assertThat(measureHistory.findMeasureHistoryByMetricNames(
      db.getSession(), ENTITY_ID, EntityType.PROJECT_BRANCH, List.of("coverage"), SECOND_DAY.plusSeconds(86_401), SECOND_DAY.plusSeconds(86_401)))
      .extracting(MeasureHistoryQueryRow::recordedAt, MeasureHistoryQueryRow::textValue)
      .containsExactly(tuple(SECOND_DAY.plusSeconds(86_401), "90.0"));
    assertThat(measureHistory.findLatestProjectMeasureValues(db.getSession(), List.of(), coverage.id(), null)).isEmpty();
    assertThat(measureHistory.findMeasureHistoryByMetricNames(
      db.getSession(), ENTITY_ID, EntityType.PROJECT_BRANCH, List.of(), FIRST_DAY, SECOND_DAY)).isEmpty();
  }

  @Test
  void issueCountHistory_andProjectIssueCounts_shouldQuerySlicedAndUnslicedData() {
    IssueCountDimension bug = insertDimension("java:S100", IssueType.BUG, (short) 2, (short) 3, (short) 4);
    IssueCountDimension codeSmell = insertDimension("java:S200", IssueType.CODE_SMELL, (short) 2, (short) 3, (short) 4);

    issueCountHistory.upsert(db.getSession(), new IssueCountHistoryRow(ENTITY_ID, EntityType.PROJECT_BRANCH, bug.id(), FIRST_DAY, 3));
    issueCountHistory.upsert(db.getSession(), new IssueCountHistoryRow(ENTITY_ID, EntityType.PROJECT_BRANCH, bug.id(), SECOND_DAY, 7));
    issueCountHistory.upsert(db.getSession(), new IssueCountHistoryRow(ENTITY_ID, EntityType.PROJECT_BRANCH, codeSmell.id(), SECOND_DAY, 5));
    issueCountHistory.upsert(db.getSession(), new IssueCountHistoryRow("branch-2", EntityType.PROJECT_BRANCH, bug.id(), SECOND_DAY, 11));

    IssueCountHistoryQuery unsliced = IssueCountHistoryQuery.builder(ENTITY_ID, EntityType.PROJECT_BRANCH, FIRST_DAY)
      .endDate(SECOND_DAY)
      .build();
    IssueCountHistoryQuery sliced = IssueCountHistoryQuery.builder(ENTITY_ID, EntityType.PROJECT_BRANCH, FIRST_DAY)
      .endDate(SECOND_DAY)
      .sliceBy("TYPE")
      .build();

    assertThat(issueCountHistory.query(db.getSession(), unsliced))
      .extracting(IssueCountHistoryQueryRow::recordedAt, IssueCountHistoryQueryRow::dimension, IssueCountHistoryQueryRow::value)
      .containsExactly(tuple(SECOND_DAY, "all", 12), tuple(FIRST_DAY, "all", 3));
    assertThat(issueCountHistory.query(db.getSession(), sliced))
      .extracting(IssueCountHistoryQueryRow::recordedAt, IssueCountHistoryQueryRow::dimension, IssueCountHistoryQueryRow::value)
      .containsExactly(
        tuple(SECOND_DAY, String.valueOf(IssueType.CODE_SMELL.getDbConstant()), 5),
        tuple(SECOND_DAY, String.valueOf(IssueType.BUG.getDbConstant()), 7),
        tuple(FIRST_DAY, String.valueOf(IssueType.BUG.getDbConstant()), 3));
    assertThat(issueCountHistory.query(db.getSession(), queryWithSlice("RULE_KEY")))
      .extracting(IssueCountHistoryQueryRow::dimension)
      .contains("java:S100", "java:S200");
    assertThat(issueCountHistory.query(db.getSession(), queryWithSlice("SEVERITY")))
      .extracting(IssueCountHistoryQueryRow::dimension)
      .containsOnly("4");
    assertThat(issueCountHistory.query(db.getSession(), queryWithSlice("SOFTWARE_QUALITY")))
      .extracting(IssueCountHistoryQueryRow::dimension)
      .containsOnly("MAINTAINABILITY", "RELIABILITY", "SECURITY");
    assertThat(issueCountHistory.query(db.getSession(), queryWithSlice("STATUS")))
      .extracting(IssueCountHistoryQueryRow::dimension)
      .containsOnly("OPEN");

    assertThat(issueCountHistory.findLatestDimensionCountsForEntity(
      db.getSession(), ENTITY_ID, EntityType.PROJECT_BRANCH)).containsEntry(
        bug.id(), new IssueCountHistoryRow(ENTITY_ID, EntityType.PROJECT_BRANCH, bug.id(), SECOND_DAY, 7));
    assertThat(issueCountHistory.findLatestProjectIssueCounts(
      db.getSession(), List.of(ENTITY_ID, "branch-2"), noFilters(), null))
      .containsExactlyInAnyOrderEntriesOf(Map.of(ENTITY_ID, 12L, "branch-2", 11L));
    assertThat(issueCountHistory.findLatestProjectIssueCounts(
      db.getSession(), List.of(ENTITY_ID), new IssueCountHistoryFilters(List.of("java:S100"), null, null, null, null), null))
      .containsExactly(Map.entry(ENTITY_ID, 7L));
    assertThat(issueCountHistory.findLatestDimensionCountsForEntity(
      db.getSession(), "missing-branch", EntityType.PROJECT_BRANCH)).isEmpty();
  }

  @Test
  void issueCountProjectBreakdown_shouldPartitionLargeBranchInputAndRespectCutoff() {
    IssueCountDimension dimension = insertDimension("java:S300", IssueType.BUG, (short) 2, (short) 3, (short) 4);
    Instant later = SECOND_DAY.plusSeconds(1);
    issueCountHistory.upsert(db.getSession(), new IssueCountHistoryRow("branch-1", EntityType.PROJECT_BRANCH, dimension.id(), FIRST_DAY, 3));
    issueCountHistory.upsert(db.getSession(), new IssueCountHistoryRow("branch-1", EntityType.PROJECT_BRANCH, dimension.id(), later, 7));

    List<String> branchIds = new ArrayList<>();
    branchIds.add("branch-1");
    for (int i = 0; i < 1_001; i++) {
      branchIds.add("missing-branch-" + i);
    }

    assertThat(issueCountHistory.findLatestProjectIssueCounts(
      db.getSession(), branchIds, noFilters(), SECOND_DAY))
      .containsExactly(Map.entry("branch-1", 3L));
    assertThat(issueCountHistory.findLatestProjectIssueCounts(
      db.getSession(), List.of("branch-1"), noFilters(), later))
      .containsExactly(Map.entry("branch-1", 7L));
  }

  @Test
  void issueDensityHistory_shouldCombineIssueCountsWithMeasureHistory() {
    IssueCountDimension dimension = insertDimension("java:S350", IssueType.BUG, (short) 2, (short) 3, (short) 4);
    MeasureKeyMapping ncloc = metric("ncloc", "INT");
    issueCountHistory.upsert(db.getSession(), new IssueCountHistoryRow(
      ENTITY_ID, EntityType.PROJECT_BRANCH, dimension.id(), FIRST_DAY, 2));
    issueCountHistory.upsert(db.getSession(), new IssueCountHistoryRow(
      ENTITY_ID, EntityType.PROJECT_BRANCH, dimension.id(), SECOND_DAY, 4));
    measureHistory.upsert(db.getSession(), new MeasureHistoryRow(
      ncloc.id(), ENTITY_ID, EntityType.PROJECT_BRANCH, FIRST_DAY, "100"));
    measureHistory.upsert(db.getSession(), new MeasureHistoryRow(
      ncloc.id(), ENTITY_ID, EntityType.PROJECT_BRANCH, SECOND_DAY, "200"));
    db.commit();

    IssueDensityHistoryResponse result = new IssueCountHistoryService(
      historyDbClient,
      new MeasuresHistoryService(historyDbClient))
      .queryIssueDensityHistory(
        ENTITY_ID,
        EntityType.PROJECT_BRANCH,
        FIRST_DAY,
        SECOND_DAY,
        null,
        null,
        null,
        null,
        null,
        null);

    assertThat(result.issueDensityHistory())
      .containsExactly(
        new IssueDensityHistoryPoint(SECOND_DAY, List.of(new IssueDensityDistribution("all", 20d))),
        new IssueDensityHistoryPoint(FIRST_DAY, List.of(new IssueDensityDistribution("all", 20d))));
  }

  @Test
  void issueResolutionHistory_andProjectIssueResolution_shouldReturnAllStatistics() {
    IssueCountDimension bug = insertDimension("java:S400", IssueType.BUG, (short) 2, (short) 3, (short) 4);
    IssueCountDimension vulnerability = insertDimension("java:S500", IssueType.VULNERABILITY, (short) 2, (short) 3, (short) 4);
    upsertTtr(ENTITY_ID, EntityType.PROJECT_BRANCH, FIRST_DAY, bug, 30, 1, 10, 1);
    upsertTtr(ENTITY_ID, EntityType.PROJECT_BRANCH, SECOND_DAY, bug, 60, 2, 20, 2);
    upsertTtr(ENTITY_ID, EntityType.PROJECT_BRANCH, SECOND_DAY, vulnerability, 40, 2, 20, 2);

    IssueResolutionHistoryQuery unsliced = IssueResolutionHistoryQuery.builder(ENTITY_ID, EntityType.PROJECT_BRANCH, FIRST_DAY)
      .endDate(SECOND_DAY)
      .build();
    IssueResolutionHistoryQuery sliced = IssueResolutionHistoryQuery.builder(ENTITY_ID, EntityType.PROJECT_BRANCH, FIRST_DAY)
      .endDate(SECOND_DAY)
      .sliceBy("TYPE")
      .build();

    assertThat(issueTtrHistory.queryResolvedIssues(db.getSession(), unsliced))
      .extracting(IssueResolutionHistoryQueryRow::date, IssueResolutionHistoryQueryRow::dimension, IssueResolutionHistoryQueryRow::value)
      .containsExactly(tuple(SECOND_DAY, "all", 4), tuple(FIRST_DAY, "all", 1));
    assertThat(issueTtrHistory.queryMttr(db.getSession(), unsliced))
      .extracting(IssueResolutionHistoryQueryRow::date, IssueResolutionHistoryQueryRow::dimension, IssueResolutionHistoryQueryRow::value)
      .containsExactly(tuple(SECOND_DAY, "all", 26), tuple(FIRST_DAY, "all", 30));
    assertThat(issueTtrHistory.queryRecentMttr(db.getSession(), unsliced))
      .extracting(IssueResolutionHistoryQueryRow::date, IssueResolutionHistoryQueryRow::dimension, IssueResolutionHistoryQueryRow::value)
      .containsExactly(tuple(SECOND_DAY, "all", 10), tuple(FIRST_DAY, "all", 10));
    assertThat(issueTtrHistory.queryResolvedIssues(db.getSession(), sliced))
      .extracting(IssueResolutionHistoryQueryRow::date, IssueResolutionHistoryQueryRow::dimension, IssueResolutionHistoryQueryRow::value)
      .containsExactly(
        tuple(SECOND_DAY, String.valueOf(IssueType.BUG.getDbConstant()), 2),
        tuple(SECOND_DAY, String.valueOf(IssueType.VULNERABILITY.getDbConstant()), 2),
        tuple(FIRST_DAY, String.valueOf(IssueType.BUG.getDbConstant()), 1));

    ProjectIssueResolutionQuery projectQuery = new ProjectIssueResolutionQuery(
      List.of(ENTITY_ID, "branch-2"), SECOND_DAY, noFilters());
    upsertTtr("branch-2", EntityType.PROJECT_BRANCH, SECOND_DAY, bug, 40, 4, 12, 2);
    assertThat(issueTtrHistory.queryProjectResolvedIssues(db.getSession(), projectQuery))
      .containsExactlyInAnyOrder(
        new ProjectIssueResolutionValue(ENTITY_ID, 5L), new ProjectIssueResolutionValue("branch-2", 4L));
    assertThat(issueTtrHistory.queryProjectMttr(db.getSession(), projectQuery))
      .containsExactlyInAnyOrder(
        new ProjectIssueResolutionValue(ENTITY_ID, 26L), new ProjectIssueResolutionValue("branch-2", 10L));
    assertThat(issueTtrHistory.queryProjectRecentMttr(db.getSession(), projectQuery))
      .containsExactlyInAnyOrder(
        new ProjectIssueResolutionValue(ENTITY_ID, 10L), new ProjectIssueResolutionValue("branch-2", 6L));
  }

  @Test
  void issueResolutionAggregation_shouldIncludeExplicitAndMainProjectBranches() {
    IssueCountDimension dimension = insertDimension("java:S600", IssueType.BUG, (short) 2, (short) 3, (short) 4);
    var portfolio = db.components().insertPrivatePortfolioDto("portfolio-history");
    var explicitProject = db.components().insertPrivateProject("explicit-project-history");
    var explicitProjectBranch = db.components().insertProjectBranch(explicitProject.getProjectDto());
    var mainProject = db.components().insertPrivateProject("main-project-history");
    db.components().addPortfolioProject(portfolio, explicitProject.getProjectDto(), mainProject.getProjectDto());
    db.components().addPortfolioProjectBranch(portfolio, explicitProject.getProjectDto(), explicitProjectBranch.getUuid());
    var portfolioComponent = db.getDbClient().componentDao().selectByUuid(db.getSession(), portfolio.getUuid()).orElseThrow();
    db.components().insertComponent(newProjectCopy(db.components().getComponentDto(explicitProjectBranch), portfolioComponent));
    db.components().insertComponent(newProjectCopy(mainProject.getMainBranchComponent(), portfolioComponent));

    upsertTtr(explicitProjectBranch.getUuid(), EntityType.PROJECT_BRANCH, FIRST_DAY, dimension, 10, 1, 0, 0);
    upsertTtr(mainProject.mainBranchUuid(), EntityType.PROJECT_BRANCH, FIRST_DAY, dimension, 20, 2, 0, 0);

    issueTtrHistory.recordIssueTtrHistoryForAggregation(
      db.getSession(), portfolio.getUuid(), EntityType.PORTFOLIO, FIRST_DAY);

    assertThat(issueTtrHistory.queryResolvedIssues(
      db.getSession(), IssueResolutionHistoryQuery.builder(portfolio.getUuid(), EntityType.PORTFOLIO, FIRST_DAY)
        .endDate(FIRST_DAY)
        .build()))
      .extracting(IssueResolutionHistoryQueryRow::value)
      .containsExactly(3);
  }

  @Test
  void issueResolutionAggregation_shouldOnlyIncludeProjectBranchesFromSubportfolioTree() {
    IssueCountDimension dimension = insertDimension("java:S650", IssueType.BUG, (short) 2, (short) 3, (short) 4);
    var portfolio = db.components().insertPrivatePortfolioDto("portfolio-subtree-history");
    var portfolioComponent = db.getDbClient().componentDao().selectByUuid(db.getSession(), portfolio.getUuid()).orElseThrow();
    var subportfolioComponent = db.components().insertSubportfolio(portfolioComponent);
    var parentProject = db.components().insertPrivateProject("parent-project-history");
    var subportfolioProject = db.components().insertPrivateProject("subportfolio-project-history");
    db.components().insertComponent(newProjectCopy(parentProject.getMainBranchComponent(), portfolioComponent));
    db.components().insertComponent(newProjectCopy(subportfolioProject.getMainBranchComponent(), subportfolioComponent));

    upsertTtr(parentProject.mainBranchUuid(), EntityType.PROJECT_BRANCH, FIRST_DAY, dimension, 999, 99, 0, 0);
    upsertTtr(subportfolioProject.mainBranchUuid(), EntityType.PROJECT_BRANCH, FIRST_DAY, dimension, 20, 2, 0, 0);

    issueTtrHistory.recordIssueTtrHistoryForAggregation(
      db.getSession(), subportfolioComponent.uuid(), EntityType.PORTFOLIO, FIRST_DAY);

    assertThat(issueTtrHistory.queryResolvedIssues(
      db.getSession(), IssueResolutionHistoryQuery.builder(subportfolioComponent.uuid(), EntityType.PORTFOLIO, FIRST_DAY)
        .endDate(FIRST_DAY)
        .build()))
      .extracting(IssueResolutionHistoryQueryRow::value)
      .containsExactly(2);
  }

  @Test
  void issueResolutionAggregation_shouldNotCountTheSamePortfolioProjectBranchMoreThanOnce() {
    IssueCountDimension dimension = insertDimension("java:S675", IssueType.BUG, (short) 2, (short) 3, (short) 4);
    var portfolio = db.components().insertPrivatePortfolioDto("portfolio-duplicate-history");
    var portfolioComponent = db.getDbClient().componentDao().selectByUuid(db.getSession(), portfolio.getUuid()).orElseThrow();
    var subportfolioComponent = db.components().insertSubportfolio(portfolioComponent);
    var project = db.components().insertPrivateProject("duplicated-portfolio-project-history");
    db.components().insertComponent(newProjectCopy(project.getMainBranchComponent(), portfolioComponent));
    db.components().insertComponent(newProjectCopy(project.getMainBranchComponent(), subportfolioComponent));

    upsertTtr(project.mainBranchUuid(), EntityType.PROJECT_BRANCH, FIRST_DAY, dimension, 20, 2, 0, 0);

    issueTtrHistory.recordIssueTtrHistoryForAggregation(
      db.getSession(), portfolio.getUuid(), EntityType.PORTFOLIO, FIRST_DAY);

    assertThat(issueTtrHistory.queryResolvedIssues(
      db.getSession(), IssueResolutionHistoryQuery.builder(portfolio.getUuid(), EntityType.PORTFOLIO, FIRST_DAY)
        .endDate(FIRST_DAY)
        .build()))
      .extracting(IssueResolutionHistoryQueryRow::value)
      .containsExactly(2);
  }

  @Test
  void issueResolutionAggregation_shouldNotCountTheSameApplicationProjectBranchMoreThanOnce() {
    IssueCountDimension dimension = insertDimension("java:S680", IssueType.BUG, (short) 2, (short) 3, (short) 4);
    var application = db.components().insertPrivateApplication("application-duplicate-history");
    var project = db.components().insertPrivateProject("duplicated-application-project-history");
    db.components().insertComponent(newProjectCopy(project.getMainBranchComponent(), application.getMainBranchComponent()));
    db.components().insertComponent(newProjectCopy(project.getMainBranchComponent(), application.getMainBranchComponent())
      .setKey("second-application-project-copy"));

    upsertTtr(project.mainBranchUuid(), EntityType.PROJECT_BRANCH, FIRST_DAY, dimension, 20, 2, 0, 0);

    issueTtrHistory.recordIssueTtrHistoryForAggregation(
      db.getSession(), application.mainBranchUuid(), EntityType.APPLICATION, FIRST_DAY);

    assertThat(issueTtrHistory.queryResolvedIssues(
      db.getSession(), IssueResolutionHistoryQuery.builder(application.mainBranchUuid(), EntityType.APPLICATION, FIRST_DAY)
        .endDate(FIRST_DAY)
        .build()))
      .extracting(IssueResolutionHistoryQueryRow::value)
      .containsExactly(2);
  }

  /**
   * The outer portfolio must roll up project-branch rows only, not the aggregate rows of nested entities:
   *
   * <pre>
   * outer portfolio
   * |-- application reference (copy_component_uuid = application branch)
   * |   `-- application project branch (copy_component_uuid = project branch)
   * `-- referenced portfolio (copy_component_uuid = referenced portfolio)
   *     `-- referenced portfolio project branch (copy_component_uuid = project branch)
   *
   * Existing history rows: application aggregate + application project branch,
   *                       referenced portfolio aggregate + referenced project branch
   * Expected outer portfolio: project branches only = 100 minutes / 5 issues
   * </pre>
   */
  @Test
  void issueResolutionAggregation_shouldNotIncludeNestedApplicationOrPortfolioAggregates() {
    IssueCountDimension dimension = insertDimension("java:S690", IssueType.BUG, (short) 2, (short) 3, (short) 4);
    var portfolio = db.components().insertPrivatePortfolioDto("portfolio-nested-aggregate-history");
    var portfolioComponent = db.getDbClient().componentDao().selectByUuid(db.getSession(), portfolio.getUuid()).orElseThrow();

    var application = db.components().insertPrivateApplication("nested-application-history");
    var applicationProject = db.components().insertPrivateProject("nested-application-project-history");
    db.components().addApplicationProject(application.getProjectDto(), applicationProject.getProjectDto());
    db.components().addPortfolioApplicationBranch(portfolio.getUuid(), application.projectUuid(), application.mainBranchUuid());
    var applicationReference = db.components().insertComponent(
      newSubPortfolio(portfolioComponent, "application-reference-uuid", "application-reference")
        .setCopyComponentUuid(application.mainBranchUuid()));
    db.components().insertComponent(newProjectCopy(applicationProject.getMainBranchComponent(), applicationReference));

    var referencedPortfolio = db.components().insertPrivatePortfolioDto("referenced-portfolio-history");
    var referencedPortfolioProject = db.components().insertPrivateProject("referenced-portfolio-project-history");
    db.components().addPortfolioReference(portfolio, referencedPortfolio);
    db.components().addPortfolioProject(referencedPortfolio, referencedPortfolioProject.getProjectDto());
    var portfolioReference = db.components().insertComponent(
      newSubPortfolio(portfolioComponent, "portfolio-reference-uuid", "portfolio-reference")
        .setCopyComponentUuid(referencedPortfolio.getUuid()));
    db.components().insertComponent(newProjectCopy(referencedPortfolioProject.getMainBranchComponent(), portfolioReference));

    // These aggregate rows are already present because the application and referenced portfolio are refreshed independently.
    upsertTtr(application.mainBranchUuid(), EntityType.APPLICATION, FIRST_DAY, dimension, 30, 2, 20, 1);
    upsertTtr(applicationProject.mainBranchUuid(), EntityType.PROJECT_BRANCH, FIRST_DAY, dimension, 30, 2, 20, 1);
    upsertTtr(referencedPortfolio.getUuid(), EntityType.PORTFOLIO, FIRST_DAY, dimension, 70, 3, 40, 2);
    upsertTtr(referencedPortfolioProject.mainBranchUuid(), EntityType.PROJECT_BRANCH, FIRST_DAY, dimension, 70, 3, 40, 2);

    issueTtrHistory.recordIssueTtrHistoryForAggregation(
      db.getSession(), portfolio.getUuid(), EntityType.PORTFOLIO, FIRST_DAY);

    Map<String, Object> aggregatedRow = db.selectFirst(db.getSession(),
      "select total_minutes_to_resolution as \"totalMinutes\", issues_resolved as \"issuesResolved\", " +
        "total_minutes_to_resolution_recent as \"recentTotalMinutes\", issues_resolved_recent as \"recentIssuesResolved\" " +
        "from issue_ttr_history where entity_id = '" + portfolio.getUuid() + "' and entity_type = '" + EntityType.PORTFOLIO.name() +
        "' and dimension_id = " + dimension.id() + " and recorded_at_epoch = " + FIRST_DAY.toEpochMilli());

    assertThat(((Number) aggregatedRow.get("totalMinutes")).longValue()).isEqualTo(100L);
    assertThat(((Number) aggregatedRow.get("issuesResolved")).intValue()).isEqualTo(5);
    assertThat(((Number) aggregatedRow.get("recentTotalMinutes")).longValue()).isEqualTo(60L);
    assertThat(((Number) aggregatedRow.get("recentIssuesResolved")).intValue()).isEqualTo(3);
  }

  @Test
  void issueResolutionAggregation_shouldOnlyResolveProjectCopyComponentsAsBranches() {
    var portfolio = db.components().insertPrivatePortfolioDto("portfolio-tree-history");
    var portfolioComponent = db.getDbClient().componentDao().selectByUuid(db.getSession(), portfolio.getUuid()).orElseThrow();
    var subportfolioComponent = db.components().insertSubportfolio(portfolioComponent);
    var portfolioProject = db.components().insertPrivateProject("portfolio-tree-project-history");
    db.components().insertComponent(newProjectCopy(portfolioProject.getMainBranchComponent(), subportfolioComponent));

    var application = db.components().insertPrivateApplication("application-tree-history");
    var applicationProject = db.components().insertPrivateProject("application-tree-project-history");
    db.components().insertComponent(newProjectCopy(applicationProject.getMainBranchComponent(), application.getMainBranchComponent()));

    var mapper = db.getSession().getMapper(IssueTtrHistoryMapperFragmentsTestMapper.class);
    assertThat(mapper.countAggregationBranches(portfolio.getUuid(), EntityType.PORTFOLIO)).isOne();
    assertThat(mapper.countAggregationBranches(application.mainBranchUuid(), EntityType.APPLICATION)).isOne();
  }

  @Test
  void issueResolutionAggregation_shouldIncludeExplicitAndMainApplicationBranches() {
    IssueCountDimension dimension = insertDimension("java:S700", IssueType.BUG, (short) 2, (short) 3, (short) 4);
    var portfolio = db.components().insertPrivatePortfolioDto("portfolio-application-history");

    var explicitApplication = db.components().insertPrivateApplication("explicit-application-history");
    var explicitProject = db.components().insertPrivateProject("explicit-application-project-history");
    var explicitApplicationBranch = db.components().insertProjectBranch(explicitApplication.getProjectDto());
    var explicitProjectBranch = db.components().insertProjectBranch(explicitProject.getProjectDto());
    db.components().addApplicationProject(explicitApplication.getProjectDto(), explicitProject.getProjectDto());
    db.components().addProjectBranchToApplicationBranch(explicitApplicationBranch, explicitProjectBranch);
    db.components().addPortfolioApplicationBranch(
      portfolio.getUuid(), explicitApplication.projectUuid(), explicitApplicationBranch.getUuid());

    var mainApplication = db.components().insertPrivateApplication("main-application-history");
    var mainProject = db.components().insertPrivateProject("main-application-project-history");
    db.components().addApplicationProject(mainApplication.getProjectDto(), mainProject.getProjectDto());
    db.components().addPortfolioApplicationBranch(
      portfolio.getUuid(), mainApplication.projectUuid(), mainApplication.mainBranchUuid());
    var portfolioComponent = db.getDbClient().componentDao().selectByUuid(db.getSession(), portfolio.getUuid()).orElseThrow();
    db.components().insertComponent(newProjectCopy(db.components().getComponentDto(explicitProjectBranch), portfolioComponent));
    db.components().insertComponent(newProjectCopy(mainProject.getMainBranchComponent(), portfolioComponent));

    upsertTtr(explicitProjectBranch.getUuid(), EntityType.PROJECT_BRANCH, FIRST_DAY, dimension, 10, 1, 0, 0);
    upsertTtr(mainProject.mainBranchUuid(), EntityType.PROJECT_BRANCH, FIRST_DAY, dimension, 20, 2, 0, 0);

    issueTtrHistory.recordIssueTtrHistoryForAggregation(
      db.getSession(), portfolio.getUuid(), EntityType.PORTFOLIO, FIRST_DAY);

    assertThat(issueTtrHistory.queryResolvedIssues(
      db.getSession(), IssueResolutionHistoryQuery.builder(portfolio.getUuid(), EntityType.PORTFOLIO, FIRST_DAY)
        .endDate(FIRST_DAY)
        .build()))
      .extracting(IssueResolutionHistoryQueryRow::value)
      .containsExactly(3);
  }

  private IssueCountDimension insertDimension(String ruleKey, IssueType issueType, short maintainability, short security, short reliability) {
    return dimensions.getOrCreate(db.getSession(), List.of(new IssueCountDimensionKey(
      null,
      "MAIN",
      "CRITICAL",
      "OPEN",
      "TO_REVIEW",
      issueType.getDbConstant(),
      ruleKey,
      maintainability,
      security,
      reliability))).iterator().next();
  }

  private MeasureKeyMapping metric(String name, String type) {
    return measureKeys.getOrCreate(db.getSession(), Map.of(name, type)).iterator().next();
  }

  private void upsertTtr(
    String entityId,
    EntityType entityType,
    Instant recordedAt,
    IssueCountDimension dimension,
    long totalMinutes,
    int issuesResolved,
    long recentTotalMinutes,
    int recentIssuesResolved) {
    issueTtrHistory.upsert(db.getSession(), new IssueTtrHistory(
      entityId,
      entityType,
      dimension.id(),
      recordedAt,
      totalMinutes,
      issuesResolved,
      recentTotalMinutes,
      recentIssuesResolved));
  }

  private static IssueCountHistoryFilters noFilters() {
    return new IssueCountHistoryFilters(null, null, null, null, null);
  }

  private static IssueCountHistoryQuery queryWithSlice(String sliceBy) {
    return IssueCountHistoryQuery.builder(ENTITY_ID, EntityType.PROJECT_BRANCH, FIRST_DAY)
      .endDate(SECOND_DAY)
      .sliceBy(sliceBy)
      .build();
  }

  private static class AggregationBranchesMyBatisConfExtension implements BaseMyBatisConfExtension {
    @Override
    public Stream<Class<?>> getMapperClasses() {
      return Stream.of(IssueTtrHistoryMapperFragmentsTestMapper.class);
    }
  }
}
