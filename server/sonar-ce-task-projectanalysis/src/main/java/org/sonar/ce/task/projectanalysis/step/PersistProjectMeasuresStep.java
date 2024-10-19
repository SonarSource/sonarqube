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

import java.util.Map;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.measure.MeasureToMeasureDto;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.ProjectMeasureDao;
import org.sonar.db.measure.ProjectMeasureDto;

import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

public class PersistProjectMeasuresStep implements ComputationStep {

  private final DbClient dbClient;
  private final MetricRepository metricRepository;
  private final MeasureToMeasureDto measureToMeasureDto;
  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;

  public PersistProjectMeasuresStep(DbClient dbClient, MetricRepository metricRepository, MeasureToMeasureDto measureToMeasureDto, TreeRootHolder treeRootHolder,
    MeasureRepository measureRepository) {
    this.dbClient = dbClient;
    this.metricRepository = metricRepository;
    this.measureToMeasureDto = measureToMeasureDto;
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
  }

  @Override
  public String getDescription() {
    return "Persist project measures";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    try (DbSession dbSession = dbClient.openSession(true)) {
      MeasureVisitor visitor = new MeasureVisitor(dbSession);
      new DepthTraversalTypeAwareCrawler(visitor).visit(treeRootHolder.getRoot());
      dbSession.commit();
      context.getStatistics().add("inserts", visitor.inserts);
    }
  }

  private class MeasureVisitor extends TypeAwareVisitorAdapter {
    private final DbSession session;
    private int inserts = 0;

    private MeasureVisitor(DbSession session) {
      super(CrawlerDepthLimit.LEAVES, PRE_ORDER);
      this.session = session;
    }

    @Override
    public void visitProject(Component project) {
      persistMeasures(project);
    }

    @Override
    public void visitView(Component view) {
      persistMeasures(view);
    }

    @Override
    public void visitSubView(Component subView) {
      persistMeasures(subView);
    }

    @Override
    public void visitProjectView(Component projectView) {
      // measures of project copies are never read. No need to persist them.
    }

    private void persistMeasures(Component component) {
      Map<String, Measure> measures = measureRepository.getRawMeasures(component);
      ProjectMeasureDao projectMeasureDao = dbClient.projectMeasureDao();

      for (Map.Entry<String, Measure> e : measures.entrySet()) {
        Measure measure = e.getValue();
        if (measure.isEmpty()) {
          continue;
        }
        String metricKey = e.getKey();
        Metric metric = metricRepository.getByKey(metricKey);
        if (!metric.isDeleteHistoricalData()) {
          ProjectMeasureDto projectMeasureDto = measureToMeasureDto.toProjectMeasureDto(measure, metric, component);
          projectMeasureDao.insert(session, projectMeasureDto);
          inserts++;
        }
      }
    }
  }
}
