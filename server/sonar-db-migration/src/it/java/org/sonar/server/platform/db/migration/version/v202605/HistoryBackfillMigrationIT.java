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
package org.sonar.server.platform.db.migration.version.v202605;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/** Exercises the raw JDBC history backfill against the portable H2 migration schema. */
class HistoryBackfillMigrationIT {

  private static final long NOW = Instant.parse("2026-09-09T12:34:56Z").toEpochMilli();
  private static final long FROZEN_EPOCH = Instant.parse("2026-09-09T00:00:00Z").toEpochMilli();

  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(BackfillProjectBranchHistory.class);

  private final DataChange persistEpoch = new PersistHistoryBackfillUtcDayEpoch(db.database(), system2);
  private final DataChange backfillProjects = new BackfillProjectBranchHistory(db.database());

  @BeforeEach
  void setUp() throws SQLException {
    executeUpdate("delete from internal_properties where kee = ?", PersistHistoryBackfillUtcDayEpoch.FROZEN_EPOCH_PROPERTY);
    persistEpoch.execute();
  }

  @Test
  void backfillsCurrentProjectIssueCountsAndTrackedMeasures_withoutTtrHistory() throws SQLException {
    insertProject("project-1");
    insertBranch("project-1", "project-main", true, "TRK");
    insertProcessedSnapshot("project-main", "project-analysis", true);
    insertRule("rule-1", "S100");
    insertFile("project-main", "main-file", "FIL");
    insertFile("project-main", "test-file", "UTS");
    insertIssue("open-main", "rule-1", "project-main", "main-file", 2, "CRITICAL", "RESOLVED", "FALSE-POSITIVE");
    insertIssue("open-test", "rule-1", "project-main", "test-file", 1, "MAJOR", "OPEN", null);
    insertIssue("closed", "rule-1", "project-main", "main-file", 2, "BLOCKER", "CLOSED", "FIXED");
    insertRuleDefaultImpact("rule-1", "SECURITY", "MEDIUM");
    insertIssueImpact("open-main", "SECURITY", "HIGH");
    insertMetric("metric-ncloc", "ncloc", "INT");
    insertMetric("metric-coverage", "coverage", "PERCENT");
    insertMetric("metric-debt", "new_technical_debt", "WORK_DUR");
    insertMetric("metric-alert", "alert_status", "LEVEL");
    insertMetric("metric-duplicated-blocks", "new_duplicated_blocks", "INT");
    insertMetric("metric-technical-debt", "sqale_index", "WORK_DUR");
    insertMetric("metric-security-rating", "security_rating", "RATING");
    insertCurrentMeasures("project-main", "{\"ncloc\":42.0,\"coverage\":80.5,\"new_technical_debt\":123.0,\"alert_status\":\"OK\",\"new_duplicated_blocks\":3.0,\"sqale_index\":456.0,\"security_rating\":1.0,\"custom_metric\":17.0}");

    backfillProjects.execute();

    assertThat(projectIssueHistory("project-main"))
      .extracting(row -> tuple(
        number(row, "ISSUE_COUNT"), row.get("ISSUE_CODE_SCOPE"), row.get("ISSUE_SEVERITY"), row.get("ISSUE_STATUS"),
        row.get("STATUS"), row.get("RULE_KEY"), number(row, "SECURITY_RATING"), number(row, "RECORDED_AT_EPOCH")))
      .containsExactlyInAnyOrder(
        tuple(1L, "MAIN", "CRITICAL", "FALSE_POSITIVE", "RESOLVED", "java:S100", 4L, FROZEN_EPOCH),
        tuple(1L, "TEST", "MAJOR", "OPEN", "OPEN", "java:S100", 3L, FROZEN_EPOCH));
    assertThat(projectMeasureHistory("project-main"))
      .extracting(row -> tuple(row.get("METRIC_NAME"), row.get("METRIC_TYPE"), row.get("TEXT_VALUE"), number(row, "RECORDED_AT_EPOCH")))
      .containsExactlyInAnyOrder(
        tuple("alert_status", "LEVEL", "OK", FROZEN_EPOCH),
        tuple("coverage", "PERCENT", "80.5", FROZEN_EPOCH),
        tuple("ncloc", "INT", "42", FROZEN_EPOCH),
        tuple("new_duplicated_blocks", "INT", "3", FROZEN_EPOCH),
        tuple("sqale_index", "WORK_DUR", "456", FROZEN_EPOCH),
        tuple("security_rating", "RATING", "1.0", FROZEN_EPOCH),
        tuple("new_technical_debt", "WORK_DUR", "123", FROZEN_EPOCH));
    assertNoTtrHistoryWrites();
  }

  @Test
  void aggregatesDefaultAndOverrideImpactsOncePerIssue_andExcludesHotspotsClosedAndNullStatuses() throws SQLException {
    insertProjectWithProcessedBranch("aggregation-project", "aggregation-main", true);
    insertRule("aggregation-rule", "S400");
    insertFile("aggregation-main", "aggregation-file", "FIL");
    insertRuleDefaultImpact("aggregation-rule", "MAINTAINABILITY", "MEDIUM");
    insertRuleDefaultImpact("aggregation-rule", "SECURITY", "HIGH");
    insertIssue("default-one", "aggregation-rule", "aggregation-main", "aggregation-file", 2, "MAJOR", "OPEN", null);
    insertIssue("default-two", "aggregation-rule", "aggregation-main", "aggregation-file", 2, "MAJOR", "OPEN", null);
    insertIssue("partial-override", "aggregation-rule", "aggregation-main", "aggregation-file", 2, "MAJOR", "OPEN", null);
    insertIssueImpact("partial-override", "MAINTAINABILITY", "CRITICAL");
    for (String issueKey : List.of("override-one", "override-two")) {
      insertIssue(issueKey, "aggregation-rule", "aggregation-main", "aggregation-file", 2, "MAJOR", "OPEN", null);
      insertIssueImpact(issueKey, "MAINTAINABILITY", "LOW");
      insertIssueImpact(issueKey, "SECURITY", "HIGH");
    }
    insertIssue("hotspot", "aggregation-rule", "aggregation-main", "aggregation-file", 4, "MAJOR", "OPEN", null);
    insertIssue("closed", "aggregation-rule", "aggregation-main", "aggregation-file", 2, "MAJOR", "CLOSED", "FIXED");
    insertIssue("null-status", "aggregation-rule", "aggregation-main", "aggregation-file", 2, "MAJOR", null, null);

    backfillProjects.execute();

    assertThat(projectIssueDimensions("aggregation-main"))
      .extracting(row -> tuple(
        number(row, "ISSUE_COUNT"), number(row, "ISSUE_TYPE"), row.get("ISSUE_CODE_SCOPE"), row.get("ISSUE_SEVERITY"),
        row.get("ISSUE_STATUS"), row.get("STATUS"), row.get("RULE_KEY"), number(row, "MAINTAINABILITY_RATING"),
        number(row, "SECURITY_RATING"), number(row, "RELIABILITY_RATING")))
      .containsExactlyInAnyOrder(
        tuple(2L, 2L, "MAIN", "MAJOR", "OPEN", "OPEN", "java:S400", 3L, 4L, 0L),
        tuple(2L, 2L, "MAIN", "MAJOR", "OPEN", "OPEN", "java:S400", 2L, 4L, 0L),
        tuple(1L, 2L, "MAIN", "MAJOR", "OPEN", "OPEN", "java:S400", 4L, 0L, 0L));
  }

  @Test
  void backfillsScaMeasures_andReplaysWithoutDuplicatesOrTtrHistory() throws SQLException {
    insertProject("sca-entity");
    insertBranch("sca-entity", "sca-main", true, "TRK");
    insertProcessedSnapshot("sca-main", "sca-analysis", true);
    for (String metricKey : List.of(
      "new_sca_count_any_issue", "new_sca_count_any_security", "new_sca_count_licensing", "new_sca_count_malware", "new_sca_count_vulnerability",
      "sca_count_any_issue", "sca_count_any_security", "sca_count_licensing", "sca_count_malware", "sca_count_vulnerability")) {
      insertMetric(metricKey, metricKey, "INT");
    }
    insertMetric("sca-rating", "sca_rating_any_issue", "RATING");
    insertMetric("new-sca-rating", "new_sca_rating_any_issue", "RATING");
    insertMetric("sca-ncloc", "ncloc", "INT");
    insertMetric("sca-custom", "custom_metric", "INT");
    insertMetric("sca-untracked-rating", "sca_rating_vulnerability", "RATING");
    insertCurrentMeasures("sca-main", """
      {
        "new_sca_count_any_issue":10.0,
        "new_sca_count_any_security":7.0,
        "new_sca_count_licensing":3.0,
        "new_sca_count_malware":0.0,
        "new_sca_count_vulnerability":7.0,
        "new_sca_rating_any_issue":1.0,
        "sca_count_any_issue":25.0,
        "sca_count_any_security":20.0,
        "sca_count_licensing":5.0,
        "sca_count_malware":2.0,
        "sca_count_vulnerability":18.0,
        "sca_rating_any_issue":5.0,
        "ncloc":42.0,
        "custom_metric":99.0,
        "sca_rating_vulnerability":4.0
      }
      """);
    List<Tuple> expectedMeasures = List.of(
      tuple("new_sca_count_any_issue", "INT", "10"),
      tuple("new_sca_count_any_security", "INT", "7"),
      tuple("new_sca_count_licensing", "INT", "3"),
      tuple("new_sca_count_malware", "INT", "0"),
      tuple("new_sca_count_vulnerability", "INT", "7"),
      tuple("new_sca_rating_any_issue", "RATING", "1.0"),
      tuple("sca_count_any_issue", "INT", "25"),
      tuple("sca_count_any_security", "INT", "20"),
      tuple("sca_count_licensing", "INT", "5"),
      tuple("sca_count_malware", "INT", "2"),
      tuple("sca_count_vulnerability", "INT", "18"),
      tuple("sca_rating_any_issue", "RATING", "5.0"),
      tuple("ncloc", "INT", "42"));

    for (int attempt = 0; attempt < 2; attempt++) {
      backfillProjects.execute();

      List<Map<String, Object>> history = db.select("""
        select h.entity_type, h.recorded_at_epoch, h.text_value, m.metric_name, m.metric_type
        from measure_history h
        join measure_key_mapping m on m.id = h.metric_id
        where h.entity_id = 'sca-main'
        """);
      assertThat(history)
        .extracting(row -> tuple(row.get("METRIC_NAME"), row.get("METRIC_TYPE"), row.get("TEXT_VALUE")))
        .containsExactlyInAnyOrderElementsOf(expectedMeasures);
      assertThat(history).allSatisfy(row -> assertThat(row)
        .containsEntry("ENTITY_TYPE", "PROJECT_BRANCH")
        .containsEntry("RECORDED_AT_EPOCH", FROZEN_EPOCH));
      assertThat(db.countRowsOfTable("measure_key_mapping")).isEqualTo(expectedMeasures.size());
      assertNoTtrHistoryWrites();
    }
  }

  @ParameterizedTest
  @CsvSource({
    "FIL, MAIN, FALSE-POSITIVE, FALSE_POSITIVE",
    "FIL, MAIN, WONTFIX, ACCEPTED",
    "FIL, MAIN, FIXED, FIXED",
    "UTS, TEST, FALSE-POSITIVE, FALSE_POSITIVE",
    "UTS, TEST, WONTFIX, ACCEPTED",
    "UTS, TEST, FIXED, FIXED"
  })
  void retainsIssuesWithoutImpacts_andMapsResolvedStatusesAndCodeScopes(String qualifier, String codeScope, String resolution, String issueStatus)
    throws SQLException {
    insertProjectWithProcessedBranch("no-impact-project", "no-impact-main", true);
    insertRule("no-impact-rule", "S401");
    insertFile("no-impact-main", "no-impact-file", qualifier);
    insertIssue("no-impact-issue", "no-impact-rule", "no-impact-main", "no-impact-file", 1, "MINOR", "RESOLVED", resolution);

    backfillProjects.execute();

    assertThat(projectIssueDimensions("no-impact-main"))
      .extracting(row -> tuple(
        number(row, "ISSUE_COUNT"), number(row, "ISSUE_TYPE"), row.get("ISSUE_CODE_SCOPE"), row.get("ISSUE_SEVERITY"),
        row.get("ISSUE_STATUS"), row.get("STATUS"), row.get("RULE_KEY"), number(row, "MAINTAINABILITY_RATING"),
        number(row, "SECURITY_RATING"), number(row, "RELIABILITY_RATING")))
      .containsExactly(tuple(1L, 1L, codeScope, "MINOR", issueStatus, "RESOLVED", "java:S401", 0L, 0L, 0L));
  }

  @Test
  void excludesPullRequestBranchesFromProjectHistoryBackfill() throws SQLException {
    insertProject("project-branches");
    insertBranch("project-branches", "project-normal", true, "TRK");
    insertBranch("project-branches", "project-pull-request", false, "TRK", "PULL_REQUEST");
    insertProcessedSnapshot("project-normal", "project-normal-analysis", true);
    insertProcessedSnapshot("project-pull-request", "project-pull-request-analysis", true);
    insertRule("project-branch-rule", "S101");
    insertIssueOnNewFile("project-normal", "project-normal-issue", "project-branch-rule");
    insertIssueOnNewFile("project-pull-request", "project-pull-request-issue", "project-branch-rule");
    insertMetric("project-branch-ncloc", "ncloc", "INT");
    insertCurrentMeasures("project-normal", "{\"ncloc\":42.0}");
    insertCurrentMeasures("project-pull-request", "{\"ncloc\":84.0}");

    backfillProjects.execute();

    assertThat(projectIssueHistory("project-normal")).hasSize(1);
    assertThat(projectMeasureHistory("project-normal")).hasSize(1);
    assertThat(projectIssueHistory("project-pull-request")).isEmpty();
    assertThat(projectMeasureHistory("project-pull-request")).isEmpty();
  }

  @Test
  void backfillsCanonicalRuleKeysLongerThanTheLegacyHistoryColumn() throws SQLException {
    String repository = "r".repeat(255);
    String rule = "k".repeat(200);
    String canonicalRuleKey = repository + ":" + rule;
    insertProject("long-rule-project");
    insertBranch("long-rule-project", "long-rule-main", true, "TRK");
    insertProcessedSnapshot("long-rule-main", "long-rule-analysis", true);
    insertRule("long-rule", repository, rule);
    insertIssueOnNewFile("long-rule-main", "long-rule-issue", "long-rule");

    backfillProjects.execute();

    assertThat(canonicalRuleKey).hasSizeGreaterThan(200);
    assertThat(projectIssueHistory("long-rule-main"))
      .extracting(row -> row.get("RULE_KEY"))
      .containsExactly(canonicalRuleKey);
  }

  @Test
  void backfillsOversizedTrackedMeasureValuesWithoutTruncationOrDuplicateRows() throws SQLException {
    String oversizedValue = "a".repeat(100_001);
    insertProject("oversized-measure-project");
    insertBranch("oversized-measure-project", "oversized-measure-main", true, "TRK");
    insertProcessedSnapshot("oversized-measure-main", "oversized-measure-analysis", true);
    insertMetric("oversized-language-distribution", "ncloc_language_distribution", "DATA");
    insertCurrentMeasures("oversized-measure-main", "{\"ncloc_language_distribution\":\"" + oversizedValue + "\"}");

    backfillProjects.execute();
    backfillProjects.execute();

    assertThat(readMeasureHistoryText("oversized-measure-main", "ncloc_language_distribution")).isEqualTo(oversizedValue);
    assertThat(db.select("select metric_id from measure_history where entity_id = 'oversized-measure-main'")).hasSize(1);
  }

  @Test
  void skipsProjectBranchesWithoutALatestProcessedAnalysis() throws SQLException {
    insertProject("project-unprocessed");
    insertBranch("project-unprocessed", "project-unprocessed-main", true, "TRK");
    insertProcessedSnapshot("project-unprocessed-main", "project-unprocessed-analysis", false);
    insertProject("project-disabled");
    insertBranch("project-disabled", "project-disabled-main", true, "TRK");
    insertProcessedSnapshot("project-disabled-main", "project-disabled-analysis", true);
    executeUpdate("update components set enabled = ? where uuid = ?", false, "project-disabled-main");
    insertRule("rule-unprocessed", "S101");
    insertFile("project-unprocessed-main", "unprocessed-file", "FIL");
    insertIssue("unprocessed-issue", "rule-unprocessed", "project-unprocessed-main", "unprocessed-file", 2, "MAJOR", "OPEN", null);
    insertCurrentMeasures("project-unprocessed-main", "{\"ncloc\":42.0}");

    backfillProjects.execute();

    assertThat(db.countRowsOfTable("issue_count_history")).isZero();
    assertThat(db.countRowsOfTable("measure_history")).isZero();
    assertThat(db.countRowsOfTable("measure_key_mapping")).isZero();
  }

  @Test
  void doesNotBackfillPortfolioHistory() throws SQLException {
    insertRootProject("portfolio-1", "VW");
    insertBranch("portfolio-1", "portfolio-main", true, "VW");
    insertProcessedSnapshot("portfolio-main", "portfolio-analysis", true);
    insertMetric("portfolio-ncloc", "ncloc", "INT");
    insertCurrentMeasures("portfolio-main", "{\"ncloc\":42.0}");

    backfillProjects.execute();

    assertThat(db.countRowsOfTable("issue_count_dimensions")).isZero();
    assertThat(db.countRowsOfTable("issue_count_history")).isZero();
    assertThat(db.countRowsOfTable("measure_key_mapping")).isZero();
    assertThat(db.countRowsOfTable("measure_history")).isZero();
    assertNoTtrHistoryWrites();
  }

  @Test
  void sameDayRetryIsIdempotent_andWritesZeroForADisappearedDimension() throws SQLException {
    insertProjectWithProcessedBranch("retry-project", "retry-main", true);
    insertRule("retry-rule", "S300");
    insertIssueOnNewFile("retry-main", "retry-issue", "retry-rule");
    insertMetric("retry-ncloc", "ncloc", "INT");
    insertCurrentMeasures("retry-main", "{\"ncloc\":1.0}");

    backfillProjects.execute();
    backfillProjects.execute();

    assertThat(projectIssueHistory("retry-main")).hasSize(1);
    assertThat(projectMeasureHistory("retry-main")).hasSize(1);
    executeUpdate("update issues set status = 'CLOSED' where kee = ?", "retry-issue");

    backfillProjects.execute();
    backfillProjects.execute();

    assertThat(projectIssueHistory("retry-main"))
      .extracting(row -> tuple(number(row, "ISSUE_COUNT"), number(row, "RECORDED_AT_EPOCH")))
      .containsExactly(tuple(0L, FROZEN_EPOCH));
    assertThat(projectMeasureHistory("retry-main")).hasSize(1);
    assertThat(db.select("select text_value from internal_properties where kee = '" + PersistHistoryBackfillUtcDayEpoch.FROZEN_EPOCH_PROPERTY + "'"))
      .extracting(row -> row.get("TEXT_VALUE"))
      .containsExactly(Long.toString(FROZEN_EPOCH));
  }

  @Test
  void flushesBoundedWriteBatchesDuringALargeBranchBackfill() throws SQLException {
    insertProject("batch-project");
    insertBranch("batch-project", "batch-main", true, "TRK");
    insertProcessedSnapshot("batch-main", "batch-analysis", true);
    insertFile("batch-main", "batch-file", "FIL");
    for (int index = 0; index <= 200; index++) {
      String ruleUuid = "batch-rule-" + index;
      insertRule(ruleUuid, "S" + index);
      insertIssue("batch-issue-" + index, ruleUuid, "batch-main", "batch-file", 2, "MAJOR", "OPEN", null);
    }

    backfillProjects.execute();

    assertThat(db.select("select dimension_id from issue_count_history where entity_id = 'batch-main'")).hasSize(201);
    assertThat(db.countRowsOfTable("issue_count_dimensions")).isEqualTo(201);
  }

  private void insertProjectWithProcessedBranch(String projectUuid, String branchUuid, boolean isMain) {
    if (db.select("select uuid from projects where uuid = '" + projectUuid + "'").isEmpty()) {
      insertProject(projectUuid);
    }
    insertBranch(projectUuid, branchUuid, isMain, "TRK");
    insertProcessedSnapshot(branchUuid, branchUuid + "-analysis", true);
  }

  private void insertProject(String uuid) {
    insertRootProject(uuid, "TRK");
  }

  private void insertRootProject(String uuid, String qualifier) {
    db.executeInsert("projects",
      "uuid", uuid,
      "kee", uuid,
      "qualifier", qualifier,
      "private", true,
      "creation_method", "LOCAL_API",
      "contains_ai_code", false,
      "detected_ai_code", false,
      "ai_code_fix_enabled", false,
      "created_at", NOW,
      "updated_at", NOW);
  }

  private void insertBranch(String projectUuid, String branchUuid, boolean isMain, String qualifier) {
    insertBranch(projectUuid, branchUuid, isMain, qualifier, "BRANCH");
  }

  private void insertBranch(String projectUuid, String branchUuid, boolean isMain, String qualifier, String branchType) {
    db.executeInsert("project_branches",
      "uuid", branchUuid,
      "project_uuid", projectUuid,
      "kee", branchUuid,
      "branch_type", branchType,
      "created_at", NOW,
      "updated_at", NOW,
      "exclude_from_purge", false,
      "need_issue_sync", false,
      "is_main", isMain);
    db.executeInsert("components",
      "uuid", branchUuid,
      "kee", branchUuid,
      "enabled", true,
      "private", true,
      "qualifier", qualifier,
      "scope", "PRJ",
      "uuid_path", branchUuid + ".",
      "branch_uuid", branchUuid);
  }

  private void insertFile(String branchUuid, String fileUuid, String qualifier) {
    db.executeInsert("components",
      "uuid", fileUuid,
      "kee", fileUuid,
      "enabled", true,
      "private", true,
      "qualifier", qualifier,
      "scope", "FIL",
      "uuid_path", branchUuid + "." + fileUuid + ".",
      "branch_uuid", branchUuid);
  }

  private void insertProcessedSnapshot(String branchUuid, String analysisUuid, boolean isLast) {
    db.executeInsert("snapshots",
      "uuid", analysisUuid,
      "root_component_uuid", branchUuid,
      "status", "P",
      "islast", isLast,
      "purged", false,
      "created_at", NOW - 7 * 86_400_000L);
  }

  private void insertRule(String ruleUuid, String ruleKey) {
    insertRule(ruleUuid, "java", ruleKey);
  }

  private void insertRule(String ruleUuid, String repository, String ruleKey) {
    db.executeInsert("rules",
      "uuid", ruleUuid,
      "plugin_rule_key", ruleKey,
      "plugin_name", repository,
      "scope", "MAIN",
      "is_template", false,
      "is_ad_hoc", false,
      "is_external", false);
  }

  private void insertIssue(
    String issueKey,
    String ruleUuid,
    String branchUuid,
    String componentUuid,
    int issueType,
    String severity,
    String status,
    String resolution) {
    db.executeInsert("issues",
      "kee", issueKey,
      "rule_uuid", ruleUuid,
      "severity", severity,
      "manual_severity", false,
      "status", status,
      "resolution", resolution,
      "component_uuid", componentUuid,
      "project_uuid", branchUuid,
      "issue_type", issueType,
      "from_sonarqube_update", false);
  }

  private void insertIssueOnNewFile(String branchUuid, String issueKey, String ruleUuid) {
    String fileUuid = issueKey + "-file";
    insertFile(branchUuid, fileUuid, "FIL");
    insertIssue(issueKey, ruleUuid, branchUuid, fileUuid, 2, "MAJOR", "OPEN", null);
  }

  private void insertRuleDefaultImpact(String ruleUuid, String quality, String severity) {
    db.executeInsert("rules_default_impacts",
      "rule_uuid", ruleUuid,
      "software_quality", quality,
      "severity", severity);
  }

  private void insertIssueImpact(String issueKey, String quality, String severity) {
    db.executeInsert("issues_impacts",
      "issue_key", issueKey,
      "software_quality", quality,
      "severity", severity,
      "manual_severity", false);
  }

  private void insertMetric(String uuid, String name, String valueType) {
    db.executeInsert("metrics",
      "uuid", uuid,
      "name", name,
      "direction", 0,
      "qualitative", false,
      "val_type", valueType);
  }

  private void insertCurrentMeasures(String branchUuid, String json) {
    db.executeInsert("measures",
      "component_uuid", branchUuid,
      "branch_uuid", branchUuid,
      "json_value", json,
      "json_value_hash", 0L,
      "created_at", NOW,
      "updated_at", NOW);
  }

  private List<Map<String, Object>> projectIssueHistory(String entityUuid) {
    return db.select("""
      select h.issue_count, h.recorded_at_epoch, d.issue_code_scope, d.issue_severity, d.issue_status,
        d.status, d.rule_key, d.security_rating
      from issue_count_history h
      join issue_count_dimensions d on d.id = h.dimension_id
      where h.entity_id = '%s' and h.entity_type = 'PROJECT_BRANCH'
      """.formatted(entityUuid));
  }

  private List<Map<String, Object>> projectIssueDimensions(String entityUuid) {
    return db.select("""
      select h.issue_count, d.issue_type, d.issue_code_scope, d.issue_severity, d.issue_status, d.status,
        d.rule_key, d.maintainability_rating, d.security_rating, d.reliability_rating
      from issue_count_history h
      join issue_count_dimensions d on d.id = h.dimension_id
      where h.entity_id = '%s' and h.entity_type = 'PROJECT_BRANCH'
      """.formatted(entityUuid));
  }

  private List<Map<String, Object>> projectMeasureHistory(String entityUuid) {
    return db.select("""
      select h.recorded_at_epoch, h.text_value, m.metric_name, m.metric_type
      from measure_history h
      join measure_key_mapping m on m.id = h.metric_id
      where h.entity_id = '%s' and h.entity_type = 'PROJECT_BRANCH'
      """.formatted(entityUuid));
  }

  private void executeUpdate(String sql, String value) throws SQLException {
    try (Connection connection = db.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
      connection.setAutoCommit(false);
      statement.setString(1, value);
      statement.executeUpdate();
      connection.commit();
    }
  }

  private void executeUpdate(String sql, boolean booleanValue, String stringValue) throws SQLException {
    try (Connection connection = db.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
      connection.setAutoCommit(false);
      statement.setBoolean(1, booleanValue);
      statement.setString(2, stringValue);
      statement.executeUpdate();
      connection.commit();
    }
  }

  private static long number(Map<String, Object> row, String column) {
    return ((Number) row.get(column)).longValue();
  }

  private void assertNoTtrHistoryWrites() {
    assertThat(db.countRowsOfTable("issue_ttr_history")).isZero();
    assertThat(db.countRowsOfTable("sca_ttr_history")).isZero();
  }

  private String readMeasureHistoryText(String entityUuid, String metricName) throws SQLException {
    try (Connection connection = db.openConnection();
      PreparedStatement statement = connection.prepareStatement("""
        select history.text_value
        from measure_history history
        join measure_key_mapping mapping on mapping.id = history.metric_id
        where history.entity_id = ? and mapping.metric_name = ?
        """)) {
      statement.setString(1, entityUuid);
      statement.setString(2, metricName);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        try (Reader reader = resultSet.getCharacterStream(1)) {
          StringBuilder result = new StringBuilder();
          char[] buffer = new char[4_096];
          int read;
          while ((read = reader.read(buffer)) != -1) {
            result.append(buffer, 0, read);
          }
          return result.toString();
        } catch (IOException e) {
          throw new SQLException("Failed to read oversized measure history", e);
        }
      }
    }
  }
}
