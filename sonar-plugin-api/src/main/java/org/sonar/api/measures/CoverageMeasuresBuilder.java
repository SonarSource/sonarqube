/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.measures;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.utils.KeyValueFormat;

import java.util.*;

/**
 * @since 2.7
 */
public final class CoverageMeasuresBuilder {

  /**
   * Metrics of generated measures
   */
  public static final List<Metric> METRICS = Arrays.asList(
      CoreMetrics.LINES_TO_COVER, CoreMetrics.UNCOVERED_LINES, CoreMetrics.COVERAGE_LINE_HITS_DATA,
      CoreMetrics.CONDITIONS_TO_COVER, CoreMetrics.UNCOVERED_CONDITIONS, CoreMetrics.CONDITIONS_BY_LINE_DATA,
      CoreMetrics.COVERED_CONDITIONS_BY_LINE_DATA, CoreMetrics.BRANCH_COVERAGE_HITS_DATA);


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
    if (hitsByLine.containsKey(lineId)) {
      throw new IllegalArgumentException("Line " + lineId + " is count twice (hits=" + hits + ")");
    }
    hitsByLine.put(lineId, hits);
    if (hits > 0) {
      totalCoveredLines += 1;
    }
    return this;
  }

  public CoverageMeasuresBuilder setConditions(int lineId, int conditions, int coveredConditions) {
    if (conditionsByLine.containsKey(lineId)) {
      throw new IllegalArgumentException("Line " + lineId + " is count twice (conditions=" + conditions + ")");
    }
    if (conditions > 0) {
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
    Collection<Measure> measures = Lists.newArrayList();
    if (getLinesToCover() > 0) {
      measures.add(new Measure(CoreMetrics.LINES_TO_COVER, (double) getLinesToCover()));
      measures.add(new Measure(CoreMetrics.UNCOVERED_LINES, (double) (getLinesToCover() - getCoveredLines())));
      measures.add(new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA).setData(KeyValueFormat.format(hitsByLine)).setPersistenceMode(PersistenceMode.DATABASE));
    }
    if (getConditions() > 0) {
      measures.add(new Measure(CoreMetrics.CONDITIONS_TO_COVER, (double) getConditions()));
      measures.add(new Measure(CoreMetrics.UNCOVERED_CONDITIONS, (double) (getConditions() - getCoveredConditions())));
      measures.add(createConditionsByLineData());
      measures.add(createCoveredConditionsByLineData());
      measures.add(createBranchCoverageByLine());
    }
    return measures;
  }

  private Measure createCoveredConditionsByLineData() {
    return new Measure(CoreMetrics.COVERED_CONDITIONS_BY_LINE_DATA)
        .setData(KeyValueFormat.format(coveredConditionsByLine))
        .setPersistenceMode(PersistenceMode.DATABASE);
  }

  private Measure createConditionsByLineData() {
    return new Measure(CoreMetrics.CONDITIONS_BY_LINE_DATA)
        .setData(KeyValueFormat.format(conditionsByLine))
        .setPersistenceMode(PersistenceMode.DATABASE);
  }

  private Measure createBranchCoverageByLine() {
    PropertiesBuilder<Integer, String> builder = new PropertiesBuilder<Integer, String>(CoreMetrics.BRANCH_COVERAGE_HITS_DATA);
    for (Map.Entry<Integer, Integer> entry : conditionsByLine.entrySet()) {
      Integer lineId = entry.getKey();
      int conditions = entry.getValue();
      int coveredConditions = coveredConditionsByLine.get(lineId);
      builder.add(lineId, formatBranchCoverage(conditions, coveredConditions));
    }
    return builder.build().setPersistenceMode(PersistenceMode.DATABASE);
  }

  static String formatBranchCoverage(int conditions, int coveredConditions) {
    long branchCoverage = Math.round(100.0 * coveredConditions / conditions);
    return branchCoverage + "%";
  }

  public static CoverageMeasuresBuilder create() {
    return new CoverageMeasuresBuilder();
  }
}
