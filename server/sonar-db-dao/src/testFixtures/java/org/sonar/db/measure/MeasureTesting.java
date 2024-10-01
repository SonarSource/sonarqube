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
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;

import static com.google.common.base.Preconditions.checkNotNull;

public class MeasureTesting {

  private static int cursor = RandomUtils.nextInt(100);

  private MeasureTesting() {
    // static methods only
  }

  public static MeasureDto newMeasureDto(MetricDto metricDto, ComponentDto component, SnapshotDto analysis) {
    checkNotNull(metricDto.getUuid());
    checkNotNull(metricDto.getKey());
    checkNotNull(component.uuid());
    checkNotNull(analysis.getUuid());
    return new MeasureDto()
      .setMetricUuid(metricDto.getUuid())
      .setComponentUuid(component.uuid())
      .setAnalysisUuid(analysis.getUuid());
  }

  public static MeasureDto newMeasure() {
    return new MeasureDto()
      .setMetricUuid(String.valueOf(cursor++))
      .setComponentUuid(String.valueOf(cursor++))
      .setAnalysisUuid(String.valueOf(cursor++))
      .setData(String.valueOf(cursor++))
      .setAlertStatus(String.valueOf(cursor++))
      .setAlertText(String.valueOf(cursor++))
      .setValue((double) cursor++);
  }

  public static LiveMeasureDto newLiveMeasure() {
    return new LiveMeasureDto()
      .setMetricUuid(String.valueOf(cursor++))
      .setComponentUuid(String.valueOf(cursor++))
      .setProjectUuid(String.valueOf(cursor++))
      .setData(String.valueOf(cursor++))
      .setValue((double) cursor++);
  }

  public static LiveMeasureDto newLiveMeasure(ComponentDto component, MetricDto metric) {
    return new LiveMeasureDto()
      .setMetricUuid(metric.getUuid())
      .setComponentUuid(component.uuid())
      .setProjectUuid(component.branchUuid())
      .setData(String.valueOf(cursor++))
      .setValue((double) cursor++);
  }

  public static JsonMeasureDto newJsonMeasure() {
    JsonMeasureDto measureDto = new JsonMeasureDto()
      .setComponentUuid(String.valueOf(cursor++))
      .setBranchUuid(String.valueOf(cursor++))
      .addValue("metric" + cursor++, (double) cursor++);
    measureDto.computeJsonValueHash();
    return measureDto;
  }

  public static LiveMeasureDto createLiveMeasure(MetricDto metricDto, ComponentDto componentDto) {
    BiConsumer<MetricDto, MeasureAdapter> populator = specificLiveMeasurePopulator.getOrDefault(metricDto.getKey(), defaultLiveMeasurePopulator);
    LiveMeasureDto liveMeasureDto = newLiveMeasure(componentDto, metricDto);
    populator.accept(metricDto, new MeasureAdapter(liveMeasureDto));
    return liveMeasureDto;
  }

  public static MeasureDto createProjectMeasure(MetricDto metricDto, SnapshotDto snapshotDto, ComponentDto projectComponentDto) {
    BiConsumer<MetricDto, MeasureAdapter> populator = specificLiveMeasurePopulator.getOrDefault(metricDto.getKey(), defaultLiveMeasurePopulator);
    MeasureDto measureDto = newMeasureDto(metricDto, projectComponentDto, snapshotDto);
    populator.accept(metricDto, new MeasureAdapter(measureDto));
    return measureDto;
  }

  private static final Consumer<MeasureAdapter> ratingMeasurePopulator =
    m -> {
      int rating = ThreadLocalRandom.current().nextInt(1, 5);
      char textValue = (char) ('A' + rating - 1);
      m.setValue((double) rating).setData("" + textValue);
    };

  private static final Map<String, BiConsumer<MetricDto, MeasureAdapter>> specificLiveMeasurePopulator = Map.of(
    CoreMetrics.DEVELOPMENT_COST_KEY, (metric, m) -> m.setData("" + Math.round(ThreadLocalRandom.current().nextDouble(100, 10_000))),
    CoreMetrics.LAST_COMMIT_DATE_KEY, (metric, m) -> m.setValue((double)
      Instant.now().minusSeconds(Math.round(ThreadLocalRandom.current().nextDouble(100_000, 1_000_000))).toEpochMilli()),
    CoreMetrics.SQALE_RATING_KEY, (metric, m) -> ratingMeasurePopulator.accept(m),
    CoreMetrics.RELIABILITY_RATING_KEY, (metric, m) -> ratingMeasurePopulator.accept(m),
    CoreMetrics.SECURITY_REVIEW_RATING_KEY, (metric, m) -> ratingMeasurePopulator.accept(m),
    CoreMetrics.SECURITY_RATING_KEY, (metric, m) -> ratingMeasurePopulator.accept(m),
    CoreMetrics.ALERT_STATUS_KEY, (metric, m) -> {
      boolean isOk = ThreadLocalRandom.current().nextDouble() > 0.5;
      m.setData(isOk ? "OK" : "ERROR");
      m.setAlert(isOk ? "OK" : "ERROR");
    },
    CoreMetrics.QUALITY_GATE_DETAILS_KEY, (metric, m) -> m.setData("{\"level\":\"OK\"}")
  );

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
    private final MeasureDto projectMeasure;
    private final LiveMeasureDto liveMeasure;

    private MeasureAdapter(LiveMeasureDto liveMeasure) {
      this.projectMeasure = null;
      this.liveMeasure = liveMeasure;
    }

    private MeasureAdapter(MeasureDto projectMeasure) {
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
