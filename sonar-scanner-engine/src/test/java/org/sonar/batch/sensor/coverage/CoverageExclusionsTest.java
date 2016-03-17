/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.sensor.coverage;

import org.junit.rules.TemporaryFolder;

import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.config.ExclusionProperties;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoverageExclusionsTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private Settings settings;
  private DefaultFileSystem fs;

  private CoverageExclusions filter;

  @Before
  public void createFilter() {
    settings = new Settings(new PropertyDefinitions(ExclusionProperties.all()));
    fs = new DefaultFileSystem(temp.getRoot());
    filter = new CoverageExclusions(settings, fs);
  }

  @Test
  public void shouldValidateStrictlyPositiveLine() {
    DefaultInputFile file = new DefaultInputFile("module", "testfile");
    Measure measure = mock(Measure.class);
    Map<Integer, Integer> map = ImmutableMap.of(0, 3);

    String data = KeyValueFormat.format(map);
    when(measure.getMetric()).thenReturn(CoreMetrics.IT_CONDITIONS_BY_LINE);
    when(measure.getData()).thenReturn(data);

    fs.add(file);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("must be > 0");
    filter.validate(measure, "testfile");
  }

  @Test
  public void shouldValidateFileExists() {
    DefaultInputFile file = new DefaultInputFile("module", "testfile");
    Measure measure = mock(Measure.class);
    Map<Integer, Integer> map = ImmutableMap.of(0, 3);

    String data = KeyValueFormat.format(map);
    when(measure.getMetric()).thenReturn(CoreMetrics.IT_CONDITIONS_BY_LINE);
    when(measure.getData()).thenReturn(data);

    fs.add(file);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("resource is not indexed as a file");
    filter.validate(measure, "dummy");
  }

  @Test
  public void shouldValidateMaxLine() {
    DefaultInputFile file = new DefaultInputFile("module", "testfile");
    file.setLines(10);
    Measure measure = mock(Measure.class);
    Map<Integer, Integer> map = ImmutableMap.of(11, 3);

    String data = KeyValueFormat.format(map);
    when(measure.getMetric()).thenReturn(CoreMetrics.COVERED_CONDITIONS_BY_LINE);
    when(measure.getData()).thenReturn(data);

    exception.expect(IllegalStateException.class);
    filter.validate(measure, file);
  }

  @Test
  public void shouldNotFilterNonCoverageMetrics() {
    Measure otherMeasure = mock(Measure.class);
    when(otherMeasure.getMetric()).thenReturn(CoreMetrics.LINES);
    assertThat(filter.accept(mock(Resource.class), otherMeasure)).isTrue();
  }

  @Test
  public void shouldFilterFileBasedOnPattern() {
    Resource resource = File.create("src/org/polop/File.php", null, false);
    Measure coverageMeasure = mock(Measure.class);
    when(coverageMeasure.getMetric()).thenReturn(CoreMetrics.LINES_TO_COVER);

    settings.setProperty("sonar.coverage.exclusions", "src/org/polop/*");
    filter.initPatterns();
    assertThat(filter.accept(resource, coverageMeasure)).isFalse();
  }

  @Test
  public void shouldNotFilterFileBasedOnPattern() {
    Resource resource = File.create("src/org/polop/File.php", null, false);
    Measure coverageMeasure = mock(Measure.class);
    when(coverageMeasure.getMetric()).thenReturn(CoreMetrics.COVERAGE);

    settings.setProperty("sonar.coverage.exclusions", "src/org/other/*");
    filter.initPatterns();
    assertThat(filter.accept(resource, coverageMeasure)).isTrue();
  }
}
