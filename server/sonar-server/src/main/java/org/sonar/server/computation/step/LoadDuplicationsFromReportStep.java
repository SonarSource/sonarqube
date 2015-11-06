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

import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.util.CloseableIterator;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.ReportTreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.duplication.DuplicationRepository;
import org.sonar.server.computation.duplication.TextBlock;

import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

/**
 * Loads duplication information from the report and loads them into the {@link DuplicationRepository}.
 */
public class LoadDuplicationsFromReportStep implements ComputationStep {
  private final ReportTreeRootHolder treeRootHolder;
  private final BatchReportReader batchReportReader;
  private final DuplicationRepository duplicationRepository;

  public LoadDuplicationsFromReportStep(ReportTreeRootHolder treeRootHolder, BatchReportReader batchReportReader, DuplicationRepository duplicationRepository) {
    this.treeRootHolder = treeRootHolder;
    this.batchReportReader = batchReportReader;
    this.duplicationRepository = duplicationRepository;
  }

  @Override
  public String getDescription() {
    return "Load inner and project duplications";
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, POST_ORDER) {
        @Override
        public void visitFile(Component file) {
          CloseableIterator<BatchReport.Duplication> duplications = batchReportReader.readComponentDuplications(file.getReportAttributes().getRef());
          try {
            while (duplications.hasNext()) {
              loadDuplications(file, duplications.next());
            }
          } finally {
            duplications.close();
          }
        }
      }).visit(treeRootHolder.getRoot());
  }

  private void loadDuplications(Component file, BatchReport.Duplication duplication) {
    TextBlock original = convert(duplication.getOriginPosition());
    for (BatchReport.Duplicate duplicate : duplication.getDuplicateList()) {
      if (duplicate.hasOtherFileRef()) {
        duplicationRepository.addDuplication(file, original, treeRootHolder.getComponentByRef(duplicate.getOtherFileRef()), convert(duplicate.getRange()));
      } else {
        duplicationRepository.addDuplication(file, original, convert(duplicate.getRange()));
      }
    }
  }

  private static TextBlock convert(BatchReport.TextRange textRange) {
    return new TextBlock(textRange.getStartLine(), textRange.getEndLine());
  }
}
