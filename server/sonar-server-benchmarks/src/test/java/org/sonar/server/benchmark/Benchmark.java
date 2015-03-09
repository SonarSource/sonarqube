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
package org.sonar.server.benchmark;

import org.hamcrest.Matchers;
import org.junit.rules.ErrorCollector;
import org.slf4j.LoggerFactory;

public class Benchmark extends ErrorCollector {

  private static final boolean ENABLED = "true".equals(System.getProperty("enableBenchmarkAssertions"));
  public static final double DEFAULT_ERROR_MARGIN_PERCENTS = 0.08;

  static {
    if (ENABLED) {
      LoggerFactory.getLogger(Benchmark.class).warn("Assertions are calibrated for SonarSource dedicated box. " +
        "They can be disabled by setting the property -DenableBenchmarkAssertions=false.");
    }
  }

  public void expectBetween(String label, long val, long min, long max) {
    if (ENABLED) {
      checkThat(label, val, Matchers.allOf(Matchers.greaterThan(min), Matchers.lessThan(max)));
    }
  }

  public void expectAround(String label, long val, long expect, double marginPercents) {
    if (ENABLED) {
      if (marginPercents > 0.2) {
        throw new IllegalArgumentException("Error margin must be less than 0.2 (20%). Has: " + marginPercents);
      }
      long min = (long) (expect * (1.0 - marginPercents));
      long max = (long) (expect * (1.0 + marginPercents));
      checkThat(label, val, Matchers.allOf(Matchers.greaterThan(min), Matchers.lessThan(max)));
    }
  }

}
