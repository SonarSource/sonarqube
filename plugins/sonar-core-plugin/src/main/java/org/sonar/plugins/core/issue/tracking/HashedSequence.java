/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.core.issue.tracking;

import java.util.Collection;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Wraps a {@link Sequence} to assign hash codes to elements.
 */
public final class HashedSequence<S extends Sequence> implements Sequence {

  final S base;
  final int[] hashes;
  final Multimap<Integer, Integer> linesByHash;

  public static <S extends Sequence> HashedSequence<S> wrap(S base, SequenceComparator<S> cmp) {
    int size = base.length();
    int[] hashes = new int[size];
    Multimap<Integer, Integer> linesByHash = LinkedHashMultimap.create();
    for (int i = 0; i < size; i++) {
      hashes[i] = cmp.hash(base, i);
      // indices in array are shifted one line before
      linesByHash.put(hashes[i], i + 1);
    }
    return new HashedSequence<S>(base, hashes, linesByHash);
  }

  private HashedSequence(S base, int[] hashes, Multimap<Integer, Integer> linesByHash) {
    this.base = base;
    this.hashes = hashes;
    this.linesByHash = linesByHash;
  }

  public int length() {
    return base.length();
  }

  public Collection<Integer> getLinesForHash(Integer hash) {
    return linesByHash.get(hash);
  }

  public Integer getHash(Integer line) {
    // indices in array are shifted one line before
    return hashes[line - 1];
  }
}
