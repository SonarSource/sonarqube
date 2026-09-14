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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.rule.RuleType;
import org.sonar.db.Database;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;

/**
 * JDBC implementation for the one-time project branch history backfill step.
 *
 * <p>The SQL deliberately avoids dialect-specific JSON and upsert operators. It runs while the database migration
 * lock is held, uses bounded JDBC batches, and uses the history tables' unique keys with explicit reads before
 * updates or inserts for retry safety.
 */
final class HistoryBackfillSupport {

  private static final Logger LOGGER = LoggerFactory.getLogger(HistoryBackfillSupport.class);
  private static final Gson GSON = new Gson();
  private static final int WRITE_BATCH_SIZE = 200;
  private static final int DIMENSION_CACHE_SIZE = 10_000;
  private static final int PROGRESS_INTERVAL = 100;
  private static final int INVALID_ISSUE_LOG_LIMIT = 10;
  private static final long MILLIS_PER_DAY = 86_400_000L;
  private static final String PROJECT_BRANCH_ENTITY_TYPE = "PROJECT_BRANCH";
  private static final String UNIT_TEST_FILE_QUALIFIER = "UTS";
  private static final String CLOSED_STATUS = "CLOSED";
  private static final int SECURITY_HOTSPOT_TYPE = RuleType.SECURITY_HOTSPOT.getDbConstant();

  /*
   * This immutable copy of the history consumer catalogue keeps a released migration independent from future
   * history-core changes. It intentionally excludes custom metrics and portfolio-only computed outputs.
   */
  // The fixed history catalogue retains legacy CoreMetrics keys intentionally.
  @SuppressWarnings("deprecation")
  private static final Set<String> TRACKED_MEASURE_KEYS = Set.of(
    CoreMetrics.ALERT_STATUS_KEY,
    CoreMetrics.BRANCH_COVERAGE_KEY,
    CoreMetrics.COMMENT_LINES_KEY,
    CoreMetrics.COMMENT_LINES_DENSITY_KEY,
    CoreMetrics.CONDITIONS_TO_COVER_KEY,
    CoreMetrics.COVERAGE_KEY,
    CoreMetrics.DUPLICATED_BLOCKS_KEY,
    CoreMetrics.DUPLICATED_FILES_KEY,
    CoreMetrics.DUPLICATED_LINES_KEY,
    CoreMetrics.DUPLICATED_LINES_DENSITY_KEY,
    CoreMetrics.EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY,
    "effort_to_reach_software_quality_maintainability_rating_a",
    CoreMetrics.LINE_COVERAGE_KEY,
    CoreMetrics.LINES_KEY,
    CoreMetrics.LINES_TO_COVER_KEY,
    CoreMetrics.MAINTAINABILITY_ISSUES_KEY,
    CoreMetrics.NCLOC_KEY,
    CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY,
    CoreMetrics.NEW_BRANCH_COVERAGE_KEY,
    CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY,
    CoreMetrics.NEW_COVERAGE_KEY,
    CoreMetrics.NEW_BLOCKS_DUPLICATED_KEY,
    CoreMetrics.NEW_DUPLICATED_LINES_KEY,
    CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY,
    CoreMetrics.NEW_LINE_COVERAGE_KEY,
    CoreMetrics.NEW_LINES_KEY,
    CoreMetrics.NEW_LINES_TO_COVER_KEY,
    CoreMetrics.NEW_MAINTAINABILITY_ISSUES_KEY,
    CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY,
    CoreMetrics.NEW_NCLOC_KEY,
    CoreMetrics.NEW_RELIABILITY_ISSUES_KEY,
    CoreMetrics.NEW_RELIABILITY_RATING_KEY,
    CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT_KEY,
    CoreMetrics.NEW_SECURITY_HOTSPOTS_KEY,
    CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY,
    CoreMetrics.NEW_SECURITY_ISSUES_KEY,
    CoreMetrics.NEW_SECURITY_RATING_KEY,
    CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT_KEY,
    CoreMetrics.NEW_SECURITY_REVIEW_RATING_KEY,
    CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY,
    CoreMetrics.NEW_TECHNICAL_DEBT_KEY,
    "new_software_quality_maintainability_debt_ratio",
    "new_software_quality_maintainability_rating",
    "new_software_quality_maintainability_remediation_effort",
    "new_software_quality_reliability_rating",
    "new_software_quality_reliability_remediation_effort",
    "new_software_quality_security_rating",
    "new_software_quality_security_remediation_effort",
    CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY,
    CoreMetrics.NEW_UNCOVERED_LINES_KEY,
    CoreMetrics.NEW_VULNERABILITIES_KEY,
    CoreMetrics.RELIABILITY_ISSUES_KEY,
    CoreMetrics.RELIABILITY_RATING_KEY,
    CoreMetrics.RELIABILITY_REMEDIATION_EFFORT_KEY,
    CoreMetrics.SECURITY_HOTSPOTS_KEY,
    CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_KEY,
    CoreMetrics.SECURITY_ISSUES_KEY,
    CoreMetrics.SECURITY_RATING_KEY,
    CoreMetrics.SECURITY_REMEDIATION_EFFORT_KEY,
    CoreMetrics.SECURITY_REVIEW_RATING_KEY,
    CoreMetrics.SQALE_DEBT_RATIO_KEY,
    CoreMetrics.SQALE_RATING_KEY,
    "software_quality_maintainability_debt_ratio",
    "software_quality_maintainability_rating",
    "software_quality_maintainability_remediation_effort",
    "software_quality_reliability_rating",
    "software_quality_reliability_remediation_effort",
    "software_quality_security_rating",
    "software_quality_security_remediation_effort",
    CoreMetrics.TECHNICAL_DEBT_KEY,
    CoreMetrics.UNCOVERED_CONDITIONS_KEY,
    CoreMetrics.UNCOVERED_LINES_KEY,
    CoreMetrics.VIOLATIONS_KEY,
    CoreMetrics.VULNERABILITIES_KEY);

  // Frozen from history-sca:1.0.1.20780 for project branch analysis.
  private static final Set<String> TRACKED_SCA_MEASURE_KEYS = Set.of(
    "new_sca_count_any_issue",
    "new_sca_count_any_security",
    "new_sca_count_licensing",
    "new_sca_count_malware",
    "new_sca_count_vulnerability",
    "new_sca_rating_any_issue",
    "sca_count_any_issue",
    "sca_count_any_security",
    "sca_count_licensing",
    "sca_count_malware",
    "sca_count_vulnerability",
    "sca_rating_any_issue");

  /**
   * This predicate models a project branch after {@code EnableAnalysisStep}: the persisted root is enabled and a latest
   * snapshot is both selected ({@code islast}) and processed ({@code status = 'P'}). Only normal project branches are
   * eligible; pull-request branches do not receive project history backfill.
   */
  private static final String ELIGIBLE_PROJECT_BRANCHES_COUNT_SQL = """
    SELECT COUNT(*)
    FROM project_branches pb
    INNER JOIN projects p ON p.uuid = pb.project_uuid AND p.qualifier = 'TRK'
    INNER JOIN components root ON root.uuid = pb.uuid
      AND root.branch_uuid = pb.uuid
      AND root.qualifier = 'TRK'
      AND root.enabled = ?
    WHERE pb.branch_type = 'BRANCH'
      AND EXISTS (
      SELECT 1
      FROM snapshots s
      WHERE s.root_component_uuid = pb.uuid
        AND s.islast = ?
        AND s.status = 'P'
    )
    """;

  private static final String ELIGIBLE_PROJECT_BRANCHES_SQL = """
    SELECT pb.uuid
    FROM project_branches pb
    INNER JOIN projects p ON p.uuid = pb.project_uuid AND p.qualifier = 'TRK'
    INNER JOIN components root ON root.uuid = pb.uuid
      AND root.branch_uuid = pb.uuid
      AND root.qualifier = 'TRK'
      AND root.enabled = ?
    WHERE pb.branch_type = 'BRANCH'
      AND EXISTS (
      SELECT 1
      FROM snapshots s
      WHERE s.root_component_uuid = pb.uuid
        AND s.islast = ?
        AND s.status = 'P'
    )
    ORDER BY pb.uuid
    """;

  private static final String ISSUE_ROWS_SQL = """
    SELECT
      i.kee,
      i.issue_type,
      i.severity,
      i.status,
      i.resolution,
      c.qualifier,
      r.plugin_name,
      r.plugin_rule_key,
      ii.software_quality,
      ii.severity,
      rdi.software_quality,
      rdi.severity
    FROM issues i
    INNER JOIN rules r ON r.uuid = i.rule_uuid
    INNER JOIN components c ON c.uuid = i.component_uuid
    LEFT JOIN issues_impacts ii ON ii.issue_key = i.kee
    LEFT JOIN rules_default_impacts rdi ON rdi.rule_uuid = r.uuid
    WHERE i.project_uuid = ?
      AND c.branch_uuid = ?
      AND i.status <> ?
      AND i.issue_type <> ?
    ORDER BY i.kee
    """;

  private HistoryBackfillSupport() {
    // Utility class
  }

  static void backfillProjectBranches(Database database, DataChange.Context context) throws SQLException {
    backfill(database, context);
  }

  static long toUtcDayEpoch(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
  }

  static long parseFrozenEpoch(String value) {
    try {
      long epoch = Long.parseLong(value);
      if (Math.floorMod(epoch, MILLIS_PER_DAY) != 0) {
        throw new IllegalStateException("History backfill epoch is not a UTC-day epoch: " + value);
      }
      return epoch;
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Invalid history backfill epoch: " + value, e);
    }
  }

  // Ownership transfers to the caller, which closes the statement via try-with-resources or HistoryBackfillWriter.close().
  @SuppressWarnings("java:S2095")
  static PreparedStatement prepareScrollingSelect(Database database, Connection connection, String sql) throws SQLException {
    PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    statement.setFetchSize(Objects.requireNonNull(database.getDialect(), "Database dialect must not be null").getScrollDefaultFetchSize());
    return statement;
  }

  @CheckForNull
  static String formatMeasureValue(@CheckForNull String metricType, @CheckForNull Object value) {
    if (value == null) {
      return null;
    }
    if (metricType == null) {
      return String.valueOf(value);
    }
    return switch (metricType) {
      case "BOOL" -> formatBoolean(value);
      case "INT", "MILLISEC", "WORK_DUR" -> formatInteger(value);
      default -> String.valueOf(value);
    };
  }

  private static void backfill(Database database, DataChange.Context context) throws SQLException {
    long startedAt = System.nanoTime();
    PhaseStatistics statistics = new PhaseStatistics();
    try (Connection sourceConnection = database.getDataSource().getConnection();
      Connection writeConnection = database.getDataSource().getConnection()) {
      sourceConnection.setAutoCommit(false);
      if (sourceConnection.getMetaData().supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED)) {
        sourceConnection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
      }
      writeConnection.setAutoCommit(false);
      long frozenEpoch = readFrozenEpoch(database, sourceConnection);
      statistics.discoveredEntities = countEligibleProjectBranches(context);
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Starting project branch history backfill: {} frozenEpoch={}", statistics.describe(elapsedMillis(startedAt)), frozenEpoch);
      }

      try (HistoryBackfillWriter writer = new HistoryBackfillWriter(database, sourceConnection, writeConnection, frozenEpoch, statistics)) {
        processProjectBranches(database, context, sourceConnection, writer, statistics, startedAt);
        writer.flushAndCommit();
      } catch (SQLException | RuntimeException e) {
        rollback(writeConnection, e);
        throw e;
      }
    }
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Completed project branch history backfill: {}", statistics.describe(elapsedMillis(startedAt)));
    }
  }

  private static long countEligibleProjectBranches(DataChange.Context context) throws SQLException {
    try (Select select = context.prepareSelect(ELIGIBLE_PROJECT_BRANCHES_COUNT_SQL)) {
      Long count = select
        .setBoolean(1, true)
        .setBoolean(2, true)
        .get(Select.LONG_READER);
      return count == null ? 0L : count;
    }
  }

  private static void processProjectBranches(
    Database database,
    DataChange.Context context,
    Connection sourceConnection,
    HistoryBackfillWriter writer,
    PhaseStatistics statistics,
    long startedAt) throws SQLException {
    try (Select select = context.prepareSelect(ELIGIBLE_PROJECT_BRANCHES_SQL)) {
      select
        .setBoolean(1, true)
        .setBoolean(2, true)
        .scroll(row -> {
          String branchUuid = row.getString(1);
          recordProjectBranch(database, sourceConnection, writer, branchUuid);
          statistics.processedEntities++;
          logProgressIfNeeded(statistics, startedAt);
        });
    }
  }

  private static void recordProjectBranch(
    Database database,
    Connection sourceConnection,
    HistoryBackfillWriter writer,
    String branchUuid) throws SQLException {
    Map<IssueDimension, Integer> issueCounts = new HashMap<>();
    collectIssueCounts(database, sourceConnection, branchUuid, issueCounts, writer.statistics);
    recordIssueBaseline(writer, branchUuid, issueCounts);
    recordMeasureBaseline(writer, branchUuid, readCurrentMeasures(database, sourceConnection, branchUuid));
  }

  static void collectIssueCounts(
    Database database,
    Connection connection,
    String sourceBranchUuid,
    Map<IssueDimension, Integer> issueCounts,
    PhaseStatistics statistics) throws SQLException {
    try (PreparedStatement statement = prepareScrollingSelect(database, connection, ISSUE_ROWS_SQL)) {
      statement.setString(1, sourceBranchUuid);
      statement.setString(2, sourceBranchUuid);
      statement.setString(3, CLOSED_STATUS);
      statement.setInt(4, SECURITY_HOTSPOT_TYPE);
      try (ResultSet resultSet = statement.executeQuery()) {
        MutableIssue currentIssue = null;
        String currentIssueKey = null;
        while (resultSet.next()) {
          String issueKey = resultSet.getString(1);
          if (!issueKey.equals(currentIssueKey)) {
            if (currentIssue != null) {
              recordIssueCount(issueCounts, currentIssue, sourceBranchUuid, statistics);
            }
            currentIssueKey = issueKey;
            currentIssue = new MutableIssue(resultSet);
          }
          currentIssue.addImpacts(resultSet.getString(9), resultSet.getString(10), resultSet.getString(11), resultSet.getString(12));
        }
        if (currentIssue != null) {
          recordIssueCount(issueCounts, currentIssue, sourceBranchUuid, statistics);
        }
      }
    }
  }

  private static void recordIssueCount(
    Map<IssueDimension, Integer> issueCounts,
    MutableIssue issue,
    String sourceBranchUuid,
    PhaseStatistics statistics) {
    try {
      incrementIssueCount(issueCounts, issue.toDimension());
      statistics.issuesProcessed++;
    } catch (IllegalStateException | IllegalArgumentException e) {
      statistics.skippedIssues++;
      if (statistics.skippedIssues <= INVALID_ISSUE_LOG_LIMIT || statistics.skippedIssues % PROGRESS_INTERVAL == 0) {
        LOGGER.warn("Skipping issue that cannot be mapped to history dimension: branch={}, skippedIssues={}",
          sourceBranchUuid, statistics.skippedIssues, e);
      }
    }
  }

  private static void recordIssueBaseline(
    HistoryBackfillWriter writer,
    String branchUuid,
    Map<IssueDimension, Integer> issueCounts) throws SQLException {
    Map<Integer, LatestIssueCount> latestCounts = writer.findLatestIssueCounts(branchUuid, PROJECT_BRANCH_ENTITY_TYPE);
    Set<Integer> currentDimensionIds = new HashSet<>();
    for (Map.Entry<IssueDimension, Integer> entry : issueCounts.entrySet()) {
      int dimensionId = writer.getOrCreateDimension(entry.getKey());
      currentDimensionIds.add(dimensionId);
      writer.upsertIssueCount(branchUuid, PROJECT_BRANCH_ENTITY_TYPE, dimensionId, entry.getValue(), latestCounts.get(dimensionId));
    }
    for (Map.Entry<Integer, LatestIssueCount> latestCount : latestCounts.entrySet()) {
      if (!currentDimensionIds.contains(latestCount.getKey()) && latestCount.getValue().issueCount() != 0) {
        writer.upsertIssueCount(branchUuid, PROJECT_BRANCH_ENTITY_TYPE, latestCount.getKey(), 0, latestCount.getValue());
      }
    }
  }

  static void incrementIssueCount(Map<IssueDimension, Integer> issueCounts, IssueDimension dimension) {
    issueCounts.merge(dimension, 1, Integer::sum);
  }

  private static Map<String, Object> readCurrentMeasures(Database database, Connection connection, String branchUuid) throws SQLException {
    try (PreparedStatement statement = prepareScrollingSelect(database, connection, "select json_value from measures where component_uuid = ?")) {
      statement.setString(1, branchUuid);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Map.of();
        }
        try (Reader json = resultSet.getCharacterStream(1)) {
          if (json == null) {
            throw new IllegalStateException("Current measures JSON is null for project branch " + branchUuid);
          }
          Map<String, Object> values = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
          }.getType());
          if (values == null) {
            throw new IllegalStateException("Current measures JSON is null for project branch " + branchUuid);
          }
          return new TreeMap<>(values);
        } catch (IOException | RuntimeException e) {
          throw new IllegalStateException("Failed to decode current measures JSON for project branch " + branchUuid, e);
        }
      }
    }
  }

  private static void recordMeasureBaseline(
    HistoryBackfillWriter writer,
    String branchUuid,
    Map<String, Object> measures) throws SQLException {
    Map<Integer, ExactMeasureValue> measuresAtEpoch = writer.findMeasuresAtEpoch(branchUuid, PROJECT_BRANCH_ENTITY_TYPE);
    for (Map.Entry<String, Object> measure : measures.entrySet()) {
      String metricKey = measure.getKey();
      Object value = measure.getValue();
      boolean tracked = TRACKED_MEASURE_KEYS.contains(metricKey) || TRACKED_SCA_MEASURE_KEYS.contains(metricKey);
      if (value == null || !tracked) {
        continue;
      }
      String metricType = writer.findMetricType(metricKey);
      int metricId = writer.getOrCreateMetric(metricKey, metricType == null ? "STRING" : metricType);
      writer.upsertMeasure(branchUuid, PROJECT_BRANCH_ENTITY_TYPE, metricId, formatMeasureValue(metricType, value), measuresAtEpoch.get(metricId));
    }
  }

  private static long readFrozenEpoch(Database database, Connection connection) throws SQLException {
    try (PreparedStatement statement = prepareScrollingSelect(database, connection, "select text_value from internal_properties where kee = ?")) {
      statement.setString(1, PersistHistoryBackfillUtcDayEpoch.FROZEN_EPOCH_PROPERTY);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          throw new IllegalStateException("Missing persisted history backfill epoch");
        }
        String value = resultSet.getString(1);
        if (value == null) {
          throw new IllegalStateException("Missing persisted history backfill epoch value");
        }
        return parseFrozenEpoch(value);
      }
    }
  }

  private static void logProgressIfNeeded(PhaseStatistics statistics, long startedAt) {
    if (statistics.processedEntities % PROGRESS_INTERVAL == 0 && LOGGER.isInfoEnabled()) {
      LOGGER.info("Project branch history backfill progress: {}", statistics.describe(elapsedMillis(startedAt)));
    }
  }

  private static long elapsedMillis(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000L;
  }

  private static void rollback(Connection connection, Exception original) {
    try {
      connection.rollback();
    } catch (SQLException rollbackFailure) {
      original.addSuppressed(rollbackFailure);
    }
  }

  private static String formatBoolean(Object value) {
    try {
      double numericValue = value instanceof Number number ? number.doubleValue() : Double.parseDouble(value.toString());
      return Boolean.toString(Math.abs(numericValue - 1.0D) < 0.000001D);
    } catch (NumberFormatException e) {
      return String.valueOf(value);
    }
  }

  private static String formatInteger(Object value) {
    try {
      BigDecimal decimal = new BigDecimal(value instanceof Number number ? number.toString() : value.toString());
      return decimal.toBigIntegerExact().toString();
    } catch (ArithmeticException | NumberFormatException e) {
      return String.valueOf(value);
    }
  }

  static void setLargeText(Database database, PreparedStatement statement, int parameterIndex, @CheckForNull String value) throws SQLException {
    String dialectId = Objects.requireNonNull(Objects.requireNonNull(database.getDialect(), "Database dialect must not be null").getId(),
      "Database dialect id must not be null");
    if (PostgreSql.ID.equals(dialectId)) {
      if (value == null) {
        statement.setNull(parameterIndex, Types.VARCHAR);
      } else {
        statement.setString(parameterIndex, value);
      }
    } else if (value == null) {
      statement.setNull(parameterIndex, Types.CLOB);
    } else {
      statement.setCharacterStream(parameterIndex, new StringReader(value), value.length());
    }
  }

  @CheckForNull
  static String readLargeText(ResultSet resultSet, int columnIndex) throws SQLException {
    try (Reader reader = resultSet.getCharacterStream(columnIndex)) {
      if (reader == null) {
        return null;
      }
      char[] buffer = new char[4_096];
      StringBuilder text = new StringBuilder();
      int read;
      while ((read = reader.read(buffer)) != -1) {
        text.append(buffer, 0, read);
      }
      return text.toString();
    } catch (IOException e) {
      throw new SQLException("Failed to read measure history CLOB", e);
    }
  }

  static final class PhaseStatistics {
    private long discoveredEntities;
    private long processedEntities;
    private long skippedIssues;
    private long issuesProcessed;
    private long issueRecordsWritten;
    private long issueRecordsUnchanged;
    private long measureRecordsWritten;
    private long measureRecordsUnchanged;

    PhaseStatistics() {
    }

    String describe(long elapsedMillis) {
      return "discovered=" + discoveredEntities +
        ", processed=" + processedEntities +
        ", skippedIssues=" + skippedIssues +
        ", issuesProcessed=" + issuesProcessed +
        ", issueRecordsWritten=" + issueRecordsWritten +
        ", issueRecordsUnchanged=" + issueRecordsUnchanged +
        ", measureRecordsWritten=" + measureRecordsWritten +
        ", measureRecordsUnchanged=" + measureRecordsUnchanged +
        ", elapsedMs=" + elapsedMillis;
    }

    long issuesProcessed() {
      return issuesProcessed;
    }
  }

  private static final class MutableIssue {
    private final String issueKey;
    private final int issueType;
    private final String severity;
    private final String status;
    private final String resolution;
    private final String qualifier;
    private final String ruleRepository;
    private final String ruleKey;
    private final Map<String, String> overriddenImpacts = new HashMap<>();
    private final Map<String, String> defaultImpacts = new HashMap<>();

    private MutableIssue(ResultSet resultSet) throws SQLException {
      this.issueKey = resultSet.getString(1);
      this.issueType = resultSet.getInt(2);
      this.severity = resultSet.getString(3);
      this.status = resultSet.getString(4);
      this.resolution = resultSet.getString(5);
      this.qualifier = resultSet.getString(6);
      this.ruleRepository = resultSet.getString(7);
      this.ruleKey = resultSet.getString(8);
    }

    private void addImpacts(
      @CheckForNull String overriddenQuality,
      @CheckForNull String overriddenSeverity,
      @CheckForNull String defaultQuality,
      @CheckForNull String defaultSeverity) {
      if (overriddenQuality != null) {
        overriddenImpacts.put(overriddenQuality, overriddenSeverity);
      }
      if (defaultQuality != null) {
        defaultImpacts.put(defaultQuality, defaultSeverity);
      }
    }

    private IssueDimension toDimension() {
      if (severity == null || ruleRepository == null || ruleKey == null) {
        throw new IllegalStateException("Cannot backfill issue history for invalid issue " + issueKey);
      }
      Map<String, String> effectiveImpacts = new HashMap<>(overriddenImpacts.isEmpty() ? defaultImpacts : overriddenImpacts);
      IssueStatus issueStatus = IssueStatus.of(status, resolution);
      return new IssueDimension(
        issueType == SECURITY_HOTSPOT_TYPE ? resolution : null,
        UNIT_TEST_FILE_QUALIFIER.equals(qualifier) ? "TEST" : "MAIN",
        severity,
        issueStatus == null ? null : issueStatus.name(),
        status,
        issueType,
        ruleRepository + ":" + ruleKey,
        toRating(effectiveImpacts.get("MAINTAINABILITY")),
        toRating(effectiveImpacts.get("SECURITY")),
        toRating(effectiveImpacts.get("RELIABILITY")));
    }

    private static short toRating(@CheckForNull String severity) {
      if (severity == null || "NOT_PRESENT".equals(severity)) {
        return 0;
      }
      return switch (severity) {
        case "INFO" -> 1;
        case "LOW", "MINOR" -> 2;
        case "MEDIUM", "MAJOR" -> 3;
        case "HIGH", "CRITICAL" -> 4;
        case "BLOCKER" -> 5;
        default -> throw new IllegalArgumentException("Unsupported software quality severity: " + severity);
      };
    }
  }

  record IssueDimension(
    @CheckForNull String hotspotResolution,
    String issueCodeScope,
    String issueSeverity,
    @CheckForNull String issueStatus,
    @CheckForNull String status,
    int issueType,
    String ruleKey,
    short maintainabilityRating,
    short securityRating,
    short reliabilityRating) {

    // Existing history dimensions use MD5 as an identifier, not for security.
    @SuppressWarnings("java:S4790")
    String hash() {
      String input = (hotspotResolution == null ? "" : hotspotResolution) + "|" +
        issueCodeScope + "|" +
        issueSeverity + "|" +
        (issueStatus == null ? "" : issueStatus) + "|" +
        (status == null ? "" : status) + "|" +
        issueType + "|" +
        ruleKey + "|" +
        maintainabilityRating + "|" +
        securityRating + "|" +
        reliabilityRating;
      try {
        byte[] digest = MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(32);
        for (byte value : digest) {
          result.append(Character.forDigit((value >>> 4) & 0xF, 16));
          result.append(Character.forDigit(value & 0xF, 16));
        }
        return result.toString();
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("MD5 is unavailable", e);
      }
    }
  }

  private static final class HistoryBackfillWriter implements AutoCloseable {
    private final Database database;
    private final Connection writeConnection;
    private final long frozenEpoch;
    private final PhaseStatistics statistics;
    private final Map<IssueDimension, Integer> dimensions = new LinkedHashMap<>(DIMENSION_CACHE_SIZE + 1, 1.0F, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<IssueDimension, Integer> eldest) {
        return size() > DIMENSION_CACHE_SIZE;
      }
    };
    private final Map<String, Integer> metrics = new HashMap<>();
    private final Map<String, String> metricTypes = new HashMap<>();
    private final PreparedStatement selectDimension;
    private final PreparedStatement insertDimension;
    private final PreparedStatement selectMetric;
    private final PreparedStatement insertMetric;
    private final PreparedStatement selectMetricType;
    private final PreparedStatement selectLatestIssueCounts;
    private final PreparedStatement updateIssue;
    private final PreparedStatement insertIssue;
    private final PreparedStatement selectMeasuresAtEpoch;
    private final PreparedStatement updateMeasure;
    private final PreparedStatement insertMeasure;
    private final PendingWrites pendingWrites = new PendingWrites();

    private HistoryBackfillWriter(Database database, Connection sourceConnection, Connection writeConnection, long frozenEpoch, PhaseStatistics statistics) throws SQLException {
      this.database = database;
      this.writeConnection = writeConnection;
      this.frozenEpoch = frozenEpoch;
      this.statistics = statistics;
      this.selectDimension = prepareScrollingSelect(database, writeConnection, "select id from issue_count_dimensions where dimension_hash = ?");
      this.insertDimension = writeConnection.prepareStatement("""
        insert into issue_count_dimensions (
          hotspot_resolution, issue_code_scope, issue_severity, issue_status, status, issue_type, rule_key,
          dimension_hash, maintainability_rating, security_rating, reliability_rating)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """);
      this.selectMetric = prepareScrollingSelect(database, writeConnection, "select id from measure_key_mapping where metric_name = ?");
      this.insertMetric = writeConnection.prepareStatement("insert into measure_key_mapping (metric_name, metric_type) values (?, ?)");
      this.selectMetricType = prepareScrollingSelect(database, sourceConnection, "select val_type from metrics where name = ?");
      this.selectLatestIssueCounts = prepareScrollingSelect(database, writeConnection, """
        select dimension_id, issue_count, recorded_at_epoch
        from (
          select dimension_id, issue_count, recorded_at_epoch,
            row_number() over (partition by dimension_id order by recorded_at_epoch desc) as rank_number
          from issue_count_history
          where entity_id = ? and entity_type = ? and recorded_at_epoch <= ?
        ) ranked
        where rank_number = 1
        """);
      this.updateIssue = writeConnection.prepareStatement("""
        update issue_count_history set issue_count = ?
        where entity_id = ? and entity_type = ? and dimension_id = ? and recorded_at_epoch = ?
        """);
      this.insertIssue = writeConnection.prepareStatement("""
        insert into issue_count_history (entity_id, entity_type, dimension_id, recorded_at_epoch, issue_count)
        values (?, ?, ?, ?, ?)
        """);
      this.selectMeasuresAtEpoch = prepareScrollingSelect(database, writeConnection, """
        select metric_id, text_value from measure_history
        where entity_id = ? and entity_type = ? and recorded_at_epoch = ?
        """);
      this.updateMeasure = writeConnection.prepareStatement("""
        update measure_history set text_value = ?
        where metric_id = ? and entity_id = ? and entity_type = ? and recorded_at_epoch = ?
        """);
      this.insertMeasure = writeConnection.prepareStatement("""
        insert into measure_history (metric_id, entity_id, entity_type, recorded_at_epoch, text_value)
        values (?, ?, ?, ?, ?)
        """);
    }

    private int getOrCreateDimension(IssueDimension dimension) throws SQLException {
      Integer cached = dimensions.get(dimension);
      if (cached != null) {
        return cached;
      }
      Integer id = findId(selectDimension, dimension.hash());
      if (id == null) {
        insertDimension(dimension);
        id = findId(selectDimension, dimension.hash());
        if (id == null) {
          throw new IllegalStateException("Issue-count dimension was not created for hash " + dimension.hash());
        }
      }
      dimensions.put(dimension, id);
      return id;
    }

    private int getOrCreateMetric(String metricName, String metricType) throws SQLException {
      Integer cached = metrics.get(metricName);
      if (cached != null) {
        return cached;
      }
      Integer id = findId(selectMetric, metricName);
      if (id == null) {
        insertMetric.setString(1, metricName);
        insertMetric.setString(2, metricType);
        insertMetric.executeUpdate();
        id = findId(selectMetric, metricName);
        if (id == null) {
          throw new IllegalStateException("Measure key mapping was not created for metric " + metricName);
        }
      }
      metrics.put(metricName, id);
      return id;
    }

    @CheckForNull
    private String findMetricType(String metricName) throws SQLException {
      if (metricTypes.containsKey(metricName)) {
        return metricTypes.get(metricName);
      }
      selectMetricType.setString(1, metricName);
      try (ResultSet resultSet = selectMetricType.executeQuery()) {
        String metricType = resultSet.next() ? resultSet.getString(1) : null;
        metricTypes.put(metricName, metricType);
        return metricType;
      }
    }

    private Map<Integer, LatestIssueCount> findLatestIssueCounts(String entityUuid, String entityType) throws SQLException {
      Map<Integer, LatestIssueCount> counts = new HashMap<>();
      selectLatestIssueCounts.setString(1, entityUuid);
      selectLatestIssueCounts.setString(2, entityType);
      selectLatestIssueCounts.setLong(3, frozenEpoch);
      try (ResultSet resultSet = selectLatestIssueCounts.executeQuery()) {
        while (resultSet.next()) {
          counts.put(resultSet.getInt(1), new LatestIssueCount(resultSet.getInt(2), resultSet.getLong(3)));
        }
      }
      return counts;
    }

    private void upsertIssueCount(
      String entityUuid,
      String entityType,
      int dimensionId,
      int issueCount,
      @CheckForNull LatestIssueCount existing) throws SQLException {
      if (existing != null && existing.recordedAtEpoch() == frozenEpoch && existing.issueCount() == issueCount) {
        statistics.issueRecordsUnchanged++;
        return;
      }
      if (existing == null || existing.recordedAtEpoch() != frozenEpoch) {
        insertIssue.setString(1, entityUuid);
        insertIssue.setString(2, entityType);
        insertIssue.setInt(3, dimensionId);
        insertIssue.setLong(4, frozenEpoch);
        insertIssue.setInt(5, issueCount);
        insertIssue.addBatch();
        pendingWrites.issueInserts++;
      } else {
        updateIssue.setInt(1, issueCount);
        updateIssue.setString(2, entityUuid);
        updateIssue.setString(3, entityType);
        updateIssue.setInt(4, dimensionId);
        updateIssue.setLong(5, frozenEpoch);
        updateIssue.addBatch();
        pendingWrites.issueUpdates++;
      }
      statistics.issueRecordsWritten++;
      flushWhenFull();
    }

    private void upsertMeasure(
      String entityUuid,
      String entityType,
      int metricId,
      @CheckForNull String textValue,
      @CheckForNull ExactMeasureValue existing) throws SQLException {
      if (existing != null && sameValue(existing.value(), textValue)) {
        statistics.measureRecordsUnchanged++;
        return;
      }
      if (existing == null) {
        insertMeasure.setInt(1, metricId);
        insertMeasure.setString(2, entityUuid);
        insertMeasure.setString(3, entityType);
        insertMeasure.setLong(4, frozenEpoch);
        setLargeText(database, insertMeasure, 5, textValue);
        insertMeasure.addBatch();
        pendingWrites.measureInserts++;
      } else {
        setLargeText(database, updateMeasure, 1, textValue);
        updateMeasure.setInt(2, metricId);
        updateMeasure.setString(3, entityUuid);
        updateMeasure.setString(4, entityType);
        updateMeasure.setLong(5, frozenEpoch);
        updateMeasure.addBatch();
        pendingWrites.measureUpdates++;
      }
      statistics.measureRecordsWritten++;
      flushWhenFull();
    }

    private Map<Integer, ExactMeasureValue> findMeasuresAtEpoch(String entityUuid, String entityType) throws SQLException {
      Map<Integer, ExactMeasureValue> measures = new HashMap<>();
      selectMeasuresAtEpoch.setString(1, entityUuid);
      selectMeasuresAtEpoch.setString(2, entityType);
      selectMeasuresAtEpoch.setLong(3, frozenEpoch);
      try (ResultSet resultSet = selectMeasuresAtEpoch.executeQuery()) {
        while (resultSet.next()) {
          measures.put(resultSet.getInt(1), new ExactMeasureValue(readLargeText(resultSet, 2)));
        }
      }
      return measures;
    }

    private void insertDimension(IssueDimension dimension) throws SQLException {
      insertDimension.setString(1, dimension.hotspotResolution());
      insertDimension.setString(2, dimension.issueCodeScope());
      insertDimension.setString(3, dimension.issueSeverity());
      insertDimension.setString(4, dimension.issueStatus());
      insertDimension.setString(5, dimension.status());
      insertDimension.setInt(6, dimension.issueType());
      insertDimension.setString(7, dimension.ruleKey());
      insertDimension.setString(8, dimension.hash());
      insertDimension.setShort(9, dimension.maintainabilityRating());
      insertDimension.setShort(10, dimension.securityRating());
      insertDimension.setShort(11, dimension.reliabilityRating());
      insertDimension.executeUpdate();
    }

    private void flushWhenFull() throws SQLException {
      if (pendingWrites() >= WRITE_BATCH_SIZE) {
        flushAndCommit();
      }
    }

    private void flushAndCommit() throws SQLException {
      executeBatch(updateIssue, pendingWrites.issueUpdates);
      executeBatch(insertIssue, pendingWrites.issueInserts);
      executeBatch(updateMeasure, pendingWrites.measureUpdates);
      executeBatch(insertMeasure, pendingWrites.measureInserts);
      pendingWrites.reset();
      writeConnection.commit();
    }

    private int pendingWrites() {
      return pendingWrites.total();
    }

    @Override
    public void close() throws SQLException {
      SQLException failure = null;
      for (PreparedStatement statement : new PreparedStatement[] {
        selectDimension, insertDimension, selectMetric, insertMetric, selectMetricType, selectLatestIssueCounts,
        updateIssue, insertIssue, selectMeasuresAtEpoch, updateMeasure, insertMeasure}) {
        try {
          statement.close();
        } catch (SQLException e) {
          if (failure == null) {
            failure = e;
          } else {
            failure.addSuppressed(e);
          }
        }
      }
      if (failure != null) {
        throw failure;
      }
    }

    private static Integer findId(PreparedStatement statement, String key) throws SQLException {
      statement.setString(1, key);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? resultSet.getInt(1) : null;
      }
    }

    private static boolean sameValue(@CheckForNull String left, @CheckForNull String right) {
      return Objects.equals(left, right);
    }

    private static void executeBatch(PreparedStatement statement, int count) throws SQLException {
      if (count > 0) {
        statement.executeBatch();
        statement.clearBatch();
      }
    }

    private static final class PendingWrites {
      private int issueUpdates;
      private int issueInserts;
      private int measureUpdates;
      private int measureInserts;

      private PendingWrites() {
      }

      private int total() {
        return issueUpdates + issueInserts + measureUpdates + measureInserts;
      }

      private void reset() {
        issueUpdates = 0;
        issueInserts = 0;
        measureUpdates = 0;
        measureInserts = 0;
      }
    }
  }

  private record LatestIssueCount(int issueCount, long recordedAtEpoch) {
  }

  private record ExactMeasureValue(@CheckForNull String value) {
  }
}
