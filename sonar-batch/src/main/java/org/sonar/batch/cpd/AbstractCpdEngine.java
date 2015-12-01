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

import com.google.common.base.Function;
import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Duplicate;
import org.sonar.batch.protocol.output.BatchReport.Duplication;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;

import static com.google.common.collect.FluentIterable.from;

public abstract class AbstractCpdEngine extends CpdEngine {

  private static final Logger LOG = Loggers.get(AbstractCpdEngine.class);

  static final int MAX_CLONE_GROUP_PER_FILE = 100;
  static final int MAX_CLONE_PART_PER_GROUP = 100;

  private final ReportPublisher publisher;
  private final BatchComponentCache batchComponentCache;

  public AbstractCpdEngine(ReportPublisher publisher, BatchComponentCache batchComponentCache) {
    this.publisher = publisher;
    this.batchComponentCache = batchComponentCache;
  }

  protected final void saveDuplications(final InputFile inputFile, List<CloneGroup> duplications) {
    if (duplications.size() > MAX_CLONE_GROUP_PER_FILE) {
      LOG.warn("Too many duplication groups on file " + inputFile.relativePath() + ". Keep only the first " + MAX_CLONE_GROUP_PER_FILE + " groups.");
    }
    final BatchComponent component = batchComponentCache.get(inputFile);
    Iterable<org.sonar.batch.protocol.output.BatchReport.Duplication> reportDuplications = from(duplications)
      .limit(MAX_CLONE_GROUP_PER_FILE)
      .transform(
        new Function<CloneGroup, BatchReport.Duplication>() {
          private final BatchReport.Duplication.Builder dupBuilder = BatchReport.Duplication.newBuilder();
          private final BatchReport.Duplicate.Builder blockBuilder = BatchReport.Duplicate.newBuilder();

          @Override
          public BatchReport.Duplication apply(CloneGroup input) {
            return toReportDuplication(component, inputFile, dupBuilder, blockBuilder, input);
          }

        });
    publisher.getWriter().writeComponentDuplications(component.batchId(), reportDuplications);
  }

  private Duplication toReportDuplication(BatchComponent component, InputFile inputFile, Duplication.Builder dupBuilder, Duplicate.Builder blockBuilder, CloneGroup input) {
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
          LOG.warn("Too many duplication references on file " + inputFile.relativePath() + " for block at line " + originBlock.getStartLine() + ". Keep only the first "
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
