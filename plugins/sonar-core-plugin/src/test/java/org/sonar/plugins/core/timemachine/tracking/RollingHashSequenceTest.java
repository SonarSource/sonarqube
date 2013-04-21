/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.timemachine.tracking;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class RollingHashSequenceTest {

  @Test
  public void test_hash() {
    StringText seq = new StringText("line0 \n line1 \n line2");
    StringTextComparator cmp = StringTextComparator.IGNORE_WHITESPACE;
    RollingHashSequence<StringText> seq2 = RollingHashSequence.wrap(seq, cmp, 1);
    RollingHashSequenceComparator<StringText> cmp2 = new RollingHashSequenceComparator<StringText>(cmp);

    assertThat(seq2.length()).isEqualTo(3);
    assertThat(cmp2.hash(seq2, 0)).isEqualTo(cmp.hash(seq, 0) * 31 + cmp.hash(seq, 1));
    assertThat(cmp2.hash(seq2, 1)).isEqualTo((cmp.hash(seq, 0) * 31 + cmp.hash(seq, 1)) * 31 + cmp.hash(seq, 2));
    assertThat(cmp2.hash(seq2, 2)).isEqualTo((cmp.hash(seq, 1) * 31 + cmp.hash(seq, 2)) * 31);
  }

  @Test
  public void test_equals() {
    StringTextComparator baseCmp = StringTextComparator.IGNORE_WHITESPACE;
    RollingHashSequence<StringText> a = RollingHashSequence.wrap(new StringText("line0 \n line1 \n line2"), baseCmp, 1);
    RollingHashSequence<StringText> b = RollingHashSequence.wrap(new StringText("line0 \n line1 \n line2 \n line3"), baseCmp, 1);
    RollingHashSequenceComparator<StringText> cmp = new RollingHashSequenceComparator<StringText>(baseCmp);

    assertThat(cmp.equals(a, 0, b, 0)).isTrue();
    assertThat(cmp.equals(a, 1, b, 1)).isTrue();
    assertThat(cmp.equals(a, 2, b, 2)).isFalse();
  }

}
