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
public class RollingHashSequence<S extends Sequence> implements Sequence {

  final S base;
  final int[] hashes;

  public static <S extends Sequence> RollingHashSequence<S> wrap(S base, SequenceComparator<S> cmp, int lines) {
    int size = base.length();
    int[] hashes = new int[size];

    RollingHashCalculator hashCalulator = new RollingHashCalculator(lines * 2 + 1);
    for (int i = 0; i <= Math.min(size - 1, lines); i++) {
      hashCalulator.add(cmp.hash(base, i));
    }
    for (int i = 0; i < size; i++) {
      hashes[i] = hashCalulator.getHash();
      if (i - lines >= 0) {
        hashCalulator.remove(cmp.hash(base, i - lines));
      }
      if (i + lines + 1 < size) {
        hashCalulator.add(cmp.hash(base, i + lines + 1));
      } else {
        hashCalulator.add(0);
      }
    }

    return new RollingHashSequence<S>(base, hashes);
  }

  private RollingHashSequence(S base, int[] hashes) {
    this.base = base;
    this.hashes = hashes;
  }

  public int length() {
    return base.length();
  }

  private static class RollingHashCalculator {

    private static final int PRIME_BASE = 31;

    private final int power;
    private int hash;

    public RollingHashCalculator(int size) {
      int pow = 1;
      for (int i = 0; i < size - 1; i++) {
        pow = pow * PRIME_BASE;
      }
      this.power = pow;
    }

    public void add(int value) {
      hash = hash * PRIME_BASE + value;
    }

    public void remove(int value) {
      hash = hash - power * value;
    }

    public int getHash() {
      return hash;
    }

  }

}
