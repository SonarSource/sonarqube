/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.measures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.KeyValueFormat;

/**
 * @since 2.7
 * @deprecated since 5.2 use {@link SensorContext#newCoverage()}
 */
@Deprecated
public final class CoverageMeasuresBuilder {

  /**
   * Metrics of generated measures
   */
  public static final List<Metric> METRICS = Arrays.<Metric>asList(
    CoreMetrics.LINES_TO_COVER, CoreMetrics.UNCOVERED_LINES, CoreMetrics.COVERAGE_LINE_HITS_DATA,
    CoreMetrics.CONDITIONS_TO_COVER, CoreMetrics.UNCOVERED_CONDITIONS, CoreMetrics.CONDITIONS_BY_LINE,
    CoreMetrics.COVERED_CONDITIONS_BY_LINE);

  private int totalCoveredLines = 0;
  private int totalConditions = 0;
  private int totalCoveredConditions = 0;
  private SortedMap<Integer, Integer> hitsByLine = new TreeMap<>();
  private SortedMap<Integer, Integer> conditionsByLine = new TreeMap<>();
  private SortedMap<Integer, Integer> coveredConditionsByLine = new TreeMap<>();

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

  public Collection<Measure> createMeasures() {
    Collection<Measure> measures = new ArrayList<>();
    if (getLinesToCover() > 0) {
      measures.add(new Measure(CoreMetrics.LINES_TO_COVER, (double) getLinesToCover()));
      measures.add(new Measure(CoreMetrics.UNCOVERED_LINES, (double) (getLinesToCover() - getCoveredLines())));
      measures.add(new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA).setData(KeyValueFormat.format(hitsByLine)));
    }
    if (getConditions() > 0) {
      measures.add(new Measure(CoreMetrics.CONDITIONS_TO_COVER, (double) getConditions()));
      measures.add(new Measure(CoreMetrics.UNCOVERED_CONDITIONS, (double) (getConditions() - getCoveredConditions())));
      measures.add(createConditionsByLine());
      measures.add(createCoveredConditionsByLine());
    }
    return measures;
  }

  private Measure createCoveredConditionsByLine() {
    return new Measure(CoreMetrics.COVERED_CONDITIONS_BY_LINE)
      .setData(KeyValueFormat.format(coveredConditionsByLine));
  }

  private Measure createConditionsByLine() {
    return new Measure(CoreMetrics.CONDITIONS_BY_LINE)
      .setData(KeyValueFormat.format(conditionsByLine));
  }

  public static CoverageMeasuresBuilder create() {
    return new CoverageMeasuresBuilder();
  }

}
