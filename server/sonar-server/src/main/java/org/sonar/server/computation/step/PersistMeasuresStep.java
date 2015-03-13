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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.measure.MetricCache;
import org.sonar.server.db.DbClient;

import java.util.List;

import static org.sonar.server.computation.measure.BatchReportMeasureUtils.checkMeasure;
import static org.sonar.server.computation.measure.BatchReportMeasureUtils.valueAsDouble;

public class PersistMeasuresStep implements ComputationStep {

  private final DbClient dbClient;
  private final RuleCache ruleCache;
  private final MetricCache metricCache;

  public PersistMeasuresStep(DbClient dbClient, RuleCache ruleCache, MetricCache metricCache) {
    this.dbClient = dbClient;
    this.ruleCache = ruleCache;
    this.metricCache = metricCache;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT, Qualifiers.VIEW};
  }

  @Override
  public String getDescription() {
    return "Persist measures";
  }

  @Override
  public void execute(ComputationContext context) {
    int rootComponentRef = context.getReportMetadata().getRootComponentRef();
    try (DbSession dbSession = dbClient.openSession(true)) {
      recursivelyProcessComponent(dbSession, context, rootComponentRef);
      dbSession.commit();
    }
  }

  private void recursivelyProcessComponent(DbSession dbSession, ComputationContext context, int componentRef) {
    BatchReportReader reportReader = context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    List<BatchReport.Measure> measures = reportReader.readComponentMeasures(componentRef);
    persistMeasures(dbSession, measures, component);
    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(dbSession, context, childRef);
    }
  }

  private void persistMeasures(DbSession dbSession, List<BatchReport.Measure> batchReportMeasures, final BatchReport.Component component) {
    List<MeasureDto> measures = Lists.transform(batchReportMeasures, new Function<BatchReport.Measure, MeasureDto>() {
      @Override
      public MeasureDto apply(BatchReport.Measure batchMeasure) {
        return toMeasureDto(batchMeasure, component);
      }
    });

    for (MeasureDto measure : measures) {
      dbClient.measureDao().insert(dbSession, measure);
    }
  }

  @VisibleForTesting
  MeasureDto toMeasureDto(BatchReport.Measure in, BatchReport.Component component) {
    checkMeasure(in);

    MeasureDto out = new MeasureDto()
      .setTendency(in.hasTendency() ? in.getTendency() : null)
      .setVariation(1, in.hasVariationValue1() ? in.getVariationValue1() : null)
      .setVariation(2, in.hasVariationValue2() ? in.getVariationValue2() : null)
      .setVariation(3, in.hasVariationValue3() ? in.getVariationValue3() : null)
      .setVariation(4, in.hasVariationValue4() ? in.getVariationValue4() : null)
      .setVariation(5, in.hasVariationValue5() ? in.getVariationValue5() : null)
      .setAlertStatus(in.hasAlertStatus() ? in.getAlertStatus() : null)
      .setAlertText(in.hasAlertText() ? in.getAlertText() : null)
      .setDescription(in.hasDescription() ? in.getDescription() : null)
      .setSeverity(in.hasSeverity() ? in.getSeverity().name() : null)
      .setComponentId(component.getId())
      .setSnapshotId(component.getSnapshotId())
      .setMetricId(metricCache.get(in.getMetricKey()).getId())
      .setRuleId(ruleCache.get(RuleKey.parse(in.getRuleKey())).getId())
      .setCharacteristicId(in.hasCharactericId() ? in.getCharactericId() : null);
    out.setValue(valueAsDouble(in));
    out.setData(in.hasStringValue() ? in.getStringValue() : null);
    return out;
  }
}
