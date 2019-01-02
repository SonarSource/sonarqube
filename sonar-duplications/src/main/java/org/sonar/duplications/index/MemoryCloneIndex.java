/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.duplications.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.PackedMemoryCloneIndex.ResourceBlocks;

public class MemoryCloneIndex implements CloneIndex {

  private Map<String, List<Block>> byResource = new LinkedHashMap<>();
  private Map<ByteArray, List<Block>> byHash = new LinkedHashMap<>();

  @Override
  public Collection<Block> getByResourceId(String resourceId) {
    return byResource.computeIfAbsent(resourceId, k -> new ArrayList<>());
  }

  @Override
  public Collection<Block> getBySequenceHash(ByteArray sequenceHash) {
    return byHash.computeIfAbsent(sequenceHash, k -> new ArrayList<>());
  }

  @Override
  public void insert(Block block) {
    getByResourceId(block.getResourceId()).add(block);
    getBySequenceHash(block.getBlockHash()).add(block);
  }

  @Override
  public Iterator<ResourceBlocks> iterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int noResources() {
    return byResource.keySet().size();
  }

}
