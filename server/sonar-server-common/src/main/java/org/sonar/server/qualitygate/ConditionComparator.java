/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.qualitygate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;

/**
 * Sorts conditions, first based on an hardcoded list of metric keys, then alphabetically by metric key.
 */
public class ConditionComparator<T> implements Comparator<T> {
  private static final List<String> CONDITIONS_ORDER = Arrays.asList(NEW_RELIABILITY_RATING_KEY, RELIABILITY_RATING_KEY,
    NEW_SECURITY_RATING_KEY, SECURITY_RATING_KEY, NEW_MAINTAINABILITY_RATING_KEY, SQALE_RATING_KEY,
    NEW_COVERAGE_KEY, COVERAGE_KEY,
    NEW_DUPLICATED_LINES_DENSITY_KEY, DUPLICATED_LINES_DENSITY_KEY);
  private static final Map<String, Integer> CONDITIONS_ORDER_IDX = IntStream.range(0, CONDITIONS_ORDER.size()).boxed()
    .collect(Collectors.toMap(CONDITIONS_ORDER::get, x -> x));

  private final Function<T, String> metricKeyExtractor;

  public ConditionComparator(Function<T, String> metricKeyExtractor) {
    this.metricKeyExtractor = metricKeyExtractor;
  }

  @Override public int compare(T c1, T c2) {
    Function<T, Integer> byList = c -> CONDITIONS_ORDER_IDX.getOrDefault(metricKeyExtractor.apply(c), Integer.MAX_VALUE);
    return Comparator.comparing(byList).thenComparing(metricKeyExtractor).compare(c1, c2);
  }
}
