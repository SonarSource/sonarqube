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
package org.sonar.server.computation.task.projectanalysis.qualitymodel;

import com.google.common.base.Optional;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.formula.counter.LongValue;
import org.sonar.server.computation.task.projectanalysis.issue.IntegrateIssuesVisitor;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolder;
import org.sonar.server.computation.task.projectanalysis.scm.Changeset;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfo;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepository;

import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT_KEY;
import static org.sonar.api.utils.KeyValueFormat.newIntegerConverter;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

/**
 * This visitor depends on {@link IntegrateIssuesVisitor} for the computation of
 * metric {@link CoreMetrics#NEW_TECHNICAL_DEBT}.
 *
 * Compute following measure :
 * {@link CoreMetrics#NEW_SQALE_DEBT_RATIO_KEY}
 * {@link CoreMetrics#NEW_MAINTAINABILITY_RATING_KEY}
 */
public class NewMaintainabilityMeasuresVisitor extends PathAwareVisitorAdapter<NewMaintainabilityMeasuresVisitor.Counter> {
  private static final Logger LOG = Loggers.get(NewMaintainabilityMeasuresVisitor.class);

  private final ScmInfoRepository scmInfoRepository;
  private final MeasureRepository measureRepository;
  private final PeriodHolder periodHolder;
  private final RatingSettings ratingSettings;

  private final Metric newDebtMetric;
  private final Metric nclocDataMetric;

  private final Metric newDevelopmentCostMetric;
  private final Metric newDebtRatioMetric;
  private final Metric newMaintainabilityRatingMetric;

  public NewMaintainabilityMeasuresVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, ScmInfoRepository scmInfoRepository,
    PeriodHolder periodHolder, RatingSettings ratingSettings) {
    super(CrawlerDepthLimit.FILE, POST_ORDER, CounterFactory.INSTANCE);
    this.measureRepository = measureRepository;
    this.scmInfoRepository = scmInfoRepository;
    this.periodHolder = periodHolder;
    this.ratingSettings = ratingSettings;

    // computed by NewDebtAggregator which is executed by IntegrateIssuesVisitor
    this.newDebtMetric = metricRepository.getByKey(NEW_TECHNICAL_DEBT_KEY);
    // which line is ncloc and which isn't
    this.nclocDataMetric = metricRepository.getByKey(NCLOC_DATA_KEY);

    // output metrics
    this.newDevelopmentCostMetric = metricRepository.getByKey(NEW_DEVELOPMENT_COST_KEY);
    this.newDebtRatioMetric = metricRepository.getByKey(NEW_SQALE_DEBT_RATIO_KEY);
    this.newMaintainabilityRatingMetric = metricRepository.getByKey(NEW_MAINTAINABILITY_RATING_KEY);
  }

  @Override
  public void visitProject(Component project, Path<Counter> path) {
    computeAndSaveNewDebtRatioMeasure(project, path);
  }

  @Override
  public void visitModule(Component module, Path<Counter> path) {
    computeAndSaveNewDebtRatioMeasure(module, path);
    increaseNewDebtAndDevCostOfParent(path);
  }

  @Override
  public void visitDirectory(Component directory, Path<Counter> path) {
    computeAndSaveNewDebtRatioMeasure(directory, path);
    increaseNewDebtAndDevCostOfParent(path);
  }

  @Override
  public void visitFile(Component file, Path<Counter> path) {
    initNewDebtRatioCounter(file, path);
    computeAndSaveNewDebtRatioMeasure(file, path);
    increaseNewDebtAndDevCostOfParent(path);
  }

  private void computeAndSaveNewDebtRatioMeasure(Component component, Path<Counter> path) {
    if (!periodHolder.hasPeriod()) {
      return;
    }
    double density = computeDensity(path.current());
    double newDebtRatio = 100.0 * density;
    double newMaintainability = ratingSettings.getDebtRatingGrid().getRatingForDensity(density).getIndex();
    long newDevelopmentCost = path.current().getDevCost().getValue();
    measureRepository.add(component, this.newDevelopmentCostMetric, newMeasureBuilder().setVariation(newDevelopmentCost).createNoValue());
    measureRepository.add(component, this.newDebtRatioMetric, newMeasureBuilder().setVariation(newDebtRatio).createNoValue());
    measureRepository.add(component, this.newMaintainabilityRatingMetric, newMeasureBuilder().setVariation(newMaintainability).createNoValue());
  }

  private static double computeDensity(Counter counter) {
    LongValue newDebt = counter.getNewDebt();
    if (newDebt.isSet()) {
      long developmentCost = counter.getDevCost().getValue();
      if (developmentCost != 0L) {
        return newDebt.getValue() / (double) developmentCost;
      }
    }
    return 0d;
  }

  private static long getLongValue(Optional<Measure> measure) {
    if (!measure.isPresent()) {
      return 0L;
    }
    return getLongValue(measure.get());
  }

  private static long getLongValue(Measure measure) {
    if (measure.hasVariation()) {
      return (long) measure.getVariation();
    }
    return 0L;
  }

  private void initNewDebtRatioCounter(Component file, Path<Counter> path) {
    // first analysis, no period, no differential value to compute, save processing time and return now
    if (!periodHolder.hasPeriod()) {
      return;
    }

    Optional<Measure> nclocDataMeasure = measureRepository.getRawMeasure(file, this.nclocDataMetric);
    if (!nclocDataMeasure.isPresent()) {
      return;
    }

    Optional<ScmInfo> scmInfoOptional = scmInfoRepository.getScmInfo(file);
    if (!scmInfoOptional.isPresent()) {
      LOG.trace(String.format("No changeset for file %s. Dev cost will be zero.", file.getKey()));
      return;
    }

    ScmInfo scmInfo = scmInfoOptional.get();
    initNewDebtRatioCounter(path.current(), file, nclocDataMeasure.get(), scmInfo);
  }

  private void initNewDebtRatioCounter(Counter devCostCounter, Component file, Measure nclocDataMeasure, ScmInfo scmInfo) {
    boolean hasDevCost = false;

    long lineDevCost = ratingSettings.getDevCost(file.getFileAttributes().getLanguageKey());
    for (Integer nclocLineIndex : nclocLineIndexes(nclocDataMeasure)) {
      if (scmInfo.hasChangesetForLine(nclocLineIndex)) {
        Changeset changeset = scmInfo.getChangesetForLine(nclocLineIndex);
        Period period = periodHolder.getPeriod();
        if (isLineInPeriod(changeset.getDate(), period)) {
          devCostCounter.incrementDevCost(lineDevCost);
          hasDevCost = true;
        }
      }
    }
    if (hasDevCost) {
      long newDebt = getLongValue(measureRepository.getRawMeasure(file, this.newDebtMetric));
      devCostCounter.incrementNewDebt(newDebt);
    }
  }

  private static void increaseNewDebtAndDevCostOfParent(Path<Counter> path) {
    path.parent().add(path.current());
  }

  /**
   * A line belongs to a Period if its date is older than the SNAPSHOT's date of the period.
   */
  private static boolean isLineInPeriod(long lineDate, Period period) {
    return lineDate > period.getSnapshotDate();
  }

  /**
   * NCLOC_DATA contains Key-value pairs, where key - is a number of line, and value - is an indicator of whether line
   * contains code (1) or not (0).
   *
   * This method parses the value of the NCLOC_DATA measure and return the line numbers of lines which contain code.
   */
  private static Iterable<Integer> nclocLineIndexes(Measure nclocDataMeasure) {
    Map<Integer, Integer> parsedNclocData = KeyValueFormat.parse(nclocDataMeasure.getData(), newIntegerConverter(), newIntegerConverter());
    return parsedNclocData.entrySet()
      .stream()
      .filter(entry -> entry.getValue() == 1)
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
  }

  public static final class Counter {
    private final LongValue newDebt = new LongValue();
    private final LongValue devCost = new LongValue();

    public void add(Counter counter) {
      this.newDebt.increment(counter.newDebt);
      this.devCost.increment(counter.devCost);
    }

    LongValue incrementNewDebt(long value) {
      return newDebt.increment(value);
    }

    LongValue incrementDevCost(long value) {
      return devCost.increment(value);
    }

    LongValue getNewDebt() {
      return this.newDebt;
    }

    LongValue getDevCost() {
      return this.devCost;
    }
  }

  private static class CounterFactory extends SimpleStackElementFactory<Counter> {
    public static final CounterFactory INSTANCE = new CounterFactory();

    @Override
    public Counter createForAny(Component component) {
      return new Counter();
    }
  }
}
