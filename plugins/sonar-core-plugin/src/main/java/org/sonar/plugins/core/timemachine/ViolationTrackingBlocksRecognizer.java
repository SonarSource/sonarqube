/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.core.timemachine;

import org.sonar.plugins.core.timemachine.tracking.HashedSequence;
import org.sonar.plugins.core.timemachine.tracking.HashedSequenceComparator;
import org.sonar.plugins.core.timemachine.tracking.StringText;
import org.sonar.plugins.core.timemachine.tracking.StringTextComparator;

public class ViolationTrackingBlocksRecognizer {

  private final HashedSequence<StringText> a;
  private final HashedSequence<StringText> b;
  private final HashedSequenceComparator<StringText> cmp;

  public ViolationTrackingBlocksRecognizer(String referenceSource, String source) {
    this(new StringText(referenceSource), new StringText(source), StringTextComparator.IGNORE_WHITESPACE);
  }

  private ViolationTrackingBlocksRecognizer(StringText a, StringText b, StringTextComparator cmp) {
    this.a = wrap(a, cmp);
    this.b = wrap(b, cmp);
    this.cmp = new HashedSequenceComparator<StringText>(cmp);
  }

  private static HashedSequence<StringText> wrap(StringText seq, StringTextComparator cmp) {
    int size = seq.length();
    int[] hashes = new int[size];
    for (int i = 0; i < size; i++) {
      hashes[i] = cmp.hash(seq, i);
    }
    return new HashedSequence<StringText>(seq, hashes);
  }

  public int computeLengthOfMaximalBlock(int startA, int startB) {
    if (!cmp.equals(a, startA, b, startB)) {
      return 0;
    }
    int length = 0;
    int ai = startA;
    int bi = startB;
    while (ai < a.length() && bi < b.length() && cmp.equals(a, ai, b, bi)) {
      ai++;
      bi++;
      length++;
    }
    ai = startA;
    bi = startB;
    while (ai >= 0 && bi >= 0 && cmp.equals(a, ai, b, bi)) {
      ai--;
      bi--;
      length++;
    }
    // Note that position (startA, startB) was counted twice
    return length - 1;
  }

}
