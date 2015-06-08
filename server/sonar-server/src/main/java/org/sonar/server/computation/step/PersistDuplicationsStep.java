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
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.db.DbClient;

import static org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor.Order.PRE_ORDER;

/**
 * Persist duplications into
 */
public class PersistDuplicationsStep implements ComputationStep {

  private final DbClient dbClient;
  private final DbIdsRepository dbIdsRepository;
  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;

  public PersistDuplicationsStep(DbClient dbClient, DbIdsRepository dbIdsRepository, TreeRootHolder treeRootHolder, BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.dbIdsRepository = dbIdsRepository;
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(true);
    try {
      MetricDto duplicationMetric = dbClient.metricDao().selectByKey(session, CoreMetrics.DUPLICATIONS_DATA_KEY);
      new DuplicationVisitor(session, duplicationMetric).visit(treeRootHolder.getRoot());
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private class DuplicationVisitor extends DepthTraversalTypeAwareVisitor {

    private final DbSession session;
    private final MetricDto duplicationMetric;

    private DuplicationVisitor(DbSession session, MetricDto duplicationMetric) {
      super(Component.Type.FILE, PRE_ORDER);
      this.session = session;
      this.duplicationMetric = duplicationMetric;
    }

    @Override
    public void visitFile(Component file) {
      visitComponent(file);
    }

    private void visitComponent(Component component) {
      List<BatchReport.Duplication> duplications = reportReader.readComponentDuplications(component.getRef());
      if (!duplications.isEmpty()) {
        saveDuplications( component, duplications);
      }
    }

    private void saveDuplications(Component component, List<BatchReport.Duplication> duplications) {
      String duplicationXml = createXmlDuplications(component.getKey(), duplications);
      MeasureDto measureDto = new MeasureDto()
        .setMetricId(duplicationMetric.getId())
        .setData(duplicationXml)
        .setComponentId(dbIdsRepository.getComponentId(component))
        .setSnapshotId(dbIdsRepository.getSnapshotId(component));
      dbClient.measureDao().insert(session, measureDto);
    }

    private String createXmlDuplications(String componentKey, Iterable<BatchReport.Duplication> duplications) {
      StringBuilder xml = new StringBuilder();
      xml.append("<duplications>");
      for (BatchReport.Duplication duplication : duplications) {
        xml.append("<g>");
        appendDuplication(xml, componentKey, duplication.getOriginPosition());
        for (BatchReport.Duplicate duplicationBlock : duplication.getDuplicateList()) {
          processDuplicationBlock(xml, duplicationBlock, componentKey);
        }
        xml.append("</g>");
      }
      xml.append("</duplications>");
      return xml.toString();
    }

    private void processDuplicationBlock(StringBuilder xml, BatchReport.Duplicate duplicate, String componentKey) {
      if (duplicate.hasOtherFileKey()) {
        // componentKey is only set for cross project duplications
        String crossProjectComponentKey = duplicate.getOtherFileKey();
        appendDuplication(xml, crossProjectComponentKey, duplicate);
      } else {
        if (duplicate.hasOtherFileRef()) {
          // Duplication is on a different file
          appendDuplication(xml, treeRootHolder.getComponentByRef(duplicate.getOtherFileRef()).getKey(), duplicate);
        } else {
          // Duplication is on a the same file
          appendDuplication(xml, componentKey, duplicate);
        }
      }
    }

    private void appendDuplication(StringBuilder xml, String componentKey, BatchReport.Duplicate duplicate) {
      appendDuplication(xml, componentKey, duplicate.getRange());
    }

    private void appendDuplication(StringBuilder xml, String componentKey, Range range) {
      int length = range.getEndLine() - range.getStartLine() + 1;
      xml.append("<b s=\"").append(range.getStartLine())
        .append("\" l=\"").append(length)
        .append("\" r=\"").append(StringEscapeUtils.escapeXml(componentKey))
        .append("\"/>");
    }
  }

  @Override
  public String getDescription() {
    return "Persist duplications";
  }

}
