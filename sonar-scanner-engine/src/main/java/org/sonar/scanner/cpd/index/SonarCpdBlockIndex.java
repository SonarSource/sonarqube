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
package org.sonar.scanner.cpd.index;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.AbstractCloneIndex;
import org.sonar.duplications.index.CloneIndex;
import org.sonar.duplications.index.PackedMemoryCloneIndex;
import org.sonar.duplications.index.PackedMemoryCloneIndex.ResourceBlocks;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.report.ReportPublisher;

public class SonarCpdBlockIndex extends AbstractCloneIndex {

  private final CloneIndex mem = new PackedMemoryCloneIndex();
  private final ReportPublisher publisher;
  private final BatchComponentCache batchComponentCache;
  private final Settings settings;
  // Files already tokenized
  private final Set<InputFile> indexedFiles = new HashSet<>();

  public SonarCpdBlockIndex(ReportPublisher publisher, BatchComponentCache batchComponentCache, Settings settings) {
    this.publisher = publisher;
    this.batchComponentCache = batchComponentCache;
    this.settings = settings;
  }

  public void insert(InputFile inputFile, Collection<Block> blocks) {
    if (isCrossProjectDuplicationEnabled(settings)) {
      int id = batchComponentCache.get(inputFile).batchId();
      if (publisher.getWriter().hasComponentData(FileStructure.Domain.CPD_TEXT_BLOCKS, id)) {
        throw new UnsupportedOperationException("Trying to save CPD tokens twice for the same file is not supported: " + inputFile.absolutePath());
      }
      final ScannerReport.CpdTextBlock.Builder builder = ScannerReport.CpdTextBlock.newBuilder();
      publisher.getWriter().writeCpdTextBlocks(id, Iterables.transform(blocks, new Function<Block, ScannerReport.CpdTextBlock>() {
        @Override
        public ScannerReport.CpdTextBlock apply(Block input) {
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
    indexedFiles.add(inputFile);
  }

  public boolean isIndexed(InputFile inputFile) {
    return indexedFiles.contains(inputFile);
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

  @Override
  public int noResources() {
    return mem.noResources();
  }

}
