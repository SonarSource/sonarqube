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

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Coverage;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.scan.measure.MeasureCache;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoveragePublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MeasureCache measureCache;
  private CoveragePublisher publisher;

  private org.sonar.api.resources.Resource sampleFile;

  @Before
  public void prepare() {
    Project p = new Project("foo").setAnalysisDate(new Date(1234567L));
    ResourceCache resourceCache = new ResourceCache();
    sampleFile = org.sonar.api.resources.File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php");
    resourceCache.add(p, null).setSnapshot(new Snapshot().setId(2));
    resourceCache.add(sampleFile, null).setInputPath(new DefaultInputFile("foo", "src/Foo.php").setLines(5));
    measureCache = mock(MeasureCache.class);
    when(measureCache.byMetric(anyString(), anyString())).thenReturn(Collections.<Measure>emptyList());
    publisher = new CoveragePublisher(resourceCache, measureCache);
  }

  @Test
  public void publishCoverage() throws Exception {

    Measure utLineHits = new Measure<>(CoreMetrics.COVERAGE_LINE_HITS_DATA).setData("2=1;3=1;5=0;6=3");
    when(measureCache.byMetric("foo:src/Foo.php", CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY)).thenReturn(Arrays.asList(utLineHits));

    Measure conditionsByLine = new Measure<>(CoreMetrics.CONDITIONS_BY_LINE).setData("3=4");
    when(measureCache.byMetric("foo:src/Foo.php", CoreMetrics.CONDITIONS_BY_LINE_KEY)).thenReturn(Arrays.asList(conditionsByLine));

    Measure coveredConditionsByUts = new Measure<>(CoreMetrics.COVERED_CONDITIONS_BY_LINE).setData("3=2");
    when(measureCache.byMetric("foo:src/Foo.php", CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY)).thenReturn(Arrays.asList(coveredConditionsByUts));

    Measure itLineHits = new Measure<>(CoreMetrics.IT_COVERAGE_LINE_HITS_DATA).setData("2=0;3=0;5=1");
    when(measureCache.byMetric("foo:src/Foo.php", CoreMetrics.IT_COVERAGE_LINE_HITS_DATA_KEY)).thenReturn(Arrays.asList(itLineHits));

    Measure coveredConditionsByIts = new Measure<>(CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE).setData("3=1");
    when(measureCache.byMetric("foo:src/Foo.php", CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE_KEY)).thenReturn(Arrays.asList(coveredConditionsByIts));

    Measure overallCoveredConditions = new Measure<>(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE).setData("3=2");
    when(measureCache.byMetric("foo:src/Foo.php", CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE_KEY)).thenReturn(Arrays.asList(overallCoveredConditions));

    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);

    publisher.publish(writer);

    try (InputStream inputStream = FileUtils.openInputStream(new BatchReportReader(outputDir).readComponentCoverage(2))) {
      assertThat(BatchReport.Coverage.PARSER.parseDelimitedFrom(inputStream)).isEqualTo(Coverage.newBuilder()
        .setLine(2)
        .setUtHits(true)
        .setItHits(false)
        .build());
      assertThat(BatchReport.Coverage.PARSER.parseDelimitedFrom(inputStream)).isEqualTo(Coverage.newBuilder()
        .setLine(3)
        .setUtHits(true)
        .setItHits(false)
        .setConditions(4)
        .setUtCoveredConditions(2)
        .setItCoveredConditions(1)
        .setOverallCoveredConditions(2)
        .build());
      assertThat(BatchReport.Coverage.PARSER.parseDelimitedFrom(inputStream)).isEqualTo(Coverage.newBuilder()
        .setLine(5)
        .setUtHits(false)
        .setItHits(true)
        .build());

    }
  }

}
