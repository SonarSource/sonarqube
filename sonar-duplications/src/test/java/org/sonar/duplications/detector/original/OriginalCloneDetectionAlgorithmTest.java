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
package org.sonar.duplications.detector.original;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.Arrays;
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

import com.google.common.collect.Lists;

public class OriginalCloneDetectionAlgorithmTest {

  @Rule
  public TestNamePrinter name = new TestNamePrinter();

  private static int LINES_PER_BLOCK = 5;

  /**
   * To simplify testing we assume that each block starts from a new line and contains {@link #LINES_PER_BLOCK} lines,
   * so we can simply use index and hash.
   */
  private static Block newBlock(String resourceId, ByteArray hash, int index) {
    return new Block(resourceId, hash, index, index, index + LINES_PER_BLOCK);
  }

  private static ClonePart newClonePart(String resourceId, int unitStart, int cloneUnitLength) {
    return new ClonePart(resourceId, unitStart, unitStart, unitStart + cloneUnitLength + LINES_PER_BLOCK - 1);
  }

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
    CloneIndex cloneIndex = createIndex(
        blocksForResource("y").withHashes("2", "3", "4", "5"),
        blocksForResource("z").withHashes("3", "4"));
    List<Block> fileBlocks = blocksForResource("x").withHashes("1", "2", "3", "4", "5", "6");
    List<CloneGroup> clones = OriginalCloneDetectionAlgorithm.detect(cloneIndex, fileBlocks);
    print(clones);
    assertThat(clones.size(), is(2));
    Iterator<CloneGroup> clonesIterator = clones.iterator();

    CloneGroup clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(4));
    assertThat(clone.getCloneParts().size(), is(2));
    assertThat(clone.getOriginPart(), is(newClonePart("x", 1, 4)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("x", 1, 4)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("y", 0, 4)));

    clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(2));
    assertThat(clone.getCloneParts().size(), is(3));
    assertThat(clone.getOriginPart(), is(newClonePart("x", 2, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("x", 2, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("y", 1, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("z", 0, 2)));
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
        blocksForResource("a").withHashes("2", "3", "4", "5"),
        blocksForResource("b").withHashes("3", "4"));
    List<Block> fileBlocks = blocksForResource("c").withHashes("1", "2", "3", "4", "5", "6");
    List<CloneGroup> clones = OriginalCloneDetectionAlgorithm.detect(cloneIndex, fileBlocks);
    print(clones);
    assertThat(clones.size(), is(2));
    Iterator<CloneGroup> clonesIterator = clones.iterator();

    CloneGroup clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(4));
    assertThat(clone.getCloneParts().size(), is(2));
    assertThat(clone.getOriginPart(), is(newClonePart("c", 1, 4)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("c", 1, 4)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 0, 4)));

    clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(2));
    assertThat(clone.getCloneParts().size(), is(3));
    assertThat(clone.getOriginPart(), is(newClonePart("c", 2, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("c", 2, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 1, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 0, 2)));
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
    CloneIndex cloneIndex = createIndex(
        blocksForResource("b").withHashes("3", "4", "5", "6"),
        blocksForResource("c").withHashes("5", "6", "7"));
    List<Block> fileBlocks =
        blocksForResource("a").withHashes("1", "2", "3", "4", "5", "6", "7", "8", "9");
    List<CloneGroup> clones = OriginalCloneDetectionAlgorithm.detect(cloneIndex, fileBlocks);
    print(clones);
    assertThat(clones.size(), is(3));
    Iterator<CloneGroup> clonesIterator = clones.iterator();

    CloneGroup clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(4));
    assertThat(clone.getCloneParts().size(), is(2));
    assertThat(clone.getOriginPart(), is(newClonePart("a", 2, 4)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 2, 4)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 0, 4)));

    clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(2));
    assertThat(clone.getCloneParts().size(), is(3));
    assertThat(clone.getOriginPart(), is(newClonePart("a", 4, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 4, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 2, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("c", 0, 2)));

    clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(3));
    assertThat(clone.getCloneParts().size(), is(2));
    assertThat(clone.getOriginPart(), is(newClonePart("a", 4, 3)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 4, 3)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("c", 0, 3)));
  }

  /**
   * Given:
   * <pre>
   * b: 1 2 3 4 1 2 3 4 1 2 3 4
   * c: 1 2 3 4
   * a: 1 2 3 4 5
   * </pre>
   * Expected:
   * <pre>
   * a-b-b-b-c (1 2 3 4)
   * </pre>
   */
  @Test
  public void example2() {
    CloneIndex cloneIndex = createIndex(
        blocksForResource("b").withHashes("1", "2", "3", "4", "1", "2", "3", "4", "1", "2", "3", "4"),
        blocksForResource("c").withHashes("1", "2", "3", "4"));
    List<Block> fileBlocks =
        blocksForResource("a").withHashes("1", "2", "3", "5");
    List<CloneGroup> clones = OriginalCloneDetectionAlgorithm.detect(cloneIndex, fileBlocks);
    print(clones);
    assertThat(clones.size(), is(1));
    Iterator<CloneGroup> clonesIterator = clones.iterator();

    CloneGroup clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(3));
    assertThat(clone.getCloneParts().size(), is(5));
    assertThat(clone.getOriginPart(), is(newClonePart("a", 0, 3)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 0, 3)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 0, 3)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 4, 3)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 8, 3)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("c", 0, 3)));
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
        blocksForResource("b").withHashes("1", "2", "3", "4"));
    List<Block> fileBlocks =
        blocksForResource("a").withHashes("1", "2", "3");
    List<CloneGroup> clones = OriginalCloneDetectionAlgorithm.detect(cloneIndex, fileBlocks);
    print(clones);
    assertThat(clones.size(), is(1));
    Iterator<CloneGroup> clonesIterator = clones.iterator();

    CloneGroup clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(3));
    assertThat(clone.getCloneParts().size(), is(2));
    assertThat(clone.getOriginPart(), is(newClonePart("a", 0, 3)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 0, 3)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 0, 3)));
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
    CloneIndex cloneIndex = createIndex();
    List<Block> fileBlocks =
        blocksForResource("a").withHashes("1", "2", "3", "1", "2", "4");
    List<CloneGroup> clones = OriginalCloneDetectionAlgorithm.detect(cloneIndex, fileBlocks);
    print(clones);

    assertThat(clones.size(), is(1));
    Iterator<CloneGroup> clonesIterator = clones.iterator();

    CloneGroup clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(2));
    assertThat(clone.getCloneParts().size(), is(2));
    assertThat(clone.getOriginPart(), is(newClonePart("a", 0, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 0, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 3, 2)));
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
    CloneIndex cloneIndex = createIndex(
        blocksForResource("b").withHashes("1", "2", "1", "2"));
    List<Block> fileBlocks =
        blocksForResource("a").withHashes("1", "2", "1");
    List<CloneGroup> clones = OriginalCloneDetectionAlgorithm.detect(cloneIndex, fileBlocks);
    print(clones);

    assertThat(clones.size(), is(2));
    Iterator<CloneGroup> clonesIterator = clones.iterator();

    CloneGroup clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(2));
    assertThat(clone.getOriginPart(), is(newClonePart("a", 0, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 0, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 0, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 2, 2)));

    clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(3));
    assertThat(clone.getOriginPart(), is(newClonePart("a", 0, 3)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 0, 3)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 0, 3)));
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
    CloneIndex cloneIndex = createIndex(
        blocksForResource("b").withHashes("1", "2", "1", "2", "1", "2", "1"));
    List<Block> fileBlocks =
        blocksForResource("a").withHashes("1", "2", "1", "2", "1", "2");
    List<CloneGroup> clones = OriginalCloneDetectionAlgorithm.detect(cloneIndex, fileBlocks);
    print(clones);

    assertThat(clones.size(), is(2));
    Iterator<CloneGroup> clonesIterator = clones.iterator();

    CloneGroup clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(5));
    assertThat(clone.getCloneParts().size(), is(3));
    assertThat(clone.getOriginPart(), is(newClonePart("a", 0, 5)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 0, 5)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 0, 5)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 2, 5)));

    clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(6));
    assertThat(clone.getCloneParts().size(), is(2));
    assertThat(clone.getOriginPart(), is(newClonePart("a", 0, 6)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 0, 6)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 0, 6)));
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
    CloneIndex cloneIndex = createIndex(
        blocksForResource("a").withHashes("1", "2", "3"),
        blocksForResource("b").withHashes("1", "2", "4"));
    // Note about blocks with hashes "3", "4" and "5": those blocks here in order to not face another problem - with EOF (see separate test)
    List<Block> fileBlocks =
        blocksForResource("a").withHashes("1", "2", "5");
    List<CloneGroup> clones = OriginalCloneDetectionAlgorithm.detect(cloneIndex, fileBlocks);
    print(clones);

    assertThat(clones.size(), is(1));
    Iterator<CloneGroup> clonesIterator = clones.iterator();

    CloneGroup clone = clonesIterator.next();
    assertThat(clone.getCloneUnitLength(), is(2));
    assertThat(clone.getCloneParts().size(), is(2));
    assertThat(clone.getOriginPart(), is(newClonePart("a", 0, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("a", 0, 2)));
    assertThat(clone.getCloneParts(), hasItem(newClonePart("b", 0, 2)));
  }

  /**
   * Given: file with repeated hashes
   * Expected: only one query of index for each unique hash
   */
  @Test
  public void only_one_query_of_index_for_each_unique_hash() {
    CloneIndex index = spy(createIndex());
    List<Block> fileBlocks =
        blocksForResource("a").withHashes("1", "2", "1", "2");
    OriginalCloneDetectionAlgorithm.detect(index, fileBlocks);

    verify(index).getBySequenceHash(new ByteArray("1".getBytes()));
    verify(index).getBySequenceHash(new ByteArray("2".getBytes()));
    verifyNoMoreInteractions(index);
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
    List<Block> fileBlocks = Arrays.asList(
        new Block("a", new ByteArray("1".getBytes()), 0, 0, 1),
        new Block("a", new ByteArray("2".getBytes()), 1, 0, 1),
        new Block("a", new ByteArray("1".getBytes()), 2, 0, 1));
    List<CloneGroup> clones = OriginalCloneDetectionAlgorithm.detect(cloneIndex, fileBlocks);
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

  /**
   * Given: empty list of blocks for file
   * Expected: {@link Collections#EMPTY_LIST}
   */
  @Test
  public void shouldReturnEmptyListWhenNoBlocksForFile() {
    List<CloneGroup> result = OriginalCloneDetectionAlgorithm.detect(null, new ArrayList<Block>());
    assertThat(result, sameInstance(Collections.EMPTY_LIST));
  }

  private void print(List<CloneGroup> clones) {
    for (CloneGroup clone : clones) {
      System.out.println(clone);
    }
    System.out.println();
  }

  private static CloneIndex createIndex(List<Block>... blocks) {
    CloneIndex cloneIndex = new MemoryCloneIndex();
    for (List<Block> b : blocks) {
      for (Block block : b) {
        cloneIndex.insert(block);
      }
    }
    return cloneIndex;
  }

  private static BlocksBuilder blocksForResource(String resourceId) {
    return new BlocksBuilder(resourceId);
  }

  private static class BlocksBuilder {
    String resourceId;

    public BlocksBuilder(String resourceId) {
      this.resourceId = resourceId;
    }

    List<Block> withHashes(String... hashes) {
      ByteArray[] arrays = new ByteArray[hashes.length];
      for (int i = 0; i < hashes.length; i++) {
        arrays[i] = new ByteArray(hashes[i].getBytes());
      }
      return withHashes(arrays);
    }

    List<Block> withHashes(ByteArray... hashes) {
      List<Block> result = Lists.newArrayList();
      int index = 0;
      for (ByteArray hash : hashes) {
        result.add(newBlock(resourceId, hash, index));
        index++;
      }
      return result;
    }
  }

}
