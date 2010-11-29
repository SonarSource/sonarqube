/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.timemachine;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class TendencyAnalyserTest {
  private TendencyAnalyser analyser = new TendencyAnalyser();

  private List<Double> getValues(Double[] array) {
    return Arrays.asList(array);
  }


  protected void assertBetween(String typeLabel, Double value, Double min, Double max) {
    assertTrue(typeLabel + " " + value + "<" + min, value >= min);
    assertTrue(typeLabel + "=" + value + ">" + max, value <= max);
  }

  @Test
  public void testNoData() {
    assertThat(analyser.analyse(Collections.<Double>emptyList()), nullValue());
  }

  @Test
  public void testNotEnoughData() {
    assertThat(analyser.analyseLevel(Arrays.asList(10.0)), nullValue());
  }

  @Test
  public void testTendencyOnThreeDays() {
    Double[] doubles = new Double[]{10.0, null, 9.9};
    TendencyAnalyser.SlopeData slopeData = analyser.analyse(getValues(doubles));
    assertBetween("slope", slopeData.getSlope(), -0.5, 0.5);
    assertEquals(TendencyAnalyser.TENDENCY_NEUTRAL, slopeData.getLevel());
  }

  @Test
  public void testTendencyOnTwoZeroDays() {
    Double[] doubles = new Double[]{0.0, 0.0};
    TendencyAnalyser.SlopeData slopeData = analyser.analyse(getValues(doubles));
    assertBetween("slope", slopeData.getSlope(), -0.0, 0.0);
    assertEquals(TendencyAnalyser.TENDENCY_NEUTRAL, slopeData.getLevel());
  }

  @Test
  public void testTendencyOnThreeZeroDays() {
    Double[] doubles = new Double[]{0.0, 0.0, 0.0};
    TendencyAnalyser.SlopeData slopeData = analyser.analyse(getValues(doubles));
    assertBetween("slope", slopeData.getSlope(), -0.0, 0.0);
    assertEquals(TendencyAnalyser.TENDENCY_NEUTRAL, slopeData.getLevel());
  }

  @Test
  public void testBigDownOnThreeDays() {
    Double[] doubles = new Double[]{90.0, 91.0, 50.0};
    TendencyAnalyser.SlopeData slopeData = analyser.analyse(getValues(doubles));
    assertTrue("slope", slopeData.getSlope() < -2.0);
    assertEquals(TendencyAnalyser.TENDENCY_BIG_DOWN, slopeData.getLevel());
  }

  @Test
  public void testFlatTendency() {
    Double[] doubles = new Double[]{10.0, 10.2, 9.9};
    TendencyAnalyser.SlopeData slopeData = analyser.analyse(getValues(doubles));
    assertBetween("slope", slopeData.getSlope(), -0.5, 0.5);
    assertEquals(TendencyAnalyser.TENDENCY_NEUTRAL, slopeData.getLevel());
  }

  @Test
  public void testFlatTendencyWithPeak() {
    Double[] doubles = new Double[]{10.0, 15.0, 10.0};
    TendencyAnalyser.SlopeData slopeData = analyser.analyse(getValues(doubles));
    assertBetween("slope", slopeData.getSlope(), -0.5, 0.5);
    assertEquals(TendencyAnalyser.TENDENCY_NEUTRAL, slopeData.getLevel());
  }

  @Test
  public void testBigUpTendencyOnThreeValues() {
    Double[] doubles = new Double[]{10.0, 12.0, 15.5};
    TendencyAnalyser.SlopeData slopeData = analyser.analyse(getValues(doubles));
    assertBetween("slope", slopeData.getSlope(), 2.5, 3.0);
    assertEquals(TendencyAnalyser.TENDENCY_BIG_UP, slopeData.getLevel());
  }

  @Test
  public void testBigUpTendencyOnTenValues() {
    Double[] doubles = new Double[]{45.0, 60.0, 57.0, 65.0, 58.0, 68.0, 59.0, 66.0, 76.0, 80.0};
    TendencyAnalyser.SlopeData slopeData = analyser.analyse(getValues(doubles));
    assertBetween("slope", slopeData.getSlope(), 2.5, 3.0);
    assertEquals(TendencyAnalyser.TENDENCY_BIG_UP, slopeData.getLevel());
  }

  @Test
  public void testMediumUpTendency() {
    Double[] doubles = new Double[]{5.0, 4.5, 5.1, 5.5, 5.3, 6.4, 6.3, 6.6, 6.8, 6.5};
    TendencyAnalyser.SlopeData slopeData = analyser.analyse(getValues(doubles));
    assertBetween("slope", slopeData.getSlope(), 0.0, 1.0);
    assertEquals(TendencyAnalyser.TENDENCY_UP, slopeData.getLevel());
  }

  @Test
  public void testAsymetricAlgorithm() {
    TendencyAnalyser.SlopeData slopeData1 = analyser.analyse(getValues(new Double[]{45.0, 47.0, 95.0}));
    TendencyAnalyser.SlopeData slopeData2 = analyser.analyse(getValues(new Double[]{95.0, 45.0, 47.0}));
    assertTrue(slopeData1.getSlope() != slopeData2.getSlope());
  }
}