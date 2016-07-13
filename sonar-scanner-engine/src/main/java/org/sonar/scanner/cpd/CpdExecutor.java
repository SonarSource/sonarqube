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
package org.sonar.scanner.cpd;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.detector.suffixtree.SuffixTreeCloneDetectionAlgorithm;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.index.PackedMemoryCloneIndex.ResourceBlocks;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;
import org.sonar.scanner.index.BatchComponent;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Duplicate;
import org.sonar.scanner.protocol.output.ScannerReport.Duplication;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.util.ProgressReport;

import static com.google.common.collect.FluentIterable.from;

/**
 * Runs on the root module, at the end of the project analysis.
 * It executes copy paste detection involving all files of all modules, which were indexed during sensors execution for each module
 * by {@link CpdSensor). The sensor is responsible for handling exclusions and block sizes.
 */
public class CpdExecutor {
  private static final Logger LOG = Loggers.get(CpdExecutor.class);
  // timeout for the computation of duplicates in a file (seconds)
  private static final int TIMEOUT = 5 * 60;
  static final int MAX_CLONE_GROUP_PER_FILE = 100;
  static final int MAX_CLONE_PART_PER_GROUP = 100;

  private final SonarCpdBlockIndex index;
  private final ReportPublisher publisher;
  private final BatchComponentCache batchComponentCache;
  private final Settings settings;
  private final ExecutorService executorService;
  private final ProgressReport progressReport;
  private int count;
  private int total;

  public CpdExecutor(Settings settings, SonarCpdBlockIndex index, ReportPublisher publisher, BatchComponentCache batchComponentCache) {
    this.settings = settings;
    this.index = index;
    this.publisher = publisher;
    this.batchComponentCache = batchComponentCache;
    this.executorService = Executors.newSingleThreadExecutor();
    this.progressReport = new ProgressReport("CPD computation", TimeUnit.SECONDS.toMillis(10));
  }

  public void execute() {
    total = index.noResources();
    progressReport.start(String.format("Calculating CPD for %d files", total));
    try {
      Iterator<ResourceBlocks> it = index.iterator();

      while (it.hasNext()) {
        ResourceBlocks resourceBlocks = it.next();
        runCpdAnalysis(resourceBlocks.resourceId(), resourceBlocks.blocks());
        count++;
      }
      progressReport.stop("CPD calculation finished");
    } catch (Exception e) {
      progressReport.stop("");
      throw e;
    }
  }

  private void runCpdAnalysis(String resource, final Collection<Block> fileBlocks) {
    LOG.debug("Detection of duplications for {}", resource);

    BatchComponent component = batchComponentCache.get(resource);
    if (component == null) {
      LOG.error("Resource not found in component cache: {}. Skipping CPD computation for it", resource);
      return;
    }

    InputFile inputFile = (InputFile) component.inputComponent();
    progressReport.message(String.format("%d/%d - current file: %s", count, total, inputFile.absolutePath()));

    List<CloneGroup> duplications;
    Future<List<CloneGroup>> futureResult = null;
    try {
      futureResult = executorService.submit(() -> SuffixTreeCloneDetectionAlgorithm.detect(index, fileBlocks));
      duplications = futureResult.get(TIMEOUT, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      LOG.warn("Timeout during detection of duplications for " + inputFile.absolutePath());
      if (futureResult != null) {
        futureResult.cancel(true);
      }
      return;
    } catch (Exception e) {
      throw new IllegalStateException("Fail during detection of duplication for " + inputFile.absolutePath(), e);
    }

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
    Iterable<org.sonar.scanner.protocol.output.ScannerReport.Duplication> reportDuplications = from(duplications)
      .limit(MAX_CLONE_GROUP_PER_FILE)
      .transform(
        new Function<CloneGroup, ScannerReport.Duplication>() {
          private final ScannerReport.Duplication.Builder dupBuilder = ScannerReport.Duplication.newBuilder();
          private final ScannerReport.Duplicate.Builder blockBuilder = ScannerReport.Duplicate.newBuilder();

          @Override
          public ScannerReport.Duplication apply(CloneGroup input) {
            return toReportDuplication(component, dupBuilder, blockBuilder, input);
          }

        });
    publisher.getWriter().writeComponentDuplications(component.batchId(), reportDuplications);
  }

  private Duplication toReportDuplication(BatchComponent component, Duplication.Builder dupBuilder, Duplicate.Builder blockBuilder, CloneGroup input) {
    dupBuilder.clear();
    ClonePart originBlock = input.getOriginPart();
    blockBuilder.clear();
    dupBuilder.setOriginPosition(ScannerReport.TextRange.newBuilder()
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
          .setRange(ScannerReport.TextRange.newBuilder()
            .setStartLine(duplicate.getStartLine())
            .setEndLine(duplicate.getEndLine())
            .build())
          .build());
      }
    }
    return dupBuilder.build();
  }
}
