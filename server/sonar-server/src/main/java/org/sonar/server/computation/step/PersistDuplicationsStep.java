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

import java.util.Iterator;
import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.ReportTreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;

import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Persist duplications into
 */
public class PersistDuplicationsStep implements ComputationStep {

  private final DbClient dbClient;
  private final DbIdsRepository dbIdsRepository;
  private final ReportTreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;

  public PersistDuplicationsStep(DbClient dbClient, DbIdsRepository dbIdsRepository, ReportTreeRootHolder treeRootHolder, BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.dbIdsRepository = dbIdsRepository;
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(true);
    try {
      MetricDto duplicationMetric = dbClient.metricDao().selectOrFailByKey(session, CoreMetrics.DUPLICATIONS_DATA_KEY);
      new DepthTraversalTypeAwareCrawler(new DuplicationVisitor(session, duplicationMetric))
        .visit(treeRootHolder.getRoot());
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private class DuplicationVisitor extends TypeAwareVisitorAdapter {

    private final DbSession session;
    private final MetricDto duplicationMetric;

    private DuplicationVisitor(DbSession session, MetricDto duplicationMetric) {
      super(CrawlerDepthLimit.FILE, PRE_ORDER);
      this.session = session;
      this.duplicationMetric = duplicationMetric;
    }

    @Override
    public void visitFile(Component file) {
      visitComponent(file);
    }

    private void visitComponent(Component component) {
      try (CloseableIterator<BatchReport.Duplication> duplications = reportReader.readComponentDuplications(component.getReportAttributes().getRef())) {
        if (duplications.hasNext()) {
          saveDuplications(component, duplications);
        }
      }
    }

    private void saveDuplications(Component component, Iterator<BatchReport.Duplication> duplications) {
      String duplicationXml = createXmlDuplications(component.getKey(), duplications);
      MeasureDto measureDto = new MeasureDto()
        .setMetricId(duplicationMetric.getId())
        .setData(duplicationXml)
        .setComponentId(dbIdsRepository.getComponentId(component))
        .setSnapshotId(dbIdsRepository.getSnapshotId(component));
      dbClient.measureDao().insert(session, measureDto);
    }

    private String createXmlDuplications(String componentKey, Iterator<BatchReport.Duplication> duplications) {
      StringBuilder xml = new StringBuilder();
      xml.append("<duplications>");
      while (duplications.hasNext()) {
        BatchReport.Duplication duplication = duplications.next();
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

    private void appendDuplication(StringBuilder xml, String componentKey, BatchReport.TextRange range) {
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
