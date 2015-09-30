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
package org.sonar.server.computation.sqale;

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
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.formula.counter.LongVariationValue;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolder;
import org.sonar.server.computation.scm.Changeset;
import org.sonar.server.computation.scm.ScmInfo;
import org.sonar.server.computation.scm.ScmInfoRepository;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.api.utils.KeyValueFormat.newIntegerConverter;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureVariations.newMeasureVariationsBuilder;

/**
 * This visitor depends on {@link org.sonar.server.computation.issue.IntegrateIssuesVisitor} for the computation of
 * metric {@link CoreMetrics#NEW_TECHNICAL_DEBT}.
 */
public class SqaleNewMeasuresVisitor extends PathAwareVisitorAdapter<SqaleNewMeasuresVisitor.NewDevelopmentCostCounter> {
  private static final Logger LOG = Loggers.get(SqaleNewMeasuresVisitor.class);

  private final ScmInfoRepository scmInfoRepository;
  private final MeasureRepository measureRepository;
  private final PeriodsHolder periodsHolder;
  private final SqaleRatingSettings sqaleRatingSettings;

  private final Metric newDebtMetric;
  private final Metric nclocDataMetric;
  private final Metric newDebtRatioMetric;

  public SqaleNewMeasuresVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, ScmInfoRepository scmInfoRepository, PeriodsHolder periodsHolder,
    SqaleRatingSettings sqaleRatingSettings) {
    super(CrawlerDepthLimit.FILE, POST_ORDER, NewDevelopmentCostCounterFactory.INSTANCE);
    this.measureRepository = measureRepository;
    this.scmInfoRepository = scmInfoRepository;
    this.periodsHolder = periodsHolder;
    this.sqaleRatingSettings = sqaleRatingSettings;

    // computed by NewDebtAggregator which is executed by IntegrateIssuesVisitor
    this.newDebtMetric = metricRepository.getByKey(CoreMetrics.NEW_TECHNICAL_DEBT_KEY);
    // which line is ncloc and which isn't
    this.nclocDataMetric = metricRepository.getByKey(CoreMetrics.NCLOC_DATA_KEY);
    // output metric
    this.newDebtRatioMetric = metricRepository.getByKey(CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY);
  }

  @Override
  public void visitProject(Component project, Path<NewDevelopmentCostCounter> path) {
    computeAndSaveNewDebtRatioMeasure(project, path);
  }

  @Override
  public void visitModule(Component module, Path<NewDevelopmentCostCounter> path) {
    computeAndSaveNewDebtRatioMeasure(module, path);
    increaseNewDevCostOfParent(path);
  }

  @Override
  public void visitDirectory(Component directory, Path<NewDevelopmentCostCounter> path) {
    computeAndSaveNewDebtRatioMeasure(directory, path);
    increaseNewDevCostOfParent(path);
  }

  @Override
  public void visitFile(Component file, Path<NewDevelopmentCostCounter> path) {
    if (file.getFileAttributes().isUnitTest()) {
      return;
    }

    initNewDebtRatioCounter(file, path);
    computeAndSaveNewDebtRatioMeasure(file, path);
    increaseNewDevCostOfParent(path);
  }

  private void computeAndSaveNewDebtRatioMeasure(Component component, Path<NewDevelopmentCostCounter> path) {
    MeasureVariations.Builder builder = newMeasureVariationsBuilder();
    for (Period period : periodsHolder.getPeriods()) {
      long newDevCost = path.current().getValue(period).getValue();
      double density = computeDensity(component, period, newDevCost);
      builder.setVariation(period, 100.0 * density);
    }
    if (!builder.isEmpty()) {
      Measure measure = newMeasureBuilder().setVariations(builder.build()).createNoValue();
      measureRepository.add(component, this.newDebtRatioMetric, measure);
    }
  }

  private double computeDensity(Component component, Period period, long developmentCost) {
    double debt = getLongValue(measureRepository.getRawMeasure(component, this.newDebtMetric), period);
    if (developmentCost != 0L) {
      return debt / (double) developmentCost;
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

  private void initNewDebtRatioCounter(Component file, Path<NewDevelopmentCostCounter> path) {
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
    initNewDebtRatioCounter(path.current(), file.getFileAttributes().getLanguageKey(), nclocDataMeasure.get(), scmInfo);
  }

  private void initNewDebtRatioCounter(NewDevelopmentCostCounter devCostCounter, String languageKey, Measure nclocDataMeasure, ScmInfo scmInfo) {
    long lineDevCost = sqaleRatingSettings.getDevCost(languageKey);
    for (Integer nclocLineIndex : nclocLineIndexes(nclocDataMeasure)) {
      Changeset changeset = scmInfo.getChangesetForLine(nclocLineIndex);
      for (Period period : periodsHolder.getPeriods()) {
        if (isLineInPeriod(changeset.getDate(), period)) {
          devCostCounter.increment(period, lineDevCost);
        }
      }
    }
  }

  private static void increaseNewDevCostOfParent(Path<NewDevelopmentCostCounter> path) {
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

  public static final class NewDevelopmentCostCounter {
    private final LongVariationValue.Array devCosts = LongVariationValue.newArray();

    public void add(NewDevelopmentCostCounter counter) {
      this.devCosts.incrementAll(counter.devCosts);
    }

    public LongVariationValue.Array increment(Period period, long value) {
      return devCosts.increment(period, value);
    }

    public LongVariationValue getValue(Period period) {
      return this.devCosts.get(period);
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

  private static class NewDevelopmentCostCounterFactory extends SimpleStackElementFactory<NewDevelopmentCostCounter> {
    public static final NewDevelopmentCostCounterFactory INSTANCE = new NewDevelopmentCostCounterFactory();

    @Override
    public NewDevelopmentCostCounter createForAny(Component component) {
      return new NewDevelopmentCostCounter();
    }
  }
}
