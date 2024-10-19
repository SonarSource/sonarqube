/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.xoo.lang;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeasureSensorTest {

  private MeasureSensor sensor;
  private SensorContextTester context;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File baseDir;
  private MetricFinder metricFinder;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    metricFinder = mock(MetricFinder.class);
    sensor = new MeasureSensor(metricFinder);
    context = SensorContextTester.create(baseDir);
  }

  @Test
  public void testDescriptor() {
    sensor.describe(new DefaultSensorDescriptor());
  }

  @Test
  public void testNoExecutionIfNoMeasureFile() {
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").build();
    context.fileSystem().add(inputFile);
    sensor.execute(context);
  }

  @Test
  public void testExecution() throws IOException {
    File measures = new File(baseDir, "src/foo.xoo.measures");
    FileUtils.write(measures, "ncloc:12\nbranch_coverage:5.3\nsqale_index:300\nbool:true\n\n#comment", StandardCharsets.UTF_8);
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").setModuleBaseDir(baseDir.toPath()).build();
    context.fileSystem().add(inputFile);

    Metric<Boolean> booleanMetric = new Metric.Builder("bool", "Bool", Metric.ValueType.BOOL)
      .create();

    when(metricFinder.<Integer>findByKey("ncloc")).thenReturn(CoreMetrics.NCLOC);
    when(metricFinder.<Double>findByKey("branch_coverage")).thenReturn(CoreMetrics.BRANCH_COVERAGE);
    when(metricFinder.<Long>findByKey("sqale_index")).thenReturn(CoreMetrics.TECHNICAL_DEBT);
    when(metricFinder.<Boolean>findByKey("bool")).thenReturn(booleanMetric);

    sensor.execute(context);

    assertThat(context.measure("foo:src/foo.xoo", CoreMetrics.NCLOC).value()).isEqualTo(12);
    assertThat(context.measure("foo:src/foo.xoo", CoreMetrics.BRANCH_COVERAGE).value()).isEqualTo(5.3);
    assertThat(context.measure("foo:src/foo.xoo", CoreMetrics.TECHNICAL_DEBT).value()).isEqualTo(300L);
    assertThat(context.measure("foo:src/foo.xoo", booleanMetric).value()).isTrue();
  }

  @Test
  public void testExecutionForFoldersMeasures_no_measures() throws IOException {
    File measures = new File(baseDir, "src/folder.measures");
    FileUtils.write(measures, "ncloc:12\nbranch_coverage:5.3\nsqale_index:300\nbool:true\n\n#comment", StandardCharsets.UTF_8);
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").setModuleBaseDir(baseDir.toPath()).build();
    context.fileSystem().add(inputFile);

    Metric<Boolean> booleanMetric = new Metric.Builder("bool", "Bool", Metric.ValueType.BOOL)
      .create();

    when(metricFinder.<Integer>findByKey("ncloc")).thenReturn(CoreMetrics.NCLOC);
    when(metricFinder.<Double>findByKey("branch_coverage")).thenReturn(CoreMetrics.BRANCH_COVERAGE);
    when(metricFinder.<Long>findByKey("sqale_index")).thenReturn(CoreMetrics.TECHNICAL_DEBT);
    when(metricFinder.<Boolean>findByKey("bool")).thenReturn(booleanMetric);

    sensor.execute(context);

    assertThat(context.measure("foo:src", CoreMetrics.NCLOC)).isNull();
  }

  @Test
  public void failIfMetricNotFound() throws IOException {
    File measures = new File(baseDir, "src/foo.xoo.measures");
    FileUtils.write(measures, "unknow:12\n\n#comment");
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").setModuleBaseDir(baseDir.toPath()).build();
    context.fileSystem().add(inputFile);

    assertThatThrownBy(() -> sensor.execute(context))
      .isInstanceOf(IllegalStateException.class);
  }
}
