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
package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.sonar.core.config.PurgeConstants;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureToMeasureDto;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

public class PersistMeasuresStep implements ComputationStep {

  private final DbClient dbClient;
  private final MetricRepository metricRepository;
  private final MeasureToMeasureDto measureToMeasureDto;
  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;
  private final boolean persistDirectories;

  public PersistMeasuresStep(DbClient dbClient, MetricRepository metricRepository, MeasureToMeasureDto measureToMeasureDto,
    TreeRootHolder treeRootHolder, MeasureRepository measureRepository, ConfigurationRepository settings) {
    this(dbClient, metricRepository, measureToMeasureDto, treeRootHolder,measureRepository,
      !settings.getConfiguration().getBoolean(PurgeConstants.PROPERTY_CLEAN_DIRECTORY).orElseThrow(() -> new IllegalStateException("Missing default value")));
  }

  @VisibleForTesting
  PersistMeasuresStep(DbClient dbClient, MetricRepository metricRepository, MeasureToMeasureDto measureToMeasureDto, TreeRootHolder treeRootHolder,
    MeasureRepository measureRepository, boolean persistDirectories) {
    this.dbClient = dbClient;
    this.metricRepository = metricRepository;
    this.measureToMeasureDto = measureToMeasureDto;
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.persistDirectories = persistDirectories;
  }

  @Override
  public String getDescription() {
    return "Persist measures";
  }

  @Override
  public void execute() {
    try (DbSession dbSession = dbClient.openSession(true)) {
      new DepthTraversalTypeAwareCrawler(new MeasureVisitor(dbSession)).visit(treeRootHolder.getRoot());
      dbSession.commit();
    }
  }

  private class MeasureVisitor extends TypeAwareVisitorAdapter {
    private final DbSession session;

    private MeasureVisitor(DbSession session) {
      super(CrawlerDepthLimit.LEAVES, PRE_ORDER);
      this.session = session;
    }

    @Override
    public void visitProject(Component project) {
      persistMeasures(project);
    }

    @Override
    public void visitModule(Component module) {
      persistMeasures(module);
    }

    @Override
    public void visitDirectory(Component directory) {
      if (persistDirectories) {
        persistMeasures(directory);
      }
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
      persistMeasures(projectView);
    }

    private void persistMeasures(Component component) {
      Multimap<String, Measure> measures = measureRepository.getRawMeasures(component);
      for (Map.Entry<String, Collection<Measure>> measuresByMetricKey : measures.asMap().entrySet()) {
        String metricKey = measuresByMetricKey.getKey();
        Metric metric = metricRepository.getByKey(metricKey);
        MeasureDao measureDao = dbClient.measureDao();
        measuresByMetricKey.getValue().stream().filter(NonEmptyMeasure.INSTANCE).forEach(measure -> {
          MeasureDto measureDto = measureToMeasureDto.toMeasureDto(measure, metric, component);
          measureDao.insert(session, measureDto);
        });
      }
    }

  }

  private enum NonEmptyMeasure implements Predicate<Measure> {
    INSTANCE;

    @Override
    public boolean test(@Nonnull Measure input) {
      return input.getValueType() != Measure.ValueType.NO_VALUE || input.hasVariation() || input.getData() != null;
    }
  }

}
