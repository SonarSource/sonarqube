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
package org.sonar.server.computation.step;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.PathAwareVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolder;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

public class NewCoverageAggregationStep implements ComputationStep {
  private static final List<String> AGGREGATED_METRIC_KEYS = ImmutableList.of(
    CoreMetrics.NEW_LINES_TO_COVER_KEY,
    CoreMetrics.NEW_UNCOVERED_LINES_KEY,
    CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY,
    CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY,
    CoreMetrics.NEW_IT_LINES_TO_COVER_KEY,
    CoreMetrics.NEW_IT_UNCOVERED_LINES_KEY,
    CoreMetrics.NEW_IT_CONDITIONS_TO_COVER_KEY,
    CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS_KEY,
    CoreMetrics.NEW_OVERALL_LINES_TO_COVER_KEY,
    CoreMetrics.NEW_OVERALL_UNCOVERED_LINES_KEY,
    CoreMetrics.NEW_OVERALL_CONDITIONS_TO_COVER_KEY,
    CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS_KEY
    );
  private static final VariationBuildersFactory VARIATION_BUILDERS_FACTORY = new VariationBuildersFactory();

  private final TreeRootHolder treeRootHolder;
  private final PeriodsHolder periodsHolder;
  private final MeasureRepository measureRepository;
  private final List<Metric> metrics;

  public NewCoverageAggregationStep(TreeRootHolder treeRootHolder, PeriodsHolder periodsHolder,
    final MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.treeRootHolder = treeRootHolder;
    this.periodsHolder = periodsHolder;
    this.measureRepository = measureRepository;
    this.metrics = from(AGGREGATED_METRIC_KEYS)
      .transform(new Function<String, Metric>() {
        @Override
        @Nonnull
        public Metric apply(@Nonnull String input) {
          return metricRepository.getByKey(input);
        }
      }).toList();
  }

  @Override
  public void execute() {
    new NewCoverageAggregationComponentVisitor()
      .visit(treeRootHolder.getRoot());
  }

  private final class NewCoverageAggregationComponentVisitor extends PathAwareVisitor<VariationBuilders> {
    public NewCoverageAggregationComponentVisitor() {
      super(FILE, POST_ORDER, VARIATION_BUILDERS_FACTORY);
    }

    @Override
    protected void visitProject(Component project, Path<VariationBuilders> path) {
      addNewMeasures(project, path.current());
    }

    @Override
    protected void visitModule(Component module, Path<VariationBuilders> path) {
      addNewMeasures(module, path.current());
      updateParentVariations(path);
    }

    @Override
    protected void visitDirectory(Component directory, Path<VariationBuilders> path) {
      addNewMeasures(directory, path.current());
      updateParentVariations(path);
    }

    @Override
    protected void visitFile(Component file, Path<VariationBuilders> path) {
      initVariationBuilders(file, path.parent());
    }
  }

  /**
   * Creates a new VariationBuilders instance for all Component types except FILE.
   */
  private static class VariationBuildersFactory extends PathAwareVisitor.SimpleStackElementFactory<VariationBuilders> {
    @Override
    public VariationBuilders createForAny(Component component) {
      return new VariationBuilders();
    }

    @Override
    public VariationBuilders createForFile(Component file) {
      return null;
    }
  }

  private void initVariationBuilders(Component file, VariationBuilders builders) {
    for (Metric metric : metrics) {
      Optional<Measure> measureOptional = measureRepository.getRawMeasure(file, metric);
      if (!measureOptional.isPresent()) {
        continue;
      }
      Measure measure = measureOptional.get();
      if (!measure.hasVariations() || measure.getValueType() != Measure.ValueType.NO_VALUE) {
        continue;
      }

      builders.getOrCreateBuilder(metric.getKey()).incrementVariationsBy(measure.getVariations(), periodsHolder);
    }
  }

  private void addNewMeasures(Component component, VariationBuilders current) {
    for (Metric metric : metrics) {
      Optional<VariationBuilder> builder = current.getBuilder(metric.getKey());
      if (builder.isPresent() && builder.get().hasVariation()) {
        measureRepository.add(component, metric, newMeasureBuilder().setVariations(builder.get().build()).createNoValue());
      }
    }
  }

  private void updateParentVariations(PathAwareVisitor.Path<VariationBuilders> path) {
    path.parent().add(path.current());
  }

  /**
   * Holds a VariationBuilder instance for each Metric for which the aggregation is computed in this step
   */
  private static final class VariationBuilders {
    private final Map<String, VariationBuilder> builders;

    public VariationBuilders() {
      this.builders = new HashMap<>(AGGREGATED_METRIC_KEYS.size());
    }

    public VariationBuilder getOrCreateBuilder(String metricKey) {
      VariationBuilder builder = this.builders.get(metricKey);
      if (builder == null) {
        builder = new VariationBuilder();
        this.builders.put(metricKey, builder);
      }
      return builder;
    }

    public Optional<VariationBuilder> getBuilder(String metricKey) {
      return Optional.fromNullable(this.builders.get(metricKey));
    }

    public void add(VariationBuilders current) {
      for (Map.Entry<String, VariationBuilder> entry : current.builders.entrySet()) {
        VariationBuilder builder = getOrCreateBuilder(entry.getKey());
        builder.incrementVariationsBy(entry.getValue());
      }
    }

  }

  /**
   * This class holds the variations for a specific Component in the tree and for a specific Metric.
   * It is mutable and exposes methods to easily increment variations from either a specific
   * MeasureVariations instance.
   * This class stores variations as int and makes calculations on int values because all metrics actually have int
   * values in their variations.
   */
  private static final class VariationBuilder {
    private final Integer[] variations = new Integer[5];

    public void incrementVariationsBy(MeasureVariations variations, PeriodsHolder periodsHolder) {
      for (Period period : periodsHolder.getPeriods()) {
        if (variations.hasVariation(period.getIndex())) {
          increment(period.getIndex() - 1, (int) variations.getVariation(period.getIndex()));
        }
      }
    }

    public boolean hasVariation() {
      for (@Nullable Integer variation : variations) {
        if (variation != null) {
          return true;
        }
      }
      return false;
    }

    public void incrementVariationsBy(VariationBuilder variationBuilder) {
      for (int i = 0; i < variationBuilder.variations.length; i++) {
        increment(i, variationBuilder.variations[i]);
      }
    }

    private void increment(int arrayIndex, @Nullable Integer value) {
      if (value == null) {
        return;
      }
      if (variations[arrayIndex] == null) {
        variations[arrayIndex] = 0;
      }
      variations[arrayIndex] += value;
    }

    public MeasureVariations build() {
      Double[] res = new Double[variations.length];
      for (int i = 0; i < variations.length; i++) {
        res[i] = variations[i] == null ? null : (double) variations[i];
      }
      return new MeasureVariations(res);
    }
  }

  @Override
  public String getDescription() {
    return "Aggregates New Coverage measures";
  }
}
