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
package org.sonar.diff;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.util.List;

/**
 * Diff algorithm, based on
 * "The String-to-String Correction Problem with Block Moves", by Waller F. Tichy.
 */
public class DiffAlgorithm {

  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println("2 arguments required");
      System.exit(1);
    }
    try {
      Text a = new Text(Files.toByteArray(new File(args[0])));
      Text b = new Text(Files.toByteArray(new File(args[1])));
      List<Edit> r = new DiffAlgorithm().diff(a, b, TextComparator.IGNORE_WHITESPACE);
      for (Edit edit : r) {
        System.out.println(edit);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public List<Edit> diff(Text a, Text b, TextComparator cmp) {
    return diff(wrap(a, cmp), wrap(b, cmp), cmp);
  }

  private static HashedSequence<Text> wrap(Text seq, TextComparator cmp) {
    int size = seq.length();
    int[] hashes = new int[size];
    for (int i = 0; i < size; i++) {
      hashes[i] = cmp.hash(seq, i);
    }
    return new HashedSequence<Text>(seq, hashes);
  }

  private List<Edit> diff(HashedSequence<Text> s, HashedSequence<Text> t, TextComparator cmp) {
    HashedSequenceComparator<Text> comparator = new HashedSequenceComparator<Text>(cmp);
    Edit lastEdit = null;
    List<Edit> r = Lists.newArrayList();

    int m = s.length();
    int n = t.length();

    int q = 0;
    while (q < n) {
      // find p and l such that (p,q,l) is a maximal block move
      int l = 0;
      int p = 0;
      int pCur = 0;

      while ((pCur + l < m) && (q + l < n)) {
        int lCur = 0;
        while ((pCur + lCur < m) && (q + lCur < n)
          && (comparator.equals(s, pCur + lCur, t, q + lCur))) {
          lCur++;
        }
        if (lCur > l) {
          l = lCur;
          p = pCur;
        }
        pCur++;
      }

      if (l > 0) {
        Edit edit = new Edit(Edit.Type.MOVE, p, p + l - 1, q, q + l - 1);
        r.add(edit);
        lastEdit = edit;
        q += l;
      } else {
        if (lastEdit == null || lastEdit.getType() != Edit.Type.INSERT) {
          Edit edit = new Edit(Edit.Type.INSERT, -1, -1, q, q);
          r.add(edit);
          lastEdit = edit;
        } else {
          lastEdit.endB++;
        }
        q++;
      }
    }

    return r;
  }

}
