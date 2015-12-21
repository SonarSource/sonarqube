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
package org.sonar.batch.cpd.index;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.AbstractCloneIndex;
import org.sonar.duplications.index.CloneIndex;
import org.sonar.duplications.index.PackedMemoryCloneIndex;
import org.sonar.duplications.index.PackedMemoryCloneIndex.ResourceBlocks;

public class SonarDuplicationsIndex extends AbstractCloneIndex {

  private final CloneIndex mem = new PackedMemoryCloneIndex();
  private final ReportPublisher publisher;
  private final BatchComponentCache batchComponentCache;
  private final Settings settings;

  public SonarDuplicationsIndex(ReportPublisher publisher, BatchComponentCache batchComponentCache, Settings settings) {
    this.publisher = publisher;
    this.batchComponentCache = batchComponentCache;
    this.settings = settings;
  }

  public void insert(InputFile inputFile, Collection<Block> blocks) {
    if (isCrossProjectDuplicationEnabled(settings)) {
      int id = batchComponentCache.get(inputFile).batchId();
      final BatchReport.CpdTextBlock.Builder builder = BatchReport.CpdTextBlock.newBuilder();
      publisher.getWriter().writeCpdTextBlocks(id, Iterables.transform(blocks, new Function<Block, BatchReport.CpdTextBlock>() {
        @Override
        public BatchReport.CpdTextBlock apply(Block input) {
          builder.clear();
          builder.setStartLine(input.getStartLine());
          builder.setEndLine(input.getEndLine());
          builder.setStartTokenIndex(input.getStartUnit());
          builder.setEndTokenIndex(input.getEndUnit());
          builder.setHash(input.getBlockHash().toHexString());
          return builder.build();
        }
      }));
    }
    for (Block block : blocks) {
      mem.insert(block);
    }
  }

  public static boolean isCrossProjectDuplicationEnabled(Settings settings) {
    return settings.getBoolean(CoreProperties.CPD_CROSS_PROJECT)
      // No cross project duplication for branches
      && StringUtils.isBlank(settings.getString(CoreProperties.PROJECT_BRANCH_PROPERTY));
  }

  public Collection<Block> getByInputFile(String resourceKey) {
    return mem.getByResourceId(resourceKey);
  }

  @Override
  public Collection<Block> getBySequenceHash(ByteArray hash) {
    return mem.getBySequenceHash(hash);
  }

  @Override
  public Collection<Block> getByResourceId(String resourceId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void insert(Block block) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<ResourceBlocks> iterator() {
    return mem.iterator();
  }

}
