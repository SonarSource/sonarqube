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

import org.junit.Before;
import org.junit.Test;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.PackedMemoryCloneIndex.ResourceBlocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class PackedMemoryCloneIndexTest {

  private PackedMemoryCloneIndex index;

  @Before
  public void setUp() {
    index = new PackedMemoryCloneIndex();
  }

  @Test
  public void test() {
    index.insert(newBlock("a", 1));
    index.insert(newBlock("a", 2));
    index.insert(newBlock("b", 1));
    index.insert(newBlock("c", 1));
    index.insert(newBlock("d", 1));
    index.insert(newBlock("e", 1));
    index.insert(newBlock("e", 2));
    index.insert(newBlock("e", 3));

    assertThat(index.noResources()).isEqualTo(5);
    assertThat(index.getBySequenceHash(new ByteArray(1L)).size(), is(5));
    assertThat(index.getBySequenceHash(new ByteArray(2L)).size(), is(2));
    assertThat(index.getBySequenceHash(new ByteArray(3L)).size(), is(1));
    assertThat(index.getBySequenceHash(new ByteArray(4L)).size(), is(0));
    assertThat(index.getByResourceId("a").size(), is(2));
    assertThat(index.getByResourceId("b").size(), is(1));
    assertThat(index.getByResourceId("e").size(), is(3));
    assertThat(index.getByResourceId("does not exist").size(), is(0));
  }

  /**
   * When: query by a hash value.
   * Expected: all blocks should have same hash, which presented in the form of the same object.
   */
  @Test
  public void should_construct_blocks_with_normalized_hash() {
    index.insert(newBlock("a", 1));
    index.insert(newBlock("b", 1));
    index.insert(newBlock("c", 1));
    ByteArray requestedHash = new ByteArray(1L);
    Collection<Block> blocks = index.getBySequenceHash(requestedHash);
    assertThat(blocks.size(), is(3));
    for (Block block : blocks) {
      assertThat(block.getBlockHash(), sameInstance(requestedHash));
    }
  }
  
  @Test
  public void iterate() {
    index.insert(newBlock("a", 1));
    index.insert(newBlock("c", 1));
    index.insert(newBlock("b", 1));
    index.insert(newBlock("c", 2));
    index.insert(newBlock("a", 2));
    
    Iterator<ResourceBlocks> it = index.iterator();
    
    ArrayList<ResourceBlocks> resourcesBlocks = new ArrayList<>();
    
    while(it.hasNext()) {
      resourcesBlocks.add(it.next());
    }
    
    assertThat(resourcesBlocks).hasSize(3);
    
    assertThat(resourcesBlocks.get(0).resourceId()).isEqualTo("a");
    assertThat(resourcesBlocks.get(1).resourceId()).isEqualTo("b");
    assertThat(resourcesBlocks.get(2).resourceId()).isEqualTo("c");
    
    assertThat(resourcesBlocks.get(0).blocks()).hasSize(2);
    assertThat(resourcesBlocks.get(1).blocks()).hasSize(1);
    assertThat(resourcesBlocks.get(2).blocks()).hasSize(2);
    
  }

  /**
   * Given: index with initial capacity 1.
   * Expected: size and capacity should be increased after insertion of two blocks.
   */
  @Test
  public void should_increase_capacity() {
    CloneIndex index = new PackedMemoryCloneIndex(8, 1);
    index.insert(newBlock("a", 1));
    index.insert(newBlock("a", 2));
    assertThat(index.getByResourceId("a").size(), is(2));
  }

  /**
   * Given: index, which accepts blocks with 4-byte hash.
   * Expected: exception during insertion of block with 8-byte hash.
   */
  @Test(expected = IllegalArgumentException.class)
  public void attempt_to_insert_hash_of_incorrect_size() {
    CloneIndex index = new PackedMemoryCloneIndex(4, 1);
    index.insert(newBlock("a", 1));
  }

  /**
   * Given: index, which accepts blocks with 4-byte hash.
   * Expected: exception during search by 8-byte hash.
   */
  @Test(expected = IllegalArgumentException.class)
  public void attempt_to_find_hash_of_incorrect_size() {
    CloneIndex index = new PackedMemoryCloneIndex(4, 1);
    index.getBySequenceHash(new ByteArray(1L));
  }

  private static Block newBlock(String resourceId, long hash) {
    return Block.builder()
        .setResourceId(resourceId)
        .setBlockHash(new ByteArray(hash))
        .setIndexInFile(1)
        .setLines(1, 2)
        .build();
  }

}
