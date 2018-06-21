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
package org.sonar.server.computation.task.projectanalysis.formula;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolder;

import static java.util.Objects.requireNonNull;

public class FormulaExecutorComponentVisitor extends PathAwareVisitorAdapter<FormulaExecutorComponentVisitor.Counters> {
  private static final SimpleStackElementFactory<Counters> COUNTERS_FACTORY = new SimpleStackElementFactory<Counters>() {

    @Override
    public Counters createForAny(Component component) {
      return new Counters();
    }

    @Override
    public Counters createForFile(Component component) {
      // No need to create a counter on leaf levels
      return null;
    }

    @Override
    public Counters createForProjectView(Component projectView) {
      // No need to create a counter on leaf levels
      return null;
    }
  };

  @CheckForNull
  private final PeriodHolder periodHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final List<Formula> formulas;

  private FormulaExecutorComponentVisitor(Builder builder, Iterable<Formula> formulas) {
    super(CrawlerDepthLimit.LEAVES, ComponentVisitor.Order.POST_ORDER, COUNTERS_FACTORY);
    this.periodHolder = builder.periodHolder;
    this.measureRepository = builder.measureRepository;
    this.metricRepository = builder.metricRepository;
    this.formulas = ImmutableList.copyOf(formulas);
  }

  public static Builder newBuilder(MetricRepository metricRepository, MeasureRepository measureRepository) {
    return new Builder(metricRepository, measureRepository);
  }

  public static class Builder {
    private final MetricRepository metricRepository;
    private final MeasureRepository measureRepository;
    @CheckForNull
    private PeriodHolder periodHolder;

    private Builder(MetricRepository metricRepository, MeasureRepository measureRepository) {
      this.metricRepository = requireNonNull(metricRepository);
      this.measureRepository = requireNonNull(measureRepository);
    }

    public Builder create(MetricRepository metricRepository, MeasureRepository measureRepository) {
      return new Builder(metricRepository, measureRepository);
    }

    public Builder withVariationSupport(PeriodHolder periodHolder) {
      this.periodHolder = requireNonNull(periodHolder);
      return this;
    }

    public FormulaExecutorComponentVisitor buildFor(Iterable<Formula> formulas) {
      return new FormulaExecutorComponentVisitor(this, formulas);
    }
  }

  @Override
  public void visitProject(Component project, Path<FormulaExecutorComponentVisitor.Counters> path) {
    process(project, path);
  }

  @Override
  public void visitModule(Component module, Path<FormulaExecutorComponentVisitor.Counters> path) {
    process(module, path);
  }

  @Override
  public void visitDirectory(Component directory, Path<FormulaExecutorComponentVisitor.Counters> path) {
    process(directory, path);
  }

  @Override
  public void visitFile(Component file, Path<FormulaExecutorComponentVisitor.Counters> path) {
    process(file, path);
  }

  @Override
  public void visitView(Component view, Path<Counters> path) {
    process(view, path);
  }

  @Override
  public void visitSubView(Component subView, Path<Counters> path) {
    process(subView, path);
  }

  @Override
  public void visitProjectView(Component projectView, Path<Counters> path) {
    process(projectView, path);
  }

  private void process(Component component, Path<FormulaExecutorComponentVisitor.Counters> path) {
    if (component.getChildren().isEmpty()) {
      processLeaf(component, path);
    } else {
      processNotLeaf(component, path);
    }
  }

  private void processNotLeaf(Component component, Path<FormulaExecutorComponentVisitor.Counters> path) {
    for (Formula formula : formulas) {
      Counter counter = path.current().getCounter(formula);
      // If there were no file under this node, the counter won't be initialized
      if (counter != null) {
        for (String metricKey : formula.getOutputMetricKeys()) {
          addNewMeasure(component, metricKey, formula, counter);
        }
        aggregateToParent(path, formula, counter);
      }
    }
  }

  private void processLeaf(Component file, Path<FormulaExecutorComponentVisitor.Counters> path) {
    CounterInitializationContext counterContext = new CounterInitializationContextImpl(file);
    for (Formula formula : formulas) {
      Counter counter = formula.createNewCounter();
      counter.initialize(counterContext);
      for (String metricKey : formula.getOutputMetricKeys()) {
        addNewMeasure(file, metricKey, formula, counter);
      }
      aggregateToParent(path, formula, counter);
    }
  }

  private void addNewMeasure(Component component, String metricKey, Formula formula, Counter counter) {
    // no new measure can be created by formulas for PROJECT_VIEW components, their measures are the copy
    if (component.getType() == Component.Type.PROJECT_VIEW) {
      return;
    }
    Metric metric = metricRepository.getByKey(metricKey);
    Optional<Measure> measure = formula.createMeasure(counter, new CreateMeasureContextImpl(component, metric));
    if (measure.isPresent()) {
      measureRepository.add(component, metric, measure.get());
    }
  }

  private static void aggregateToParent(Path<FormulaExecutorComponentVisitor.Counters> path, Formula formula, Counter currentCounter) {
    if (!path.isRoot()) {
      path.parent().aggregate(formula, currentCounter);
    }
  }

  private class CounterInitializationContextImpl implements CounterInitializationContext {
    private final Component file;

    public CounterInitializationContextImpl(Component file) {
      this.file = file;
    }

    @Override
    public Component getLeaf() {
      return file;
    }

    @Override
    public Optional<Measure> getMeasure(String metricKey) {
      return measureRepository.getRawMeasure(file, metricRepository.getByKey(metricKey));
    }

    @Override
    public Period getPeriod() {
      return periodHolder.getPeriod();
    }

    @Override
    public boolean hasPeriod() {
      return periodHolder.hasPeriod();
    }
  }

  public static class Counters {
    Map<Formula, Counter> countersByFormula = new HashMap<>();

    public void aggregate(Formula formula, Counter childCounter) {
      Counter counter = countersByFormula.get(formula);
      if (counter == null) {
        countersByFormula.put(formula, childCounter);
      } else {
        counter.aggregate(childCounter);
      }
    }

    /**
     * Counter can be null on a level when it has not been fed by children levels
     */
    @CheckForNull
    public Counter getCounter(Formula formula) {
      return countersByFormula.get(formula);
    }
  }

  private class CreateMeasureContextImpl implements CreateMeasureContext {
    private final Component component;
    private final Metric metric;

    public CreateMeasureContextImpl(Component component, Metric metric) {
      this.component = component;
      this.metric = metric;
    }

    @Override
    public Component getComponent() {
      return component;
    }

    @Override
    public Metric getMetric() {
      return metric;
    }

    @Override
    public Period getPeriod() {
      return periodHolder.getPeriod();
    }

    @Override
    public boolean hasPeriod() {
      return periodHolder.hasPeriod();
    }
  }
}
