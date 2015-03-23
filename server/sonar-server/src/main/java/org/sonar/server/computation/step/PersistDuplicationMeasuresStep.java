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

import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Qualifiers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.measure.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;

import javax.annotation.Nullable;

import java.util.List;

public class PersistDuplicationMeasuresStep implements ComputationStep {

  private final DbClient dbClient;

  public PersistDuplicationMeasuresStep(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT};
  }

  @Override
  public void execute(ComputationContext context) {
    DbSession session = dbClient.openSession(true);
    try {
      MetricDto duplicationMetric = dbClient.metricDao().selectByKey(session, CoreMetrics.DUPLICATIONS_DATA_KEY);
      int rootComponentRef = context.getReportMetadata().getRootComponentRef();
      recursivelyProcessComponent(session, context, null, rootComponentRef, duplicationMetric);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void recursivelyProcessComponent(DbSession session, ComputationContext context, @Nullable Integer parentComponentRef, int componentRef, MetricDto duplicationMetric) {
    BatchReportReader reportReader = context.getReportReader();
    String branch = context.getReportMetadata().getBranch();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    List<BatchReport.Duplication> duplications = reportReader.readComponentDuplications(componentRef);
    if (!duplications.isEmpty() && parentComponentRef != null) {
      saveDuplications(session, reportReader, reportReader.readComponent(parentComponentRef), component, duplications, duplicationMetric, branch);
    }

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(session, context, componentRef, childRef, duplicationMetric);
    }
  }

  private void saveDuplications(DbSession session, BatchReportReader reportReader, BatchReport.Component parentComponent, BatchReport.Component component,
                                List<BatchReport.Duplication> duplications, MetricDto duplicationMetric, @Nullable String branch) {
    String duplicationXml = createXmlDuplications(reportReader, parentComponent, component.getPath(), duplications, branch);
    MeasureDto measureDto = new MeasureDto()
      .setMetricId(duplicationMetric.getId())
      .setData(duplicationXml)
      .setComponentId(component.getId())
      .setSnapshotId(component.getSnapshotId());
    dbClient.measureDao().insert(session, measureDto);
  }

  private String createXmlDuplications(BatchReportReader reportReader, BatchReport.Component parentComponent, String componentPath,
    Iterable<BatchReport.Duplication> duplications, @Nullable String branch) {
    StringBuilder xml = new StringBuilder();
    xml.append("<duplications>");
    for (BatchReport.Duplication duplication : duplications) {
      xml.append("<g>");
      appendDuplication(xml, createKey(parentComponent.getKey(), componentPath, branch), duplication.getOriginBlock());
      for (BatchReport.DuplicationBlock duplicationBlock : duplication.getDuplicatedByList()) {
        processDuplicationBlock(xml, duplicationBlock, reportReader, parentComponent.getKey(), componentPath, branch);
      }
      xml.append("</g>");
    }
    xml.append("</duplications>");
    return xml.toString();
  }

  private void processDuplicationBlock(StringBuilder xml, BatchReport.DuplicationBlock duplicationBlock, BatchReportReader reportReader, String parentComponentKey,
                                      String componentPath, @Nullable String branch) {
    if (duplicationBlock.hasComponentKey()) {
      // componentKey is only set for cross project duplications
      String crossProjectComponentKey = duplicationBlock.getComponentKey();
      appendDuplication(xml, crossProjectComponentKey, duplicationBlock);
    } else {
      if (duplicationBlock.hasOtherComponentRef()) {
        // Duplication is on a different file
        BatchReport.Component duplicationComponent = reportReader.readComponent(duplicationBlock.getOtherComponentRef());
        appendDuplication(xml, createKey(parentComponentKey, duplicationComponent.getPath(), branch), duplicationBlock);
      } else {
        // Duplication is on a the same file
        appendDuplication(xml, createKey(parentComponentKey, componentPath, branch), duplicationBlock);
      }
    }
  }

  private static void appendDuplication(StringBuilder xml, String componentKey, BatchReport.DuplicationBlock duplicationBlock) {
    int length = duplicationBlock.getEndLine() - duplicationBlock.getStartLine();
    xml.append("<b s=\"").append(duplicationBlock.getStartLine())
      .append("\" l=\"").append(length)
      .append("\" r=\"").append(StringEscapeUtils.escapeXml(componentKey))
      .append("\"/>");
  }

  private static String createKey(String moduleKey, String path, @Nullable String branch) {
    return ComponentKeys.createKey(moduleKey, path, branch);
  }

  @Override
  public String getDescription() {
    return "Persist duplications";
  }

}
