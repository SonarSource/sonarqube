/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.duplication.DuplicationUnitDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.duplication.CrossProjectDuplicationStatusHolder;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Persist cross project duplications text blocks into DUPLICATIONS_INDEX table
 */
public class PersistCrossProjectDuplicationIndexStep implements ComputationStep {

  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final BatchReportReader reportReader;
  private final CrossProjectDuplicationStatusHolder crossProjectDuplicationStatusHolder;

  public PersistCrossProjectDuplicationIndexStep(CrossProjectDuplicationStatusHolder crossProjectDuplicationStatusHolder, DbClient dbClient,
    TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder,
    BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.reportReader = reportReader;
    this.crossProjectDuplicationStatusHolder = crossProjectDuplicationStatusHolder;
  }

  @Override
  public void execute() {
    if (!crossProjectDuplicationStatusHolder.isEnabled()) {
      return;
    }

    try (DbSession dbSession = dbClient.openSession(true)) {
      Component project = treeRootHolder.getRoot();
      new DepthTraversalTypeAwareCrawler(new DuplicationVisitor(dbSession, analysisMetadataHolder.getUuid())).visit(project);
      dbSession.commit();
    }
  }

  private class DuplicationVisitor extends TypeAwareVisitorAdapter {

    private final DbSession session;
    private final String analysisUuid;

    private DuplicationVisitor(DbSession session, String analysisUuid) {
      super(CrawlerDepthLimit.FILE, PRE_ORDER);
      this.session = session;
      this.analysisUuid = analysisUuid;
    }

    @Override
    public void visitFile(Component file) {
      visitComponent(file);
    }

    private void visitComponent(Component component) {
      readFromReport(component);
    }

    private void readFromReport(Component component) {
      int indexInFile = 0;
      try (CloseableIterator<ScannerReport.CpdTextBlock> blocks = reportReader.readCpdTextBlocks(component.getReportAttributes().getRef())) {
        while (blocks.hasNext()) {
          ScannerReport.CpdTextBlock block = blocks.next();
          dbClient.duplicationDao().insert(
            session,
            new DuplicationUnitDto()
              .setHash(block.getHash())
              .setStartLine(block.getStartLine())
              .setEndLine(block.getEndLine())
              .setIndexInFile(indexInFile)
              .setAnalysisUuid(analysisUuid)
              .setComponentUuid(component.getUuid()));
          indexInFile++;
        }
      }
    }

  }

  @Override
  public String getDescription() {
    return "Persist cross project duplications index";
  }

}
