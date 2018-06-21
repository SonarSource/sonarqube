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

import com.google.common.base.Function;
import javax.annotation.Nonnull;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.duplication.DetailedTextBlock;
import org.sonar.server.computation.task.projectanalysis.duplication.Duplicate;
import org.sonar.server.computation.task.projectanalysis.duplication.Duplication;
import org.sonar.server.computation.task.projectanalysis.duplication.DuplicationRepository;
import org.sonar.server.computation.task.projectanalysis.duplication.InProjectDuplicate;
import org.sonar.server.computation.task.projectanalysis.duplication.InnerDuplicate;
import org.sonar.server.computation.task.projectanalysis.duplication.TextBlock;
import org.sonar.server.computation.task.step.ComputationStep;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

/**
 * Loads duplication information from the report and loads them into the {@link DuplicationRepository}.
 */
public class LoadDuplicationsFromReportStep implements ComputationStep {
  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader batchReportReader;
  private final DuplicationRepository duplicationRepository;

  public LoadDuplicationsFromReportStep(TreeRootHolder treeRootHolder, BatchReportReader batchReportReader, DuplicationRepository duplicationRepository) {
    this.treeRootHolder = treeRootHolder;
    this.batchReportReader = batchReportReader;
    this.duplicationRepository = duplicationRepository;
  }

  @Override
  public String getDescription() {
    return "Load inner file and in project duplications";
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, POST_ORDER) {
        @Override
        public void visitFile(Component file) {
          try (CloseableIterator<ScannerReport.Duplication> duplications = batchReportReader.readComponentDuplications(file.getReportAttributes().getRef())) {
            int idGenerator = 1;
            while (duplications.hasNext()) {
              loadDuplications(file, duplications.next(), idGenerator);
              idGenerator++;
            }
          }
        }
      }).visit(treeRootHolder.getRoot());
  }

  private void loadDuplications(Component file, ScannerReport.Duplication duplication, int id) {
    duplicationRepository.add(file,
      new Duplication(
        convert(duplication.getOriginPosition(), id),
        from(duplication.getDuplicateList())
          .transform(new BatchDuplicateToCeDuplicate(file))));
  }

  private static TextBlock convert(ScannerReport.TextRange textRange) {
    return new TextBlock(textRange.getStartLine(), textRange.getEndLine());
  }

  private static DetailedTextBlock convert(ScannerReport.TextRange textRange, int id) {
    return new DetailedTextBlock(id, textRange.getStartLine(), textRange.getEndLine());
  }

  private class BatchDuplicateToCeDuplicate implements Function<ScannerReport.Duplicate, Duplicate> {
    private final Component file;

    private BatchDuplicateToCeDuplicate(Component file) {
      this.file = file;
    }

    @Override
    @Nonnull
    public Duplicate apply(@Nonnull ScannerReport.Duplicate input) {
      if (input.getOtherFileRef() != 0) {
        checkArgument(input.getOtherFileRef() != file.getReportAttributes().getRef(), "file and otherFile references can not be the same");
        return new InProjectDuplicate(
          treeRootHolder.getComponentByRef(input.getOtherFileRef()),
          convert(input.getRange()));
      }
      return new InnerDuplicate(convert(input.getRange()));
    }
  }
}
