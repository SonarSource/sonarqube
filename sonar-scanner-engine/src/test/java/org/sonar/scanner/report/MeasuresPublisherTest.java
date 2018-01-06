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
package org.sonar.scanner.report;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.deprecated.test.TestPlanBuilder;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.scan.measure.MeasureCache;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeasuresPublisherTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MeasureCache measureCache;
  private MeasuresPublisher publisher;

  private File outputDir;
  private ScannerReportWriter writer;
  private DefaultInputFile inputFile;
  private DefaultInputModule inputModule;

  @Before
  public void prepare() throws IOException {
    String moduleKey = "foo";
    inputModule = TestInputFileBuilder.newDefaultInputModule(moduleKey, temp.newFolder());
    inputFile = new TestInputFileBuilder(moduleKey, "src/Foo.php").setPublish(true).build();
    InputComponentStore componentCache = new InputComponentStore(inputModule, mock(BranchConfiguration.class));
    componentCache.put(inputFile);
    measureCache = mock(MeasureCache.class);
    when(measureCache.byComponentKey(anyString())).thenReturn(Collections.<DefaultMeasure<?>>emptyList());
    publisher = new MeasuresPublisher(componentCache, measureCache, mock(TestPlanBuilder.class));
    outputDir = temp.newFolder();
    writer = new ScannerReportWriter(outputDir);
  }

  @Test
  public void publishMeasures() throws Exception {
    DefaultMeasure<Integer> measure = new DefaultMeasure<Integer>().forMetric(CoreMetrics.LINES_TO_COVER)
      .withValue(2);
    // String value
    DefaultMeasure<String> stringMeasure = new DefaultMeasure<String>().forMetric(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION)
      .withValue("foo bar");
    when(measureCache.byComponentKey(inputFile.key())).thenReturn(asList(measure, stringMeasure));

    publisher.publish(writer);
    ScannerReportReader reader = new ScannerReportReader(outputDir);

    assertThat(reader.readComponentMeasures(inputModule.batchId())).hasSize(0);
    try (CloseableIterator<ScannerReport.Measure> componentMeasures = reader.readComponentMeasures(inputFile.batchId())) {
      assertThat(componentMeasures).hasSize(2);
    }
  }

  @Test
  public void fail_with_IAE_when_measure_has_no_value() throws Exception {
    DefaultMeasure<Integer> measure = new DefaultMeasure<Integer>().forMetric(CoreMetrics.LINES_TO_COVER);
    when(measureCache.byComponentKey(inputFile.key())).thenReturn(Collections.singletonList(measure));

    try {
      publisher.publish(writer);
      fail();
    } catch (RuntimeException e) {
      assertThat(ExceptionUtils.getFullStackTrace(e)).contains("Measure on metric 'lines_to_cover' and component 'foo:src/Foo.php' has no value, but it's not allowed");
    }
  }

}
