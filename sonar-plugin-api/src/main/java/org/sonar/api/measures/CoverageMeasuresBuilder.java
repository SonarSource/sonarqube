/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.measures;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.KeyValueFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

/**
 * @since 2.7
 */
public final class CoverageMeasuresBuilder {

  /**
   * Metrics of generated measures
   */
  public static final List<Metric> METRICS = Arrays.<Metric>asList(
    CoreMetrics.LINES_TO_COVER, CoreMetrics.UNCOVERED_LINES, CoreMetrics.COVERAGE_LINE_HITS_DATA,
    CoreMetrics.CONDITIONS_TO_COVER, CoreMetrics.UNCOVERED_CONDITIONS, CoreMetrics.CONDITIONS_BY_LINE,
    CoreMetrics.COVERED_CONDITIONS_BY_LINE);

  private int totalCoveredLines = 0, totalConditions = 0, totalCoveredConditions = 0;
  private SortedMap<Integer, Integer> hitsByLine = Maps.newTreeMap();
  private SortedMap<Integer, Integer> conditionsByLine = Maps.newTreeMap();
  private SortedMap<Integer, Integer> coveredConditionsByLine = Maps.newTreeMap();

  private CoverageMeasuresBuilder() {
    // use the factory
  }

  public CoverageMeasuresBuilder reset() {
    totalCoveredLines = 0;
    totalConditions = 0;
    totalCoveredConditions = 0;
    hitsByLine.clear();
    conditionsByLine.clear();
    coveredConditionsByLine.clear();
    return this;
  }

  public CoverageMeasuresBuilder setHits(int lineId, int hits) {
    if (!hitsByLine.containsKey(lineId)) {
      hitsByLine.put(lineId, hits);
      if (hits > 0) {
        totalCoveredLines += 1;
      }
    }
    return this;
  }

  public CoverageMeasuresBuilder setConditions(int lineId, int conditions, int coveredConditions) {
    if (conditions > 0 && !conditionsByLine.containsKey(lineId)) {
      totalConditions += conditions;
      totalCoveredConditions += coveredConditions;
      conditionsByLine.put(lineId, conditions);
      coveredConditionsByLine.put(lineId, coveredConditions);
    }
    return this;
  }

  public int getCoveredLines() {
    return totalCoveredLines;
  }

  public int getLinesToCover() {
    return hitsByLine.size();
  }

  public int getConditions() {
    return totalConditions;
  }

  public int getCoveredConditions() {
    return totalCoveredConditions;
  }

  public SortedMap<Integer, Integer> getHitsByLine() {
    return Collections.unmodifiableSortedMap(hitsByLine);
  }

  public SortedMap<Integer, Integer> getConditionsByLine() {
    return Collections.unmodifiableSortedMap(conditionsByLine);
  }

  public SortedMap<Integer, Integer> getCoveredConditionsByLine() {
    return Collections.unmodifiableSortedMap(coveredConditionsByLine);
  }

  /**
   * @deprecated since 5.0 use {@link #createMeasures(SensorContext, InputFile)}
   */
  @Deprecated
  public Collection<Measure> createMeasures() {
    Collection<Measure> measures = Lists.newArrayList();
    if (getLinesToCover() > 0) {
      measures.add(new Measure(CoreMetrics.LINES_TO_COVER, (double) getLinesToCover()));
      measures.add(new Measure(CoreMetrics.UNCOVERED_LINES, (double) (getLinesToCover() - getCoveredLines())));
      measures.add(new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA).setData(KeyValueFormat.format(hitsByLine)).setPersistenceMode(PersistenceMode.DATABASE));
    }
    if (getConditions() > 0) {
      measures.add(new Measure(CoreMetrics.CONDITIONS_TO_COVER, (double) getConditions()));
      measures.add(new Measure(CoreMetrics.UNCOVERED_CONDITIONS, (double) (getConditions() - getCoveredConditions())));
      measures.add(createConditionsByLine());
      measures.add(createCoveredConditionsByLine());
    }
    return measures;
  }

  public Collection<org.sonar.api.batch.sensor.measure.Measure> createMeasures(SensorContext context, InputFile onFile) {
    Collection<org.sonar.api.batch.sensor.measure.Measure> measures = Lists.newArrayList();
    if (getLinesToCover() > 0) {
      measures.add(context.<Integer>measureBuilder()
        .onFile(onFile)
        .forMetric(CoreMetrics.LINES_TO_COVER)
        .withValue(getLinesToCover())
        .build());
      measures.add(context.<Integer>measureBuilder()
        .onFile(onFile)
        .forMetric(CoreMetrics.UNCOVERED_LINES)
        .withValue(getLinesToCover() - getCoveredLines())
        .build());
      measures.add(context.<String>measureBuilder()
        .onFile(onFile)
        .forMetric(CoreMetrics.COVERAGE_LINE_HITS_DATA)
        .withValue(KeyValueFormat.format(hitsByLine))
        .build());
    }
    if (getConditions() > 0) {
      measures.add(context.<Integer>measureBuilder()
        .onFile(onFile)
        .forMetric(CoreMetrics.CONDITIONS_TO_COVER)
        .withValue(getConditions())
        .build());
      measures.add(context.<Integer>measureBuilder()
        .onFile(onFile)
        .forMetric(CoreMetrics.UNCOVERED_CONDITIONS)
        .withValue(getConditions() - getCoveredConditions())
        .build());
      measures.add(createConditionsByLine(context, onFile));
      measures.add(createCoveredConditionsByLine(context, onFile));
    }
    return measures;
  }

  private Measure createCoveredConditionsByLine() {
    return new Measure(CoreMetrics.COVERED_CONDITIONS_BY_LINE)
      .setData(KeyValueFormat.format(coveredConditionsByLine))
      .setPersistenceMode(PersistenceMode.DATABASE);
  }

  private Measure createConditionsByLine() {
    return new Measure(CoreMetrics.CONDITIONS_BY_LINE)
      .setData(KeyValueFormat.format(conditionsByLine))
      .setPersistenceMode(PersistenceMode.DATABASE);
  }

  private org.sonar.api.batch.sensor.measure.Measure createCoveredConditionsByLine(SensorContext context, InputFile onFile) {
    return context.<String>measureBuilder()
      .onFile(onFile)
      .forMetric(CoreMetrics.COVERED_CONDITIONS_BY_LINE)
      .withValue(KeyValueFormat.format(coveredConditionsByLine))
      .build();
  }

  private org.sonar.api.batch.sensor.measure.Measure createConditionsByLine(SensorContext context, InputFile onFile) {
    return context.<String>measureBuilder()
      .onFile(onFile)
      .forMetric(CoreMetrics.CONDITIONS_BY_LINE)
      .withValue(KeyValueFormat.format(conditionsByLine))
      .build();
  }

  public static CoverageMeasuresBuilder create() {
    return new CoverageMeasuresBuilder();
  }

}
