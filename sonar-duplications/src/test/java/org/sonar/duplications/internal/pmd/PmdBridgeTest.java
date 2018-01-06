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
package org.sonar.duplications.internal.pmd;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import net.sourceforge.pmd.cpd.JavaTokenizer;
import org.junit.Before;
import org.junit.Test;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.detector.suffixtree.SuffixTreeCloneDetectionAlgorithm;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.CloneIndex;
import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.index.PackedMemoryCloneIndex;

import static org.assertj.core.api.Assertions.assertThat;

public class PmdBridgeTest {

  private CloneIndex index;
  private TokenizerBridge bridge;

  @Before
  public void setUp() {
    index = new PackedMemoryCloneIndex();
    bridge = new TokenizerBridge(new JavaTokenizer(), 10);
  }

  @Test
  public void testDuplicationInSingleFile() throws IOException {
    File file = new File("test-resources/org/sonar/duplications/cpd/CPDTest/CPDFile3.java");
    addToIndex(file);

    List<CloneGroup> duplications = detect(file);
    assertThat(duplications.size()).isEqualTo(1);

    CloneGroup duplication = duplications.get(0);
    assertThat(duplication.getOriginPart().getResourceId()).isEqualTo(file.getAbsolutePath());
    assertThat(duplication.getCloneParts().size()).isEqualTo(2);
    assertThat(duplication.getLengthInUnits()).as("length in tokens").isEqualTo(157);

    ClonePart part = duplication.getCloneParts().get(0);
    assertThat(part.getResourceId()).isEqualTo(file.getAbsolutePath());
    assertThat(part.getStartLine()).isEqualTo(30);
    assertThat(part.getEndLine()).isEqualTo(44);
  }

  @Test
  public void testDuplicationBetweenTwoFiles() throws IOException {
    File file1 = new File("test-resources/org/sonar/duplications/cpd/CPDTest/CPDFile1.java");
    File file2 = new File("test-resources/org/sonar/duplications/cpd/CPDTest/CPDFile2.java");
    addToIndex(file1);
    addToIndex(file2);

    List<CloneGroup> duplications = detect(file1);
    assertThat(duplications.size()).isEqualTo(1);

    CloneGroup duplication = duplications.get(0);
    assertThat(duplication.getOriginPart().getResourceId()).isEqualTo(file1.getAbsolutePath());
    ClonePart part1 = new ClonePart(file1.getAbsolutePath(), 1, 18, 41);
    ClonePart part2 = new ClonePart(file2.getAbsolutePath(), 1, 18, 41);
    assertThat(duplication.getCloneParts()).containsOnly(part1, part2);
    assertThat(duplication.getLengthInUnits()).as("length in tokens").isEqualTo(115);
  }

  private List<CloneGroup> detect(File file) {
    Collection<Block> fileBlocks = index.getByResourceId(file.getAbsolutePath());
    return SuffixTreeCloneDetectionAlgorithm.detect(index, fileBlocks);
  }

  private void addToIndex(File file) throws IOException {
    List<Block> blocks = bridge.chunk(file.getAbsolutePath(), file.getAbsolutePath(), Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8));
    for (Block block : blocks) {
      index.insert(block);
    }
  }

}
