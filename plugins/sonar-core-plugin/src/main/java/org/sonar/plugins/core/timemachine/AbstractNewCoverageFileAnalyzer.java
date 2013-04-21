/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.timemachine;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.batch.*;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.core.DryRunIncompatible;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @since 2.7
 */
@DryRunIncompatible
@DependedUpon(DecoratorBarriers.END_OF_TIME_MACHINE)
public abstract class AbstractNewCoverageFileAnalyzer implements Decorator {

  private List<PeriodStruct> structs;

  public AbstractNewCoverageFileAnalyzer(TimeMachineConfiguration timeMachineConfiguration) {
    structs = Lists.newArrayList();
    for (PastSnapshot pastSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
      structs.add(new PeriodStruct(pastSnapshot));
    }
  }

  AbstractNewCoverageFileAnalyzer(List<PeriodStruct> structs) {
    this.structs = structs;
  }

  public abstract Metric getCoverageLineHitsDataMetric();

  public abstract Metric getConditionsByLineMetric();

  public abstract Metric getCoveredConditionsByLineMetric();

  public abstract  Metric getNewLinesToCoverMetric();

  public abstract Metric getNewUncoveredLinesMetric();

  public abstract Metric getNewConditionsToCoverMetric();

  public abstract Metric getNewUncoveredConditionsMetric();

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis() && !structs.isEmpty();
  }

  private boolean shouldDecorate(Resource resource) {
    return Scopes.isFile(resource) && !Qualifiers.UNIT_TEST_FILE.equals(resource.getQualifier());
  }

  @DependsUpon
  public List<Metric> dependsOnMetrics() {

    return Arrays.asList(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE,
      getCoverageLineHitsDataMetric(), getConditionsByLineMetric(), getCoveredConditionsByLineMetric());
  }

  @DependedUpon
  public List<Metric> generatesNewCoverageMetrics() {
    return Arrays.asList(getNewLinesToCoverMetric(), getNewUncoveredLinesMetric(), getNewConditionsToCoverMetric(), getNewUncoveredConditionsMetric());
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorate(resource)) {
      doDecorate(context);
    }
  }

  void doDecorate(DecoratorContext context) {
    if (parse(context)) {
      compute(context);
    }
  }

  private boolean parse(DecoratorContext context) {
    Measure lastCommits = context.getMeasure(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE);
    Measure hitsByLineMeasure = context.getMeasure(getCoverageLineHitsDataMetric());

    if (lastCommits != null && lastCommits.hasData() && hitsByLineMeasure != null && hitsByLineMeasure.hasData()) {
      Map<Integer, Date> datesByLine = KeyValueFormat.parseIntDateTime(lastCommits.getData());
      Map<Integer, Integer> hitsByLine = parseCountByLine(hitsByLineMeasure);
      Map<Integer, Integer> conditionsByLine = parseCountByLine(context.getMeasure(getConditionsByLineMetric()));
      Map<Integer, Integer> coveredConditionsByLine = parseCountByLine(context.getMeasure(getCoveredConditionsByLineMetric()));

      reset();

      for (Map.Entry<Integer, Integer> entry : hitsByLine.entrySet()) {
        int lineId = entry.getKey();
        int hits = entry.getValue();
        int conditions = (Integer) ObjectUtils.defaultIfNull(conditionsByLine.get(lineId), 0);
        int coveredConditions = (Integer) ObjectUtils.defaultIfNull(coveredConditionsByLine.get(lineId), 0);
        Date date = datesByLine.get(lineId);
        for (PeriodStruct struct : structs) {
          struct.analyze(date, hits, conditions, coveredConditions);
        }
      }

      return true;
    }
    return false;
  }

  private void reset() {
    for (PeriodStruct struct : structs) {
      struct.reset();
    }
  }

  private void compute(DecoratorContext context) {
    Measure newLines = new Measure(getNewLinesToCoverMetric());
    Measure newUncoveredLines = new Measure(getNewUncoveredLinesMetric());
    Measure newConditions = new Measure(getNewConditionsToCoverMetric());
    Measure newUncoveredConditions = new Measure(getNewUncoveredConditionsMetric());

    for (PeriodStruct struct : structs) {
      newLines.setVariation(struct.index, (double) struct.newLines);
      newUncoveredLines.setVariation(struct.index, (double) (struct.newLines - struct.newCoveredLines));
      newConditions.setVariation(struct.index, (double) struct.newConditions);
      newUncoveredConditions.setVariation(struct.index, (double) struct.newConditions - struct.newCoveredConditions);
    }

    context.saveMeasure(newLines);
    context.saveMeasure(newUncoveredLines);
    context.saveMeasure(newConditions);
    context.saveMeasure(newUncoveredConditions);
  }

  private Map<Integer, Integer> parseCountByLine(Measure measure) {
    if (measure != null && measure.hasData()) {
      return KeyValueFormat.parseIntInt(measure.getData());
    }
    return Maps.newHashMap();
  }

  public static final class PeriodStruct {
    int index;
    Date date;
    int newLines = 0, newCoveredLines = 0, newConditions = 0, newCoveredConditions = 0;

    PeriodStruct(PastSnapshot pastSnapshot) {
      this.index = pastSnapshot.getIndex();
      this.date = pastSnapshot.getTargetDate();
    }

    PeriodStruct(int index, Date date) {
      this.index = index;
      this.date = date;
    }

    void reset() {
      newLines = 0;
      newCoveredLines = 0;
      newConditions = 0;
      newCoveredConditions = 0;
    }

    void analyze(Date lineDate, int hits, int conditions, int coveredConditions) {
      if (lineDate == null) {
        // TODO warning

      } else if (date == null || lineDate.after(date)) {
        // TODO test if string comparison is faster or not
        addLine(hits > 0);
        addConditions(conditions, coveredConditions);
      }
    }

    void addLine(boolean covered) {
      newLines += 1;
      if (covered) {
        newCoveredLines += 1;
      }
    }

    void addConditions(int count, int countCovered) {
      newConditions += count;
      if (count > 0) {
        newCoveredConditions += countCovered;
      }
    }
  }
}
