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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.measure.QualityGateStatus;
import org.sonar.ce.task.projectanalysis.measure.qualitygatedetails.EvaluatedCondition;
import org.sonar.ce.task.projectanalysis.measure.qualitygatedetails.QualityGateDetailsData;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.ce.task.projectanalysis.qualitygate.ConditionEvaluator;
import org.sonar.ce.task.projectanalysis.qualitygate.ConditionStatus;
import org.sonar.ce.task.projectanalysis.qualitygate.EvaluationResult;
import org.sonar.ce.task.projectanalysis.qualitygate.EvaluationResultTextConverter;
import org.sonar.ce.task.projectanalysis.qualitygate.MutableQualityGateStatusHolder;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGate;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.stream.MoreCollectors;

import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;
import static org.sonar.ce.task.projectanalysis.qualitygate.ConditionStatus.NO_VALUE_STATUS;
import static org.sonar.ce.task.projectanalysis.qualitygate.ConditionStatus.create;

/**
 * This step:
 * <ul>
 * <li>updates the QualityGateStatus of all the project's measures for the metrics of the conditions of the current
 * QualityGate (retrieved from {@link QualityGateHolder})</li>
 * <li>computes the measures on the project for metrics {@link CoreMetrics#QUALITY_GATE_DETAILS_KEY} and
 * {@link CoreMetrics#ALERT_STATUS_KEY}</li>
 * </ul>
 *
 */
public class QualityGateMeasuresStep implements ComputationStep {
  private final TreeRootHolder treeRootHolder;
  private final QualityGateHolder qualityGateHolder;
  private final MutableQualityGateStatusHolder qualityGateStatusHolder;
  private final MeasureRepository measureRepository;
  private final MetricRepository metricRepository;
  private final EvaluationResultTextConverter evaluationResultTextConverter;
  private final SmallChangesetQualityGateSpecialCase smallChangesetQualityGateSpecialCase;

  public QualityGateMeasuresStep(TreeRootHolder treeRootHolder,
    QualityGateHolder qualityGateHolder, MutableQualityGateStatusHolder qualityGateStatusHolder,
    MeasureRepository measureRepository, MetricRepository metricRepository,
    EvaluationResultTextConverter evaluationResultTextConverter, SmallChangesetQualityGateSpecialCase smallChangesetQualityGateSpecialCase) {
    this.treeRootHolder = treeRootHolder;
    this.qualityGateHolder = qualityGateHolder;
    this.qualityGateStatusHolder = qualityGateStatusHolder;
    this.evaluationResultTextConverter = evaluationResultTextConverter;
    this.measureRepository = measureRepository;
    this.metricRepository = metricRepository;
    this.smallChangesetQualityGateSpecialCase = smallChangesetQualityGateSpecialCase;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.PROJECT, PRE_ORDER) {
        @Override
        public void visitProject(Component project) {
          executeForProject(project);
        }
      }).visit(treeRootHolder.getRoot());
  }

  private void executeForProject(Component project) {
    Optional<QualityGate> qualityGate = qualityGateHolder.getQualityGate();
    if (qualityGate.isPresent()) {
      QualityGateDetailsDataBuilder builder = new QualityGateDetailsDataBuilder();
      updateMeasures(project, qualityGate.get().getConditions(), builder);

      addProjectMeasure(project, builder);

      updateQualityGateStatusHolder(qualityGate.get(), builder);
    }
  }

  private void updateQualityGateStatusHolder(QualityGate qualityGate, QualityGateDetailsDataBuilder builder) {
    qualityGateStatusHolder.setStatus(convert(builder.getGlobalLevel()), createStatusPerCondition(qualityGate.getConditions(), builder.getEvaluatedConditions()));
  }

  private static ConditionStatus.EvaluationStatus toEvaluationStatus(Measure.Level globalLevel) {
    switch (globalLevel) {
      case OK:
        return ConditionStatus.EvaluationStatus.OK;
      case ERROR:
        return ConditionStatus.EvaluationStatus.ERROR;
      default:
        throw new IllegalArgumentException(format(
          "Unsupported value '%s' of Measure.Level can not be converted to EvaluationStatus",
          globalLevel));
    }
  }

  private static org.sonar.ce.task.projectanalysis.qualitygate.QualityGateStatus convert(Measure.Level globalLevel) {
    switch (globalLevel) {
      case OK:
        return org.sonar.ce.task.projectanalysis.qualitygate.QualityGateStatus.OK;
      case ERROR:
        return org.sonar.ce.task.projectanalysis.qualitygate.QualityGateStatus.ERROR;
      default:
        throw new IllegalArgumentException(format(
          "Unsupported value '%s' of Measure.Level can not be converted to QualityGateStatus",
          globalLevel));
    }
  }

  private static Map<Condition, ConditionStatus> createStatusPerCondition(Iterable<Condition> conditions, Iterable<EvaluatedCondition> evaluatedConditions) {
    Map<Condition, EvaluatedCondition> evaluatedConditionPerCondition = from(evaluatedConditions)
      .uniqueIndex(EvaluatedConditionToCondition.INSTANCE);

    ImmutableMap.Builder<Condition, ConditionStatus> builder = ImmutableMap.builder();
    for (Condition condition : conditions) {
      EvaluatedCondition evaluatedCondition = evaluatedConditionPerCondition.get(condition);

      if (evaluatedCondition == null) {
        builder.put(condition, NO_VALUE_STATUS);
      } else {
        builder.put(condition, create(toEvaluationStatus(evaluatedCondition.getLevel()), evaluatedCondition.getActualValue()));
      }
    }
    return builder.build();
  }

  private void updateMeasures(Component project, Set<Condition> conditions, QualityGateDetailsDataBuilder builder) {
    Multimap<Metric, Condition> conditionsPerMetric = conditions.stream().collect(MoreCollectors.index(Condition::getMetric, java.util.function.Function.identity()));
    boolean ignoredConditions = false;
    for (Map.Entry<Metric, Collection<Condition>> entry : conditionsPerMetric.asMap().entrySet()) {
      Metric metric = entry.getKey();
      Optional<Measure> measure = measureRepository.getRawMeasure(project, metric);
      if (!measure.isPresent()) {
        continue;
      }

      final MetricEvaluationResult metricEvaluationResult = evaluateQualityGate(measure.get(), entry.getValue());
      final MetricEvaluationResult finalMetricEvaluationResult;
      if (smallChangesetQualityGateSpecialCase.appliesTo(project, metricEvaluationResult)) {
        finalMetricEvaluationResult = smallChangesetQualityGateSpecialCase.apply(metricEvaluationResult);
        ignoredConditions = true;
      } else {
        finalMetricEvaluationResult = metricEvaluationResult;
      }
      String text = evaluationResultTextConverter.asText(finalMetricEvaluationResult.condition, finalMetricEvaluationResult.evaluationResult);
      builder.addLabel(text);

      Measure updatedMeasure = Measure.updatedMeasureBuilder(measure.get())
        .setQualityGateStatus(new QualityGateStatus(finalMetricEvaluationResult.evaluationResult.getLevel(), text))
        .create();
      measureRepository.update(project, metric, updatedMeasure);

      builder.addEvaluatedCondition(finalMetricEvaluationResult);
    }
    builder.setIgnoredConditions(ignoredConditions);
  }

  private static MetricEvaluationResult evaluateQualityGate(Measure measure, Collection<Condition> conditions) {
    ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
    MetricEvaluationResult metricEvaluationResult = null;
    for (Condition newCondition : conditions) {
      EvaluationResult newEvaluationResult = conditionEvaluator.evaluate(newCondition, measure);
      if (metricEvaluationResult == null || newEvaluationResult.getLevel().ordinal() > metricEvaluationResult.evaluationResult.getLevel().ordinal()) {
        metricEvaluationResult = new MetricEvaluationResult(newEvaluationResult, newCondition);
      }
    }
    return metricEvaluationResult;
  }

  private void addProjectMeasure(Component project, QualityGateDetailsDataBuilder builder) {
    Measure globalMeasure = Measure.newMeasureBuilder()
      .setQualityGateStatus(new QualityGateStatus(builder.getGlobalLevel(), StringUtils.join(builder.getLabels(), ", ")))
      .create(builder.getGlobalLevel());
    Metric metric = metricRepository.getByKey(CoreMetrics.ALERT_STATUS_KEY);
    measureRepository.add(project, metric, globalMeasure);

    String detailMeasureValue = new QualityGateDetailsData(builder.getGlobalLevel(), builder.getEvaluatedConditions(), builder.isIgnoredConditions()).toJson();
    Measure detailsMeasure = Measure.newMeasureBuilder().create(detailMeasureValue);
    Metric qgDetailsMetric = metricRepository.getByKey(CoreMetrics.QUALITY_GATE_DETAILS_KEY);
    measureRepository.add(project, qgDetailsMetric, detailsMeasure);
  }

  @Override
  public String getDescription() {
    return "Compute Quality Gate measures";
  }

  private static final class QualityGateDetailsDataBuilder {
    private Measure.Level globalLevel = Measure.Level.OK;
    private List<String> labels = new ArrayList<>();
    private List<EvaluatedCondition> evaluatedConditions = new ArrayList<>();
    private boolean ignoredConditions;

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

    public void addEvaluatedCondition(MetricEvaluationResult metricEvaluationResult) {
      Measure.Level level = metricEvaluationResult.evaluationResult.getLevel();
      if (Measure.Level.ERROR == level) {
        globalLevel = Measure.Level.ERROR;
      }
      evaluatedConditions.add(
        new EvaluatedCondition(metricEvaluationResult.condition, level, metricEvaluationResult.evaluationResult.getValue()));
    }

    public List<EvaluatedCondition> getEvaluatedConditions() {
      return evaluatedConditions;
    }

    public boolean isIgnoredConditions() {
      return ignoredConditions;
    }

    public QualityGateDetailsDataBuilder setIgnoredConditions(boolean ignoredConditions) {
      this.ignoredConditions = ignoredConditions;
      return this;
    }
  }

  private enum EvaluatedConditionToCondition implements Function<EvaluatedCondition, Condition> {
    INSTANCE;

    @Override
    @Nonnull
    public Condition apply(@Nonnull EvaluatedCondition input) {
      return input.getCondition();
    }
  }

  static class MetricEvaluationResult {
    final EvaluationResult evaluationResult;
    final Condition condition;

    MetricEvaluationResult(EvaluationResult evaluationResult, Condition condition) {
      this.evaluationResult = evaluationResult;
      this.condition = condition;
    }
  }

}
