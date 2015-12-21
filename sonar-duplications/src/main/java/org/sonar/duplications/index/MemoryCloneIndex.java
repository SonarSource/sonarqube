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
package org.sonar.duplications.index;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.PackedMemoryCloneIndex.ResourceBlocks;

import java.util.Collection;
import java.util.Iterator;

public class MemoryCloneIndex implements CloneIndex {

  private Multimap<String, Block> byResource = ArrayListMultimap.create();
  private Multimap<ByteArray, Block> byHash = ArrayListMultimap.create();

  @Override
  public Collection<Block> getByResourceId(String resourceId) {
    return byResource.get(resourceId);
  }

  @Override
  public Collection<Block> getBySequenceHash(ByteArray sequenceHash) {
    return byHash.get(sequenceHash);
  }

  @Override
  public void insert(Block block) {
    byResource.put(block.getResourceId(), block);
    byHash.put(block.getBlockHash(), block);
  }

  @Override
  public Iterator<ResourceBlocks> iterator() {
    throw new UnsupportedOperationException();
  }

}
