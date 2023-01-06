/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.duplication.CrossProjectDuplicationStatusHolder;
import org.sonar.ce.task.projectanalysis.duplication.IntegrateCrossProjectDuplications;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.duplication.DuplicationUnitDto;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.scanner.protocol.output.ScannerReport.CpdTextBlock;

/**
 * Feed the duplications repository from the cross project duplication blocks computed with duplications blocks of the analysis report.
 * Blocks can be empty if :
 * - The file is excluded from the analysis using {@link org.sonar.api.CoreProperties#CPD_EXCLUSIONS}
 * - On Java, if the number of statements of the file is too small, nothing will be sent.
 */
public class LoadCrossProjectDuplicationsRepositoryStep implements ComputationStep {

  private static final Logger LOGGER = Loggers.get(LoadCrossProjectDuplicationsRepositoryStep.class);

  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final IntegrateCrossProjectDuplications integrateCrossProjectDuplications;
  private final CrossProjectDuplicationStatusHolder crossProjectDuplicationStatusHolder;
  private final DbClient dbClient;

  public LoadCrossProjectDuplicationsRepositoryStep(TreeRootHolder treeRootHolder, BatchReportReader reportReader,
    AnalysisMetadataHolder analysisMetadataHolder, CrossProjectDuplicationStatusHolder crossProjectDuplicationStatusHolder,
    IntegrateCrossProjectDuplications integrateCrossProjectDuplications, DbClient dbClient) {
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.integrateCrossProjectDuplications = integrateCrossProjectDuplications;
    this.crossProjectDuplicationStatusHolder = crossProjectDuplicationStatusHolder;
    this.dbClient = dbClient;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    if (crossProjectDuplicationStatusHolder.isEnabled()) {
      new DepthTraversalTypeAwareCrawler(new CrossProjectDuplicationVisitor()).visit(treeRootHolder.getRoot());
    }
  }

  @Override
  public String getDescription() {
    return "Compute cross project duplications";
  }

  private class CrossProjectDuplicationVisitor extends TypeAwareVisitorAdapter {

    private CrossProjectDuplicationVisitor() {
      super(CrawlerDepthLimit.FILE, Order.PRE_ORDER);
    }

    @Override
    public void visitFile(Component file) {
      List<CpdTextBlock> cpdTextBlocks = new ArrayList<>();
      try (CloseableIterator<CpdTextBlock> blocksIt = reportReader.readCpdTextBlocks(file.getReportAttributes().getRef())) {
        while(blocksIt.hasNext()) {
          cpdTextBlocks.add(blocksIt.next());
        }
      }
      LOGGER.trace("Found {} cpd blocks on file {}", cpdTextBlocks.size(), file.getKey());
      if (cpdTextBlocks.isEmpty()) {
        return;
      }

      Collection<String> hashes = cpdTextBlocks.stream().map(CpdTextBlockToHash.INSTANCE).toList();
      List<DuplicationUnitDto> dtos = selectDuplicates(file, hashes);
      if (dtos.isEmpty()) {
        return;
      }

      Collection<Block> duplicatedBlocks = dtos.stream().map(DtoToBlock.INSTANCE).toList();
      Collection<Block> originBlocks = cpdTextBlocks.stream().map(new CpdTextBlockToBlock(file.getKey())).toList();
      LOGGER.trace("Found {} duplicated cpd blocks on file {}", duplicatedBlocks.size(), file.getKey());

      integrateCrossProjectDuplications.computeCpd(file, originBlocks, duplicatedBlocks);
    }

    private List<DuplicationUnitDto> selectDuplicates(Component file, Collection<String> hashes) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        Analysis projectAnalysis = analysisMetadataHolder.getBaseAnalysis();
        String analysisUuid = projectAnalysis == null ? null : projectAnalysis.getUuid();
        return dbClient.duplicationDao().selectCandidates(dbSession, analysisUuid, file.getFileAttributes().getLanguageKey(), hashes);
      }
    }
  }

  private enum CpdTextBlockToHash implements Function<CpdTextBlock, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull CpdTextBlock duplicationBlock) {
      return duplicationBlock.getHash();
    }
  }

  private enum DtoToBlock implements Function<DuplicationUnitDto, Block> {
    INSTANCE;

    @Override
    public Block apply(@Nonnull DuplicationUnitDto dto) {
      // Note that the dto doesn't contains start/end token indexes
      return Block.builder()
        .setResourceId(dto.getComponentKey())
        .setBlockHash(new ByteArray(dto.getHash()))
        .setIndexInFile(dto.getIndexInFile())
        .setLines(dto.getStartLine(), dto.getEndLine())
        .build();
    }
  }

  private static class CpdTextBlockToBlock implements Function<CpdTextBlock, Block> {
    private final String fileKey;
    private int indexInFile = 0;

    CpdTextBlockToBlock(String fileKey) {
      this.fileKey = fileKey;
    }

    @Override
    public Block apply(@Nonnull CpdTextBlock duplicationBlock) {
      Block block = Block.builder()
        .setResourceId(fileKey)
        .setBlockHash(new ByteArray(duplicationBlock.getHash()))
        .setIndexInFile(indexInFile)
        .setLines(duplicationBlock.getStartLine(), duplicationBlock.getEndLine())
        .setUnit(duplicationBlock.getStartTokenIndex(), duplicationBlock.getEndTokenIndex())
        .build();
      indexInFile++;
      return block;
    }
  }

}
