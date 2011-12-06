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

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.sonar.duplications.detector.CloneGroupMatcher.hasCloneGroup;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.detector.DetectorTestCase;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.CloneIndex;

public class SuffixTreeCloneDetectionAlgorithmTest extends DetectorTestCase {

  /**
   * Given: file without duplications
   * Expected: {@link Collections#EMPTY_LIST} (no need to construct suffix-tree)
   */
  @Test
  public void noDuplications() {
    CloneIndex index = createIndex();
    List<Block> fileBlocks = newBlocks("a", "1 2 3");
    List<CloneGroup> result = detect(index, fileBlocks);
    assertThat(result, sameInstance(Collections.EMPTY_LIST));
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
   */
  @Test
  public void myTest() {
    CloneIndex index = createIndex();
    List<Block> fileBlocks = newBlocks("x", "a 2 b 2 c 2 2 2");
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
   * Given:
   * <pre>
   * x: a 2 3 b 2 3 c 2 3 d 2 3 2 3 2 3
   * </pre>
   * Expected:
   * <pre>
   * x-x (2 3 2 3)
   * x-x-x-x-x-x (2 3)
   * <pre>
   */
  @Test
  public void myTest2() {
    CloneIndex index = createIndex();
    List<Block> fileBlocks = newBlocks("x", "a 2 3 b 2 3 c 2 3 d 2 3 2 3 2 3");
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

  @Override
  protected List<CloneGroup> detect(CloneIndex index, List<Block> fileBlocks) {
    return SuffixTreeCloneDetectionAlgorithm.detect(index, fileBlocks);
  }

}
