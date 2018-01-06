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
package org.sonar.scanner.issue.tracking;

/**
 * Compute hashes of block around each line
 */
public class RollingFileHashes {

  final int[] rollingHashes;
  
  private RollingFileHashes(int[] hashes) {
    this.rollingHashes = hashes;
  }

  public static RollingFileHashes create(FileHashes hashes, int halfBlockSize) {
    int size = hashes.length();
    int[] rollingHashes = new int[size];

    RollingHashCalculator hashCalulator = new RollingHashCalculator(halfBlockSize * 2 + 1);
    for (int i = 1; i <= Math.min(size, halfBlockSize + 1); i++) {
      hashCalulator.add(hashes.getHash(i).hashCode());
    }
    for (int i = 1; i <= size; i++) {
      rollingHashes[i - 1] = hashCalulator.getHash();
      if (i - halfBlockSize > 0) {
        hashCalulator.remove(hashes.getHash(i - halfBlockSize).hashCode());
      }
      if (i + 1 + halfBlockSize <= size) {
        hashCalulator.add(hashes.getHash(i + 1 + halfBlockSize).hashCode());
      } else {
        hashCalulator.add(0);
      }
    }

    return new RollingFileHashes(rollingHashes);
  }

  public int getHash(int line) {
    return rollingHashes[line - 1];
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
