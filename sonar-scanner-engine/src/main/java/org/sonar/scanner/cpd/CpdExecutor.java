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
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.detector.suffixtree.SuffixTreeCloneDetectionAlgorithm;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.index.PackedMemoryCloneIndex.ResourceBlocks;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Duplicate;
import org.sonar.scanner.protocol.output.ScannerReport.Duplication;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
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
  private static final int TIMEOUT = 5 * 60 * 1000;
  static final int MAX_CLONE_GROUP_PER_FILE = 100;
  static final int MAX_CLONE_PART_PER_GROUP = 100;

  private final SonarCpdBlockIndex index;
  private final ReportPublisher publisher;
  private final InputComponentStore componentStore;
  private final ProgressReport progressReport;
  private final CpdSettings settings;
  private final BranchConfiguration branchConfiguration;
  private int count;
  private int total;

  public CpdExecutor(CpdSettings settings, SonarCpdBlockIndex index, ReportPublisher publisher, InputComponentStore inputComponentCache,
    BranchConfiguration branchConfiguration) {
    this.settings = settings;
    this.index = index;
    this.publisher = publisher;
    this.componentStore = inputComponentCache;
    this.branchConfiguration = branchConfiguration;
    this.progressReport = new ProgressReport("CPD computation", TimeUnit.SECONDS.toMillis(10));
  }

  public void execute() {
    if (branchConfiguration.isShortLivingBranch()) {
      LOG.info("Skipping CPD calculation for short living branch");
      return;
    }
    execute(TIMEOUT);
  }

  @VisibleForTesting
  void execute(long timeout) {
    total = index.noResources();
    int filesWithoutBlocks = index.noIndexedFiles() - total;
    if (filesWithoutBlocks > 0) {
      LOG.info("{} {} had no CPD blocks", filesWithoutBlocks, pluralize(filesWithoutBlocks));
    }
    progressReport.start(String.format("Calculating CPD for %d %s", total, pluralize(total)));
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      Iterator<ResourceBlocks> it = index.iterator();

      while (it.hasNext()) {
        ResourceBlocks resourceBlocks = it.next();
        runCpdAnalysis(executorService, resourceBlocks.resourceId(), resourceBlocks.blocks(), timeout);
        count++;
      }
      progressReport.stop("CPD calculation finished");
    } catch (Exception e) {
      progressReport.stop("");
      throw e;
    } finally {
      executorService.shutdown();
    }
  }

  private static String pluralize(int files) {
    return files == 1 ? "file" : "files";
  }

  @VisibleForTesting
  void runCpdAnalysis(ExecutorService executorService, String componentKey, final Collection<Block> fileBlocks, long timeout) {
    DefaultInputComponent component = (DefaultInputComponent) componentStore.getByKey(componentKey);
    if (component == null) {
      LOG.error("Resource not found in component store: {}. Skipping CPD computation for it", componentKey);
      return;
    }

    InputFile inputFile = (InputFile) component;
    LOG.debug("Detection of duplications for {}", inputFile.absolutePath());
    progressReport.message(String.format("%d/%d - current file: %s", count, total, inputFile.absolutePath()));

    List<CloneGroup> duplications;
    Future<List<CloneGroup>> futureResult = executorService.submit(() -> SuffixTreeCloneDetectionAlgorithm.detect(index, fileBlocks));
    try {
      duplications = futureResult.get(timeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      LOG.warn("Timeout during detection of duplications for " + inputFile.absolutePath());
      futureResult.cancel(true);
      return;
    } catch (Exception e) {
      throw new IllegalStateException("Fail during detection of duplication for " + inputFile.absolutePath(), e);
    }

    List<CloneGroup> filtered;
    if (!"java".equalsIgnoreCase(inputFile.language())) {
      int minTokens = settings.getMinimumTokens(inputFile.language());
      Predicate<CloneGroup> minimumTokensPredicate = DuplicationPredicates.numberOfUnitsNotLessThan(minTokens);
      filtered = from(duplications).filter(minimumTokensPredicate).toList();
    } else {
      filtered = duplications;
    }

    saveDuplications(component, filtered);
  }

  @VisibleForTesting
  final void saveDuplications(final DefaultInputComponent component, List<CloneGroup> duplications) {
    if (duplications.size() > MAX_CLONE_GROUP_PER_FILE) {
      LOG.warn("Too many duplication groups on file " + component + ". Keep only the first " + MAX_CLONE_GROUP_PER_FILE +
        " groups.");
    }
    Iterable<ScannerReport.Duplication> reportDuplications = from(duplications)
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

  private Duplication toReportDuplication(InputComponent component, Duplication.Builder dupBuilder, Duplicate.Builder blockBuilder, CloneGroup input) {
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
          LOG.warn("Too many duplication references on file " + component + " for block at line " +
            originBlock.getStartLine() + ". Keep only the first "
            + MAX_CLONE_PART_PER_GROUP + " references.");
          break;
        }
        blockBuilder.clear();
        String componentKey = duplicate.getResourceId();
        if (!component.key().equals(componentKey)) {
          DefaultInputComponent sameProjectComponent = (DefaultInputComponent) componentStore.getByKey(componentKey);
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
