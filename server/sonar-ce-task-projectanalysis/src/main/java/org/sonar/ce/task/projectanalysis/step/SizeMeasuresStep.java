/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.formula.Formula;
import org.sonar.ce.task.projectanalysis.formula.FormulaExecutorComponentVisitor;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;

import static org.sonar.api.measures.CoreMetrics.CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.GENERATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.GENERATED_NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.STATEMENTS_KEY;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.ce.task.projectanalysis.formula.SumFormula.createIntSumFormula;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

/**
 * Compute size measures
 */
public class SizeMeasuresStep implements ComputationStep {
  private static final CounterStackElementFactory COUNTER_STACK_ELEMENT_FACTORY = new CounterStackElementFactory();
  private static final List<Formula> AGGREGATED_SIZE_MEASURE_FORMULAS = Collections.unmodifiableList(Arrays.asList(
    createIntSumFormula(GENERATED_LINES_KEY),
    createIntSumFormula(NCLOC_KEY),
    createIntSumFormula(GENERATED_NCLOC_KEY),
    createIntSumFormula(FUNCTIONS_KEY),
    createIntSumFormula(STATEMENTS_KEY),
    createIntSumFormula(CLASSES_KEY)));

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public SizeMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    new PathAwareCrawler<>(new FileAndDirectoryMeasureVisitor(
      metricRepository.getByKey(FILES_KEY),
      metricRepository.getByKey(LINES_KEY)))
      .visit(treeRootHolder.getRoot());
    new PathAwareCrawler<>(FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
      .buildFor(AGGREGATED_SIZE_MEASURE_FORMULAS))
      .visit(treeRootHolder.getRoot());
  }

  @Override
  public String getDescription() {
    return "Compute size measures";
  }

  private class FileAndDirectoryMeasureVisitor extends PathAwareVisitorAdapter<Counter> {
    private final Metric fileMetric;
    private final Metric linesMetric;

    public FileAndDirectoryMeasureVisitor(Metric fileMetric, Metric linesMetric) {
      super(CrawlerDepthLimit.LEAVES, POST_ORDER, COUNTER_STACK_ELEMENT_FACTORY);
      this.fileMetric = fileMetric;
      this.linesMetric = linesMetric;
    }

    @Override
    public void visitProject(Component project, Path<Counter> path) {
      createMeasures(project, path.current());
    }

    @Override
    public void visitDirectory(Component directory, Path<Counter> path) {
      int mainfileCount = path.current().files;
      if (mainfileCount > 0) {
        measureRepository.add(directory, fileMetric, newMeasureBuilder().create(mainfileCount));
        measureRepository.add(directory, linesMetric, newMeasureBuilder().create(path.current().lines));
        path.parent().files += mainfileCount;
        path.parent().lines += path.current().lines;
      }
    }

    private void createMeasures(Component directory, Counter counter) {
      if (counter.files > 0) {
        measureRepository.add(directory, fileMetric, newMeasureBuilder().create(counter.files));
        measureRepository.add(directory, linesMetric, newMeasureBuilder().create(counter.lines));
      }
    }

    @Override
    public void visitFile(Component file, Path<Counter> path) {
      if (file.getFileAttributes().isUnitTest()) {
        return;
      }
      int lines = file.getFileAttributes().getLines();
      measureRepository.add(file, fileMetric, newMeasureBuilder().create(1));
      measureRepository.add(file, linesMetric, newMeasureBuilder().create(lines));
      path.parent().lines += lines;
      path.parent().files += 1;
    }

    @Override
    public void visitView(Component view, Path<Counter> path) {
      createMeasures(view, path.current());
    }

    @Override
    public void visitSubView(Component subView, Path<Counter> path) {
      createMeasures(subView, path.current());
      path.parent().aggregate(path.current());
    }

    @Override
    public void visitProjectView(Component projectView, Path<Counter> path) {
      path.parent().files += getIntValue(projectView, this.fileMetric);
      path.parent().lines += getIntValue(projectView, this.linesMetric);
    }

    private int getIntValue(Component component, Metric metric) {
      Optional<Measure> fileMeasure = measureRepository.getRawMeasure(component, metric);
      return fileMeasure.map(Measure::getIntValue).orElse(0);
    }
  }

  private static class Counter {
    private int lines = 0;
    private int files = 0;

    void aggregate(Counter counter) {
      files += counter.files;
      lines += counter.lines;
    }
  }

  private static class CounterStackElementFactory extends PathAwareVisitorAdapter.SimpleStackElementFactory<Counter> {
    @Override
    public Counter createForAny(Component component) {
      return new Counter();
    }

    @Override
    public Counter createForFile(Component file) {
      return null;
    }

    @Override
    public Counter createForProjectView(Component projectView) {
      return null;
    }
  }

}
