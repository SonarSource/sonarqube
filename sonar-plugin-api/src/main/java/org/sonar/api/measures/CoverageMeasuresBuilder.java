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
   * @since 5.1
   */
  public static enum CoverageType {
    UNIT(CoreMetrics.LINES_TO_COVER, CoreMetrics.UNCOVERED_LINES, CoreMetrics.COVERAGE_LINE_HITS_DATA,
      CoreMetrics.CONDITIONS_TO_COVER, CoreMetrics.UNCOVERED_CONDITIONS, CoreMetrics.CONDITIONS_BY_LINE,
      CoreMetrics.COVERED_CONDITIONS_BY_LINE),
    IT(CoreMetrics.IT_LINES_TO_COVER, CoreMetrics.IT_UNCOVERED_LINES, CoreMetrics.IT_COVERAGE_LINE_HITS_DATA,
      CoreMetrics.IT_CONDITIONS_TO_COVER, CoreMetrics.IT_UNCOVERED_CONDITIONS, CoreMetrics.IT_CONDITIONS_BY_LINE,
      CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE),
    OVERALL(CoreMetrics.OVERALL_LINES_TO_COVER, CoreMetrics.OVERALL_UNCOVERED_LINES, CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA,
      CoreMetrics.OVERALL_CONDITIONS_TO_COVER, CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, CoreMetrics.OVERALL_CONDITIONS_BY_LINE,
      CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE);

    private final Metric<Integer> linesToCover;
    private final Metric<Integer> uncoveredLines;
    private final Metric<String> lineHits;
    private final Metric<Integer> conditionsToCover;
    private final Metric<Integer> uncoveredConditions;
    private final Metric<String> conditionsByLine;
    private final Metric<String> coveredConditionsByLine;

    private CoverageType(Metric<Integer> linesToCover, Metric<Integer> uncoveredLines, Metric<String> lineHits, Metric<Integer> conditionsToCover,
      Metric<Integer> uncoveredConditions, Metric<String> conditionsByLine, Metric<String> coveredConditionsByLine) {
      this.linesToCover = linesToCover;
      this.uncoveredLines = uncoveredLines;
      this.lineHits = lineHits;
      this.conditionsToCover = conditionsToCover;
      this.uncoveredConditions = uncoveredConditions;
      this.conditionsByLine = conditionsByLine;
      this.coveredConditionsByLine = coveredConditionsByLine;
    }

    public List<Metric> all() {
      return Arrays.<Metric>asList(linesToCover, uncoveredLines, lineHits, conditionsToCover, uncoveredConditions, conditionsByLine, coveredConditionsByLine);
    }
  }

  /**
   * Metrics of generated measures
   */
  public static final List<Metric> METRICS = CoverageType.UNIT.all();

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

  public Collection<Measure> createMeasures() {
    return createMeasures(CoverageType.UNIT);
  }

  public Collection<Measure> createMeasures(CoverageType type) {
    Collection<Measure> measures = Lists.newArrayList();
    if (getLinesToCover() > 0) {
      measures.add(new Measure(type.linesToCover, (double) getLinesToCover()));
      measures.add(new Measure(type.uncoveredLines, (double) (getLinesToCover() - getCoveredLines())));
      measures.add(new Measure(type.lineHits).setData(KeyValueFormat.format(hitsByLine)).setPersistenceMode(PersistenceMode.DATABASE));
    }
    if (getConditions() > 0) {
      measures.add(new Measure(type.conditionsToCover, (double) getConditions()));
      measures.add(new Measure(type.uncoveredConditions, (double) (getConditions() - getCoveredConditions())));
      measures.add(createConditionsByLine(type));
      measures.add(createCoveredConditionsByLine(type));
    }
    return measures;
  }

  private Measure createCoveredConditionsByLine(CoverageType type) {
    return new Measure(type.coveredConditionsByLine)
      .setData(KeyValueFormat.format(coveredConditionsByLine))
      .setPersistenceMode(PersistenceMode.DATABASE);
  }

  private Measure createConditionsByLine(CoverageType type) {
    return new Measure(type.conditionsByLine)
      .setData(KeyValueFormat.format(conditionsByLine))
      .setPersistenceMode(PersistenceMode.DATABASE);
  }

  public static CoverageMeasuresBuilder create() {
    return new CoverageMeasuresBuilder();
  }

}
