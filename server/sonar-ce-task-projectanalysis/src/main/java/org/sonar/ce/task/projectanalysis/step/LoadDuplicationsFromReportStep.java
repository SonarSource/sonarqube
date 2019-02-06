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

import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.duplication.DetailedTextBlock;
import org.sonar.ce.task.projectanalysis.duplication.Duplicate;
import org.sonar.ce.task.projectanalysis.duplication.Duplication;
import org.sonar.ce.task.projectanalysis.duplication.DuplicationRepository;
import org.sonar.ce.task.projectanalysis.duplication.InExtendedProjectDuplicate;
import org.sonar.ce.task.projectanalysis.duplication.InProjectDuplicate;
import org.sonar.ce.task.projectanalysis.duplication.InnerDuplicate;
import org.sonar.ce.task.projectanalysis.duplication.TextBlock;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Loads duplication information from the report and loads them into the {@link DuplicationRepository}.
 */
public class LoadDuplicationsFromReportStep implements ComputationStep {
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final BatchReportReader batchReportReader;
  private final DuplicationRepository duplicationRepository;

  public LoadDuplicationsFromReportStep(TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder, BatchReportReader batchReportReader,
    DuplicationRepository duplicationRepository) {
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.batchReportReader = batchReportReader;
    this.duplicationRepository = duplicationRepository;
  }

  @Override
  public String getDescription() {
    return "Load duplications";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    DuplicationVisitor visitor = new DuplicationVisitor();
    new DepthTraversalTypeAwareCrawler(visitor).visit(treeRootHolder.getReportTreeRoot());
    context.getStatistics().add("duplications", visitor.count);
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
        Component otherComponent = treeRootHolder.getReportTreeComponentByRef(input.getOtherFileRef());
        if (analysisMetadataHolder.isSLBorPR() && otherComponent.getStatus() == Component.Status.SAME) {
          return new InExtendedProjectDuplicate(otherComponent, convert(input.getRange()));
        } else {
          return new InProjectDuplicate(otherComponent, convert(input.getRange()));
        }
      }
      return new InnerDuplicate(convert(input.getRange()));
    }

    private TextBlock convert(ScannerReport.TextRange textRange) {
      return new TextBlock(textRange.getStartLine(), textRange.getEndLine());
    }
  }

  private class DuplicationVisitor extends TypeAwareVisitorAdapter {
    private int count = 0;

    private DuplicationVisitor() {
      super(CrawlerDepthLimit.FILE, Order.POST_ORDER);
    }

    @Override
    public void visitFile(Component file) {
      try (CloseableIterator<ScannerReport.Duplication> duplications = batchReportReader.readComponentDuplications(file.getReportAttributes().getRef())) {
        int idGenerator = 1;
        while (duplications.hasNext()) {
          loadDuplications(file, duplications.next(), idGenerator);
          idGenerator++;
          count++;
        }
      }
    }

    private void loadDuplications(Component file, ScannerReport.Duplication duplication, int id) {
      duplicationRepository.add(file,
        new Duplication(
          convert(duplication.getOriginPosition(), id),
          duplication.getDuplicateList().stream()
            .map(new BatchDuplicateToCeDuplicate(file)).collect(Collectors.toList())));
    }

    private DetailedTextBlock convert(ScannerReport.TextRange textRange, int id) {
      return new DetailedTextBlock(id, textRange.getStartLine(), textRange.getEndLine());
    }
  }
}
