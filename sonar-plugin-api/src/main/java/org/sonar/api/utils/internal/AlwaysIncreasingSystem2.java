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
package org.sonar.api.utils.internal;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.sonar.api.utils.System2;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A subclass of {@link System2} which implementation of {@link System2#now()} always return a bigger value than the
 * previous returned value.
 * <p>
 * This class is intended to be used in Unit tests.
 * </p>
 */
public class AlwaysIncreasingSystem2 extends System2 {
  private final AtomicLong now;
  private final long increment;

  private AlwaysIncreasingSystem2(Supplier<Long> initialValueSupplier, long increment) {
    checkArgument(increment > 0, "increment must be > 0");
    long initialValue = initialValueSupplier.get();
    checkArgument(initialValue >= 0, "Initial value must be >= 0");
    this.now = new AtomicLong(initialValue);
    this.increment = increment;
  }

  public AlwaysIncreasingSystem2(long increment) {
    this(AlwaysIncreasingSystem2::randomInitialValue, increment);
  }

  public AlwaysIncreasingSystem2(long initialValue, int increment) {
    this(() -> initialValue, increment);
  }

  /**
   * Values returned by {@link #now()} will start with a random value and increment by 100.
   */
  public AlwaysIncreasingSystem2() {
    this(AlwaysIncreasingSystem2::randomInitialValue, 100);
  }

  @Override
  public long now() {
    return now.getAndAdd(increment);
  }

  private static long randomInitialValue() {
    return (long) Math.abs(new Random().nextInt(2_000_000));
  }
}
