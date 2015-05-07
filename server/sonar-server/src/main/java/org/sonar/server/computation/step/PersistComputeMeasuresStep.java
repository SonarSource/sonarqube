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
import org.sonar.api.resources.Qualifiers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasuresCache;
import org.sonar.server.computation.measure.MetricCache;
import org.sonar.server.db.DbClient;
import org.sonar.server.util.CloseableIterator;

import java.util.Iterator;

public class PersistComputeMeasuresStep implements ComputationStep {

  private final DbClient dbClient;
  private final MetricCache metricCache;
  private final MeasuresCache measuresCache;

  public PersistComputeMeasuresStep(DbClient dbClient, MetricCache metricCache, MeasuresCache measuresCache) {
    this.dbClient = dbClient;
    this.metricCache = metricCache;
    this.measuresCache = measuresCache;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT, Qualifiers.VIEW};
  }

  @Override
  public String getDescription() {
    return "Persist compute measures";
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
    CloseableIterator<Measure> measures = measuresCache.traverse(componentRef);
    try {
      persistMeasures(dbSession, measures, component);
    } finally {
      measures.close();
    }
    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(dbSession, context, childRef);
    }
  }

  private void persistMeasures(DbSession dbSession,  Iterator<Measure> measures, final BatchReport.Component component) {
    while (measures.hasNext()) {
      dbClient.measureDao().insert(dbSession, toMeasureDto(measures.next(), component));
    }
  }

  @VisibleForTesting
  MeasureDto toMeasureDto(Measure in, BatchReport.Component component) {
    if (in.getMetricKey() == null) {
      throw new IllegalStateException(String.format("Measure %s does not have metric key", in));
    }

    MeasureDto out = new MeasureDto();
    out.setComponentId(component.getId());
    out.setSnapshotId(component.getSnapshotId());
    out.setMetricId(metricCache.get(in.getMetricKey()).getId());
    out.setByteData(in.getByteValue());
    out.setValue(in.getValue());
    return out;
  }

}
