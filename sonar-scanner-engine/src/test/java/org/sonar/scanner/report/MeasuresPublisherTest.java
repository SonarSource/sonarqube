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
package org.sonar.scanner.report;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.deprecated.test.TestPlanBuilder;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.measure.MeasureCache;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeasuresPublisherTest {

  private static final String FILE_KEY = "foo:src/Foo.php";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MeasureCache measureCache;
  private MeasuresPublisher publisher;

  private org.sonar.api.resources.Resource sampleFile;

  @Before
  public void prepare() {
    Project p = new Project("foo").setAnalysisDate(new Date(1234567L));
    BatchComponentCache resourceCache = new BatchComponentCache();
    sampleFile = org.sonar.api.resources.File.create("src/Foo.php").setEffectiveKey(FILE_KEY);
    resourceCache.add(p, null).setInputComponent(new DefaultInputModule("foo"));
    resourceCache.add(sampleFile, null).setInputComponent(new DefaultInputFile("foo", "src/Foo.php"));
    measureCache = mock(MeasureCache.class);
    when(measureCache.byComponentKey(anyString())).thenReturn(Collections.<DefaultMeasure<?>>emptyList());
    publisher = new MeasuresPublisher(resourceCache, measureCache, mock(TestPlanBuilder.class));
  }

  @Test
  public void publishMeasures() throws Exception {
    DefaultMeasure<Integer> measure = new DefaultMeasure<Integer>().forMetric(CoreMetrics.LINES_TO_COVER)
      .withValue(2);
    // String value
    DefaultMeasure<String> stringMeasure = new DefaultMeasure<String>().forMetric(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION)
      .withValue("foo bar");
    when(measureCache.byComponentKey(FILE_KEY)).thenReturn(asList(measure, stringMeasure));

    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);

    publisher.publish(writer);

    ScannerReportReader reader = new ScannerReportReader(outputDir);

    assertThat(reader.readComponentMeasures(1)).hasSize(0);
    try (CloseableIterator<ScannerReport.Measure> componentMeasures = reader.readComponentMeasures(2)) {
      assertThat(componentMeasures).hasSize(2);
    }
  }

  @Test
  public void fail_with_IAE_when_measure_has_no_value() throws Exception {
    DefaultMeasure<Integer> measure = new DefaultMeasure<Integer>().forMetric(CoreMetrics.LINES_TO_COVER);
    when(measureCache.byComponentKey(FILE_KEY)).thenReturn(Collections.singletonList(measure));

    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);

    try {
      publisher.publish(writer);
      fail();
    } catch (RuntimeException e) {
      assertThat(ExceptionUtils.getFullStackTrace(e)).contains("Measure on metric 'lines_to_cover' and component 'foo:src/Foo.php' has no value, but it's not allowed");
    }
  }

}
