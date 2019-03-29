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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.duplication.CrossProjectDuplicate;
import org.sonar.ce.task.projectanalysis.duplication.Duplicate;
import org.sonar.ce.task.projectanalysis.duplication.Duplication;
import org.sonar.ce.task.projectanalysis.duplication.DuplicationRepository;
import org.sonar.ce.task.projectanalysis.duplication.InExtendedProjectDuplicate;
import org.sonar.ce.task.projectanalysis.duplication.InProjectDuplicate;
import org.sonar.ce.task.projectanalysis.duplication.InnerDuplicate;
import org.sonar.ce.task.projectanalysis.duplication.TextBlock;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureToMeasureDto;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.LiveMeasureDto;

import static com.google.common.collect.Iterables.isEmpty;
import static org.sonar.api.measures.CoreMetrics.DUPLICATIONS_DATA_KEY;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Compute duplication data measures on files, based on the {@link DuplicationRepository}
 */
public class PersistDuplicationDataStep implements ComputationStep {

  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final DuplicationRepository duplicationRepository;
  private final MeasureToMeasureDto measureToMeasureDto;
  private final Metric duplicationDataMetric;

  public PersistDuplicationDataStep(DbClient dbClient, TreeRootHolder treeRootHolder, MetricRepository metricRepository,
    DuplicationRepository duplicationRepository, MeasureToMeasureDto measureToMeasureDto) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.duplicationRepository = duplicationRepository;
    this.measureToMeasureDto = measureToMeasureDto;
    this.duplicationDataMetric = metricRepository.getByKey(DUPLICATIONS_DATA_KEY);
  }

  @Override
  public void execute(ComputationStep.Context context) {
    boolean supportUpsert = dbClient.getDatabase().getDialect().supportsUpsert();

    // batch mode of DB session does not have benefits:
    // - on postgres the multi-row upserts are the major optimization and have exactly the same
    //   performance between batch and non-batch sessions
    // - on other dbs the sequence of inserts and updates, in order to emulate upserts,
    //   breaks the constraint of batch sessions (consecutive requests should have the same
    //   structure (same PreparedStatement))
    try (DbSession dbSession = dbClient.openSession(false);
      DuplicationVisitor visitor = new DuplicationVisitor(dbSession, supportUpsert)) {
      new DepthTraversalTypeAwareCrawler(visitor).visit(treeRootHolder.getRoot());
      context.getStatistics().add("insertsOrUpdates", visitor.insertsOrUpdates);
    }
  }

  private class DuplicationVisitor extends TypeAwareVisitorAdapter implements AutoCloseable {
    private final DbSession dbSession;
    private final boolean supportUpsert;
    private final List<LiveMeasureDto> nonPersistedBuffer = new ArrayList<>();
    private int insertsOrUpdates = 0;

    private DuplicationVisitor(DbSession dbSession, boolean supportUpsert) {
      super(CrawlerDepthLimit.FILE, PRE_ORDER);
      this.dbSession = dbSession;
      this.supportUpsert = supportUpsert;
    }

    @Override
    public void visitFile(Component file) {
      Iterable<Duplication> duplications = duplicationRepository.getDuplications(file);
      if (!isEmpty(duplications)) {
        computeDuplications(file, duplications);
      }
    }

    private void computeDuplications(Component component, Iterable<Duplication> duplications) {
      Measure measure = generateMeasure(component.getDbKey(), duplications);
      LiveMeasureDto dto = measureToMeasureDto.toLiveMeasureDto(measure, duplicationDataMetric, component);
      nonPersistedBuffer.add(dto);
      persist(false);
    }

    private void persist(boolean force) {
      // Persist a bunch of 100 or less measures. That prevents from having more than 100 XML documents
      // in memory. Consumption of memory does not explode with the number of duplications and is kept
      // under control.
      // Measures are upserted and transactions are committed every 100 rows (arbitrary number to
      // maximize the performance of a multi-rows request on PostgreSQL).
      // On PostgreSQL, a bunch of 100 measures is persisted into a single request (multi-rows upsert).
      // On other DBs, measures are persisted one by one, with update-or-insert requests.
      boolean shouldPersist = !nonPersistedBuffer.isEmpty() && (force || nonPersistedBuffer.size() > 100);
      if (!shouldPersist) {
        return;
      }
      if (supportUpsert) {
        dbClient.liveMeasureDao().upsert(dbSession, nonPersistedBuffer);
      } else {
        nonPersistedBuffer.forEach(d -> dbClient.liveMeasureDao().insertOrUpdate(dbSession, d));
      }
      insertsOrUpdates += nonPersistedBuffer.size();
      nonPersistedBuffer.clear();
      dbSession.commit();
    }

    @Override
    public void close() {
      // persist the measures remaining in the buffer
      persist(true);
    }

    private Measure generateMeasure(String componentDbKey, Iterable<Duplication> duplications) {
      StringBuilder xml = new StringBuilder();
      xml.append("<duplications>");
      for (Duplication duplication : duplications) {
        xml.append("<g>");
        appendDuplication(xml, componentDbKey, duplication.getOriginal(), false);
        for (Duplicate duplicate : duplication.getDuplicates()) {
          processDuplicationBlock(xml, duplicate, componentDbKey);
        }
        xml.append("</g>");
      }
      xml.append("</duplications>");
      return Measure.newMeasureBuilder().create(xml.toString());
    }

    private void processDuplicationBlock(StringBuilder xml, Duplicate duplicate, String componentDbKey) {
      if (duplicate instanceof InnerDuplicate) {
        // Duplication is on the same file
        appendDuplication(xml, componentDbKey, duplicate);
      } else if (duplicate instanceof InExtendedProjectDuplicate) {
        // Duplication is on a different file that is not saved in the DB
        appendDuplication(xml, ((InExtendedProjectDuplicate) duplicate).getFile().getDbKey(), duplicate.getTextBlock(), true);
      } else if (duplicate instanceof InProjectDuplicate) {
        // Duplication is on a different file
        appendDuplication(xml, ((InProjectDuplicate) duplicate).getFile().getDbKey(), duplicate);
      } else if (duplicate instanceof CrossProjectDuplicate) {
        // Only componentKey is set for cross project duplications
        String crossProjectComponentKey = ((CrossProjectDuplicate) duplicate).getFileKey();
        appendDuplication(xml, crossProjectComponentKey, duplicate);
      } else {
        throw new IllegalArgumentException("Unsupported type of Duplicate " + duplicate.getClass().getName());
      }
    }

    private void appendDuplication(StringBuilder xml, String componentDbKey, Duplicate duplicate) {
      appendDuplication(xml, componentDbKey, duplicate.getTextBlock(), false);
    }

    private void appendDuplication(StringBuilder xml, String componentDbKey, TextBlock textBlock, boolean disableLink) {
      int length = textBlock.getEnd() - textBlock.getStart() + 1;
      xml.append("<b s=\"").append(textBlock.getStart())
        .append("\" l=\"").append(length)
        .append("\" t=\"").append(disableLink)
        .append("\" r=\"").append(StringEscapeUtils.escapeXml(componentDbKey))
        .append("\"/>");
    }
  }

  @Override
  public String getDescription() {
    return "Persist duplication data";
  }

}
