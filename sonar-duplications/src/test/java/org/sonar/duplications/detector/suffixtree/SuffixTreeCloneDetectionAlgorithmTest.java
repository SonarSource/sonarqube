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
package org.sonar.duplications.detector.suffixtree;

import org.junit.Test;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.duplications.detector.DetectorTestCase;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.CloneIndex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.sonar.duplications.detector.CloneGroupMatcher.hasCloneGroup;

public class SuffixTreeCloneDetectionAlgorithmTest extends DetectorTestCase {

  /**
   * Given: file without duplications
   * Expected: {@link Collections#EMPTY_LIST} (no need to construct suffix-tree)
   */
  @Test
  public void noDuplications() {
    CloneIndex index = createIndex();
    Block[] fileBlocks = newBlocks("a", "1 2 3");
    List<CloneGroup> result = detect(index, fileBlocks);
    assertThat(result, sameInstance(Collections.EMPTY_LIST));
  }

  /**
   * See SONAR-3060
   * <p>
   * In case when file contains a lot of duplicated blocks suffix-tree works better than original algorithm,
   * which works more than 5 minutes for this example.
   * </p><p>
   * However should be noted that current implementation with suffix-tree also is not optimal,
   * even if it works for this example couple of seconds,
   * because duplications should be filtered in order to remove fully-covered.
   * But such cases nearly never appear in real-world, so current implementation is acceptable for the moment.
   * </p>
   */
  @Test
  public void huge() {
    CloneIndex index = createIndex();
    Block[] fileBlocks = new Block[5000];
    for (int i = 0; i < 5000; i++) {
      fileBlocks[i] = newBlock("x", new ByteArray("01"), i);
    }
    List<CloneGroup> result = detect(index, fileBlocks);

    assertEquals(1, result.size());
  }

  /**
   * Given:
   * <pre>
   * x: a 2 b 2 c 2 2 2
   * </pre>
   * Expected:
   * <pre>
   * x-x (2 2)
   * x-x-x-x-x (2)
   * <pre>
   * TODO Godin: however would be better to receive only (2)
   */
  @Test
  public void myTest() {
    CloneIndex index = createIndex();
    Block[] fileBlocks = newBlocks("x", "a 2 b 2 c 2 2 2");
    List<CloneGroup> result = detect(index, fileBlocks);

    print(result);
    assertEquals(2, result.size());

    assertThat(result, hasCloneGroup(2,
        newClonePart("x", 5, 2),
        newClonePart("x", 6, 2)));

    assertThat(result, hasCloneGroup(1,
        newClonePart("x", 1, 1),
        newClonePart("x", 3, 1),
        newClonePart("x", 5, 1),
        newClonePart("x", 6, 1),
        newClonePart("x", 7, 1)));
  }

  /**
   * This test and associated with it suffix-tree demonstrates that without filtering in {@link DuplicationsCollector#endOfGroup()}
   * possible to construct {@link CloneGroup}, which is fully covered by another {@link CloneGroup}.
   * 
   * Given:
   * <pre>
   * x: a 2 3 b 2 3 c 2 3 d 2 3 2 3 2 3
   * </pre>
   * Expected:
   * <pre>
   * x-x (2 3 2 3)
   * x-x-x-x-x-x (2 3)
   * <pre>
   * TODO Godin: however would be better to receive only (2 3)
   */
  @Test
  public void myTest2() {
    CloneIndex index = createIndex();
    Block[] fileBlocks = newBlocks("x", "a 2 3 b 2 3 c 2 3 d 2 3 2 3 2 3");
    List<CloneGroup> result = detect(index, fileBlocks);

    print(result);
    assertEquals(2, result.size());

    assertThat(result, hasCloneGroup(4,
        newClonePart("x", 10, 4),
        newClonePart("x", 12, 4)));

    assertThat(result, hasCloneGroup(2,
        newClonePart("x", 1, 2),
        newClonePart("x", 4, 2),
        newClonePart("x", 7, 2),
        newClonePart("x", 10, 2),
        newClonePart("x", 12, 2),
        newClonePart("x", 14, 2)));
  }

  /**
   * This test and associated with it suffix-tree demonstrates that without check of origin in {@link Search}
   * possible to construct {@link CloneGroup} with a wrong origin.
   * 
   * Given:
   * <pre>
   * a: 1 2 3 4
   * b: 4 3 2
   * c: 4 3 1
   * </pre>
   * Expected:
   * <pre>
   * a-c (1)
   * a-b (2)
   * a-b-c (3)
   * a-b-c (4)
   * <pre>
   */
  @Test
  public void myTest3() {
    CloneIndex index = createIndex(
        newBlocks("b", "4 3 2"),
        newBlocks("c", "4 3 1")
        );
    Block[] fileBlocks = newBlocks("a", "1 2 3 4");
    List<CloneGroup> result = detect(index, fileBlocks);

    print(result);
    assertEquals(4, result.size());

    assertThat(result, hasCloneGroup(1,
        newClonePart("a", 0, 1),
        newClonePart("c", 2, 1)));

    assertThat(result, hasCloneGroup(1,
        newClonePart("a", 1, 1),
        newClonePart("b", 2, 1)));

    assertThat(result, hasCloneGroup(1,
        newClonePart("a", 2, 1),
        newClonePart("b", 1, 1),
        newClonePart("c", 1, 1)));

    assertThat(result, hasCloneGroup(1,
        newClonePart("a", 3, 1),
        newClonePart("b", 0, 1),
        newClonePart("c", 0, 1)));
  }

  @Override
  protected List<CloneGroup> detect(CloneIndex index, Block[] fileBlocks) {
    return SuffixTreeCloneDetectionAlgorithm.detect(index, Arrays.asList(fileBlocks));
  }

}
