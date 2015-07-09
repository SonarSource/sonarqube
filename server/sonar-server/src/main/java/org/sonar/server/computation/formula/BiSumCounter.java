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

package org.sonar.server.computation.formula;

import com.google.common.base.Optional;

/**
 * This counter can be used to aggregate measure from two metrics
 */
public class BiSumCounter implements Counter<BiSumCounter> {

  private final SumCounter sumCounter1;
  private final SumCounter sumCounter2;

  public BiSumCounter(String metric1, String metric2) {
    this.sumCounter1 = new SumCounter(metric1);
    this.sumCounter2 = new SumCounter(metric2);
  }

  @Override
  public void aggregate(BiSumCounter counter) {
    sumCounter1.aggregate(counter.sumCounter1);
    sumCounter2.aggregate(counter.sumCounter2);
  }

  @Override
  public void aggregate(FileAggregateContext context) {
    sumCounter1.aggregate(context);
    sumCounter2.aggregate(context);
  }

  public Optional<Integer> getValue1() {
    return sumCounter1.getValue();
  }

  public Optional<Integer> getValue2() {
    return sumCounter2.getValue();
  }
}
