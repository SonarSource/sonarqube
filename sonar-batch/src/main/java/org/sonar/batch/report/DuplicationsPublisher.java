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
package org.sonar.batch.report;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.batch.sensor.duplication.Duplication.Block;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplication;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Duplication;
import org.sonar.batch.protocol.output.BatchReport.DuplicationBlock;
import org.sonar.batch.protocol.output.BatchReportWriter;

public class DuplicationsPublisher implements ReportPublisher {

  private final ResourceCache resourceCache;
  private final DuplicationCache duplicationCache;

  public DuplicationsPublisher(ResourceCache resourceCache, DuplicationCache duplicationCache) {
    this.resourceCache = resourceCache;
    this.duplicationCache = duplicationCache;
  }

  @Override
  public void publish(BatchReportWriter writer) {
    for (final BatchResource resource : resourceCache.all()) {
      Iterable<DefaultDuplication> dups = duplicationCache.byComponent(resource.resource().getEffectiveKey());
      if (dups.iterator().hasNext()) {
        Iterable<org.sonar.batch.protocol.output.BatchReport.Duplication> reportDuplications = Iterables.transform(dups,
          new Function<DefaultDuplication, BatchReport.Duplication>() {
            private final BatchReport.Duplication.Builder dupBuilder = BatchReport.Duplication.newBuilder();
            private final BatchReport.DuplicationBlock.Builder blockBuilder = BatchReport.DuplicationBlock.newBuilder();

            @Override
            public BatchReport.Duplication apply(DefaultDuplication input) {
              return toReportDuplication(resource.key(), dupBuilder, blockBuilder, input);
            }

          });
        writer.writeComponentDuplications(resource.batchId(), reportDuplications);
      }
    }
  }

  private Duplication toReportDuplication(String currentComponentKey, Duplication.Builder dupBuilder, DuplicationBlock.Builder blockBuilder, DefaultDuplication input) {
    dupBuilder.clear();
    Block originBlock = input.originBlock();
    blockBuilder.clear();
    dupBuilder.setOriginBlock(blockBuilder
      .setStartLine(originBlock.startLine())
      .setEndLine(originBlock.startLine() + originBlock.length() - 1).build());
    for (Block duplicate : input.duplicates()) {
      blockBuilder.clear();
      String componentKey = duplicate.resourceKey();
      if (!currentComponentKey.equals(componentKey)) {
        BatchResource sameProjectComponent = resourceCache.get(componentKey);
        if (sameProjectComponent != null) {
          blockBuilder.setOtherComponentRef(sameProjectComponent.batchId());
        } else {
          blockBuilder.setComponentKey(componentKey);
        }
      }
      dupBuilder.addDuplicatedBy(blockBuilder
        .setStartLine(duplicate.startLine())
        .setEndLine(duplicate.startLine() + duplicate.length() - 1).build());
    }
    return dupBuilder.build();
  }

}
