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

import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Range;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.metric.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.db.DbClient;

/**
 * Persist duplications into
 */
public class PersistDuplicationsStep implements ComputationStep {

  private final DbClient dbClient;
  private final DbComponentsRefCache dbComponentsRefCache;
  private final BatchReportReader reportReader;

  public PersistDuplicationsStep(DbClient dbClient, DbComponentsRefCache dbComponentsRefCache, BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.dbComponentsRefCache = dbComponentsRefCache;
    this.reportReader = reportReader;
  }

  @Override
  public void execute(ComputationContext context) {
    DbSession session = dbClient.openSession(true);
    try {
      MetricDto duplicationMetric = dbClient.metricDao().selectByKey(session, CoreMetrics.DUPLICATIONS_DATA_KEY);
      DuplicationContext duplicationContext = new DuplicationContext(context, duplicationMetric, session);
      int rootComponentRef = context.getReportMetadata().getRootComponentRef();
      recursivelyProcessComponent(duplicationContext, rootComponentRef);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void recursivelyProcessComponent(DuplicationContext duplicationContext, int componentRef) {
    BatchReport.Component component = reportReader.readComponent(componentRef);
    List<BatchReport.Duplication> duplications = reportReader.readComponentDuplications(componentRef);
    if (!duplications.isEmpty()) {
      saveDuplications(duplicationContext, component, duplications);
    }

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(duplicationContext, childRef);
    }
  }

  private void saveDuplications(DuplicationContext duplicationContext, BatchReport.Component component, List<BatchReport.Duplication> duplications) {

    DbComponentsRefCache.DbComponent dbComponent = dbComponentsRefCache.getByRef(component.getRef());
    String duplicationXml = createXmlDuplications(duplicationContext, dbComponent.getKey(), duplications);
    MeasureDto measureDto = new MeasureDto()
      .setMetricId(duplicationContext.metric().getId())
      .setData(duplicationXml)
      .setComponentId(dbComponent.getId())
      .setSnapshotId(component.getSnapshotId());
    dbClient.measureDao().insert(duplicationContext.session(), measureDto);
  }

  private String createXmlDuplications(DuplicationContext duplicationContext, String componentKey, Iterable<BatchReport.Duplication> duplications) {

    StringBuilder xml = new StringBuilder();
    xml.append("<duplications>");
    for (BatchReport.Duplication duplication : duplications) {
      xml.append("<g>");
      appendDuplication(xml, componentKey, duplication.getOriginPosition());
      for (BatchReport.Duplicate duplicationBlock : duplication.getDuplicateList()) {
        processDuplicationBlock(duplicationContext, xml, duplicationBlock, componentKey);
      }
      xml.append("</g>");
    }
    xml.append("</duplications>");
    return xml.toString();
  }

  private void processDuplicationBlock(DuplicationContext duplicationContext, StringBuilder xml, BatchReport.Duplicate duplicate, String componentKey) {

    if (duplicate.hasOtherFileKey()) {
      // componentKey is only set for cross project duplications
      String crossProjectComponentKey = duplicate.getOtherFileKey();
      appendDuplication(xml, crossProjectComponentKey, duplicate);
    } else {
      if (duplicate.hasOtherFileRef()) {
        // Duplication is on a different file
        BatchReport.Component duplicationComponent = reportReader.readComponent(duplicate.getOtherFileRef());
        DbComponentsRefCache.DbComponent dbComponent = dbComponentsRefCache.getByRef(duplicationComponent.getRef());
        appendDuplication(xml, dbComponent.getKey(), duplicate);
      } else {
        // Duplication is on a the same file
        appendDuplication(xml, componentKey, duplicate);
      }
    }
  }

  private static void appendDuplication(StringBuilder xml, String componentKey, BatchReport.Duplicate duplicate) {
    appendDuplication(xml, componentKey, duplicate.getRange());
  }

  private static void appendDuplication(StringBuilder xml, String componentKey, Range range) {
    int length = range.getEndLine() - range.getStartLine() + 1;
    xml.append("<b s=\"").append(range.getStartLine())
      .append("\" l=\"").append(length)
      .append("\" r=\"").append(StringEscapeUtils.escapeXml(componentKey))
      .append("\"/>");
  }

  private static class DuplicationContext {
    private DbSession session;
    private ComputationContext context;
    private MetricDto duplicationMetric;

    DuplicationContext(ComputationContext context, MetricDto duplicationMetric, DbSession session) {
      this.context = context;
      this.duplicationMetric = duplicationMetric;
      this.session = session;
    }

    public ComputationContext context() {
      return context;
    }

    public MetricDto metric() {
      return duplicationMetric;
    }

    public DbSession session() {
      return session;
    }
  }

  @Override
  public String getDescription() {
    return "Persist duplications";
  }

}
