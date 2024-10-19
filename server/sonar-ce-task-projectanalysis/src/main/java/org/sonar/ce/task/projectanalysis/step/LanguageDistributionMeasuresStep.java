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
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.base.Function;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.formula.Counter;
import org.sonar.ce.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.ce.task.projectanalysis.formula.CreateMeasureContext;
import org.sonar.ce.task.projectanalysis.formula.Formula;
import org.sonar.ce.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;

import static com.google.common.collect.Maps.asMap;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.api.utils.KeyValueFormat.format;
import static org.sonar.api.utils.KeyValueFormat.newIntegerConverter;
import static org.sonar.api.utils.KeyValueFormat.newStringConverter;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class LanguageDistributionMeasuresStep implements ComputationStep {
  private static final String UNKNOWN_LANGUAGE_KEY = "<null>";

  private static final List<Formula<?>> FORMULAS = List.of(new LanguageDistributionFormula());

  private static final String[] LANGUAGE_DISTRIBUTION_FORMULA_METRICS = new String[] {NCLOC_LANGUAGE_DISTRIBUTION_KEY};

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public LanguageDistributionMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    new PathAwareCrawler<>(FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository).buildFor(FORMULAS))
      .visit(treeRootHolder.getRoot());
  }

  private static class LanguageDistributionFormula implements Formula<LanguageDistributionCounter> {

    @Override
    public LanguageDistributionCounter createNewCounter() {
      return new LanguageDistributionCounter();
    }

    @Override
    public Optional<Measure> createMeasure(LanguageDistributionCounter counter, CreateMeasureContext context) {
      if (counter.multiset.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(newMeasureBuilder().create(format(asMap(counter.multiset.elementSet(), new LanguageToTotalCount(counter.multiset)))));
    }

    @Override
    public String[] getOutputMetricKeys() {
      return LANGUAGE_DISTRIBUTION_FORMULA_METRICS;
    }
  }

  private static class LanguageToTotalCount implements Function<String, Integer> {

    private final Multiset<String> multiset;

    public LanguageToTotalCount(Multiset<String> multiset) {
      this.multiset = multiset;
    }

    @Nullable
    @Override
    public Integer apply(@Nonnull String language) {
      return multiset.count(language);
    }
  }

  private static class LanguageDistributionCounter implements Counter<LanguageDistributionCounter> {

    private final Multiset<String> multiset = TreeMultiset.create();

    @Override
    public void aggregate(LanguageDistributionCounter counter) {
      multiset.addAll(counter.multiset);
    }

    @Override
    public void initialize(CounterInitializationContext context) {
      if (context.getLeaf().getType() == Component.Type.FILE) {
        initializeForFile(context);
      }
      initializeForOtherLeaf(context);
    }

    private void initializeForFile(CounterInitializationContext context) {
      String language = context.getLeaf().getFileAttributes().getLanguageKey();
      Optional<Measure> ncloc = context.getMeasure(CoreMetrics.NCLOC_KEY);
      if (ncloc.isPresent()) {
        multiset.add(language == null ? UNKNOWN_LANGUAGE_KEY : language, ncloc.get().getIntValue());
      }
    }

    private void initializeForOtherLeaf(CounterInitializationContext context) {
      Optional<Measure> measure = context.getMeasure(NCLOC_LANGUAGE_DISTRIBUTION_KEY);
      if (measure.isPresent()) {
        Map<String, Integer> parse = KeyValueFormat.parse(measure.get().getData(), newStringConverter(), newIntegerConverter());
        for (Map.Entry<String, Integer> entry : parse.entrySet()) {
          multiset.add(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  @Override
  public String getDescription() {
    return "Compute language distribution";
  }
}
