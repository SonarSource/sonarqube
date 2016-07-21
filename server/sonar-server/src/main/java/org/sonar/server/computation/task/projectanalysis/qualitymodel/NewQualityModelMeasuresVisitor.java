/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.formula.counter.LongVariationValue;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolder;
import org.sonar.server.computation.task.projectanalysis.scm.Changeset;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfo;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepository;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.api.utils.KeyValueFormat.newIntegerConverter;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations.newMeasureVariationsBuilder;

/**
 * This visitor depends on {@link org.sonar.server.computation.task.projectanalysis.issue.IntegrateIssuesVisitor} for the computation of
 * metric {@link CoreMetrics#NEW_TECHNICAL_DEBT}.
 *
 * Compute following measure :
 * {@link CoreMetrics#NEW_SQALE_DEBT_RATIO_KEY}
 */
public class NewQualityModelMeasuresVisitor extends PathAwareVisitorAdapter<NewQualityModelMeasuresVisitor.NewTechDebtRatioCounter> {
  private static final Logger LOG = Loggers.get(NewQualityModelMeasuresVisitor.class);

  private final ScmInfoRepository scmInfoRepository;
  private final MeasureRepository measureRepository;
  private final PeriodsHolder periodsHolder;
  private final RatingSettings ratingSettings;

  private final Metric newDebtMetric;
  private final Metric nclocDataMetric;
  private final Metric newDebtRatioMetric;

  public NewQualityModelMeasuresVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, ScmInfoRepository scmInfoRepository, PeriodsHolder periodsHolder,
                                        RatingSettings ratingSettings) {
    super(CrawlerDepthLimit.FILE, POST_ORDER, NewDevelopmentCostCounterFactory.INSTANCE);
    this.measureRepository = measureRepository;
    this.scmInfoRepository = scmInfoRepository;
    this.periodsHolder = periodsHolder;
    this.ratingSettings = ratingSettings;

    // computed by NewDebtAggregator which is executed by IntegrateIssuesVisitor
    this.newDebtMetric = metricRepository.getByKey(CoreMetrics.NEW_TECHNICAL_DEBT_KEY);
    // which line is ncloc and which isn't
    this.nclocDataMetric = metricRepository.getByKey(CoreMetrics.NCLOC_DATA_KEY);
    // output metric
    this.newDebtRatioMetric = metricRepository.getByKey(CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY);
  }

  @Override
  public void visitProject(Component project, Path<NewTechDebtRatioCounter> path) {
    computeAndSaveNewDebtRatioMeasure(project, path);
  }

  @Override
  public void visitModule(Component module, Path<NewTechDebtRatioCounter> path) {
    computeAndSaveNewDebtRatioMeasure(module, path);
    increaseNewDebtAndDevCostOfParent(path);
  }

  @Override
  public void visitDirectory(Component directory, Path<NewTechDebtRatioCounter> path) {
    computeAndSaveNewDebtRatioMeasure(directory, path);
    increaseNewDebtAndDevCostOfParent(path);
  }

  @Override
  public void visitFile(Component file, Path<NewTechDebtRatioCounter> path) {
    initNewDebtRatioCounter(file, path);
    computeAndSaveNewDebtRatioMeasure(file, path);
    increaseNewDebtAndDevCostOfParent(path);
  }

  private void computeAndSaveNewDebtRatioMeasure(Component component, Path<NewTechDebtRatioCounter> path) {
    MeasureVariations.Builder builder = newMeasureVariationsBuilder();
    for (Period period : periodsHolder.getPeriods()) {
      double density = computeDensity(path.current(), period);
      builder.setVariation(period, 100.0 * density);
    }
    if (!builder.isEmpty()) {
      Measure measure = newMeasureBuilder().setVariations(builder.build()).createNoValue();
      measureRepository.add(component, this.newDebtRatioMetric, measure);
    }
  }

  private static double computeDensity(NewTechDebtRatioCounter counter, Period period) {
    LongVariationValue newDebt = counter.getNewDebt(period);
    if (newDebt.isSet()) {
      long developmentCost = counter.getDevCost(period).getValue();
      if (developmentCost != 0L) {
        return newDebt.getValue() / (double) developmentCost;
      }
    }
    return 0d;
  }

  private static long getLongValue(Optional<Measure> measure, Period period) {
    if (!measure.isPresent()) {
      return 0L;
    }
    return getLongValue(measure.get(), period);
  }

  private static long getLongValue(Measure measure, Period period) {
    if (measure.hasVariations() && measure.getVariations().hasVariation(period.getIndex())) {
      return (long) measure.getVariations().getVariation(period.getIndex());
    }
    return 0L;
  }

  private void initNewDebtRatioCounter(Component file, Path<NewTechDebtRatioCounter> path) {
    // first analysis, no period, no differential value to compute, save processing time and return now
    if (periodsHolder.getPeriods().isEmpty()) {
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

  private void initNewDebtRatioCounter(NewTechDebtRatioCounter devCostCounter, Component file, Measure nclocDataMeasure, ScmInfo scmInfo) {
    boolean[] hasDevCost = new boolean[PeriodsHolder.MAX_NUMBER_OF_PERIODS];

    long lineDevCost = ratingSettings.getDevCost(file.getFileAttributes().getLanguageKey());
    for (Integer nclocLineIndex : nclocLineIndexes(nclocDataMeasure)) {
      Changeset changeset = scmInfo.getChangesetForLine(nclocLineIndex);
      for (Period period : periodsHolder.getPeriods()) {
        if (isLineInPeriod(changeset.getDate(), period)) {
          devCostCounter.incrementDevCost(period, lineDevCost);
          hasDevCost[period.getIndex() - 1] = true;
        }
      }
    }
    for (Period period : periodsHolder.getPeriods()) {
      if (hasDevCost[period.getIndex() - 1]) {
        long newDebt = getLongValue(measureRepository.getRawMeasure(file, this.newDebtMetric), period);
        devCostCounter.incrementNewDebt(period, newDebt);
      }
    }
  }

  private static void increaseNewDebtAndDevCostOfParent(Path<NewTechDebtRatioCounter> path) {
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
    return from(parsedNclocData.entrySet())
      .filter(NclocEntryNclocLine.INSTANCE)
      .transform(MapEntryToKey.INSTANCE);
  }

  public static final class NewTechDebtRatioCounter {
    private final LongVariationValue.Array newDebt = LongVariationValue.newArray();
    private final LongVariationValue.Array devCost = LongVariationValue.newArray();

    public void add(NewTechDebtRatioCounter counter) {
      this.newDebt.incrementAll(counter.newDebt);
      this.devCost.incrementAll(counter.devCost);
    }

    public LongVariationValue.Array incrementNewDebt(Period period, long value) {
      return newDebt.increment(period, value);
    }

    public LongVariationValue.Array incrementDevCost(Period period, long value) {
      return devCost.increment(period, value);
    }

    public LongVariationValue getNewDebt(Period period) {
      return this.newDebt.get(period);
    }

    public LongVariationValue getDevCost(Period period) {
      return this.devCost.get(period);
    }

  }

  private enum NclocEntryNclocLine implements Predicate<Map.Entry<Integer, Integer>> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull Map.Entry<Integer, Integer> input) {
      return input.getValue() == 1;
    }
  }

  private enum MapEntryToKey implements Function<Map.Entry<Integer, Integer>, Integer> {
    INSTANCE;

    @Override
    @Nullable
    public Integer apply(@Nonnull Map.Entry<Integer, Integer> input) {
      return input.getKey();
    }
  }

  private static class NewDevelopmentCostCounterFactory extends SimpleStackElementFactory<NewTechDebtRatioCounter> {
    public static final NewDevelopmentCostCounterFactory INSTANCE = new NewDevelopmentCostCounterFactory();

    @Override
    public NewTechDebtRatioCounter createForAny(Component component) {
      return new NewTechDebtRatioCounter();
    }
  }
}
