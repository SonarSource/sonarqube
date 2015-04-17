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
package org.sonar.plugins.core.timemachine;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Changesets.Changeset;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.report.ReportPublisher;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NewCoverageFileAnalyzerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DecoratorContext context;
  private NewCoverageFileAnalyzer decorator;
  private BatchReportWriter writer;

  @Before
  public void prepare() throws Exception {
    context = mock(DecoratorContext.class);
    Resource f = File.create("src/Foo.java").setEffectiveKey("foo:src/Foo.java");
    when(context.getResource()).thenReturn(f);
    ResourceCache cache = new ResourceCache();
    cache.add(f, null);
    List<AbstractNewCoverageFileAnalyzer.PeriodStruct> structs = Arrays.asList(
      new AbstractNewCoverageFileAnalyzer.PeriodStruct(1, newDate("2009-12-25")),
      new AbstractNewCoverageFileAnalyzer.PeriodStruct(3, newDate("2011-02-18")));
    ReportPublisher publishReportJob = mock(ReportPublisher.class);
    java.io.File reportBaseDir = temp.newFolder();
    when(publishReportJob.getReportDir()).thenReturn(reportBaseDir);
    writer = new BatchReportWriter(reportBaseDir);
    decorator = new NewCoverageFileAnalyzer(structs, publishReportJob, cache);

  }

  @Test
  public void shouldDoNothingIfNoScmData() throws ParseException {
    when(context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA))
      .thenReturn(new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "1=10"));

    decorator.doDecorate(context);
    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void shouldDoNothingIfNoCoverageData() throws ParseException {
    writer.writeComponentChangesets(BatchReport.Changesets.newBuilder()
      .setComponentRef(1)
      .addChangeset(Changeset.newBuilder()
        .setDate(DateUtils.parseDateTime("2008-05-18T00:00:00+0000").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .build());

    decorator.doDecorate(context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void shouldGetNewLines() throws ParseException {
    when(context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)).thenReturn(
      new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "10=2;11=3"));
    writer.writeComponentChangesets(BatchReport.Changesets.newBuilder()
      .setComponentRef(1)
      .addChangeset(Changeset.newBuilder()
        .build())
      .addChangeset(Changeset.newBuilder()
        .setDate(DateUtils.parseDateTime("2007-01-15T00:00:00+0000").getTime())
        .build())
      .addChangeset(Changeset.newBuilder()
        .setDate(DateUtils.parseDateTime("2011-01-01T00:00:00+0000").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(2)
      .build());

    decorator.doDecorate(context);

    // line 11 has been updated after date1 (2009-12-25). This line is covered.
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_LINES_TO_COVER, 1, 1.0)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_LINES, 1, 0.0)));

    // no line have been updated after date3 (2011-02-18)
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_LINES_TO_COVER, 3, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_LINES, 3, null)));

    // no data on other periods
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_LINES_TO_COVER, 2, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_LINES_TO_COVER, 4, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_LINES_TO_COVER, 5, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_LINES, 2, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_LINES, 4, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_LINES, 5, null)));
  }

  @Test
  public void shouldGetNewConditions() throws ParseException {
    when(context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)).thenReturn(
      new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "10=2;11=3"));
    when(context.getMeasure(CoreMetrics.CONDITIONS_BY_LINE)).thenReturn(
      new Measure(CoreMetrics.CONDITIONS_BY_LINE, "11=4"));
    when(context.getMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE)).thenReturn(
      new Measure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "11=1"));
    writer.writeComponentChangesets(BatchReport.Changesets.newBuilder()
      .setComponentRef(1)
      .addChangeset(Changeset.newBuilder()
        .build())
      .addChangeset(Changeset.newBuilder()
        .setDate(DateUtils.parseDateTime("2007-01-15T00:00:00+0000").getTime())
        .build())
      .addChangeset(Changeset.newBuilder()
        .setDate(DateUtils.parseDateTime("2011-01-01T00:00:00+0000").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(2)
      .build());

    decorator.doDecorate(context);

    // line 11 has been updated after date1 (2009-12-25). This line has 1 covered condition amongst 4
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_CONDITIONS_TO_COVER, 1, 4.0)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_CONDITIONS, 1, 3.0)));

    // no line have been updated after date3 (2011-02-18)
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_CONDITIONS_TO_COVER, 3, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_CONDITIONS, 3, null)));

    // no data on other periods
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_CONDITIONS_TO_COVER, 2, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_CONDITIONS_TO_COVER, 4, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_CONDITIONS_TO_COVER, 5, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_CONDITIONS, 2, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_CONDITIONS, 4, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_CONDITIONS, 5, null)));
  }

  @Test
  public void shouldNotGetNewConditionsWhenNewLineHasNoConditions() throws ParseException {
    when(context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)).thenReturn(
      new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "10=2;11=3"));
    when(context.getMeasure(CoreMetrics.CONDITIONS_BY_LINE)).thenReturn(
      new Measure(CoreMetrics.CONDITIONS_BY_LINE, "10=1"));
    when(context.getMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE)).thenReturn(
      new Measure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "10=1"));
    writer.writeComponentChangesets(BatchReport.Changesets.newBuilder()
      .setComponentRef(1)
      .addChangeset(Changeset.newBuilder()
        .build())
      .addChangeset(Changeset.newBuilder()
        .setDate(DateUtils.parseDateTime("2007-01-15T00:00:00+0000").getTime())
        .build())
      .addChangeset(Changeset.newBuilder()
        .setDate(DateUtils.parseDateTime("2011-01-01T00:00:00+0000").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .addChangesetIndexByLine(2)
      .build());

    decorator.doDecorate(context);

    // line 11 has been updated after date1 (2009-12-25) but it has no conditions
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_CONDITIONS_TO_COVER, 1, 0.0)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_CONDITIONS, 1, 0.0)));
  }

  @Test
  public void shouldLeaveNullValueWhenNothingHasChanged() throws Exception {

    when(context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)).thenReturn(
      new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "2=1;3=1"));
    when(context.getMeasure(CoreMetrics.CONDITIONS_BY_LINE)).thenReturn(
      new Measure(CoreMetrics.CONDITIONS_BY_LINE, "2=1"));
    when(context.getMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE)).thenReturn(
      new Measure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "2=1"));
    writer.writeComponentChangesets(BatchReport.Changesets.newBuilder()
      .setComponentRef(1)
      .addChangeset(Changeset.newBuilder()
        .setDate(DateUtils.parseDateTime("2008-08-02T13:56:37+0200").getTime())
        .build())
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .build());

    decorator.doDecorate(context);

    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_LINES_TO_COVER, 1, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_LINES, 1, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_CONDITIONS_TO_COVER, 1, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_CONDITIONS, 1, null)));

    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_LINES_TO_COVER, 3, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_LINES, 3, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_CONDITIONS_TO_COVER, 3, null)));
    verify(context).saveMeasure(argThat(new VariationMatcher(CoreMetrics.NEW_UNCOVERED_CONDITIONS, 3, null)));
  }

  static class VariationMatcher extends ArgumentMatcher<Measure> {
    Metric metric;
    int index;
    Double variation;

    VariationMatcher(Metric metric, int index, Double variation) {
      this.metric = metric;
      this.index = index;
      this.variation = variation;
    }

    @Override
    public boolean matches(Object o) {
      Measure m = (Measure) o;
      if (m.getMetric().equals(metric)) {
        if ((variation == null && m.getVariation(index) == null) ||
          (variation != null && variation.equals(m.getVariation(index)))) {
          return true;
        }
      }
      return false;
    }
  }

  private Date newDate(String s) throws ParseException {
    return new SimpleDateFormat(DateUtils.DATE_FORMAT).parse(s);
  }
}
