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
package org.sonar.ce.task.projectanalysis.filemove;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class SourceSimilarityImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SourceSimilarityImpl underTest = new SourceSimilarityImpl();

  @Test
  public void zero_if_fully_different() {
    List<String> left = asList("a", "b", "c");
    List<String> right = asList("d", "e");
    assertThat(underTest.score(left, right)).isEqualTo(0);
  }

  @Test
  public void one_hundred_if_same() {
    assertThat(underTest.score(asList("a", "b", "c"), asList("a", "b", "c"))).isEqualTo(100);
    assertThat(underTest.score(asList(""), asList(""))).isEqualTo(100);
  }

  @Test
  public void partially_same() {
    assertThat(underTest.score(asList("a", "b", "c", "d"), asList("a", "b", "e", "f"))).isEqualTo(50);
    assertThat(underTest.score(asList("a"), asList("a", "b", "c"))).isEqualTo(33);
    assertThat(underTest.score(asList("a", "b", "c"), asList("a"))).isEqualTo(33);
  }

  @Test
  public void finding_threshold_in_line_count_to_go_below_85_score() {
    assertThat(underTest.score(listOf(100), listOf(115))).isEqualTo(86);
    assertThat(underTest.score(listOf(100), listOf(116))).isEqualTo(86);
    assertThat(underTest.score(listOf(100), listOf(117))).isEqualTo(85); // 85.47% - 117%
    assertThat(underTest.score(listOf(100), listOf(118))).isEqualTo(84); // 84.74% - 118%

    assertThat(underTest.score(listOf(50), listOf(58))).isEqualTo(86); // 86.20% - 116%
    assertThat(underTest.score(listOf(50), listOf(59))).isEqualTo(84); // 84.74% - 118%

    assertThat(underTest.score(listOf(25), listOf(29))).isEqualTo(86); // 86.20% - 116%
    assertThat(underTest.score(listOf(25), listOf(30))).isEqualTo(83); // 83.33% - 120%

    assertThat(underTest.score(listOf(12), listOf(14))).isEqualTo(85); // 85.71% - 116.67%
    assertThat(underTest.score(listOf(12), listOf(15))).isEqualTo(80); // 80.00% - 125%

    assertThat(underTest.score(listOf(10), listOf(11))).isEqualTo(90); // 90.90% - 110%
    assertThat(underTest.score(listOf(10), listOf(12))).isEqualTo(83); // 83.33% - 120%

    assertThat(underTest.score(listOf(5), listOf(5))).isEqualTo(100); // 100% - 100%
    assertThat(underTest.score(listOf(5), listOf(6))).isEqualTo(83); // 83.33% - 120%

    assertThat(underTest.score(listOf(200), listOf(234))).isEqualTo(85); // 85.47% - 117%
    assertThat(underTest.score(listOf(200), listOf(236))).isEqualTo(84); // 84.75% - 118%

    assertThat(underTest.score(listOf(300), listOf(352))).isEqualTo(85); // 85.23% - 117.33%
    assertThat(underTest.score(listOf(300), listOf(354))).isEqualTo(84); // 84.74% - 118%

    assertThat(underTest.score(listOf(400), listOf(470))).isEqualTo(85); // 85.10% - 117.50%
    assertThat(underTest.score(listOf(400), listOf(471))).isEqualTo(84); // 84.92% - 117.75%

    assertThat(underTest.score(listOf(500), listOf(588))).isEqualTo(85); // 85.03% - 117.60%
    assertThat(underTest.score(listOf(500), listOf(589))).isEqualTo(84); // 84.88% - 117.80%
  }

  @Test
  public void verify_84_percent_ratio_for_lower_bound() {
    IntStream.range(0, 1000)
      .forEach(ref -> lowerBoundGivesNonMeaningfulScore(ref, 0.84));
  }

  @Test
  public void verify_118_percent_ratio_for_upper_bound() {
    IntStream.range(0, 1000)
      .forEach(ref -> upperBoundGivesNonMeaningfulScore(ref, 1.18));
  }

  private void lowerBoundGivesNonMeaningfulScore(Integer ref, double ratio) {
    int lowerBound = (int) Math.floor(ref * ratio);
    assertThat(underTest.score(listOf(ref), listOf(lowerBound)))
      .describedAs("Score for %s%% lines of %s (ie. %s lines) should be 84 or less", ratio * 100, ref, lowerBound)
      .isLessThanOrEqualTo(84);
  }

  private void upperBoundGivesNonMeaningfulScore(Integer ref, double ratio) {
    int upperBound = (int) Math.ceil(ref * ratio);
    assertThat(underTest.score(listOf(ref), listOf(upperBound)))
      .describedAs("Score for %s%% lines of %s (ie. %s lines) should be 84 or less", ratio * 100, ref, upperBound)
      .isLessThanOrEqualTo(84);
  }

  /**
   * Creates a list of {@code numberOfElements} int values as String, starting with zero.
   */
  private static List<String> listOf(int numberOfElements) {
    return IntStream.range(0, numberOfElements).mapToObj(String::valueOf).collect(Collectors.toList());
  }

  @Test
  public void two_empty_lists_are_not_considered_as_equal() {
    assertThat(underTest.score(emptyList(), emptyList())).isEqualTo(0);
  }
}
