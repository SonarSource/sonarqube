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
package org.sonar.duplications.detector.original;

import org.junit.Test;
import org.sonar.duplications.block.Block;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BlocksGroupTest {

  /**
   * {@link BlocksGroup} uses only resourceId and index from block, thus we can simplify testing.
   */
  private static Block newBlock(String resourceId, int indexInFile) {
    return Block.builder()
        .setResourceId(resourceId)
        .setIndexInFile(indexInFile)
        .setLines(indexInFile, indexInFile)
        .build();
  }

  public static BlocksGroup newBlocksGroup(Block... blocks) {
    BlocksGroup result = BlocksGroup.empty();
    for (Block block : blocks) {
      result.blocks.add(block);
    }
    return result;
  }

  @Test
  public void shouldReturnSize() {
    BlocksGroup group = newBlocksGroup(newBlock("a", 1), newBlock("b", 2));
    assertThat(group.size(), is(2));
  }

  @Test
  public void shouldCreateEmptyGroup() {
    assertThat(BlocksGroup.empty().size(), is(0));
  }

  @Test
  public void testSubsumedBy() {
    BlocksGroup group1 = newBlocksGroup(newBlock("a", 1), newBlock("b", 2));
    BlocksGroup group2 = newBlocksGroup(newBlock("a", 2), newBlock("b", 3), newBlock("c", 4));
    // block "c" from group2 does not have corresponding block in group1
    assertThat(group2.subsumedBy(group1, 1), is(false));
  }

  @Test
  public void testSubsumedBy2() {
    BlocksGroup group1 = newBlocksGroup(newBlock("a", 1), newBlock("b", 2));
    BlocksGroup group2 = newBlocksGroup(newBlock("a", 2), newBlock("b", 3));
    BlocksGroup group3 = newBlocksGroup(newBlock("a", 3), newBlock("b", 4));
    BlocksGroup group4 = newBlocksGroup(newBlock("a", 4), newBlock("b", 5));

    assertThat(group2.subsumedBy(group1, 1), is(true)); // correction of index - 1

    assertThat(group3.subsumedBy(group1, 2), is(true)); // correction of index - 2
    assertThat(group3.subsumedBy(group2, 1), is(true)); // correction of index - 1

    assertThat(group4.subsumedBy(group1, 3), is(true)); // correction of index - 3
    assertThat(group4.subsumedBy(group2, 2), is(true)); // correction of index - 2
    assertThat(group4.subsumedBy(group3, 1), is(true)); // correction of index - 1
  }

  @Test
  public void testIntersect() {
    BlocksGroup group1 = newBlocksGroup(newBlock("a", 1), newBlock("b", 2));
    BlocksGroup group2 = newBlocksGroup(newBlock("a", 2), newBlock("b", 3));
    BlocksGroup intersection = group1.intersect(group2);
    assertThat(intersection.size(), is(2));
  }

  /**
   * Results for this test taken from results of work of naive implementation.
   */
  @Test
  public void testSubsumedBy3() {
    // ['a'[2|2-7]:3, 'b'[0|0-5]:3] subsumedBy ['a'[1|1-6]:2] false
    assertThat(newBlocksGroup(newBlock("a", 2), newBlock("b", 0))
        .subsumedBy(newBlocksGroup(newBlock("a", 1)), 1),
        is(false));

    // ['a'[3|3-8]:4, 'b'[1|1-6]:4] subsumedBy ['a'[1|1-6]:2] false
    assertThat(newBlocksGroup(newBlock("a", 3), newBlock("b", 1))
        .subsumedBy(newBlocksGroup(newBlock("a", 1)), 1),
        is(false));

    // ['a'[4|4-9]:5, 'b'[2|2-7]:5] subsumedBy ['a'[1|1-6]:2] false
    assertThat(newBlocksGroup(newBlock("a", 4), newBlock("b", 2))
        .subsumedBy(newBlocksGroup(newBlock("a", 1)), 1),
        is(false));

    // ['a'[5|5-10]:6, 'b'[3|3-8]:6] subsumedBy ['a'[1|1-6]:2] false
    assertThat(newBlocksGroup(newBlock("a", 5), newBlock("b", 3))
        .subsumedBy(newBlocksGroup(newBlock("a", 1)), 1),
        is(false));

    // ['a'[3|3-8]:4, 'b'[1|1-6]:4] subsumedBy ['a'[2|2-7]:3, 'b'[0|0-5]:3] true
    assertThat(newBlocksGroup(newBlock("a", 3), newBlock("b", 1))
        .subsumedBy(newBlocksGroup(newBlock("a", 2), newBlock("b", 0)), 1),
        is(true));

    // ['a'[4|4-9]:5, 'b'[2|2-7]:5, 'c'[0|0-5]:5] subsumedBy ['a'[3|3-8]:4, 'b'[1|1-6]:4] false
    assertThat(newBlocksGroup(newBlock("a", 4), newBlock("b", 2), newBlock("c", 0))
        .subsumedBy(newBlocksGroup(newBlock("a", 3), newBlock("b", 1)), 1),
        is(false));

    // ['a'[5|5-10]:6, 'b'[3|3-8]:6, 'c'[1|1-6]:6] subsumedBy ['a'[3|3-8]:4, 'b'[1|1-6]:4] false
    assertThat(newBlocksGroup(newBlock("a", 5), newBlock("b", 3), newBlock("c", 1))
        .subsumedBy(newBlocksGroup(newBlock("a", 3), newBlock("b", 1)), 1),
        is(false));

    // ['a'[6|6-11]:7, 'c'[2|2-7]:7] subsumedBy ['a'[3|3-8]:4, 'b'[1|1-6]:4] false
    assertThat(newBlocksGroup(newBlock("a", 6), newBlock("c", 2))
        .subsumedBy(newBlocksGroup(newBlock("a", 3), newBlock("b", 1)), 1),
        is(false));

    // ['a'[5|5-10]:6, 'b'[3|3-8]:6, 'c'[1|1-6]:6] subsumedBy ['a'[4|4-9]:5, 'b'[2|2-7]:5, 'c'[0|0-5]:5] true
    assertThat(newBlocksGroup(newBlock("a", 5), newBlock("b", 3), newBlock("c", 1))
        .subsumedBy(newBlocksGroup(newBlock("a", 4), newBlock("b", 2), newBlock("c", 0)), 1),
        is(true));

    // ['a'[6|6-11]:7, 'c'[2|2-7]:7] subsumedBy ['a'[5|5-10]:6, 'b'[3|3-8]:6, 'c'[1|1-6]:6] true
    assertThat(newBlocksGroup(newBlock("a", 6), newBlock("c", 2))
        .subsumedBy(newBlocksGroup(newBlock("a", 5), newBlock("b", 3), newBlock("c", 1)), 1),
        is(true));
  }

  /**
   * Results for this test taken from results of work of naive implementation.
   */
  @Test
  public void testIntersect2() {
    // ['a'[2|2-7]:3, 'b'[0|0-5]:3]
    // intersect ['a'[3|3-8]:4, 'b'[1|1-6]:4]
    // as ['a'[3|3-8]:4, 'b'[1|1-6]:4]
    assertThat(newBlocksGroup(newBlock("a", 2), newBlock("b", 0))
        .intersect(newBlocksGroup(newBlock("a", 3), newBlock("b", 1)))
        .size(), is(2));

    // ['a'[3|3-8]:4, 'b'[1|1-6]:4]
    // intersect ['a'[4|4-9]:5, 'b'[2|2-7]:5, 'c'[0|0-5]:5]
    // as ['a'[4|4-9]:5, 'b'[2|2-7]:5]
    assertThat(newBlocksGroup(newBlock("a", 3), newBlock("b", 1))
        .intersect(newBlocksGroup(newBlock("a", 4), newBlock("b", 2), newBlock("c", 0)))
        .size(), is(2));

    // ['a'[4|4-9]:5, 'b'[2|2-7]:5]
    // intersect ['a'[5|5-10]:6, 'b'[3|3-8]:6, 'c'[1|1-6]:6]
    // as ['a'[5|5-10]:6, 'b'[3|3-8]:6]
    assertThat(newBlocksGroup(newBlock("a", 4), newBlock("b", 2))
        .intersect(newBlocksGroup(newBlock("a", 5), newBlock("b", 3), newBlock("c", 1)))
        .size(), is(2));

    // ['a'[5|5-10]:6, 'b'[3|3-8]:6]
    // intersect ['a'[6|6-11]:7, 'c'[2|2-7]:7]
    // as ['a'[6|6-11]:7]
    assertThat(newBlocksGroup(newBlock("a", 5), newBlock("b", 3))
        .intersect(newBlocksGroup(newBlock("a", 6), newBlock("c", 2)))
        .size(), is(1));

    // ['a'[4|4-9]:5, 'b'[2|2-7]:5, 'c'[0|0-5]:5]
    // intersect ['a'[5|5-10]:6, 'b'[3|3-8]:6, 'c'[1|1-6]:6]
    // as ['a'[5|5-10]:6, 'b'[3|3-8]:6, 'c'[1|1-6]:6]
    assertThat(newBlocksGroup(newBlock("a", 4), newBlock("b", 2), newBlock("c", 0))
        .intersect(newBlocksGroup(newBlock("a", 5), newBlock("b", 3), newBlock("c", 1)))
        .size(), is(3));

    // ['a'[5|5-10]:6, 'b'[3|3-8]:6, 'c'[1|1-6]:6]
    // intersect ['a'[6|6-11]:7, 'c'[2|2-7]:7]
    // as ['a'[6|6-11]:7, 'c'[2|2-7]:7]
    assertThat(newBlocksGroup(newBlock("a", 5), newBlock("b", 3), newBlock("c", 1))
        .intersect(newBlocksGroup(newBlock("a", 6), newBlock("c", 2)))
        .size(), is(2));

    // ['a'[6|6-11]:7, 'c'[2|2-7]:7]
    // intersect ['a'[7|7-12]:8]
    // as ['a'[7|7-12]:8]
    assertThat(newBlocksGroup(newBlock("a", 6), newBlock("c", 7))
        .intersect(newBlocksGroup(newBlock("a", 7)))
        .size(), is(1));
  }

}
