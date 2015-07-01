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

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.PathAwareVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.Counter;
import org.sonar.server.computation.formula.CounterContext;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaRepository;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;

public class ComputeFormulaMeasuresStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;
  private final MetricRepository metricRepository;
  private final FormulaRepository formulaRepository;

  public ComputeFormulaMeasuresStep(TreeRootHolder treeRootHolder, MeasureRepository measureRepository, MetricRepository metricRepository, FormulaRepository formulaRepository) {
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.metricRepository = metricRepository;
    this.formulaRepository = formulaRepository;
  }

  @Override
  public void execute() {
    new ComputeFormulaMeasureVisitor().visit(treeRootHolder.getRoot());
  }

  private class ComputeFormulaMeasureVisitor extends PathAwareVisitor<Counters> {

    public ComputeFormulaMeasureVisitor() {
      super(Component.Type.FILE, ComponentVisitor.Order.POST_ORDER, new SimpleStackElementFactory<Counters>() {

        @Override
        public Counters createForAny(Component component) {
          return new Counters();
        }

        @Override
        public Counters createForFile(Component component) {
          // No need to create a counter on file levels
          return null;
        }
      });
    }

    @Override
    protected void visitProject(Component project, Path<Counters> path) {
      processNotFile(project, path);
    }

    @Override
    protected void visitModule(Component module, Path<Counters> path) {
      processNotFile(module, path);
    }

    @Override
    protected void visitDirectory(Component directory, Path<Counters> path) {
      processNotFile(directory, path);
    }

    @Override
    protected void visitFile(Component file, Path<Counters> path) {
      processFile(file, path);
    }

    private void processNotFile(Component component, PathAwareVisitor.Path<Counters> path) {
      for (Formula formula : formulaRepository.getFormulas()) {
        Counter counter = path.current().getCounter(formula.getOutputMetricKey());
        addNewMeasure(component, path, formula, counter);
        aggregateToParent(path, formula, counter);
      }
    }

    private void processFile(Component file, PathAwareVisitor.Path<Counters> path) {
      CounterContext counterContext = new CounterContextImpl(file);
      for (Formula formula : formulaRepository.getFormulas()) {
        Counter counter = newCounter(formula);
        counter.aggregate(counterContext);
        addNewMeasure(file, path, formula, counter);
        aggregateToParent(path, formula, counter);
      }
    }

    private void addNewMeasure(Component component, PathAwareVisitor.Path<Counters> path, Formula formula, Counter counter) {
      Metric metric = metricRepository.getByKey(formula.getOutputMetricKey());
      Optional<Measure> measure = formula.createMeasure(counter, component.getType());
      if (measure.isPresent()) {
        measureRepository.add(component, metric, measure.get());
      }
    }

    private void aggregateToParent(PathAwareVisitor.Path<Counters> path, Formula formula, Counter currentCounter) {
      if (!path.isRoot()) {
        path.parent().aggregate(formula.getOutputMetricKey(), currentCounter);
      }
    }
  }

  private static Counter newCounter(Formula formula) {
    return formula.createNewCounter();
  }

  @Override
  public String getDescription() {
    return "Compute formula measures";
  }

  private static class Counters {
    Map<String, Counter> countersByFormula = new HashMap<>();

    public void aggregate(String metricKey, Counter childCounter) {
      Counter counter = countersByFormula.get(metricKey);
      if (counter == null) {
        countersByFormula.put(metricKey, childCounter);
      } else {
        counter.aggregate(childCounter);
      }
    }

    public Counter getCounter(String metricKey) {
      Counter counter = countersByFormula.get(metricKey);
      if (counter == null) {
        throw new IllegalStateException(String.format("No counter found on metric '%s'", metricKey));
      }
      return counter;
    }
  }

  private class CounterContextImpl implements CounterContext {

    private final Component file;

    public CounterContextImpl(Component file) {
      this.file = file;
    }

    @Override
    public Optional<Measure> getMeasure(String metricKey) {
      return measureRepository.getRawMeasure(file, metricRepository.getByKey(metricKey));
    }
  }
}
