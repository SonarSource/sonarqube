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

import com.google.common.collect.ImmutableList;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.PathAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.formula.Formula;
import org.sonar.server.computation.formula.FormulaExecutorComponentCrawler;
import org.sonar.server.computation.formula.SumFormula;
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
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.ComponentCrawler.Order.POST_ORDER;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

/**
 * Compute size measures
 */
public class SizeMeasuresStep implements ComputationStep {
  private static final CounterStackElementFactory COUNTER_STACK_ELEMENT_FACTORY = new CounterStackElementFactory();
  private static final ImmutableList<Formula> AGGREGATED_SIZE_MEASURE_FORMULAS = ImmutableList.<Formula>of(
    new SumFormula(LINES_KEY),
    new SumFormula(GENERATED_LINES_KEY),
    new SumFormula(NCLOC_KEY),
    new SumFormula(GENERATED_NCLOC_KEY),
    new SumFormula(FUNCTIONS_KEY),
    new SumFormula(STATEMENTS_KEY),
    new SumFormula(CLASSES_KEY),
    new SumFormula(ACCESSORS_KEY)
    );

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

    new FileAndDirectoryMeasureCrawler(directoryMetric, fileMetric)
      .visit(treeRootHolder.getRoot());
    FormulaExecutorComponentCrawler.newBuilder(metricRepository, measureRepository)
      .buildFor(AGGREGATED_SIZE_MEASURE_FORMULAS)
      .visit(treeRootHolder.getRoot());
  }

  @Override
  public String getDescription() {
    return "File and Directory measures";
  }

  private class FileAndDirectoryMeasureCrawler extends PathAwareCrawler<Counter> {
    private final Metric directoryMetric;
    private final Metric fileMetric;

    public FileAndDirectoryMeasureCrawler(Metric directoryMetric, Metric fileMetric) {
      super(FILE, POST_ORDER, COUNTER_STACK_ELEMENT_FACTORY);
      this.directoryMetric = directoryMetric;
      this.fileMetric = fileMetric;
    }

    @Override
    protected void visitProject(Component project, Path<Counter> path) {
      createMeasures(project, path.current().directories, path.current().files);
    }

    @Override
    protected void visitModule(Component module, Path<Counter> path) {
      createMeasures(module, path.current().directories, path.current().files);

      path.parent().directories += path.current().directories;
      path.parent().files += path.current().files;
    }

    @Override
    protected void visitDirectory(Component directory, Path<Counter> path) {
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
    protected void visitFile(Component file, Path<Counter> path) {
      if (file.getFileAttributes().isUnitTest()) {
        return;
      }
      measureRepository.add(file, fileMetric, newMeasureBuilder().create(1));

      path.parent().files += 1;
    }

  }

  private static class Counter {
    private int files = 0;
    private int directories = 0;

  }

  private static class CounterStackElementFactory extends PathAwareCrawler.SimpleStackElementFactory<Counter> {
    @Override
    public Counter createForAny(Component component) {
      return new Counter();
    }

    @Override
    public Counter createForFile(Component file) {
      return null;
    }
  }
}
