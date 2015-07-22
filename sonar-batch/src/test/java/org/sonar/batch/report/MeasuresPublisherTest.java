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
package org.sonar.batch.report;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.core.util.CloseableIterator;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeasuresPublisherTest {

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
    sampleFile = org.sonar.api.resources.File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php");
    resourceCache.add(p, null);
    resourceCache.add(sampleFile, null);
    measureCache = mock(MeasureCache.class);
    when(measureCache.byResource(any(Resource.class))).thenReturn(Collections.<Measure>emptyList());
    publisher = new MeasuresPublisher(resourceCache, measureCache);
  }

  @Test
  public void publishMeasures() throws Exception {
    Measure measure = new Measure<>(CoreMetrics.COVERAGE)
      .setValue(2.0)
      .setPersonId(2);
    // Manual measure
    Measure manual = new Measure<>(new Metric<>("manual_metric", ValueType.BOOL))
      .setValue(1.0);
    // Sqale rating have both a value and a data
    Measure rating = new Measure<>(CoreMetrics.SQALE_RATING)
      .setValue(2.0)
      .setData("A");
    // Long measure
    Measure longMeasure = new Measure<>(CoreMetrics.TECHNICAL_DEBT)
      .setValue(1.0);
    // String value
    Measure stringMeasure = new Measure<>(CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION)
      .setData("foo bar");
    when(measureCache.byResource(sampleFile)).thenReturn(asList(measure, manual, rating, longMeasure, stringMeasure));

    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);

    publisher.publish(writer);

    BatchReportReader reader = new BatchReportReader(outputDir);

    assertThat(reader.readComponentMeasures(1)).hasSize(0);
    try (CloseableIterator<BatchReport.Measure> componentMeasures = reader.readComponentMeasures(2)) {
      assertThat(componentMeasures).hasSize(5);
    }
  }

  @Test
  public void fail_with_IAE_when_measure_has_no_value() throws Exception {
    Measure measure = new Measure<>(CoreMetrics.COVERAGE);
    when(measureCache.byResource(sampleFile)).thenReturn(Collections.singletonList(measure));

    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);

    try {
      publisher.publish(writer);
      fail();
    } catch (RuntimeException e) {
      assertThat(ExceptionUtils.getFullStackTrace(e)).contains("Measure on metric 'coverage' and component 'foo:src/Foo.php' has no value, but it's not allowed");
    }
  }

}
