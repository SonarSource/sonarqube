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
package org.sonar.plugins.core.timemachine;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.RequiresDB;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.components.Period;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Changesets.Changeset;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.report.ReportPublisher;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @since 2.7
 */
@RequiresDB
@DependedUpon(DecoratorBarriers.END_OF_TIME_MACHINE)
public abstract class AbstractNewCoverageFileAnalyzer implements Decorator {

  private final List<PeriodStruct> structs;
  private final ReportPublisher publishReportJob;
  private final ResourceCache resourceCache;

  public AbstractNewCoverageFileAnalyzer(TimeMachineConfiguration timeMachineConfiguration, ReportPublisher publishReportJob, ResourceCache resourceCache) {
    this(Lists.<PeriodStruct>newArrayList(), publishReportJob, resourceCache);
    for (Period period : timeMachineConfiguration.periods()) {
      structs.add(new PeriodStruct(period.getIndex(), period.getDate()));
    }
  }

  AbstractNewCoverageFileAnalyzer(List<PeriodStruct> structs, ReportPublisher publishReportJob, ResourceCache resourceCache) {
    this.resourceCache = resourceCache;
    this.publishReportJob = publishReportJob;
    this.structs = structs;
  }

  public abstract Metric getCoverageLineHitsDataMetric();

  public abstract Metric getConditionsByLineMetric();

  public abstract Metric getCoveredConditionsByLineMetric();

  public abstract Metric getNewLinesToCoverMetric();

  public abstract Metric getNewUncoveredLinesMetric();

  public abstract Metric getNewConditionsToCoverMetric();

  public abstract Metric getNewUncoveredConditionsMetric();

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return !structs.isEmpty();
  }

  private boolean shouldDecorate(Resource resource) {
    return Scopes.isFile(resource) && !Qualifiers.UNIT_TEST_FILE.equals(resource.getQualifier());
  }

  @DependsUpon
  public List<Metric> dependsOnMetrics() {

    return Arrays.asList(getCoverageLineHitsDataMetric(), getConditionsByLineMetric(), getCoveredConditionsByLineMetric());
  }

  @DependedUpon
  public List<Metric> generatesNewCoverageMetrics() {
    return Arrays.asList(getNewLinesToCoverMetric(), getNewUncoveredLinesMetric(), getNewConditionsToCoverMetric(), getNewUncoveredConditionsMetric());
  }

  @Override
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
    BatchReportReader reader = new BatchReportReader(publishReportJob.getReportDir());
    BatchReport.Changesets componentScm = reader.readChangesets(resourceCache.get(context.getResource()).batchId());
    Measure hitsByLineMeasure = context.getMeasure(getCoverageLineHitsDataMetric());

    if (componentScm != null && hitsByLineMeasure != null && hitsByLineMeasure.hasData()) {
      Map<Integer, Integer> hitsByLine = parseCountByLine(hitsByLineMeasure);
      Map<Integer, Integer> conditionsByLine = parseCountByLine(context.getMeasure(getConditionsByLineMetric()));
      Map<Integer, Integer> coveredConditionsByLine = parseCountByLine(context.getMeasure(getCoveredConditionsByLineMetric()));

      reset();

      for (Map.Entry<Integer, Integer> entry : hitsByLine.entrySet()) {
        int lineId = entry.getKey();
        int hits = entry.getValue();
        int conditions = (Integer) ObjectUtils.defaultIfNull(conditionsByLine.get(lineId), 0);
        int coveredConditions = (Integer) ObjectUtils.defaultIfNull(coveredConditionsByLine.get(lineId), 0);
        Changeset changeset = componentScm.getChangeset(componentScm.getChangesetIndexByLine(lineId - 1));
        Date date = changeset.hasDate() ? new Date(changeset.getDate()) : null;
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
      if (struct.hasNewCode()) {
        newLines.setVariation(struct.index, (double) struct.getNewLines());
        newUncoveredLines.setVariation(struct.index, (double) (struct.getNewLines() - struct.getNewCoveredLines()));
        newConditions.setVariation(struct.index, (double) struct.getNewConditions());
        newUncoveredConditions.setVariation(struct.index, (double) struct.getNewConditions() - struct.getNewCoveredConditions());
      }
    }

    context.saveMeasure(newLines);
    context.saveMeasure(newUncoveredLines);
    context.saveMeasure(newConditions);
    context.saveMeasure(newUncoveredConditions);
  }

  private Map<Integer, Integer> parseCountByLine(@Nullable Measure measure) {
    if (measure != null && measure.hasData()) {
      return KeyValueFormat.parseIntInt(measure.getData());
    }
    return Maps.newHashMap();
  }

  public static final class PeriodStruct {
    int index;
    Date date;
    Integer newLines;
    Integer newCoveredLines;
    Integer newConditions;
    Integer newCoveredConditions;

    PeriodStruct(int index, @Nullable Date date) {
      this.index = index;
      this.date = date;
    }

    void reset() {
      newLines = null;
      newCoveredLines = null;
      newConditions = null;
      newCoveredConditions = null;
    }

    void analyze(@Nullable Date lineDate, int hits, int conditions, int coveredConditions) {
      if (lineDate == null) {
        // TODO warning

      } else if (date == null || lineDate.after(date)) {
        // TODO test if string comparison is faster or not
        addLine(hits > 0);
        addConditions(conditions, coveredConditions);
      }
    }

    void addLine(boolean covered) {
      if (newLines == null) {
        newLines = 0;
      }
      newLines += 1;
      if (covered) {
        if (newCoveredLines == null) {
          newCoveredLines = 0;
        }
        newCoveredLines += 1;
      }
    }

    void addConditions(int count, int countCovered) {
      if (newConditions == null) {
        newConditions = 0;
      }
      newConditions += count;
      if (count > 0) {
        if (newCoveredConditions == null) {
          newCoveredConditions = 0;
        }
        newCoveredConditions += countCovered;
      }
    }

    boolean hasNewCode() {
      return newLines != null;
    }

    public int getNewLines() {
      return newLines != null ? newLines : 0;
    }

    public int getNewCoveredLines() {
      return newCoveredLines != null ? newCoveredLines : 0;
    }

    public int getNewConditions() {
      return newConditions != null ? newConditions : 0;
    }

    public int getNewCoveredConditions() {
      return newCoveredConditions != null ? newCoveredConditions : 0;
    }
  }
}
