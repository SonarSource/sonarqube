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

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.protocol.output.BatchReport.CpdTextBlock;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.duplication.DuplicationUnitDto;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.*;
import org.sonar.server.computation.duplication.IntegrateCrossProjectDuplications;
import org.sonar.server.computation.snapshot.Snapshot;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Feed the duplications repository from the cross project duplication blocks computed with duplications blocks of the analysis report.
 *
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
  private final DbClient dbClient;

  public LoadCrossProjectDuplicationsRepositoryStep(TreeRootHolder treeRootHolder, BatchReportReader reportReader,
    AnalysisMetadataHolder analysisMetadataHolder, IntegrateCrossProjectDuplications integrateCrossProjectDuplications, DbClient dbClient) {
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.integrateCrossProjectDuplications = integrateCrossProjectDuplications;
    this.dbClient = dbClient;
  }

  @Override
  public void execute() {
    if (analysisMetadataHolder.isCrossProjectDuplicationEnabled() && analysisMetadataHolder.getBranch() == null) {
      LOGGER.info("Cross project duplication enabled");
      new DepthTraversalTypeAwareCrawler(new CrossProjectDuplicationVisitor()).visit(treeRootHolder.getRoot());
    } else {
      LOGGER.info("Cross project duplication disabled");
    }
  }

  @Override
  public String getDescription() {
    return "Feed duplications repository with cross project duplications";
  }

  private class CrossProjectDuplicationVisitor extends TypeAwareVisitorAdapter {

    private CrossProjectDuplicationVisitor() {
      super(CrawlerDepthLimit.FILE, PRE_ORDER);
    }

    @Override
    public void visitFile(Component file) {
      visitComponent(file);
    }

    private void visitComponent(Component component) {
      List<CpdTextBlock> cpdTextBlocks = newArrayList(reportReader.readCpdTextBlocks(component.getReportAttributes().getRef()));
      LOGGER.trace("Found {} cpd blocks on file {}", cpdTextBlocks.size(), component.getKey());
      if (cpdTextBlocks.isEmpty()) {
        return;
      }

      Collection<String> hashes = from(cpdTextBlocks).transform(CpdTextBlockToHash.INSTANCE).toList();
      List<DuplicationUnitDto> dtos = selectDuplicates(component, hashes);
      Collection<Block> duplicatedBlocks = from(dtos).transform(DtoToBlock.INSTANCE).toList();
      Collection<Block> originBlocks = from(cpdTextBlocks).transform(new CpdTextBlockToBlock(component.getKey())).toList();

      integrateCrossProjectDuplications.computeCpd(component, originBlocks, duplicatedBlocks);
    }

    private List<DuplicationUnitDto> selectDuplicates(Component file, Collection<String> hashes) {
      DbSession dbSession = dbClient.openSession(false);
      try {
        Snapshot projectSnapshot = analysisMetadataHolder.getBaseProjectSnapshot();
        Long projectSnapshotId = projectSnapshot == null ? null : projectSnapshot.id();
        return dbClient.duplicationDao().selectCandidates(dbSession, projectSnapshotId, file.getFileAttributes().getLanguageKey(), hashes);
      } finally {
        dbClient.closeSession(dbSession);
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

    public CpdTextBlockToBlock(String fileKey) {
      this.fileKey = fileKey;
    }

    @Override
    public Block apply(@Nonnull CpdTextBlock duplicationBlock) {
      return Block.builder()
        .setResourceId(fileKey)
        .setBlockHash(new ByteArray(duplicationBlock.getHash()))
        .setIndexInFile(indexInFile++)
        .setLines(duplicationBlock.getStartLine(), duplicationBlock.getEndLine())
        .setUnit(duplicationBlock.getStartTokenIndex(), duplicationBlock.getEndTokenIndex())
        .build();
    }
  }

}
