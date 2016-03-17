/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.duplication.DuplicationUnitDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.duplication.CrossProjectDuplicationStatusHolder;

import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Persist cross project duplications text blocks into DUPLICATIONS_INDEX table
 */
public class PersistCrossProjectDuplicationIndexStep implements ComputationStep {

  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;
  private final DbIdsRepository dbIdsRepository;
  private final CrossProjectDuplicationStatusHolder crossProjectDuplicationStatusHolder;

  public PersistCrossProjectDuplicationIndexStep(DbClient dbClient, DbIdsRepository dbIdsRepository, TreeRootHolder treeRootHolder, BatchReportReader reportReader,
    CrossProjectDuplicationStatusHolder crossProjectDuplicationStatusHolder) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.dbIdsRepository = dbIdsRepository;
    this.crossProjectDuplicationStatusHolder = crossProjectDuplicationStatusHolder;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(true);
    try {
      if (crossProjectDuplicationStatusHolder.isEnabled()) {
        Component project = treeRootHolder.getRoot();
        long projectSnapshotId = dbIdsRepository.getSnapshotId(project);
        new DepthTraversalTypeAwareCrawler(new DuplicationVisitor(session, projectSnapshotId)).visit(project);
      }
      session.commit();
    } finally {
      dbClient.closeSession(session);
    }
  }

  private class DuplicationVisitor extends TypeAwareVisitorAdapter {

    private final DbSession session;
    private final long projectSnapshotId;

    private DuplicationVisitor(DbSession session, long projectSnapshotId) {
      super(CrawlerDepthLimit.FILE, PRE_ORDER);
      this.session = session;
      this.projectSnapshotId = projectSnapshotId;
    }

    @Override
    public void visitFile(Component file) {
      visitComponent(file);
    }

    private void visitComponent(Component component) {
      int indexInFile = 0;
      CloseableIterator<ScannerReport.CpdTextBlock> blocks = reportReader.readCpdTextBlocks(component.getReportAttributes().getRef());
      try {
        while (blocks.hasNext()) {
          ScannerReport.CpdTextBlock block = blocks.next();
          dbClient.duplicationDao().insert(
            session,
            new DuplicationUnitDto()
              .setHash(block.getHash())
              .setStartLine(block.getStartLine())
              .setEndLine(block.getEndLine())
              .setIndexInFile(indexInFile)
              .setSnapshotId(dbIdsRepository.getSnapshotId(component))
              .setProjectSnapshotId(projectSnapshotId)
            );
          indexInFile++;
        }
      } finally {
        blocks.close();
      }
    }
  }

  @Override
  public String getDescription() {
    return "Persist cross project duplications index";
  }

}
