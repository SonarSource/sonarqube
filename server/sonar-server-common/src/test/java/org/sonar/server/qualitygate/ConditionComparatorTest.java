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
import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;

public class ConditionComparatorTest {
  @Test
  public void sort_by_hardcoded_metric_keys_then_alphabetically() {
    ConditionComparator<String> comparator = new ConditionComparator<>(x -> x);
    List<String> conditions = Arrays.asList(NEW_DUPLICATED_LINES_DENSITY_KEY, RELIABILITY_RATING_KEY, "rule1", SQALE_RATING_KEY, "abc",
      NEW_SECURITY_RATING_KEY, "metric", NEW_COVERAGE_KEY);
    conditions.sort(comparator);
    assertThat(conditions).contains(RELIABILITY_RATING_KEY, NEW_SECURITY_RATING_KEY, SQALE_RATING_KEY,
      NEW_COVERAGE_KEY, "abc", "metric", "rule1");
  }
}
