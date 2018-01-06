/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.duplications.detector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.CloneIndex;
import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.index.MemoryCloneIndex;
import org.sonar.duplications.junit.TestNamePrinter;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.duplications.detector.CloneGroupMatcher.hasCloneGroup;

public abstract class DetectorTestCase {

  @Rule
  public TestNamePrinter testNamePrinter = new TestNamePrinter();

  protected static int LINES_PER_BLOCK = 5;

  /**
   * To simplify testing we assume that each block starts from a new line and contains {@link #LINES_PER_BLOCK} lines,
   * so we can simply use index and hash.
   */
  protected static Block newBlock(String resourceId, ByteArray hash, int index) {
    return Block.builder()
      .setResourceId(resourceId)
      .setBlockHash(hash)
      .setIndexInFile(index)
      .setLines(index, index + LINES_PER_BLOCK)
      .build();
  }

  protected static ClonePart newClonePart(String resourceId, int unitStart, int cloneUnitLength) {
    return new ClonePart(resourceId, unitStart, unitStart, unitStart + cloneUnitLength + LINES_PER_BLOCK - 1);
  }

  protected abstract List<CloneGroup> detect(CloneIndex index, Block[] fileBlocks);

  /**
   * Given:
   * <pre>
   * y:   2 3 4 5
   * z:     3 4
   * x: 1 2 3 4 5 6
   * </pre>
   * Expected:
   * <pre>
   * x-y (2 3 4 5)
   * x-y-z (3 4)
   * </pre>
   */
  @Test
  public void exampleFromPaper() {
    CloneIndex index = createIndex(
      newBlocks("y", "2 3 4 5"),
      newBlocks("z", "3 4"));
    Block[] fileBlocks = newBlocks("x", "1 2 3 4 5 6");
    List<CloneGroup> result = detect(index, fileBlocks);

    print(result);
    assertEquals(2, result.size());

    assertThat(result, hasCloneGroup(4,
      newClonePart("x", 1, 4),
      newClonePart("y", 0, 4)));

    assertThat(result, hasCloneGroup(2,
      newClonePart("x", 2, 2),
      newClonePart("y", 1, 2),
      newClonePart("z", 0, 2)));
  }

  /**
   * Given:
   * <pre>
   * a:   2 3 4 5
   * b:     3 4
   * c: 1 2 3 4 5 6
   * </pre>
   * Expected:
   * <pre>
   * c-a (2 3 4 5)
   * c-a-b (3 4)
   * </pre>
   */
  @Test
  public void exampleFromPaperWithModifiedResourceIds() {
    CloneIndex cloneIndex = createIndex(
      newBlocks("a", "2 3 4 5"),
      newBlocks("b", "3 4"));
    Block[] fileBlocks = newBlocks("c", "1 2 3 4 5 6");
    List<CloneGroup> clones = detect(cloneIndex, fileBlocks);

    print(clones);
    assertThat(clones.size(), is(2));

    assertThat(clones, hasCloneGroup(4,
      newClonePart("c", 1, 4),
      newClonePart("a", 0, 4)));

    assertThat(clones, hasCloneGroup(2,
      newClonePart("c", 2, 2),
      newClonePart("a", 1, 2),
      newClonePart("b", 0, 2)));
  }

  /**
   * Given:
   * <pre>
   * b:     3 4 5 6
   * c:         5 6 7
   * a: 1 2 3 4 5 6 7 8 9
   * </pre>
   * Expected:
   * <pre>
   * a-b (3 4 5 6)
   * a-b-c (5 6)
   * a-c (5 6 7)
   * </pre>
   */
  @Test
  public void example1() {
    CloneIndex index = createIndex(
      newBlocks("b", "3 4 5 6"),
      newBlocks("c", "5 6 7"));
    Block[] fileBlocks = newBlocks("a", "1 2 3 4 5 6 7 8 9");
    List<CloneGroup> result = detect(index, fileBlocks);

    print(result);
    assertThat(result.size(), is(3));

    assertThat(result, hasCloneGroup(4,
      newClonePart("a", 2, 4),
      newClonePart("b", 0, 4)));

    assertThat(result, hasCloneGroup(3,
      newClonePart("a", 4, 3),
      newClonePart("c", 0, 3)));

    assertThat(result, hasCloneGroup(2,
      newClonePart("a", 4, 2),
      newClonePart("b", 2, 2),
      newClonePart("c", 0, 2)));
  }

  /**
   * Given:
   * <pre>
   * b: 1 2 3 4 1 2 3 4 1 2 3 4
   * c: 1 2 3 4
   * a: 1 2 3 5
   * </pre>
   * Expected:
   * <pre>
   * a-b-b-b-c (1 2 3)
   * </pre>
   */
  @Test
  public void example2() {
    CloneIndex index = createIndex(
      newBlocks("b", "1 2 3 4 1 2 3 4 1 2 3 4"),
      newBlocks("c", "1 2 3 4"));
    Block[] fileBlocks = newBlocks("a", "1 2 3 5");
    List<CloneGroup> result = detect(index, fileBlocks);

    print(result);
    assertThat(result.size(), is(1));

    assertThat(result, hasCloneGroup(3,
      newClonePart("a", 0, 3),
      newClonePart("b", 0, 3),
      newClonePart("b", 4, 3),
      newClonePart("b", 8, 3),
      newClonePart("c", 0, 3)));
  }

  /**
   * Test for problem, which was described in original paper - same clone would be reported twice.
   * Given:
   * <pre>
   * a: 1 2 3 1 2 4
   * </pre>
   * Expected only one clone:
   * <pre>
   * a-a (1 2)
   * </pre>
   */
  @Test
  public void clonesInFileItself() {
    CloneIndex index = createIndex();
    Block[] fileBlocks = newBlocks("a", "1 2 3 1 2 4");
    List<CloneGroup> result = detect(index, fileBlocks);

    print(result);
    assertThat(result.size(), is(1));

    assertThat(result, hasCloneGroup(2,
      newClonePart("a", 0, 2),
      newClonePart("a", 3, 2)));
  }

  /**
   * Given:
   * <pre>
   * b: 1 2 1 2
   * a: 1 2 1
   * </pre>
   * Expected:
   * <pre>
   * a-b-b (1 2)
   * a-b (1 2 1)
   * </pre>
   * "a-a-b-b (1)" should not be reported, because fully covered by "a-b (1 2 1)"
   */
  @Test
  public void covered() {
    CloneIndex index = createIndex(
      newBlocks("b", "1 2 1 2"));
    Block[] fileBlocks = newBlocks("a", "1 2 1");
    List<CloneGroup> result = detect(index, fileBlocks);

    print(result);
    assertThat(result.size(), is(2));

    assertThat(result, hasCloneGroup(3,
      newClonePart("a", 0, 3),
      newClonePart("b", 0, 3)));

    assertThat(result, hasCloneGroup(2,
      newClonePart("a", 0, 2),
      newClonePart("b", 0, 2),
      newClonePart("b", 2, 2)));
  }

  /**
   * Given:
   * <pre>
   * b: 1 2 1 2 1 2 1
   * a: 1 2 1 2 1 2
   * </pre>
   * Expected:
   * <pre>
   * a-b-b (1 2 1 2 1) - note that there is overlapping among parts for "b"
   * a-b (1 2 1 2 1 2)
   * </pre>
   */
  @Test
  public void problemWithNestedCloneGroups() {
    CloneIndex index = createIndex(
      newBlocks("b", "1 2 1 2 1 2 1"));
    Block[] fileBlocks = newBlocks("a", "1 2 1 2 1 2");
    List<CloneGroup> result = detect(index, fileBlocks);

    print(result);
    assertThat(result.size(), is(2));

    assertThat(result, hasCloneGroup(6,
      newClonePart("a", 0, 6),
      newClonePart("b", 0, 6)));

    assertThat(result, hasCloneGroup(5,
      newClonePart("a", 0, 5),
      newClonePart("b", 0, 5),
      newClonePart("b", 2, 5)));
  }

  /**
   * Given:
   * <pre>
   * a: 1 2 3
   * b: 1 2 4
   * a: 1 2 5
   * </pre>
   * Expected:
   * <pre>
   * a-b (1 2) - instead of "a-a-b", which will be the case if file from index not ignored
   * </pre>
   */
  @Test
  public void fileAlreadyInIndex() {
    CloneIndex index = createIndex(
      newBlocks("a", "1 2 3"),
      newBlocks("b", "1 2 4"));
    // Note about blocks with hashes "3", "4" and "5": those blocks here in order to not face another problem - with EOF (see separate test)
    Block[] fileBlocks = newBlocks("a", "1 2 5");
    List<CloneGroup> result = detect(index, fileBlocks);

    print(result);
    assertThat(result.size(), is(1));

    assertThat(result, hasCloneGroup(2,
      newClonePart("a", 0, 2),
      newClonePart("b", 0, 2)));
  }

  /**
   * Given: file with repeated hashes
   * Expected: only one query of index for each unique hash
   */
  @Test
  public void only_one_query_of_index_for_each_unique_hash() {
    CloneIndex index = spy(createIndex());
    Block[] fileBlocks = newBlocks("a", "1 2 1 2");
    detect(index, fileBlocks);

    verify(index).getBySequenceHash(new ByteArray("01"));
    verify(index).getBySequenceHash(new ByteArray("02"));
    verifyNoMoreInteractions(index);
  }

  /**
   * Given: empty list of blocks for file
   * Expected: {@link Collections#EMPTY_LIST}
   */
  @Test
  public void shouldReturnEmptyListWhenNoBlocksForFile() {
    List<CloneGroup> result = detect(null, new Block[0]);
    assertThat(result, sameInstance(Collections.EMPTY_LIST));
  }

  /**
   * Given:
   * <pre>
   * b: 1 2 3 4
   * a: 1 2 3
   * </pre>
   * Expected clone which ends at the end of file "a":
   * <pre>
   * a-b (1 2 3)
   * </pre>
   */
  @Test
  public void problemWithEndOfFile() {
    CloneIndex cloneIndex = createIndex(
      newBlocks("b", "1 2 3 4"));
    Block[] fileBlocks = newBlocks("a", "1 2 3");
    List<CloneGroup> clones = detect(cloneIndex, fileBlocks);

    print(clones);
    assertThat(clones.size(), is(1));

    assertThat(clones, hasCloneGroup(3,
      newClonePart("a", 0, 3),
      newClonePart("b", 0, 3)));
  }

  /**
   * Given file with two lines, containing following statements:
   * <pre>
   * 0: A,B,A,B
   * 1: A,B,A
   * </pre>
   * with block size 5 each block will span both lines, and hashes will be:
   * <pre>
   * A,B,A,B,A=1
   * B,A,B,A,B=2
   * A,B,A,B,A=1
   * </pre>
   * Expected: one clone with two parts, which contain exactly the same lines
   */
  @Test
  public void same_lines_but_different_indexes() {
    CloneIndex cloneIndex = createIndex();
    Block.Builder block = Block.builder()
      .setResourceId("a")
      .setLines(0, 1);
    Block[] fileBlocks = new Block[] {
      block.setBlockHash(new ByteArray("1".getBytes())).setIndexInFile(0).build(),
      block.setBlockHash(new ByteArray("2".getBytes())).setIndexInFile(1).build(),
      block.setBlockHash(new ByteArray("1".getBytes())).setIndexInFile(2).build()
    };
    List<CloneGroup> clones = detect(cloneIndex, fileBlocks);

    print(clones);
    assertThat(clones.size(), is(1));
    Iterator<CloneGroup> clonesIterator = clones.iterator();

    CloneGroup clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(1));
    assertThat(clone.getCloneParts().size(), is(2));
    assertThat(clone.getOriginPart(), is(new ClonePart("a", 0, 0, 1)));
    assertThat(clone.getCloneParts(), hasItem(new ClonePart("a", 0, 0, 1)));
    assertThat(clone.getCloneParts(), hasItem(new ClonePart("a", 2, 0, 1)));
  }

  protected static void print(List<CloneGroup> clones) {
    for (CloneGroup clone : clones) {
      System.out.println(clone);
    }
    System.out.println();
  }

  protected static Block[] newBlocks(String resourceId, String hashes) {
    List<Block> result = new ArrayList<>();
    int indexInFile = 0;
    for (int i = 0; i < hashes.length(); i += 2) {
      Block block = newBlock(resourceId, new ByteArray("0" + hashes.charAt(i)), indexInFile);
      result.add(block);
      indexInFile++;
    }
    return result.toArray(new Block[result.size()]);
  }

  protected static CloneIndex createIndex(Block[]... blocks) {
    CloneIndex cloneIndex = new MemoryCloneIndex();
    for (Block[] b : blocks) {
      for (Block block : b) {
        cloneIndex.insert(block);
      }
    }
    return cloneIndex;
  }

}
