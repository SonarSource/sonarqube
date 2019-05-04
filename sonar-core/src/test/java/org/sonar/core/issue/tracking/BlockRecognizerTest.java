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
package org.sonar.core.issue.tracking;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockRecognizerTest {

  @Test
  public void lengthOfMaximalBlock() {
    /**
     * - line 4 of first sequence is "d"
     * - line 4 of second sequence is "d"
     * - in each sequence, the 3 lines before and the line after are similar -> block size is 5
     */
    assertThat(compute(seq("abcde"), seq("abcde"), 4, 4)).isEqualTo(5);

    assertThat(compute(seq("abcde"), seq("abcd"), 4, 4)).isEqualTo(4);
    assertThat(compute(seq("bcde"), seq("abcde"), 4, 4)).isEqualTo(0);
    assertThat(compute(seq("bcde"), seq("abcde"), 3, 4)).isEqualTo(4);
  }

  @Test
  public void isOverLimit() {
    assertThat(BlockRecognizer.isOverLimit(20, 40)).isFalse();
    assertThat(BlockRecognizer.isOverLimit(3, 100_000)).isTrue();

    // multiplication of these two ints is higher than Integer.MAX_VALUE
    assertThat(BlockRecognizer.isOverLimit(50_000, 60_000)).isTrue();
  }

  private int compute(LineHashSequence seqA, LineHashSequence seqB, int ai, int bi) {
    return BlockRecognizer.lengthOfMaximalBlock(seqA, ai, seqB, bi);
  }

  private static LineHashSequence seq(String text) {
    List<String> hashes = new ArrayList<>();
    for (int i = 0; i < text.length(); i++) {
      hashes.add("" + text.charAt(i));
    }
    return new LineHashSequence(hashes);
  }

}
