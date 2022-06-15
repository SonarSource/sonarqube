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
package org.sonar.db.measure;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;

import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.api.utils.KeyValueFormat.parseStringInt;
import static org.sonar.db.component.DbTagsReader.readDbTags;

public class ProjectMeasuresIndexerIterator extends CloseableIterator<ProjectMeasuresIndexerIterator.ProjectMeasures> {

  public static final Set<String> METRIC_KEYS = ImmutableSortedSet.of(
    CoreMetrics.NCLOC_KEY,
    CoreMetrics.LINES_KEY,
    CoreMetrics.DUPLICATED_LINES_DENSITY_KEY,
    CoreMetrics.COVERAGE_KEY,
    CoreMetrics.SQALE_RATING_KEY,
    CoreMetrics.RELIABILITY_RATING_KEY,
    CoreMetrics.SECURITY_RATING_KEY,
    CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_KEY,
    CoreMetrics.SECURITY_REVIEW_RATING_KEY,
    CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY,
    CoreMetrics.ALERT_STATUS_KEY,
    CoreMetrics.NEW_SECURITY_RATING_KEY,
    CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY,
    CoreMetrics.NEW_SECURITY_REVIEW_RATING_KEY,
    CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY,
    CoreMetrics.NEW_COVERAGE_KEY,
    CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY,
    CoreMetrics.NEW_LINES_KEY,
    CoreMetrics.NEW_RELIABILITY_RATING_KEY);

  private static final String SQL_PROJECTS = "SELECT p.uuid, p.kee, p.name, s.created_at, p.tags, p.qualifier " +
    "FROM projects p " +
    "LEFT OUTER JOIN snapshots s ON s.component_uuid=p.uuid AND s.islast=? " +
    "WHERE p.qualifier in (?, ?)";

  private static final String PROJECT_FILTER = " AND p.uuid=?";

  private static final String SQL_MEASURES = "SELECT m.name, pm.value, pm.variation, pm.text_value FROM live_measures pm " +
    "INNER JOIN metrics m ON m.uuid = pm.metric_uuid " +
    "WHERE pm.component_uuid = ? " +
    "AND m.name IN ({metricNames}) " +
    "AND (pm.value IS NOT NULL OR pm.variation IS NOT NULL OR pm.text_value IS NOT NULL) " +
    "AND m.enabled = ? ";

  private static final String SQL_NCLOC_LANGUAGE_DISTRIBUTION = "SELECT m.name, pm.value, pm.variation, pm.text_value FROM live_measures pm " +
    "INNER JOIN metrics m ON m.uuid = pm.metric_uuid " +
    "WHERE pm.component_uuid = ? " +
    "AND m.name = ? " +
    "AND (pm.value IS NOT NULL OR pm.variation IS NOT NULL OR pm.text_value IS NOT NULL) " +
    "AND m.enabled = ? ";

  private static final String SQL_PROJECT_BRANCHES = "SELECT uuid FROM project_branches pb " +
    "WHERE pb.project_uuid = ? ";

  private static final String SQL_BIGGEST_NCLOC_VALUE = "SELECT max(lm.value) FROM metrics m " +
    "INNER JOIN live_measures lm ON m.uuid = lm.metric_uuid " +
    "WHERE lm.component_uuid IN ({projectBranches}) " +
    "AND m.name = ? AND lm.value IS NOT NULL AND m.enabled = ? ";

  private static final String SQL_BIGGEST_BRANCH = "SELECT lm.component_uuid FROM metrics m " +
    "INNER JOIN live_measures lm ON m.uuid = lm.metric_uuid " +
    "WHERE lm.component_uuid IN ({projectBranches}) " +
    "AND m.name = ? AND lm.value = ? AND m.enabled = ? ";

  private static final boolean ENABLED = true;
  private static final int FIELD_METRIC_NAME = 1;
  private static final int FIELD_MEASURE_VALUE = 2;
  private static final int FIELD_MEASURE_VARIATION = 3;
  private static final int FIELD_MEASURE_TEXT_VALUE = 4;

  private final DbSession dbSession;
  private final PreparedStatement measuresStatement;
  private final Iterator<Project> projects;

  private ProjectMeasuresIndexerIterator(DbSession dbSession, PreparedStatement measuresStatement, List<Project> projects) {
    this.dbSession = dbSession;
    this.measuresStatement = measuresStatement;
    this.projects = projects.iterator();
  }

  public static ProjectMeasuresIndexerIterator create(DbSession session, @Nullable String projectUuid) {
    List<Project> projects = selectProjects(session, projectUuid);
    PreparedStatement projectsStatement = createMeasuresStatement(session);
    return new ProjectMeasuresIndexerIterator(session, projectsStatement, projects);
  }

  private static List<Project> selectProjects(DbSession session, @Nullable String projectUuid) {
    List<Project> projects = new ArrayList<>();
    try (PreparedStatement stmt = createProjectsStatement(session, projectUuid);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        String uuid = rs.getString(1);
        String key = rs.getString(2);
        String name = rs.getString(3);
        Long analysisDate = DatabaseUtils.getLong(rs, 4);
        List<String> tags = readDbTags(DatabaseUtils.getString(rs, 5));
        String qualifier = rs.getString(6);
        Project project = new Project(uuid, key, name, qualifier, tags, analysisDate);
        projects.add(project);
      }
      return projects;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute request to select all projects", e);
    }
  }

  private static PreparedStatement createProjectsStatement(DbSession session, @Nullable String projectUuid) {
    try {
      StringBuilder sql = new StringBuilder(SQL_PROJECTS);
      if (projectUuid != null) {
        sql.append(PROJECT_FILTER);
      }
      PreparedStatement stmt = session.getConnection().prepareStatement(sql.toString());
      stmt.setBoolean(1, true);
      stmt.setString(2, Qualifiers.PROJECT);
      stmt.setString(3, Qualifiers.APP);
      if (projectUuid != null) {
        stmt.setString(4, projectUuid);
      }
      return stmt;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all project measures", e);
    }
  }

  private static PreparedStatement createMeasuresStatement(DbSession session) {
    try {
      String metricNameQuestionMarks = METRIC_KEYS.stream()
        .filter(m -> !m.equals(NCLOC_LANGUAGE_DISTRIBUTION_KEY))
        .map(x -> "?").collect(Collectors.joining(","));
      String sql = StringUtils.replace(SQL_MEASURES, "{metricNames}", metricNameQuestionMarks);
      return session.getConnection().prepareStatement(sql);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select measures", e);
    }
  }

  @Override
  @CheckForNull
  protected ProjectMeasures doNext() {
    if (!projects.hasNext()) {
      return null;
    }
    Project project = projects.next();
    Measures measures = selectMeasures(project.getUuid());
    return new ProjectMeasures(project, measures);
  }

  private Measures selectMeasures(String projectUuid) {
    Measures measures = new Measures();
    ResultSet rs = null;
    try {
      prepareMeasuresStatement(projectUuid);
      rs = measuresStatement.executeQuery();

      while (rs.next()) {
        readMeasure(rs, measures);
      }

      List<String> projectBranches = selectProjectBranches(dbSession, projectUuid);
      long biggestNcloc = selectProjectBiggestNcloc(dbSession, projectBranches);
      String biggestBranch = selectProjectBiggestBranch(dbSession, projectBranches, biggestNcloc);
      PreparedStatement prepareNclocByLanguageStatement = prepareNclocByLanguageStatement(dbSession, biggestBranch);
      rs = prepareNclocByLanguageStatement.executeQuery();

      if (rs.next()) {
        readMeasure(rs, measures);
      }

      return measures;
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Fail to execute request to select measures of project %s", projectUuid), e);
    } finally {
      DatabaseUtils.closeQuietly(rs);
    }
  }

  private void prepareMeasuresStatement(String projectUuid) throws SQLException {
    AtomicInteger index = new AtomicInteger(1);
    measuresStatement.setString(index.getAndIncrement(), projectUuid);
    METRIC_KEYS
      .stream()
      .filter(m -> !m.equals(NCLOC_LANGUAGE_DISTRIBUTION_KEY))
      .forEach(DatabaseUtils.setStrings(measuresStatement, index::getAndIncrement));
    measuresStatement.setBoolean(index.getAndIncrement(), ENABLED);
  }

  private static PreparedStatement prepareNclocByLanguageStatement(DbSession session, String projectUuid) {
    try {
      PreparedStatement stmt = session.getConnection().prepareStatement(SQL_NCLOC_LANGUAGE_DISTRIBUTION);
      AtomicInteger index = new AtomicInteger(1);
      stmt.setString(index.getAndIncrement(), projectUuid);
      stmt.setString(index.getAndIncrement(), NCLOC_LANGUAGE_DISTRIBUTION_KEY);
      stmt.setBoolean(index.getAndIncrement(), ENABLED);
      return stmt;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute request to select ncloc_language_distribution measure", e);
    }
  }

  private static List<String> selectProjectBranches(DbSession session, @Nullable String projectUuid) {
    ResultSet rs = null;
    List<String> projectBranches = new ArrayList<>();
    try (PreparedStatement stmt = session.getConnection().prepareStatement(SQL_PROJECT_BRANCHES)){
      AtomicInteger index = new AtomicInteger(1);
      stmt.setString(index.getAndIncrement(), projectUuid);

      rs = stmt.executeQuery();

      while (rs.next()) {
        String uuid = rs.getString(1);
        projectBranches.add(uuid);
      }
      return projectBranches;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute request to select all project branches", e);
    } finally {
      DatabaseUtils.closeQuietly(rs);
    }
  }

  private static long selectProjectBiggestNcloc(DbSession session, List<String> projectBranches) {
    try {
      long ncloc = 0;

      AtomicInteger index = new AtomicInteger(1);
      PreparedStatement nclocStatement = getPreparedStatement(projectBranches, SQL_BIGGEST_NCLOC_VALUE, session);
      projectBranches.forEach(DatabaseUtils.setStrings(nclocStatement, index::getAndIncrement));

      nclocStatement.setString(index.getAndIncrement(), CoreMetrics.NCLOC_KEY);
      nclocStatement.setBoolean(index.getAndIncrement(), ENABLED);

      ResultSet rs = nclocStatement.executeQuery();

      if (rs.next()) {
        ncloc = rs.getLong(1);
      }
      return ncloc;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute request to select the project biggest ncloc", e);
    }
  }

  private static String selectProjectBiggestBranch(DbSession session, List<String> projectBranches, long ncloc) {
    try {
      String biggestBranchUuid = "";

      AtomicInteger index = new AtomicInteger(1);
      PreparedStatement nclocStatement = getPreparedStatement(projectBranches, SQL_BIGGEST_BRANCH, session);
      projectBranches.forEach(DatabaseUtils.setStrings(nclocStatement, index::getAndIncrement));

      nclocStatement.setString(index.getAndIncrement(), CoreMetrics.NCLOC_KEY);
      nclocStatement.setLong(index.getAndIncrement(), ncloc);
      nclocStatement.setBoolean(index.getAndIncrement(), ENABLED);

      ResultSet rs = nclocStatement.executeQuery();

      if (rs.next()) {
        biggestBranchUuid = rs.getString(1);
      }
      return biggestBranchUuid;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute request to select the project biggest branch", e);
    }
  }

  private static PreparedStatement getPreparedStatement(List<String> projectBranches, String replacement, DbSession session) throws SQLException {
    String projectBranchesQuestionMarks = projectBranches.stream().map(x -> "?").collect(Collectors.joining(","));
    String sql = StringUtils.replace(replacement, "{projectBranches}", projectBranchesQuestionMarks);
    return session.getConnection().prepareStatement(sql);
  }

  private static void readMeasure(ResultSet rs, Measures measures) throws SQLException {
    String metricKey = rs.getString(FIELD_METRIC_NAME);
    Optional<Double> value = metricKey.startsWith("new_") ? getDouble(rs, FIELD_MEASURE_VARIATION) : getDouble(rs, FIELD_MEASURE_VALUE);
    if (value.isPresent()) {
      measures.addNumericMeasure(metricKey, value.get());
      return;
    }
    if (ALERT_STATUS_KEY.equals(metricKey)) {
      readTextValue(rs, measures::setQualityGateStatus);
      return;
    }
    if (NCLOC_LANGUAGE_DISTRIBUTION_KEY.equals(metricKey)) {
      readTextValue(rs, measures::setNclocByLanguages);
      return;
    }
  }

  private static void readTextValue(ResultSet rs, Consumer<String> action) throws SQLException {
    String textValue = rs.getString(FIELD_MEASURE_TEXT_VALUE);
    if (!rs.wasNull()) {
      action.accept(textValue);
    }
  }

  @Override
  protected void doClose() throws Exception {
    measuresStatement.close();
  }

  private static Optional<Double> getDouble(ResultSet rs, int index) {
    try {
      Double value = rs.getDouble(index);
      if (!rs.wasNull()) {
        return Optional.of(value);
      }
      return Optional.empty();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to get double value", e);
    }
  }

  public static class Project {
    private final String uuid;
    private final String key;
    private final String name;
    private final String qualifier;
    private final Long analysisDate;
    private final List<String> tags;

    public Project(String uuid, String key, String name, String qualifier, List<String> tags, @Nullable Long analysisDate) {
      this.uuid = uuid;
      this.key = key;
      this.name = name;
      this.qualifier = qualifier;
      this.tags = tags;
      this.analysisDate = analysisDate;
    }

    public String getUuid() {
      return uuid;
    }

    public String getKey() {
      return key;
    }

    public String getName() {
      return name;
    }

    public String getQualifier() {
      return qualifier;
    }

    public List<String> getTags() {
      return tags;
    }

    @CheckForNull
    public Long getAnalysisDate() {
      return analysisDate;
    }
  }

  public static class Measures {
    private Map<String, Double> numericMeasures = new HashMap<>();
    private String qualityGateStatus;
    private Map<String, Integer> nclocByLanguages = new LinkedHashMap<>();

    Measures addNumericMeasure(String metricKey, double value) {
      numericMeasures.put(metricKey, value);
      return this;
    }

    public Map<String, Double> getNumericMeasures() {
      return numericMeasures;
    }

    Measures setQualityGateStatus(@Nullable String qualityGateStatus) {
      this.qualityGateStatus = qualityGateStatus;
      return this;
    }

    @CheckForNull
    public String getQualityGateStatus() {
      return qualityGateStatus;
    }

    Measures setNclocByLanguages(String nclocByLangues) {
      this.nclocByLanguages = ImmutableMap.copyOf(parseStringInt(nclocByLangues));
      return this;
    }

    public Map<String, Integer> getNclocByLanguages() {
      return nclocByLanguages;
    }
  }

  public static class ProjectMeasures {
    private Project project;
    private Measures measures;

    public ProjectMeasures(Project project, Measures measures) {
      this.project = project;
      this.measures = measures;
    }

    public Project getProject() {
      return project;
    }

    public Measures getMeasures() {
      return measures;
    }

  }

}
