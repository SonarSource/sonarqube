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
package org.sonar.scanner;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.CoreMetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.EXECUTABLE_LINES_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA_KEY;

public class DefaultFileLinesContextTest {

  private static final String HITS_METRIC_KEY = "hits";
  private static final String AUTHOR_METRIC_KEY = "author";
  private static final String BRANCHES_METRIC_KEY = "branches";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DefaultFileLinesContext fileLineMeasures;

  private SensorStorage sensorStorage;
  private DefaultInputFile file;

  @Before
  public void setUp() {
    MetricFinder metricFinder = mock(MetricFinder.class);
    org.sonar.api.batch.measure.Metric<String> hitsMetric = mock(org.sonar.api.batch.measure.Metric.class);
    when(hitsMetric.valueType()).thenReturn(String.class);
    when(hitsMetric.key()).thenReturn(HITS_METRIC_KEY);
    when(metricFinder.<String>findByKey(HITS_METRIC_KEY)).thenReturn(hitsMetric);
    org.sonar.api.batch.measure.Metric<String> authorMetric = mock(org.sonar.api.batch.measure.Metric.class);
    when(authorMetric.valueType()).thenReturn(String.class);
    when(authorMetric.key()).thenReturn(AUTHOR_METRIC_KEY);
    when(metricFinder.<String>findByKey(AUTHOR_METRIC_KEY)).thenReturn(authorMetric);
    org.sonar.api.batch.measure.Metric<String> branchesMetric = mock(org.sonar.api.batch.measure.Metric.class);
    when(branchesMetric.valueType()).thenReturn(String.class);
    when(branchesMetric.key()).thenReturn(BRANCHES_METRIC_KEY);
    when(metricFinder.<String>findByKey(BRANCHES_METRIC_KEY)).thenReturn(branchesMetric);
    when(metricFinder.<String>findByKey(CoreMetrics.NCLOC_DATA_KEY)).thenReturn(CoreMetrics.NCLOC_DATA);
    when(metricFinder.<String>findByKey(CoreMetrics.EXECUTABLE_LINES_DATA_KEY)).thenReturn(CoreMetrics.EXECUTABLE_LINES_DATA);
    sensorStorage = mock(SensorStorage.class);
    file = new TestInputFileBuilder("foo", "src/foo.php").initMetadata("Foo\nbar\nbiz").build();
    fileLineMeasures = new DefaultFileLinesContext(sensorStorage, file, metricFinder);
  }

  @Test
  public void shouldSave() {
    fileLineMeasures.setIntValue(HITS_METRIC_KEY, 1, 2);
    fileLineMeasures.setIntValue(HITS_METRIC_KEY, 3, 0);
    fileLineMeasures.save();

    assertThat(fileLineMeasures.toString()).isEqualTo("DefaultFileLinesContext{{hits={1=2, 3=0}}}");

    ArgumentCaptor<DefaultMeasure> captor = ArgumentCaptor.forClass(DefaultMeasure.class);
    verify(sensorStorage).store(captor.capture());

    DefaultMeasure measure = captor.getValue();
    assertThat(measure.inputComponent()).isEqualTo(file);
    assertThat(measure.metric().key()).isEqualTo(HITS_METRIC_KEY);
    assertThat(measure.value()).isEqualTo("1=2;3=0");
  }

  @Test
  public void validateLineGreaterThanZero() {
    thrown.expectMessage("Line number should be positive for file src/foo.php.");
    fileLineMeasures.setIntValue(HITS_METRIC_KEY, 0, 2);
  }

  @Test
  public void validateLineLowerThanLineCount() {
    thrown.expectMessage("Line 4 is out of range for file src/foo.php. File has 3 lines");
    fileLineMeasures.setIntValue(HITS_METRIC_KEY, 4, 2);
  }

  @Test
  public void optimizeValues() {
    fileLineMeasures.setIntValue(NCLOC_DATA_KEY, 1, 0);
    fileLineMeasures.setIntValue(NCLOC_DATA_KEY, 2, 1);
    fileLineMeasures.setIntValue(EXECUTABLE_LINES_DATA_KEY, 1, 0);
    fileLineMeasures.setIntValue(EXECUTABLE_LINES_DATA_KEY, 2, 1);
    fileLineMeasures.save();

    ArgumentCaptor<DefaultMeasure> captor = ArgumentCaptor.forClass(DefaultMeasure.class);
    verify(sensorStorage, times(2)).store(captor.capture());

    List<DefaultMeasure> measures = captor.getAllValues();
    assertThat(measures).extracting(DefaultMeasure::inputComponent, m -> m.metric().key(), DefaultMeasure::value)
      .containsExactlyInAnyOrder(tuple(file, NCLOC_DATA_KEY, "2=1"),
        tuple(file, EXECUTABLE_LINES_DATA_KEY, "2=1"));
  }

  @Test
  public void shouldSaveSeveral() {
    fileLineMeasures.setIntValue(HITS_METRIC_KEY, 1, 2);
    fileLineMeasures.setIntValue(HITS_METRIC_KEY, 3, 4);
    fileLineMeasures.setStringValue(AUTHOR_METRIC_KEY, 1, "simon");
    fileLineMeasures.setStringValue(AUTHOR_METRIC_KEY, 3, "evgeny");
    fileLineMeasures.save();
    fileLineMeasures.setIntValue(BRANCHES_METRIC_KEY, 1, 2);
    fileLineMeasures.setIntValue(BRANCHES_METRIC_KEY, 3, 4);
    fileLineMeasures.save();

    ArgumentCaptor<DefaultMeasure> captor = ArgumentCaptor.forClass(DefaultMeasure.class);
    verify(sensorStorage, times(3)).store(captor.capture());

    List<DefaultMeasure> measures = captor.getAllValues();
    assertThat(measures).extracting(DefaultMeasure::inputComponent, m -> m.metric().key(), DefaultMeasure::value)
      .containsExactlyInAnyOrder(tuple(file, HITS_METRIC_KEY, "1=2;3=4"),
        tuple(file, AUTHOR_METRIC_KEY, "1=simon;3=evgeny"),
        tuple(file, BRANCHES_METRIC_KEY, "1=2;3=4"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldNotModifyAfterSave() {
    fileLineMeasures.setIntValue(HITS_METRIC_KEY, 1, 2);
    fileLineMeasures.save();
    fileLineMeasures.setIntValue(HITS_METRIC_KEY, 1, 2);
  }

}
