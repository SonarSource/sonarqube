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
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.PathAwareCrawler;
import org.sonar.server.computation.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaExecutorComponentVisitor;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;

import static org.sonar.api.measures.CoreMetrics.ACCESSORS_KEY;
import static org.sonar.api.measures.CoreMetrics.CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.GENERATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.GENERATED_NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.STATEMENTS_KEY;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.formula.SumFormula.createIntSumFormula;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

/**
 * Compute size measures
 */
public class SizeMeasuresStep implements ComputationStep {
  private static final CounterStackElementFactory COUNTER_STACK_ELEMENT_FACTORY = new CounterStackElementFactory();
  private static final List<Formula> AGGREGATED_SIZE_MEASURE_FORMULAS = ImmutableList.<Formula>of(
    createIntSumFormula(LINES_KEY),
    createIntSumFormula(GENERATED_LINES_KEY),
    createIntSumFormula(NCLOC_KEY),
    createIntSumFormula(GENERATED_NCLOC_KEY),
    createIntSumFormula(FUNCTIONS_KEY),
    createIntSumFormula(STATEMENTS_KEY),
    createIntSumFormula(CLASSES_KEY),
    createIntSumFormula(ACCESSORS_KEY));

  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public SizeMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void execute() {
    Metric fileMetric = metricRepository.getByKey(CoreMetrics.FILES_KEY);
    Metric directoryMetric = metricRepository.getByKey(CoreMetrics.DIRECTORIES_KEY);

    new PathAwareCrawler<>(new FileAndDirectoryMeasureVisitor(directoryMetric, fileMetric))
      .visit(treeRootHolder.getRoot());
    new PathAwareCrawler<>(FormulaExecutorComponentVisitor.newBuilder(metricRepository, measureRepository)
      .buildFor(AGGREGATED_SIZE_MEASURE_FORMULAS))
        .visit(treeRootHolder.getRoot());
  }

  @Override
  public String getDescription() {
    return "File and Directory measures";
  }

  private class FileAndDirectoryMeasureVisitor extends PathAwareVisitorAdapter<Counter> {
    private final Metric directoryMetric;
    private final Metric fileMetric;

    public FileAndDirectoryMeasureVisitor(Metric directoryMetric, Metric fileMetric) {
      super(CrawlerDepthLimit.LEAVES, POST_ORDER, COUNTER_STACK_ELEMENT_FACTORY);
      this.directoryMetric = directoryMetric;
      this.fileMetric = fileMetric;
    }

    @Override
    public void visitProject(Component project, Path<Counter> path) {
      createMeasures(project, path.current().directories, path.current().files);
    }

    @Override
    public void visitModule(Component module, Path<Counter> path) {
      createMeasures(module, path.current().directories, path.current().files);

      path.parent().directories += path.current().directories;
      path.parent().files += path.current().files;
    }

    @Override
    public void visitDirectory(Component directory, Path<Counter> path) {
      createMeasures(directory, 1, path.current().files);

      path.parent().directories += 1;
      path.parent().files += path.current().files;
    }

    private void createMeasures(Component directory, int dirCount, int fileCount) {
      measureRepository.add(directory, directoryMetric, newMeasureBuilder().create(dirCount));
      if (fileCount > 0) {
        measureRepository.add(directory, fileMetric, newMeasureBuilder().create(fileCount));
      }
    }

    @Override
    public void visitFile(Component file, Path<Counter> path) {
      if (file.getFileAttributes().isUnitTest()) {
        return;
      }
      measureRepository.add(file, fileMetric, newMeasureBuilder().create(1));

      path.parent().files += 1;
    }

    @Override
    public void visitView(Component view, Path<Counter> path) {
      createMeasures(view, path.current().directories, path.current().files);
    }

    @Override
    public void visitSubView(Component subView, Path<Counter> path) {
      createMeasures(subView, path.current().directories, path.current().files);

      path.parent().directories += path.current().directories;
      path.parent().files += path.current().files;
    }

    @Override
    public void visitProjectView(Component projectView, Path<Counter> path) {
      path.parent().directories += getIntValue(projectView, this.directoryMetric);
      path.parent().files += getIntValue(projectView, this.fileMetric);
    }

    private int getIntValue(Component component, Metric metric) {
      Optional<Measure> fileMeasure = measureRepository.getRawMeasure(component, metric);
      return fileMeasure.isPresent() ? fileMeasure.get().getIntValue() : 0;
    }

  }

  private static class Counter {
    private int files = 0;
    private int directories = 0;

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
