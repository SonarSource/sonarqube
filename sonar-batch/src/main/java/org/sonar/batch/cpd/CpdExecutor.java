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
package org.sonar.batch.cpd;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.cpd.index.SonarDuplicationsIndex;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Duplicate;
import org.sonar.batch.protocol.output.BatchReport.Duplication;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.detector.suffixtree.SuffixTreeCloneDetectionAlgorithm;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.index.PackedMemoryCloneIndex.ResourceBlocks;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.FluentIterable.from;

/**
 * Runs on the root module, at the end of the project analysis.
 * It executes copy paste detection involving all files of all modules, which were indexed during sensors execution for each module
 * by {@link CpdSensor). The sensor is responsible for handling exclusions and block sizes.
 */
public class CpdExecutor {
  private static final Logger LOG = Loggers.get(CpdExecutor.class);
  static final int MAX_CLONE_GROUP_PER_FILE = 100;
  static final int MAX_CLONE_PART_PER_GROUP = 100;

  private final SonarDuplicationsIndex index;
  private final ReportPublisher publisher;
  private final BatchComponentCache batchComponentCache;
  private final Settings settings;

  public CpdExecutor(Settings settings, SonarDuplicationsIndex index, ReportPublisher publisher, BatchComponentCache batchComponentCache) {
    this.settings = settings;
    this.index = index;
    this.publisher = publisher;
    this.batchComponentCache = batchComponentCache;
  }

  public void execute() {
    Iterator<ResourceBlocks> it = index.iterator();

    while (it.hasNext()) {
      ResourceBlocks resourceBlocks = it.next();
      runCpdAnalysis(resourceBlocks.resourceId(), resourceBlocks.blocks());
    }
  }

  private void runCpdAnalysis(String resource, Collection<Block> fileBlocks) {
    LOG.debug("Detection of duplications for {}", resource);

    BatchComponent component = batchComponentCache.get(resource);
    if (component == null) {
      LOG.error("Resource not found in component cache: {}. Skipping CPD computation for it", resource);
      return;
    }

    List<CloneGroup> duplications;
    try {
      duplications = SuffixTreeCloneDetectionAlgorithm.detect(index, fileBlocks);
    } catch (Exception e) {
      throw new IllegalStateException("Fail during detection of duplication for " + resource, e);
    }

    InputFile inputFile = (InputFile) component.inputComponent();

    List<CloneGroup> filtered;
    if (!"java".equalsIgnoreCase(inputFile.language())) {
      Predicate<CloneGroup> minimumTokensPredicate = DuplicationPredicates.numberOfUnitsNotLessThan(getMinimumTokens(inputFile.language()));
      filtered = from(duplications).filter(minimumTokensPredicate).toList();
    } else {
      filtered = duplications;
    }

    saveDuplications(component, filtered);
  }

  @VisibleForTesting
  /**
   * Not applicable to Java, as the {@link BlockChunker} that it uses does not record start and end units of each block. 
   * Also, it uses statements instead of tokens. 
   * @param languageKey
   * @return
   */
  int getMinimumTokens(String languageKey) {
    int minimumTokens = settings.getInt("sonar.cpd." + languageKey + ".minimumTokens");
    if (minimumTokens == 0) {
      minimumTokens = 100;
    }

    return minimumTokens;
  }

  @VisibleForTesting
  final void saveDuplications(final BatchComponent component, List<CloneGroup> duplications) {
    if (duplications.size() > MAX_CLONE_GROUP_PER_FILE) {
      LOG.warn("Too many duplication groups on file " + component.inputComponent() + ". Keep only the first " + MAX_CLONE_GROUP_PER_FILE +
        " groups.");
    }
    Iterable<org.sonar.batch.protocol.output.BatchReport.Duplication> reportDuplications = from(duplications)
      .limit(MAX_CLONE_GROUP_PER_FILE)
      .transform(
        new Function<CloneGroup, BatchReport.Duplication>() {
          private final BatchReport.Duplication.Builder dupBuilder = BatchReport.Duplication.newBuilder();
          private final BatchReport.Duplicate.Builder blockBuilder = BatchReport.Duplicate.newBuilder();

          @Override
          public BatchReport.Duplication apply(CloneGroup input) {
            return toReportDuplication(component, dupBuilder, blockBuilder, input);
          }

        });
    publisher.getWriter().writeComponentDuplications(component.batchId(), reportDuplications);
  }

  private Duplication toReportDuplication(BatchComponent component, Duplication.Builder dupBuilder, Duplicate.Builder blockBuilder, CloneGroup input) {
    dupBuilder.clear();
    ClonePart originBlock = input.getOriginPart();
    blockBuilder.clear();
    dupBuilder.setOriginPosition(BatchReport.TextRange.newBuilder()
      .setStartLine(originBlock.getStartLine())
      .setEndLine(originBlock.getEndLine())
      .build());
    int clonePartCount = 0;
    for (ClonePart duplicate : input.getCloneParts()) {
      if (!duplicate.equals(originBlock)) {
        clonePartCount++;
        if (clonePartCount > MAX_CLONE_PART_PER_GROUP) {
          LOG.warn("Too many duplication references on file " + component.inputComponent() + " for block at line " +
            originBlock.getStartLine() + ". Keep only the first "
            + MAX_CLONE_PART_PER_GROUP + " references.");
          break;
        }
        blockBuilder.clear();
        String componentKey = duplicate.getResourceId();
        if (!component.key().equals(componentKey)) {
          BatchComponent sameProjectComponent = batchComponentCache.get(componentKey);
          blockBuilder.setOtherFileRef(sameProjectComponent.batchId());
        }
        dupBuilder.addDuplicate(blockBuilder
          .setRange(BatchReport.TextRange.newBuilder()
            .setStartLine(duplicate.getStartLine())
            .setEndLine(duplicate.getEndLine())
            .build())
          .build());
      }
    }
    return dupBuilder.build();
  }
}
