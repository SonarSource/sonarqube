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
package org.sonar.plugins.core.timemachine;

import org.junit.Test;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

public class TendencyAnalyserTest {
  static TendencyAnalyser.SlopeData analyse(Double... values) {
    return new TendencyAnalyser().analyse(Arrays.asList(values));
  }

  static Integer analyseLevel(Double... values) {
    return new TendencyAnalyser().analyseLevel(Arrays.asList(values));
  }

  @Test
  public void testNoData() {
    TendencyAnalyser.SlopeData slopeData = analyse();

    assertThat(slopeData).isNull();
  }

  @Test
  public void testNotEnoughData() {
    assertThat(analyseLevel(10.0)).isNull();
  }

  @Test
  public void testTendencyOnThreeDays() {
    TendencyAnalyser.SlopeData slopeData = analyse(10.0, null, 9.9);

    assertThat(slopeData.getSlope()).isGreaterThan(-0.5).isLessThan(0.5);
    assertThat(slopeData.getLevel()).isEqualTo(TendencyAnalyser.TENDENCY_NEUTRAL);
  }

  @Test
  public void testTendencyOnTwoZeroDays() {
    TendencyAnalyser.SlopeData slopeData = analyse(0.0, 0.0);

    assertThat(slopeData.getSlope()).isZero();
    assertThat(slopeData.getLevel()).isEqualTo(TendencyAnalyser.TENDENCY_NEUTRAL);
  }

  @Test
  public void testTendencyOnThreeZeroDays() {
    TendencyAnalyser.SlopeData slopeData = analyse(0.0, 0.0, 0.0);

    assertThat(slopeData.getSlope()).isZero();
    assertThat(slopeData.getLevel()).isEqualTo(TendencyAnalyser.TENDENCY_NEUTRAL);
  }

  @Test
  public void testBigDownOnThreeDays() {
    TendencyAnalyser.SlopeData slopeData = analyse(90.0, 91.0, 50.0);

    assertThat(slopeData.getSlope()).isLessThan(-2.0);
    assertThat(slopeData.getLevel()).isEqualTo(TendencyAnalyser.TENDENCY_BIG_DOWN);
  }

  @Test
  public void testFlatTendency() {
    TendencyAnalyser.SlopeData slopeData = analyse(10.0, 10.2, 9.9);

    assertThat(slopeData.getSlope()).isGreaterThan(-0.5).isLessThan(0.5);
    assertThat(slopeData.getLevel()).isEqualTo(TendencyAnalyser.TENDENCY_NEUTRAL);
  }

  @Test
  public void testFlatTendencyWithPeak() {
    TendencyAnalyser.SlopeData slopeData = analyse(10.0, 15.0, 10.0);

    assertThat(slopeData.getSlope()).isGreaterThan(-0.5).isLessThan(0.5);
    assertThat(slopeData.getLevel()).isEqualTo(TendencyAnalyser.TENDENCY_NEUTRAL);
  }

  @Test
  public void testBigUpTendencyOnThreeValues() {
    TendencyAnalyser.SlopeData slopeData = analyse(10.0, 12.0, 15.5);

    assertThat(slopeData.getSlope()).isGreaterThan(2.5).isLessThan(3.0);
    assertThat(slopeData.getLevel()).isEqualTo(TendencyAnalyser.TENDENCY_BIG_UP);
  }

  @Test
  public void testBigUpTendencyOnTenValues() {
    TendencyAnalyser.SlopeData slopeData = analyse(45.0, 60.0, 57.0, 65.0, 58.0, 68.0, 59.0, 66.0, 76.0, 80.0);

    assertThat(slopeData.getSlope()).isGreaterThan(2.5).isLessThan(3.0);
    assertThat(slopeData.getLevel()).isEqualTo(TendencyAnalyser.TENDENCY_BIG_UP);
  }

  @Test
  public void testMediumUpTendency() {
    TendencyAnalyser.SlopeData slopeData = analyse(5.0, 4.5, 5.1, 5.5, 5.3, 6.4, 6.3, 6.6, 6.8, 6.5);

    assertThat(slopeData.getSlope()).isGreaterThan(0.0).isLessThan(1.0);
    assertThat(slopeData.getLevel()).isEqualTo(TendencyAnalyser.TENDENCY_UP);
  }

  @Test
  public void testAsymetricAlgorithm() {
    TendencyAnalyser.SlopeData slopeData1 = analyse(45.0, 47.0, 95.0);
    TendencyAnalyser.SlopeData slopeData2 = analyse(95.0, 45.0, 47.0);

    assertThat(slopeData1.getSlope()).isNotEqualTo(slopeData2.getSlope());
  }
}
