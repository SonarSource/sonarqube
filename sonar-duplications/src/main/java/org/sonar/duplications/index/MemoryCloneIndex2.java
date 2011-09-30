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
package org.sonar.duplications.index;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class MemoryCloneIndex2 extends AbstractCloneIndex {

  private Map<String, List<Block>> byResource = Maps.newHashMap();
  private Map<ByteArray, List<Block>> byHash = Maps.newHashMap();

  public Collection<Block> getByResourceId(String resourceId) {
    return get(byResource, resourceId);
  }

  public Collection<Block> getBySequenceHash(ByteArray sequenceHash) {
    return get(byHash, sequenceHash);
  }

  public void insert(Block block) {
    put(byResource, block.getResourceId(), block);
    put(byHash, block.getBlockHash(), block);
  }

  private static <T> List<Block> get(Map<T, List<Block>> map, T key) {
    List<Block> blocks = map.get(key);
    return blocks != null ? blocks : Collections.EMPTY_LIST;
  }

  private static <T> void put(Map<T, List<Block>> map, T key, Block value) {
    List<Block> blocks = map.get(key);
    if (blocks == null) {
      blocks = Lists.newLinkedList();
      map.put(key, blocks);
    }
    blocks.add(value);
  }

}
