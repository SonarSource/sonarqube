/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.duplications.detector.suffixtree;

import java.util.*;

import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.CloneIndex;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public final class SuffixTreeCloneDetectionAlgorithm {

  public static List<CloneGroup> detect(CloneIndex cloneIndex, Collection<Block> fileBlocks) {
    if (fileBlocks.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    GeneralisedHashText text = retrieveFromIndex(cloneIndex, fileBlocks);
    if (text == null) {
      return Collections.EMPTY_LIST;
    }
    DuplicationsCollector reporter = new DuplicationsCollector(text);
    Search.perform(text, reporter);
    return reporter.getResult();
  }

  private SuffixTreeCloneDetectionAlgorithm() {
  }

  private static GeneralisedHashText retrieveFromIndex(CloneIndex index, Collection<Block> fileBlocks) {
    String originResourceId = fileBlocks.iterator().next().getResourceId();

    Set<ByteArray> hashes = Sets.newHashSet();
    for (Block fileBlock : fileBlocks) {
      hashes.add(fileBlock.getBlockHash());
    }

    Map<String, List<Block>> collection = Maps.newHashMap();
    for (ByteArray hash : hashes) {
      Collection<Block> blocks = index.getBySequenceHash(hash);
      for (Block blockFromIndex : blocks) {
        // Godin: skip blocks for this file if they come from index
        String resourceId = blockFromIndex.getResourceId();
        if (!originResourceId.equals(resourceId)) {
          List<Block> list = collection.get(resourceId);
          if (list == null) {
            list = Lists.newArrayList();
            collection.put(resourceId, list);
          }
          list.add(blockFromIndex);
        }
      }
    }

    if (collection.isEmpty() && hashes.size() == fileBlocks.size()) { // optimization for the case when there is no duplications
      return null;
    }

    GeneralisedHashText text = new GeneralisedHashText();
    List<Block> sortedFileBlocks = Lists.newArrayList(fileBlocks);
    Collections.sort(sortedFileBlocks, BLOCK_COMPARATOR);
    text.addAll(sortedFileBlocks);
    text.addTerminator();

    for (List<Block> list : collection.values()) {
      Collections.sort(list, BLOCK_COMPARATOR);

      int i = 0;
      while (i < list.size()) {
        int j = i + 1;
        while ((j < list.size()) && (list.get(j).getIndexInFile() == list.get(j - 1).getIndexInFile() + 1)) {
          j++;
        }
        text.addAll(list.subList(i, j));
        text.addTerminator();
        i = j;
      }
    }

    text.finish();

    return text;
  }

  private static final Comparator<Block> BLOCK_COMPARATOR = new Comparator<Block>() {
    public int compare(Block o1, Block o2) {
      return o1.getIndexInFile() - o2.getIndexInFile();
    }
  };

}
