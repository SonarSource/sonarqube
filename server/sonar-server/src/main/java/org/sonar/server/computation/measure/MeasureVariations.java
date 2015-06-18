/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.computation.measure;

import com.google.common.base.Objects;
import java.util.Arrays;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

@Immutable
public final class MeasureVariations {
  private final Double[] variations = new Double[5];

  public MeasureVariations(Double... variations) {
    checkArgument(variations.length <= 5, "There can not be more than 5 variations");
    checkArgument(!from(Arrays.asList(variations)).filter(notNull()).isEmpty(), "There must be at least one variation");
    System.arraycopy(variations, 0, this.variations, 0, variations.length);
  }

  public boolean hasVariation1() {
    return hasVariation(1);
  }

  public boolean hasVariation2() {
    return hasVariation(2);
  }

  public boolean hasVariation3() {
    return hasVariation(3);
  }

  public boolean hasVariation4() {
    return hasVariation(4);
  }

  public boolean hasVariation5() {
    return hasVariation(5);
  }

  private void checkHasVariation(int i) {
    if (!hasVariation(i)) {
      throw new IllegalStateException(String.format("Variation %s has not been set", i));
    }
  }

  public boolean hasVariation(int i) {
    return variations[i - 1] != null;
  }

  public double getVariation1() {
    return getVariation(1);
  }

  public double getVariation2() {
    return getVariation(2);
  }

  public double getVariation3() {
    return getVariation(3);
  }

  public double getVariation4() {
    return getVariation(4);
  }

  public double getVariation5() {
    return getVariation(5);
  }

  public double getVariation(int i) {
    checkHasVariation(i);
    return variations[i-1];
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("1", variations[0])
      .add("2", variations[1])
      .add("3", variations[2])
      .add("4", variations[3])
      .add("5", variations[4])
      .toString();
  }
}
