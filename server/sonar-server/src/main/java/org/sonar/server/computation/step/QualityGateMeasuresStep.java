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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.QualityGateStatus;
import org.sonar.server.computation.measure.qualitygatedetails.EvaluatedCondition;
import org.sonar.server.computation.measure.qualitygatedetails.QualityGateDetailsData;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.qualitygate.Condition;
import org.sonar.server.computation.qualitygate.ConditionEvaluator;
import org.sonar.server.computation.qualitygate.EvaluationResult;
import org.sonar.server.computation.qualitygate.EvaluationResultTextConverter;
import org.sonar.server.computation.qualitygate.QualityGate;
import org.sonar.server.computation.qualitygate.QualityGateHolder;

import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * This step:
 * <ul>
 * <li>updates the QualityGateStatus of all the project's measures for the metrics of the conditions of the current
 * QualityGate (retrieved from {@link QualityGateHolder})</li>
 * <li>computes the measures on the project for metrics {@link CoreMetrics#QUALITY_GATE_DETAILS_KEY} and
 * {@link CoreMetrics#ALERT_STATUS_KEY}</li>
 * </ul>
 *
 * It must be executed after the computation of differential measures {@link ComputeMeasureVariationsStep}
 */
public class QualityGateMeasuresStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final QualityGateHolder qualityGateHolder;
  private final MeasureRepository measureRepository;
  private final MetricRepository metricRepository;
  private final EvaluationResultTextConverter evaluationResultTextConverter;

  public QualityGateMeasuresStep(TreeRootHolder treeRootHolder, QualityGateHolder qualityGateHolder,
    MeasureRepository measureRepository, MetricRepository metricRepository,
    EvaluationResultTextConverter evaluationResultTextConverter) {
    this.treeRootHolder = treeRootHolder;
    this.qualityGateHolder = qualityGateHolder;
    this.evaluationResultTextConverter = evaluationResultTextConverter;
    this.measureRepository = measureRepository;
    this.metricRepository = metricRepository;
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.PROJECT, PRE_ORDER) {
        @Override
        public void visitProject(Component project) {
          executeForProject(project);
        }
      }).visit(treeRootHolder.getRoot());
  }

  private void executeForProject(Component project) {
    QualityGateDetailsDataBuilder builder = new QualityGateDetailsDataBuilder();

    Optional<QualityGate> qualityGate = qualityGateHolder.getQualityGate();
    if (qualityGate.isPresent()) {
      updateMeasures(project, qualityGate.get().getConditions(), builder);

      addProjectMeasure(project, builder);
    }
  }

  private void updateMeasures(Component project, Set<Condition> conditions, QualityGateDetailsDataBuilder builder) {
    for (Condition condition : conditions) {
      Optional<Measure> measure = measureRepository.getRawMeasure(project, condition.getMetric());
      if (!measure.isPresent()) {
        continue;
      }

      EvaluationResult evaluationResult = new ConditionEvaluator().evaluate(condition, measure.get());

      String text = evaluationResultTextConverter.asText(condition, evaluationResult);
      builder.addLabel(text);

      Measure updatedMeasure = Measure.updatedMeasureBuilder(measure.get())
        .setQualityGateStatus(new QualityGateStatus(evaluationResult.getLevel(), text))
        .create();
      measureRepository.update(project, condition.getMetric(), updatedMeasure);

      builder.addEvaluatedCondition(condition, evaluationResult);
    }
  }

  private void addProjectMeasure(Component project, QualityGateDetailsDataBuilder builder) {
    Measure globalMeasure = Measure.newMeasureBuilder()
      .setQualityGateStatus(new QualityGateStatus(builder.getGlobalLevel(), StringUtils.join(builder.getLabels(), ", ")))
      .create(builder.getGlobalLevel());
    Metric metric = metricRepository.getByKey(CoreMetrics.ALERT_STATUS_KEY);
    measureRepository.add(project, metric, globalMeasure);

    String detailMeasureValue = new QualityGateDetailsData(builder.getGlobalLevel(), builder.getEvaluatedConditions()).toJson();
    Measure detailsMeasure = Measure.newMeasureBuilder().create(detailMeasureValue);
    Metric qgDetailsMetric = metricRepository.getByKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY);
    measureRepository.add(project, qgDetailsMetric, detailsMeasure);
  }

  @Override
  public String getDescription() {
    return "Computes Quality Gate measures";
  }

  private static final class QualityGateDetailsDataBuilder {
    private Measure.Level globalLevel = Measure.Level.OK;
    private List<String> labels = new ArrayList<>();
    private List<EvaluatedCondition> evaluatedConditions = new ArrayList<>();

    public Measure.Level getGlobalLevel() {
      return globalLevel;
    }

    public void addLabel(@Nullable String label) {
      if (StringUtils.isNotBlank(label)) {
        labels.add(label);
      }
    }

    public List<String> getLabels() {
      return labels;
    }

    public void addEvaluatedCondition(Condition condition, EvaluationResult evaluationResult) {
      if (Measure.Level.WARN == evaluationResult.getLevel() && this.globalLevel != Measure.Level.ERROR) {
        globalLevel = Measure.Level.WARN;

      } else if (Measure.Level.ERROR == evaluationResult.getLevel()) {
        globalLevel = Measure.Level.ERROR;
      }
      evaluatedConditions.add(new EvaluatedCondition(condition, evaluationResult.getLevel(), evaluationResult.getValue()));
    }

    public List<EvaluatedCondition> getEvaluatedConditions() {
      return evaluatedConditions;
    }
  }
}
