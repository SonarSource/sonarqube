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

import com.google.common.base.Function;
import javax.annotation.Nonnull;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.util.CloseableIterator;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.duplication.DetailedTextBlock;
import org.sonar.server.computation.duplication.Duplicate;
import org.sonar.server.computation.duplication.Duplication;
import org.sonar.server.computation.duplication.DuplicationRepository;
import org.sonar.server.computation.duplication.InProjectDuplicate;
import org.sonar.server.computation.duplication.InnerDuplicate;
import org.sonar.server.computation.duplication.TextBlock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

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
          CloseableIterator<BatchReport.Duplication> duplications = batchReportReader.readComponentDuplications(file.getReportAttributes().getRef());
          try {
            int idGenerator = 1;
            while (duplications.hasNext()) {
              loadDuplications(file, duplications.next(), idGenerator);
              idGenerator++;
            }
          } finally {
            duplications.close();
          }
        }
      }).visit(treeRootHolder.getRoot());
  }

  private void loadDuplications(Component file, BatchReport.Duplication duplication, int id) {
    duplicationRepository.add(file,
      new Duplication(
        convert(duplication.getOriginPosition(), id),
        from(duplication.getDuplicateList())
          .transform(new BatchDuplicateToCeDuplicate(file))
      ));
  }

  private static TextBlock convert(BatchReport.TextRange textRange) {
    return new TextBlock(textRange.getStartLine(), textRange.getEndLine());
  }

  private static DetailedTextBlock convert(BatchReport.TextRange textRange, int id) {
    return new DetailedTextBlock(id, textRange.getStartLine(), textRange.getEndLine());
  }

  private class BatchDuplicateToCeDuplicate implements Function<BatchReport.Duplicate, Duplicate> {
    private final Component file;

    private BatchDuplicateToCeDuplicate(Component file) {
      this.file = file;
    }

    @Override
    @Nonnull
    public Duplicate apply(@Nonnull BatchReport.Duplicate input) {
      if (input.hasOtherFileRef()) {
        checkArgument(input.getOtherFileRef() != file.getReportAttributes().getRef(), "file and otherFile references can not be the same");
        return new InProjectDuplicate(
          treeRootHolder.getComponentByRef(input.getOtherFileRef()),
          convert(input.getRange()));
      }
      return new InnerDuplicate(convert(input.getRange()));
    }
  }
}
