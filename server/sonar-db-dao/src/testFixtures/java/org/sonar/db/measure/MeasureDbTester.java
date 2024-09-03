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
package org.sonar.db.measure;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;

import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;
import static org.sonar.db.measure.MeasureTesting.newProjectMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;

public class MeasureDbTester {
  private final DbClient dbClient;
  private final DbTester db;

  public MeasureDbTester(DbTester db) {
    this.dbClient = db.getDbClient();
    this.db = db;
  }

  @SafeVarargs
  public final ProjectMeasureDto insertMeasureWithSensibleValues(ComponentDto component, SnapshotDto analysis, MetricDto metricDto, Consumer<ProjectMeasureDto>... consumers) {
    ProjectMeasureDto measureDto = createProjectMeasure(metricDto, analysis, component);
    Arrays.stream(consumers).forEach(c -> c.accept(measureDto));
    dbClient.projectMeasureDao().insert(db.getSession(), measureDto);
    db.commit();
    return measureDto;
  }

  @SafeVarargs
  public final ProjectMeasureDto insertProjectMeasure(ComponentDto component, SnapshotDto analysis, MetricDto metricDto, Consumer<ProjectMeasureDto>... consumers) {
    ProjectMeasureDto projectMeasureDto = newProjectMeasureDto(metricDto, component, analysis);
    Arrays.stream(consumers).forEach(c -> c.accept(projectMeasureDto));
    dbClient.projectMeasureDao().insert(db.getSession(), projectMeasureDto);
    db.commit();
    return projectMeasureDto;
  }

  @SafeVarargs
  public final ProjectMeasureDto insertProjectMeasure(BranchDto branchDto, SnapshotDto analysis, MetricDto metricDto, Consumer<ProjectMeasureDto>... consumers) {
    ProjectMeasureDto projectMeasureDto = MeasureTesting.newProjectMeasureDto(metricDto, branchDto.getUuid(), analysis);
    Arrays.stream(consumers).forEach(c -> c.accept(projectMeasureDto));
    dbClient.projectMeasureDao().insert(db.getSession(), projectMeasureDto);
    db.commit();
    return projectMeasureDto;
  }

  @SafeVarargs
  public final LiveMeasureDto insertLiveMeasureWithSensibleValues(ComponentDto component, MetricDto metric, Consumer<LiveMeasureDto>... consumers) {
    LiveMeasureDto dto = createLiveMeasure(metric, component);
    Arrays.stream(consumers).forEach(c -> c.accept(dto));
    dbClient.liveMeasureDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  @SafeVarargs
  public final LiveMeasureDto insertLiveMeasure(ComponentDto component, MetricDto metric, Consumer<LiveMeasureDto>... consumers) {
    LiveMeasureDto dto = newLiveMeasure(component, metric);
    Arrays.stream(consumers).forEach(c -> c.accept(dto));
    dbClient.liveMeasureDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  @SafeVarargs
  public final LiveMeasureDto insertLiveMeasure(BranchDto branchDto, MetricDto metric, Consumer<LiveMeasureDto>... consumers) {
    LiveMeasureDto dto = newLiveMeasure(branchDto, metric);
    Arrays.stream(consumers).forEach(c -> c.accept(dto));
    dbClient.liveMeasureDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  @SafeVarargs
  public final LiveMeasureDto insertLiveMeasure(ProjectData projectData, MetricDto metric, Consumer<LiveMeasureDto>... consumers) {
    return insertLiveMeasure(projectData.getMainBranchComponent(), metric, consumers);
  }

  @SafeVarargs
  public final MeasureDto insertMeasure(ComponentDto component, Consumer<MeasureDto>... consumers) {
    MeasureDto dto = new MeasureDto()
      .setComponentUuid(component.uuid())
      .setBranchUuid(component.branchUuid());
    Arrays.stream(consumers).forEach(c -> c.accept(dto));
    dbClient.measureDao().insertOrUpdate(db.getSession(), dto);
    db.getSession().commit();
    return dto;
  }

  @SafeVarargs
  public final MetricDto insertMetric(Consumer<MetricDto>... consumers) {
    MetricDto metricDto = newMetricDto();
    Arrays.stream(consumers).forEach(c -> c.accept(metricDto));
    dbClient.metricDao().insert(db.getSession(), metricDto);
    db.commit();
    return metricDto;
  }

  public static LiveMeasureDto createLiveMeasure(MetricDto metricDto, ComponentDto componentDto) {
    BiConsumer<MetricDto, MeasureAdapter> populator = specificLiveMeasurePopulator.getOrDefault(metricDto.getKey(), defaultLiveMeasurePopulator);
    LiveMeasureDto liveMeasureDto = newLiveMeasure(componentDto, metricDto);
    populator.accept(metricDto, new MeasureAdapter(liveMeasureDto));
    return liveMeasureDto;
  }

  public static ProjectMeasureDto createProjectMeasure(MetricDto metricDto, SnapshotDto snapshotDto, ComponentDto projectComponentDto) {
    BiConsumer<MetricDto, MeasureAdapter> populator = specificLiveMeasurePopulator.getOrDefault(metricDto.getKey(), defaultLiveMeasurePopulator);
    ProjectMeasureDto measureDto = newProjectMeasureDto(metricDto, projectComponentDto, snapshotDto);
    populator.accept(metricDto, new MeasureAdapter(measureDto));
    return measureDto;
  }

  private static final Consumer<MeasureAdapter> ratingMeasurePopulator =
    m -> {
      int rating = ThreadLocalRandom.current().nextInt(1, 5);
      char textValue = (char) ('A' + rating - 1);
      m.setValue((double) rating).setData("" + textValue);
    };

  private static final Map<String, BiConsumer<MetricDto, MeasureAdapter>> specificLiveMeasurePopulator = new HashMap<>() {
    {
      put(CoreMetrics.DEVELOPMENT_COST_KEY, (metric, m) -> m.setData("" + Math.round(ThreadLocalRandom.current().nextDouble(100, 10_000))));
      put(CoreMetrics.LAST_COMMIT_DATE_KEY,
        (metric, m) -> m.setValue((double) Instant.now().minusSeconds(Math.round(ThreadLocalRandom.current().nextDouble(100_000, 1_000_000))).toEpochMilli()));
      put(CoreMetrics.SQALE_RATING_KEY, (metric, m) -> ratingMeasurePopulator.accept(m));
      put(CoreMetrics.RELIABILITY_RATING_KEY, (metric, m) -> ratingMeasurePopulator.accept(m));
      put(CoreMetrics.SECURITY_REVIEW_RATING_KEY, (metric, m) -> ratingMeasurePopulator.accept(m));
      put(CoreMetrics.SECURITY_RATING_KEY, (metric, m) -> ratingMeasurePopulator.accept(m));
      put(CoreMetrics.ALERT_STATUS_KEY, (metric, m) -> {
        boolean isOk = ThreadLocalRandom.current().nextDouble() > 0.5;
        m.setData(isOk ? "OK" : "ERROR");
        m.setAlert(isOk ? "OK" : "ERROR");
      });
      put(CoreMetrics.QUALITY_GATE_DETAILS_KEY, (metric, m) -> m.setData("{\"level\":\"OK\",\"conditions\":[],\"ignoredConditions\":false}"));
      put(CoreMetrics.QUALITY_PROFILES_KEY, (metric, m) -> m.setData("[{\"key\":\"62969160-dcda-40a0-9981-772b7820d587\",\"language\":\"xml\",\"name\":\"Sonar way\",\"rulesUpdatedAt\":\"2024-09-11T14:40:55+0000\"}]"));
      put(CoreMetrics.RELIABILITY_ISSUES_KEY, (metric, m) -> m.setData("{\"LOW\":0,\"MEDIUM\":1,\"HIGH\":0,\"total\":1}"));
      put(CoreMetrics.MAINTAINABILITY_ISSUES_KEY, (metric, m) -> m.setData("{\"LOW\":0,\"MEDIUM\":1,\"HIGH\":0,\"total\":1}"));
      put(CoreMetrics.SECURITY_ISSUES_KEY, (metric, m) -> m.setData("{\"LOW\":0,\"MEDIUM\":1,\"HIGH\":0,\"total\":1}"));
    }
  };

  private static final BiConsumer<MetricDto, MeasureAdapter> defaultLiveMeasurePopulator =
    (metric, m) -> {
      int min, max;
      if (metric.getWorstValue() != null && metric.getBestValue() != null) {
        min = (int) Math.min(metric.getBestValue(), metric.getWorstValue());
        max = (int) Math.max(metric.getBestValue(), metric.getWorstValue());
      } else if (metric.getDirection() != 0) {
        int worst, best;
        if (metric.getWorstValue() != null) {
          worst = metric.getWorstValue().intValue();
          best = -metric.getDirection() * 100;
        } else if (metric.getBestValue() != null) {
          best = metric.getBestValue().intValue();
          worst = best - metric.getDirection() * 100;
        } else {
          worst = 0;
          best = -metric.getDirection() * 100;
        }
        min = Math.min(best, worst);
        max = Math.max(best, worst);
      } else {
        min = 0;
        max = 100;
      }

      m.setValue((double) Math.round(ThreadLocalRandom.current().nextDouble(min, max)));
    };

  private static class MeasureAdapter {
    private final ProjectMeasureDto projectMeasure;
    private final LiveMeasureDto liveMeasure;

    private MeasureAdapter(LiveMeasureDto liveMeasure) {
      this.projectMeasure = null;
      this.liveMeasure = liveMeasure;
    }

    private MeasureAdapter(ProjectMeasureDto projectMeasure) {
      this.projectMeasure = projectMeasure;
      this.liveMeasure = null;
    }

    public MeasureAdapter setValue(Double value) {
      if (projectMeasure != null) {
        projectMeasure.setValue(value);
      } else if (liveMeasure != null) {
        liveMeasure.setValue(value);
      }
      return this;
    }

    public MeasureAdapter setData(String data) {
      if (projectMeasure != null) {
        projectMeasure.setData(data);
      } else if (liveMeasure != null) {
        liveMeasure.setData(data);
      }
      return this;
    }

    public MeasureAdapter setAlert(String value) {
      if (projectMeasure != null) {
        projectMeasure.setAlertText(value).setAlertStatus(value);
      }
      return this;
    }
  }

}
