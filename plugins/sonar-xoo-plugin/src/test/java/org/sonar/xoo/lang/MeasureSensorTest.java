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
package org.sonar.xoo.lang;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorStorage;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MeasureSensorTest {

  private MeasureSensor sensor;
  private SensorContext context = mock(SensorContext.class);
  private DefaultFileSystem fileSystem;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private File baseDir;
  private MetricFinder metricFinder;
  private SensorStorage storage;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    metricFinder = mock(MetricFinder.class);
    sensor = new MeasureSensor(metricFinder);
    fileSystem = new DefaultFileSystem(baseDir);
    when(context.fileSystem()).thenReturn(fileSystem);
    storage = mock(SensorStorage.class);
    when(context.newMeasure()).then(new Answer<DefaultMeasure>() {
      @Override
      public DefaultMeasure answer(InvocationOnMock invocation) throws Throwable {
        return new DefaultMeasure(storage);
      }
    });
  }

  @Test
  public void testDescriptor() {
    sensor.describe(new DefaultSensorDescriptor());
  }

  @Test
  public void testNoExecutionIfNoMeasureFile() {
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setAbsolutePath(new File(baseDir, "src/foo.xoo").getAbsolutePath()).setLanguage("xoo");
    fileSystem.add(inputFile);
    sensor.execute(context);
  }

  @Test
  public void testExecution() throws IOException {
    File measures = new File(baseDir, "src/foo.xoo.measures");
    FileUtils.write(measures, "ncloc:12\nbranch_coverage:5.3\nsqale_index:300\nbool:true\n\n#comment");
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setAbsolutePath(new File(baseDir, "src/foo.xoo").getAbsolutePath()).setLanguage("xoo");
    fileSystem.add(inputFile);

    Metric<Boolean> booleanMetric = new Metric.Builder("bool", "Bool", Metric.ValueType.BOOL)
      .create();

    when(metricFinder.findByKey("ncloc")).thenReturn(CoreMetrics.NCLOC);
    when(metricFinder.findByKey("branch_coverage")).thenReturn(CoreMetrics.BRANCH_COVERAGE);
    when(metricFinder.findByKey("sqale_index")).thenReturn(CoreMetrics.TECHNICAL_DEBT);
    when(metricFinder.findByKey("bool")).thenReturn(booleanMetric);

    sensor.execute(context);

    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.NCLOC).onFile(inputFile).withValue(12));
    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.BRANCH_COVERAGE).onFile(inputFile).withValue(5.3));
    verify(storage).store(new DefaultMeasure().forMetric(CoreMetrics.TECHNICAL_DEBT).onFile(inputFile).withValue(300L));
    verify(storage).store(new DefaultMeasure().forMetric(booleanMetric).onFile(inputFile).withValue(true));
  }

  @Test
  public void failIfMetricNotFound() throws IOException {
    File measures = new File(baseDir, "src/foo.xoo.measures");
    FileUtils.write(measures, "unknow:12\n\n#comment");
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setAbsolutePath(new File(baseDir, "src/foo.xoo").getAbsolutePath()).setLanguage("xoo");
    fileSystem.add(inputFile);

    thrown.expect(IllegalStateException.class);

    sensor.execute(context);
  }
}
