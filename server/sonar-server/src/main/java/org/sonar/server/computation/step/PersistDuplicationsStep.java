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

import java.util.Set;
import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.duplication.CrossProjectDuplicate;
import org.sonar.server.computation.duplication.Duplicate;
import org.sonar.server.computation.duplication.Duplication;
import org.sonar.server.computation.duplication.DuplicationRepository;
import org.sonar.server.computation.duplication.InProjectDuplicate;
import org.sonar.server.computation.duplication.InnerDuplicate;
import org.sonar.server.computation.duplication.TextBlock;

import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Persist duplications into
 */
public class PersistDuplicationsStep implements ComputationStep {

  private final DbClient dbClient;
  private final DbIdsRepository dbIdsRepository;
  private final TreeRootHolder treeRootHolder;
  private final DuplicationRepository duplicationRepository;

  public PersistDuplicationsStep(DbClient dbClient, DbIdsRepository dbIdsRepository, TreeRootHolder treeRootHolder,
    DuplicationRepository duplicationRepository) {
    this.dbClient = dbClient;
    this.dbIdsRepository = dbIdsRepository;
    this.treeRootHolder = treeRootHolder;
    this.duplicationRepository = duplicationRepository;
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
      Set<Duplication> duplications = duplicationRepository.getDuplications(file);
      if (!duplications.isEmpty()) {
        saveDuplications(file, duplications);
      }
    }

    private void saveDuplications(Component component, Iterable<Duplication> duplications) {
      String duplicationXml = createXmlDuplications(component.getKey(), duplications);
      MeasureDto measureDto = new MeasureDto()
        .setMetricId(duplicationMetric.getId())
        .setData(duplicationXml)
        .setComponentId(dbIdsRepository.getComponentId(component))
        .setSnapshotId(dbIdsRepository.getSnapshotId(component));
      dbClient.measureDao().insert(session, measureDto);
    }

    private String createXmlDuplications(String componentKey, Iterable<Duplication> duplications) {
      StringBuilder xml = new StringBuilder();
      xml.append("<duplications>");
      for (Duplication duplication : duplications) {
        xml.append("<g>");
        appendDuplication(xml, componentKey, duplication.getOriginal());
        for (Duplicate duplicate : duplication.getDuplicates()) {
          processDuplicationBlock(xml, duplicate, componentKey);
        }
        xml.append("</g>");
      }
      xml.append("</duplications>");
      return xml.toString();
    }

    private void processDuplicationBlock(StringBuilder xml, Duplicate duplicate, String componentKey) {
      if (duplicate instanceof InnerDuplicate) {
        // Duplication is on a the same file
        appendDuplication(xml, componentKey, duplicate);
      } else if (duplicate instanceof InProjectDuplicate) {
        // Duplication is on a different file
        appendDuplication(xml, ((InProjectDuplicate) duplicate).getFile().getKey(), duplicate);
      } else if (duplicate instanceof CrossProjectDuplicate) {
        // componentKey is only set for cross project duplications
        String crossProjectComponentKey = ((CrossProjectDuplicate) duplicate).getFileKey();
        appendDuplication(xml, crossProjectComponentKey, duplicate);
      } else {
        throw new IllegalArgumentException("Unsupported type of Duplicate " + duplicate.getClass().getName());
      }
    }

    private void appendDuplication(StringBuilder xml, String componentKey, Duplicate duplicate) {
      appendDuplication(xml, componentKey, duplicate.getTextBlock());
    }

    private void appendDuplication(StringBuilder xml, String componentKey, TextBlock textBlock) {
      int length = textBlock.getEnd() - textBlock.getStart() + 1;
      xml.append("<b s=\"").append(textBlock.getStart())
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
