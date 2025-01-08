/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.Component.Type;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.duplication.ComputeDuplicationDataMeasure;
import org.sonar.ce.task.projectanalysis.measure.BestValueOptimization;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.measure.MeasureToMeasureDto;
import org.sonar.ce.task.projectanalysis.measure.ValueType;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureHash;
import org.springframework.beans.factory.annotation.Autowired;

import static org.sonar.api.measures.CoreMetrics.DUPLICATIONS_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.EXECUTABLE_LINES_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA_KEY;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

public class PersistMeasuresStep implements ComputationStep {

  // 50 mb
  private static final int MAX_TRANSACTION_SIZE = 50_000_000;
  private static final Predicate<Measure> NON_EMPTY_MEASURE = measure ->
    measure.getValueType() != ValueType.NO_VALUE || measure.getData() != null;

  /**
   * List of metrics that should not be persisted
   */
  private static final Set<String> NOT_TO_PERSIST = Set.of(
    EXECUTABLE_LINES_DATA_KEY,
    NCLOC_DATA_KEY);

  private final DbClient dbClient;
  private final MetricRepository metricRepository;
  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;
  private final ComputeDuplicationDataMeasure computeDuplicationDataMeasure;
  private final int maxTransactionSize;

  @Autowired
  public PersistMeasuresStep(DbClient dbClient, MetricRepository metricRepository, TreeRootHolder treeRootHolder,
    MeasureRepository measureRepository, @Nullable ComputeDuplicationDataMeasure computeDuplicationDataMeasure) {
    this(dbClient, metricRepository, treeRootHolder, measureRepository, computeDuplicationDataMeasure, MAX_TRANSACTION_SIZE);
  }

  PersistMeasuresStep(DbClient dbClient, MetricRepository metricRepository, TreeRootHolder treeRootHolder,
    MeasureRepository measureRepository, @Nullable ComputeDuplicationDataMeasure computeDuplicationDataMeasure, int maxTransactionSize) {
    this.dbClient = dbClient;
    this.metricRepository = metricRepository;
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.computeDuplicationDataMeasure = computeDuplicationDataMeasure;
    this.maxTransactionSize = maxTransactionSize;
  }

  @Override
  public String getDescription() {
    return "Persist measures";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    Component root = treeRootHolder.getRoot();
    CollectComponentsVisitor visitor = new CollectComponentsVisitor();
    new DepthTraversalTypeAwareCrawler(visitor).visit(root);

    Set<MeasureHash> dbMeasureHashes = getDBMeasureHashes();
    Set<String> dbComponents = dbMeasureHashes.stream().map(MeasureHash::componentUuid).collect(Collectors.toSet());

    List<MeasureDto> inserts = new LinkedList<>();
    List<MeasureDto> updates = new LinkedList<>();
    int insertsOrUpdates = 0;
    int unchanged = 0;
    int size = 0;

    for (Component component : visitor.components) {
      MeasureDto measure = createMeasure(component);

      if (dbMeasureHashes.contains(new MeasureHash(measure.getComponentUuid(), measure.computeJsonValueHash()))) {
        unchanged += measure.getMetricValues().size();
      } else {
        if (dbComponents.contains(measure.getComponentUuid())) {
          updates.add(measure);
        } else {
          inserts.add(measure);
        }
        size += measure.getJsonValue().length();
        insertsOrUpdates += measure.getMetricValues().size();
      }

      if (size > maxTransactionSize) {
        persist(inserts, updates);
        inserts.clear();
        updates.clear();
        size = 0;
      }
    }
    persist(inserts, updates);

    context.getStatistics()
      .add("insertsOrUpdates", insertsOrUpdates)
      .add("unchanged", unchanged);
  }

  private Set<MeasureHash> getDBMeasureHashes() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.measureDao().selectMeasureHashesForBranch(dbSession, treeRootHolder.getRoot().getUuid());
    }
  }

  private MeasureDto createMeasure(Component component) {
    MeasureDto measureDto = new MeasureDto();
    measureDto.setComponentUuid(component.getUuid());
    measureDto.setBranchUuid(treeRootHolder.getRoot().getUuid());

    Map<String, Measure> measures = measureRepository.getRawMeasures(component);
    for (Map.Entry<String, Measure> measuresByMetricKey : measures.entrySet()) {
      String metricKey = measuresByMetricKey.getKey();
      if (shouldNotPersist(metricKey)) {
        continue;
      }
      Metric metric = metricRepository.getByKey(metricKey);
      Predicate<Measure> notBestValueOptimized = BestValueOptimization.from(metric, component).negate();
      Measure measure = measuresByMetricKey.getValue();
      Stream.of(measure)
        .filter(NON_EMPTY_MEASURE)
        .filter(notBestValueOptimized)
        .map(MeasureToMeasureDto::getMeasureValue)
        .filter(Objects::nonNull)
        .forEach(value -> measureDto.addValue(metric.getKey(), value));
    }

    if (component.getType() == Type.FILE) {
      if (computeDuplicationDataMeasure == null) {
        throw new IllegalStateException("ComputeDuplicationDataMeasure not initialized in container");
      }
      computeDuplicationDataMeasure.compute(component)
        .ifPresent(duplicationData -> measureDto.addValue(DUPLICATIONS_DATA_KEY, duplicationData));
    }

    return measureDto;
  }

  private static boolean shouldNotPersist(String metricKey) {
    return NOT_TO_PERSIST.contains(metricKey);
  }

  private void persist(Collection<MeasureDto> inserts, Collection<MeasureDto> updates) {
    if (inserts.isEmpty() && updates.isEmpty()) {
      return;
    }
    try (DbSession dbSession = dbClient.openSession(true)) {
      for (MeasureDto m : inserts) {
        dbClient.measureDao().insert(dbSession, m);
      }
      for (MeasureDto m : updates) {
        dbClient.measureDao().update(dbSession, m);
      }
      dbSession.commit();
    }
  }

  private static class CollectComponentsVisitor extends TypeAwareVisitorAdapter {
    private final List<Component> components = new LinkedList<>();

    private CollectComponentsVisitor() {
      super(CrawlerDepthLimit.LEAVES, PRE_ORDER);
    }

    @Override
    public void visitAny(Component component) {
      components.add(component);
    }
  }
}
