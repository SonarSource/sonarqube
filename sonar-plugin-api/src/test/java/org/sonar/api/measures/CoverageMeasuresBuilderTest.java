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
package org.sonar.api.measures;

import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CoverageMeasuresBuilderTest {

  @Test
  public void shouldNotCreateIfNoValues() {
    CoverageMeasuresBuilder builder = CoverageMeasuresBuilder.create();
    assertThat(builder.createMeasures().size(), is(0));
  }

  @Test
  public void shouldCreateHitsByLineData() {
    CoverageMeasuresBuilder builder = CoverageMeasuresBuilder.create();
    builder.setHits(1, 0);
    builder.setHits(2, 3);
    builder.setHits(4, 2);
    assertThat(find(builder.createMeasures(), CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY).getData(), is("1=0;2=3;4=2"));
  }

  @Test
  public void shouldCreateUncoveredLines() {
    CoverageMeasuresBuilder builder = CoverageMeasuresBuilder.create();
    builder.setHits(1, 0);
    builder.setHits(2, 3);
    builder.setHits(3, 0);
    assertThat(find(builder.createMeasures(), CoreMetrics.UNCOVERED_LINES_KEY).getIntValue(), is(2));
  }

  @Test
  public void shouldCreateConditionsByLineData() {
    CoverageMeasuresBuilder builder = CoverageMeasuresBuilder.create();
    builder.setConditions(1, 2, 2);
    builder.setConditions(2, 1, 0);
    assertThat(find(builder.createMeasures(), CoreMetrics.CONDITIONS_BY_LINE_KEY).getData(), is("1=2;2=1"));
    assertThat(find(builder.createMeasures(), CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY).getData(), is("1=2;2=0"));
  }

  @Test
  public void shouldCreateNumberOfConditionsToCover() {
    CoverageMeasuresBuilder builder = CoverageMeasuresBuilder.create();
    builder.setConditions(1, 2, 2);
    builder.setConditions(2, 1, 0);
    assertThat(find(builder.createMeasures(), CoreMetrics.CONDITIONS_TO_COVER_KEY).getIntValue(), is(3));
  }

  @Test
  public void shouldCreateNumberOfUncoveredConditions() {
    CoverageMeasuresBuilder builder = CoverageMeasuresBuilder.create();
    builder.setConditions(1, 2, 2);
    builder.setConditions(2, 1, 0);
    builder.setConditions(3, 3, 1);
    assertThat(find(builder.createMeasures(), CoreMetrics.UNCOVERED_CONDITIONS_KEY).getIntValue(), is(3));
  }

  @Test
  public void shouldSetOnlyPositiveConditions() {
    CoverageMeasuresBuilder builder = CoverageMeasuresBuilder.create();
    builder.setConditions(1, 0, 0);
    builder.setConditions(2, 1, 0);
    assertThat(find(builder.createMeasures(), CoreMetrics.CONDITIONS_BY_LINE_KEY).getData(), is("2=1"));
    assertThat(find(builder.createMeasures(), CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY).getData(), is("2=0"));
  }

  @Test
  public void shouldIgnoreDuplicatedSetHits() {
    CoverageMeasuresBuilder builder = CoverageMeasuresBuilder.create();
    builder.setHits(2, 3);
    builder.setHits(2, 5);// to ignore
    assertThat(builder.getLinesToCover(), is(1));
    assertThat(builder.getCoveredLines(), is(1));
    assertThat(builder.getHitsByLine().get(2), is(3));
  }

  @Test
  public void shouldIgnoreDuplicatedSetConditions() {
    CoverageMeasuresBuilder builder = CoverageMeasuresBuilder.create();
    builder.setConditions(1, 3, 2);
    builder.setConditions(1, 1, 0);// to ignore
    assertThat(builder.getConditions(), is(3));
    assertThat(builder.getCoveredConditions(), is(2));
    assertThat(builder.getConditionsByLine().get(1), is(3));
    assertThat(builder.getCoveredConditionsByLine().get(1), is(2));
  }


  @Test
  public void shouldResetFields() {
    CoverageMeasuresBuilder builder = CoverageMeasuresBuilder.create();
    builder.setHits(1, 4);
    builder.setConditions(1, 3, 1);
    builder.reset();
    assertThat(builder.getConditions(), is(0));
    assertThat(builder.getCoveredConditions(), is(0));
    assertThat(builder.getCoveredLines(), is(0));
    assertThat(builder.getHitsByLine().size(), is(0));
    assertThat(builder.getConditionsByLine().size(), is(0));
    assertThat(builder.getCoveredConditionsByLine().size(), is(0));
  }

  private Measure find(Collection<Measure> measures, String metricKey) {
    for (Measure measure : measures) {
      if (metricKey.equals(measure.getMetricKey())) {
        return measure;
      }
    }
    return null;
  }
}
