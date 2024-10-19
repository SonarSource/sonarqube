/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Collection;
import java.util.Iterator;

import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.PackedMemoryCloneIndex.ResourceBlocks;

public interface CloneIndex {

  /**
   * Performs search of blocks for specified resource.
   * 
   * @return collection of blocks from index for specified resource and empty collection if nothing found
   */
  Collection<Block> getByResourceId(String resourceId);

  /**
   * Performs search of blocks for specified hash value.
   * 
   * @return collection of blocks from index with specified hash and empty collection if nothing found
   */
  Collection<Block> getBySequenceHash(ByteArray hash);

  /**
   * Adds specified block into index.
   */
  void insert(Block block);

  /**
   * Iterators through the resources, providing the list of blocks for each resource.
   */
  Iterator<ResourceBlocks> iterator();

  
  int noResources();
}
