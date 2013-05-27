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

/**
 * Wraps a {@link Sequence} to assign hash codes to elements.
 */
public final class HashedSequence<S extends Sequence> implements Sequence {

  final S base;
  final int[] hashes;

  public static <S extends Sequence> HashedSequence<S> wrap(S base, SequenceComparator<S> cmp) {
    int size = base.length();
    int[] hashes = new int[size];
    for (int i = 0; i < size; i++) {
      hashes[i] = cmp.hash(base, i);
    }
    return new HashedSequence<S>(base, hashes);
  }

  private HashedSequence(S base, int[] hashes) {
    this.base = base;
    this.hashes = hashes;
  }

  public int length() {
    return base.length();
  }

}
