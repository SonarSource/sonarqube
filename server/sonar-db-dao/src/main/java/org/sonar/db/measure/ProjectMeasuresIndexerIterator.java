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
package org.sonar.db.measure;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.core.metric.SoftwareQualitiesMetrics;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;

import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.api.utils.KeyValueFormat.parseStringInt;
import static org.sonar.db.component.DbTagsReader.readDbTags;

public class ProjectMeasuresIndexerIterator extends CloseableIterator<ProjectMeasuresIndexerIterator.ProjectMeasures> {

  public static final Set<String> METRIC_KEYS = ImmutableSortedSet.of(
    NCLOC_KEY,
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
    CoreMetrics.NEW_RELIABILITY_RATING_KEY,

    //Ratings based on software quality
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY
  );

  private static final Gson GSON = new Gson();

  private static final String SQL_PROJECTS = """
    SELECT p.uuid, p.kee, p.name, p.created_at, s.created_at, p.tags, p.qualifier, p.ncloc
    FROM projects p
    INNER JOIN project_branches pb ON pb.project_uuid = p.uuid AND pb.is_main = ?
    LEFT OUTER JOIN snapshots s ON s.root_component_uuid=pb.uuid AND s.islast=?
    WHERE p.qualifier in (?, ?)""";

  private static final String PROJECT_FILTER = " AND pb.project_uuid=?";

  private static final String SQL_GENERIC_METRICS = """
    SELECT m.name
    FROM metrics m
    WHERE m.enabled = ?""";

  private static final String SQL_MEASURES = """
    SELECT m.json_value
    FROM measures m
    INNER JOIN project_branches pb ON pb.uuid = m.component_uuid
    WHERE pb.project_uuid = ?
    AND pb.is_main = ?""";

  private static final String SQL_NCLOC_LANGUAGE_DISTRIBUTION = """
    SELECT m.component_uuid, m.branch_uuid, m.json_value
    FROM measures m
    WHERE m.branch_uuid = ?""";

  private static final String SQL_BRANCH_BY_NCLOC = """
    SELECT m.component_uuid, m.json_value
    FROM measures m
    INNER JOIN project_branches pb ON m.component_uuid = pb.uuid
    WHERE pb.project_uuid = ?""";

  private static final boolean ENABLED = true;
  public static final int JSON_VALUE_FIELD = 2;

  private final DbSession dbSession;
  private final PreparedStatement measuresStatement;
  private final Iterator<Project> projects;
  private final List<String> metrics;

  private ProjectMeasuresIndexerIterator(DbSession dbSession, PreparedStatement measuresStatement, List<Project> projects, List<String> metrics) {
    this.dbSession = dbSession;
    this.measuresStatement = measuresStatement;
    this.projects = projects.iterator();
    this.metrics = metrics;
  }

  public static ProjectMeasuresIndexerIterator create(DbSession session, @Nullable String projectUuid) {
    List<Project> projects = selectProjects(session, projectUuid);
    PreparedStatement projectsStatement = createMeasuresStatement(session);
    return new ProjectMeasuresIndexerIterator(session, projectsStatement, projects, selectMetrics(session));
  }

  private static List<String> selectMetrics(DbSession session) {
    List<String> metrics = new ArrayList<>();
    try (PreparedStatement stmt = createMetricsStatement(session);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        String key = rs.getString(1);
        metrics.add(key);
      }
      return metrics;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute request to select all metrics", e);
    }
  }

  private static PreparedStatement createMetricsStatement(DbSession session) {
    try {
      PreparedStatement stmt = session.getConnection().prepareStatement(SQL_GENERIC_METRICS);
      stmt.setBoolean(1, ENABLED);
      return stmt;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all metrics", e);
    }
  }

  private static List<Project> selectProjects(DbSession session, @Nullable String projectUuid) {
    List<Project> projects = new ArrayList<>();
    try (PreparedStatement stmt = createProjectsStatement(session, projectUuid);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        String uuid = rs.getString(1);
        String key = rs.getString(2);
        String name = rs.getString(3);
        Long creationDate = rs.getLong(4);
        Long analysisDate = DatabaseUtils.getLong(rs, 5);
        List<String> tags = readDbTags(DatabaseUtils.getString(rs, 6));
        String qualifier = rs.getString(7);
        Long ncloc = DatabaseUtils.getLong(rs, 8);
        Project project = new Project.Builder()
          .setUuid(uuid)
          .setKey(key)
          .setName(name)
          .setQualifier(qualifier)
          .setTags(tags)
          .setCreationDate(creationDate)
          .setAnalysisDate(analysisDate)
          .setNcloc(ncloc)
          .build();
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
      stmt.setBoolean(2, true);
      stmt.setString(3, ComponentQualifiers.PROJECT);
      stmt.setString(4, ComponentQualifiers.APP);
      if (projectUuid != null) {
        stmt.setString(5, projectUuid);
      }
      return stmt;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all project measures", e);
    }
  }

  private static PreparedStatement createMeasuresStatement(DbSession session) {
    try {
      return session.getConnection().prepareStatement(SQL_MEASURES);
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
    Measures measures = selectMeasures(project);
    return new ProjectMeasures(project, measures);
  }

  private Measures selectMeasures(Project project) {
    String projectUuid = project.getUuid();
    try {
      Measures measures = getMeasures(projectUuid);

      Optional<String> biggestBranch = project.getNcloc()
        .flatMap(ncloc -> selectProjectBranchForNcloc(dbSession, projectUuid, ncloc));

      if (biggestBranch.isPresent()) {
        readNclocDistributionFromBiggestBranch(biggestBranch.get(), measures);
      }

      return measures;
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Fail to execute request to select measures of project %s", projectUuid), e);
    }
  }

  private Measures getMeasures(String projectUuid) throws SQLException {
    Measures measures = new Measures();
    measuresStatement.setString(1, projectUuid);
    measuresStatement.setBoolean(2, true);

    try (ResultSet rs = measuresStatement.executeQuery()) {
      while (rs.next()) {
        readMeasureLine(rs, measures);
      }
    }
    return measures;
  }

  private void readMeasureLine(ResultSet rs, Measures measures) throws SQLException {
    String jsonValue = rs.getString(1);
    Map<String, Object> metricValues = GSON.fromJson(jsonValue, new TypeToken<Map<String, Object>>() {
    }.getType());

    metricValues.forEach((metricKey, value) -> readMeasure(measures, metricKey, value));
  }

  private void readMeasure(Measures measures, String metricKey, Object value) {
    if (METRIC_KEYS.contains(metricKey) && metrics.contains(metricKey)) {
      if (ALERT_STATUS_KEY.equals(metricKey) && value instanceof String qualityGate) {
        measures.setQualityGateStatus(qualityGate);
        return;
      }
      if (NCLOC_LANGUAGE_DISTRIBUTION_KEY.equals(metricKey) && value instanceof String distribution) {
        measures.setNclocByLanguages(distribution);
        return;
      }
      if (value instanceof Double doubleValue) {
        measures.addNumericMeasure(metricKey, doubleValue);
      }
    }
  }

  private void readNclocDistributionFromBiggestBranch(String biggestBranch, Measures measures) throws SQLException {
    try (PreparedStatement prepareNclocByLanguageStatement = prepareNclocByLanguageStatement(dbSession, biggestBranch);
         ResultSet rs = prepareNclocByLanguageStatement.executeQuery()) {
      if (rs.next()) {
        readNclocDistributionKey(rs, measures);
      }
    }
  }

  private static PreparedStatement prepareNclocByLanguageStatement(DbSession session, String branchUuid) {
    try {
      PreparedStatement stmt = session.getConnection().prepareStatement(SQL_NCLOC_LANGUAGE_DISTRIBUTION);
      stmt.setString(1, branchUuid);
      return stmt;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute request to select ncloc_language_distribution measure", e);
    }
  }

  private static Optional<String> selectProjectBranchForNcloc(DbSession session, String projectUuid, long ncloc) {
    try (PreparedStatement nclocStatement = session.getConnection().prepareStatement(SQL_BRANCH_BY_NCLOC)) {
      nclocStatement.setString(1, projectUuid);

      try (ResultSet rs = nclocStatement.executeQuery()) {
        return readBranchMeasures(rs, ncloc);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute request to select the project biggest branch", e);
    }
  }

  private static Optional<String> readBranchMeasures(ResultSet rs, long ncloc) throws SQLException {
    while (rs.next()) {
      String jsonValue = rs.getString(JSON_VALUE_FIELD);
      Map<String, Object> metricValues = GSON.fromJson(jsonValue, new TypeToken<Map<String, Object>>() {
      }.getType());

      if (metricValues.containsKey(NCLOC_KEY)) {
        Object nclocValue = metricValues.get(NCLOC_KEY);
        if (nclocValue instanceof Double branchNcloc && branchNcloc == ncloc) {
          return Optional.of(rs.getString(1));
        }
      }
    }
    return Optional.empty();
  }

  private static void readNclocDistributionKey(ResultSet rs, Measures measures) throws SQLException {
    String jsonValue = rs.getString(3);
    Map<String, Object> metricValues = GSON.fromJson(jsonValue, new TypeToken<Map<String, Object>>() {
    }.getType());

    if (metricValues.containsKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY)) {
      Object distribution = metricValues.get(NCLOC_LANGUAGE_DISTRIBUTION_KEY);
      if (distribution instanceof String stringDistribution) {
        measures.setNclocByLanguages(stringDistribution);
      }
    }
  }

  @Override
  protected void doClose() throws Exception {
    measuresStatement.close();
  }

  public static class Project {
    private final String uuid;
    private final String key;
    private final String name;
    private final String qualifier;
    private final Long creationDate;
    private final Long analysisDate;
    private final List<String> tags;
    private final Long ncloc;

    private Project(Builder builder) {
      this.uuid = builder.uuid;
      this.key = builder.key;
      this.name = builder.name;
      this.qualifier = builder.qualifier;
      this.tags = builder.tags;
      this.analysisDate = builder.analysisDate;
      this.creationDate = builder.creationDate;
      this.ncloc = builder.ncloc;
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

    public Long getCreationDate() {
      return creationDate;
    }

    public Optional<Long> getNcloc() {
      return Optional.ofNullable(ncloc);
    }

    private static class Builder {
      private String uuid;
      private String key;
      private String name;
      private String qualifier;
      private Long creationDate;
      private Long analysisDate;
      private List<String> tags;
      private Long ncloc;

      public Builder setUuid(String uuid) {
        this.uuid = uuid;
        return this;
      }

      public Builder setKey(String key) {
        this.key = key;
        return this;
      }

      public Builder setName(String name) {
        this.name = name;
        return this;
      }

      public Builder setQualifier(String qualifier) {
        this.qualifier = qualifier;
        return this;
      }

      public Builder setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
        return this;
      }

      public Builder setAnalysisDate(@Nullable Long analysisDate) {
        this.analysisDate = analysisDate;
        return this;
      }

      public Builder setTags(List<String> tags) {
        this.tags = tags;
        return this;
      }

      public Builder setNcloc(@Nullable Long ncloc) {
        this.ncloc = ncloc;
        return this;
      }

      public Project build() {
        return new Project(this);
      }
    }
  }

  public static class Measures {
    private final Map<String, Double> numericMeasures = new HashMap<>();
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
    private final Project project;
    private final Measures measures;

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
