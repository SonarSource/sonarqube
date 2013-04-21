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

/**
 * Equivalence function for a {@link Sequence}.
 */
public interface SequenceComparator<S extends Sequence> {

  /**
   * Compare two items to determine if they are equivalent.
   */
  boolean equals(S a, int ai, S b, int bi);

  /**
   * Get a hash value for an item in a sequence.
   *
   * If two items are equal according to this comparator's
   * {@link #equals(Sequence, int, Sequence, int)} method,
   * then this hash method must produce the same integer result for both items.
   * However not required to have different hash values for different items.
   */
  int hash(S seq, int i);

}
