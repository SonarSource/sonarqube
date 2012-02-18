/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.cpd;

import net.sourceforge.pmd.cpd.TokenEntry;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.duplications.cpd.Match;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class CpdAnalyserTest {

  @Test
  public void testOneSimpleDuplicationBetweenTwoFiles() throws Exception {
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getSourceDirs()).thenReturn(Collections.<File> emptyList());
    File file1 = new File("target/tmp/file1.ext");
    File file2 = new File("target/tmp/file2.ext");

    Project project = new Project("key").setFileSystem(fileSystem);

    SensorContext context = mock(SensorContext.class);

    CpdMapping cpdMapping = mock(CpdMapping.class);
    Resource resource1 = new JavaFile("foo.Foo");
    Resource resource2 = new JavaFile("foo.Bar");
    when(cpdMapping.createResource((File) anyObject(), anyList())).thenReturn(resource1).thenReturn(resource2).thenReturn(resource2)
        .thenReturn(resource1);

    Match match1 = new Match(5, new TokenEntry(null, file1.getAbsolutePath(), 5), new TokenEntry(null, file2.getAbsolutePath(), 15));
    match1.setLineCount(200);

    CpdAnalyser cpdAnalyser = new CpdAnalyser(project, context, cpdMapping);
    cpdAnalyser.analyse(Arrays.asList(match1).iterator());

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);

    verify(context).saveMeasure(resource1, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource1, CoreMetrics.DUPLICATED_BLOCKS, 1d);
    verify(context).saveMeasure(resource1, CoreMetrics.DUPLICATED_LINES, 200d);
    verify(context).saveMeasure(eq(resource1), measureCaptor.capture());
    Measure measure = measureCaptor.getValue();
    assertThat(measure.getMetric(), is(CoreMetrics.DUPLICATIONS_DATA));
    assertThat(measure.getData(), is("<duplications><g>"
      + "<b s=\"5\" l=\"200\" r=\"key:foo.Foo\" />"
      + "<b s=\"15\" l=\"200\" r=\"key:foo.Bar\" />"
      + "</g></duplications>"));

    verify(context).saveMeasure(resource2, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource2, CoreMetrics.DUPLICATED_LINES, 200d);
    verify(context).saveMeasure(resource2, CoreMetrics.DUPLICATED_BLOCKS, 1d);
    verify(context).saveMeasure(eq(resource2), measureCaptor.capture());
    assertThat(measure.getMetric(), is(CoreMetrics.DUPLICATIONS_DATA));
    assertThat(measure.getData(), is("<duplications><g>"
      + "<b s=\"5\" l=\"200\" r=\"key:foo.Foo\" />"
      + "<b s=\"15\" l=\"200\" r=\"key:foo.Bar\" />"
      + "</g></duplications>"));
  }

  @Test
  public void testClassicalCaseWithTwoDuplicatedBlocsInvolvingThreeFiles() throws Exception {
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getSourceDirs()).thenReturn(Collections.<File> emptyList());
    File file1 = new File("target/tmp/file1.ext");
    File file2 = new File("target/tmp/file2.ext");
    File file3 = new File("target/tmp/file3.ext");

    Project project = new Project("key").setFileSystem(fileSystem);

    SensorContext context = mock(SensorContext.class);

    CpdMapping cpdMapping = mock(CpdMapping.class);
    Resource resource1 = new JavaFile("foo.Foo");
    Resource resource2 = new JavaFile("foo.Bar");
    Resource resource3 = new JavaFile("foo.Hotel");
    when(cpdMapping.createResource((File) anyObject(), anyList())).thenReturn(resource1).thenReturn(resource2).thenReturn(resource2)
        .thenReturn(resource1).thenReturn(resource1).thenReturn(resource3).thenReturn(resource3).thenReturn(resource1);

    Match match1 = new Match(5, new TokenEntry(null, file1.getAbsolutePath(), 5), new TokenEntry(null, file2.getAbsolutePath(), 15));
    match1.setLineCount(200);
    Match match2 = new Match(5, new TokenEntry(null, file1.getAbsolutePath(), 5), new TokenEntry(null, file3.getAbsolutePath(), 15));
    match2.setLineCount(100);

    CpdAnalyser cpdAnalyser = new CpdAnalyser(project, context, cpdMapping);
    cpdAnalyser.analyse(Arrays.asList(match1, match2).iterator());

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);

    verify(context).saveMeasure(resource1, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource1, CoreMetrics.DUPLICATED_BLOCKS, 2d);
    verify(context).saveMeasure(resource1, CoreMetrics.DUPLICATED_LINES, 200d);
    verify(context).saveMeasure(eq(resource1), measureCaptor.capture());

    Measure measure = measureCaptor.getValue();
    assertThat(measure.getMetric(), is(CoreMetrics.DUPLICATIONS_DATA));
    assertThat(measure.getData(), is("<duplications>"
      + "<g>"
      + "<b s=\"5\" l=\"100\" r=\"key:foo.Foo\" />"
      + "<b s=\"15\" l=\"100\" r=\"key:foo.Hotel\" />"
      + "</g>"
      + "<g>"
      + "<b s=\"5\" l=\"200\" r=\"key:foo.Foo\" />"
      + "<b s=\"15\" l=\"200\" r=\"key:foo.Bar\" />"
      + "</g>"
      + "</duplications>"));

    verify(context).saveMeasure(resource2, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource2, CoreMetrics.DUPLICATED_LINES, 200d);
    verify(context).saveMeasure(resource2, CoreMetrics.DUPLICATED_BLOCKS, 1d);
    verify(context).saveMeasure(eq(resource2), measureCaptor.capture());

    measure = measureCaptor.getValue();
    assertThat(measure.getMetric(), is(CoreMetrics.DUPLICATIONS_DATA));
    assertThat(measure.getData(), is("<duplications>"
      + "<g>"
      + "<b s=\"15\" l=\"200\" r=\"key:foo.Bar\" />"
      + "<b s=\"5\" l=\"200\" r=\"key:foo.Foo\" />"
      + "</g>"
      + "</duplications>"));

    verify(context).saveMeasure(resource3, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource3, CoreMetrics.DUPLICATED_LINES, 100d);
    verify(context).saveMeasure(resource3, CoreMetrics.DUPLICATED_BLOCKS, 1d);
    verify(context).saveMeasure(eq(resource3), measureCaptor.capture());

    measure = measureCaptor.getValue();
    assertThat(measure.getMetric(), is(CoreMetrics.DUPLICATIONS_DATA));
    assertThat(measure.getData(), is("<duplications>"
      + "<g>"
      + "<b s=\"15\" l=\"100\" r=\"key:foo.Hotel\" />"
      + "<b s=\"5\" l=\"100\" r=\"key:foo.Foo\" />"
      + "</g>"
      + "</duplications>"));
  }

  @Test
  public void testOneDuplicatedBlocInvolvingMoreThanTwoFiles() throws Exception {
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getSourceDirs()).thenReturn(Collections.<File> emptyList());
    File file1 = new File("target/tmp/file1.ext");
    File file2 = new File("target/tmp/file2.ext");
    File file3 = new File("target/tmp/file3.ext");
    File file4 = new File("target/tmp/file4.ext");

    Project project = new Project("key").setFileSystem(fileSystem);

    SensorContext context = mock(SensorContext.class);

    CpdMapping cpdMapping = mock(CpdMapping.class);
    Resource resource1 = new JavaFile("foo.Foo");
    Resource resource2 = new JavaFile("foo.Bar");
    Resource resource3 = new JavaFile("foo.Hotel");
    Resource resource4 = new JavaFile("foo.Coffee");
    when(cpdMapping.createResource((File) anyObject(), anyList())).thenReturn(resource1).thenReturn(resource2).thenReturn(resource3)
        .thenReturn(resource4).thenReturn(resource2).thenReturn(resource1).thenReturn(resource3).thenReturn(resource4)
        .thenReturn(resource3).thenReturn(resource1).thenReturn(resource2).thenReturn(resource4).thenReturn(resource4)
        .thenReturn(resource1).thenReturn(resource2).thenReturn(resource3);

    Match match = new Match(5, createTokenEntry(file1.getAbsolutePath(), 5), createTokenEntry(file2.getAbsolutePath(), 15));
    match.setLineCount(200);
    Set<TokenEntry> tokenEntries = new LinkedHashSet<TokenEntry>();
    tokenEntries.add(createTokenEntry(file1.getAbsolutePath(), 5));
    tokenEntries.add(createTokenEntry(file2.getAbsolutePath(), 15));
    tokenEntries.add(createTokenEntry(file3.getAbsolutePath(), 7));
    tokenEntries.add(createTokenEntry(file4.getAbsolutePath(), 10));
    match.setMarkSet(tokenEntries);

    CpdAnalyser cpdAnalyser = new CpdAnalyser(project, context, cpdMapping);
    cpdAnalyser.analyse(Arrays.asList(match).iterator());

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);

    verify(context).saveMeasure(resource1, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource1, CoreMetrics.DUPLICATED_BLOCKS, 1d);
    verify(context).saveMeasure(resource1, CoreMetrics.DUPLICATED_LINES, 200d);
    verify(context).saveMeasure(eq(resource1), measureCaptor.capture());

    Measure measure = measureCaptor.getValue();
    assertThat(measure.getMetric(), is(CoreMetrics.DUPLICATIONS_DATA));
    assertThat(measure.getData(), is("<duplications>"
      + "<g>"
      + "<b s=\"5\" l=\"200\" r=\"key:foo.Foo\" />"
      + "<b s=\"15\" l=\"200\" r=\"key:foo.Bar\" />"
      + "</g>"
      + "<g>"
      + "<b s=\"5\" l=\"200\" r=\"key:foo.Foo\" />"
      + "<b s=\"7\" l=\"200\" r=\"key:foo.Hotel\" />"
      + "</g>"
      + "<g>"
      + "<b s=\"5\" l=\"200\" r=\"key:foo.Foo\" />"
      + "<b s=\"10\" l=\"200\" r=\"key:foo.Coffee\" />"
      + "</g>"
      + "</duplications>"));

    verify(context).saveMeasure(resource3, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource3, CoreMetrics.DUPLICATED_LINES, 200d);
    verify(context).saveMeasure(resource3, CoreMetrics.DUPLICATED_BLOCKS, 1d);
    verify(context).saveMeasure(eq(resource3), measureCaptor.capture());

    measure = measureCaptor.getValue();
    assertThat(measure.getMetric(), is(CoreMetrics.DUPLICATIONS_DATA));
    assertThat(measure.getData(), is("<duplications>"
      + "<g>"
      + "<b s=\"7\" l=\"200\" r=\"key:foo.Hotel\" />"
      + "<b s=\"5\" l=\"200\" r=\"key:foo.Foo\" />"
      + "</g>"
      + "<g>"
      + "<b s=\"7\" l=\"200\" r=\"key:foo.Hotel\" />"
      + "<b s=\"15\" l=\"200\" r=\"key:foo.Bar\" />"
      + "</g>"
      + "<g>"
      + "<b s=\"7\" l=\"200\" r=\"key:foo.Hotel\" />"
      + "<b s=\"10\" l=\"200\" r=\"key:foo.Coffee\" />"
      + "</g>"
      + "</duplications>"));

    verify(context).saveMeasure(resource2, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource2, CoreMetrics.DUPLICATED_LINES, 200d);
    verify(context).saveMeasure(resource2, CoreMetrics.DUPLICATED_BLOCKS, 1d);
    verify(context).saveMeasure(eq(resource2), measureCaptor.capture());

    measure = measureCaptor.getValue();
    assertThat(measure.getMetric(), is(CoreMetrics.DUPLICATIONS_DATA));
    assertThat(measure.getData(), is("<duplications>"
      + "<g>"
      + "<b s=\"15\" l=\"200\" r=\"key:foo.Bar\" />"
      + "<b s=\"5\" l=\"200\" r=\"key:foo.Foo\" />"
      + "</g>"
      + "<g>"
      + "<b s=\"15\" l=\"200\" r=\"key:foo.Bar\" />"
      + "<b s=\"7\" l=\"200\" r=\"key:foo.Hotel\" />"
      + "</g>"
      + "<g>"
      + "<b s=\"15\" l=\"200\" r=\"key:foo.Bar\" />"
      + "<b s=\"10\" l=\"200\" r=\"key:foo.Coffee\" />"
      + "</g>"
      + "</duplications>"));

    verify(context).saveMeasure(resource4, CoreMetrics.DUPLICATED_LINES, 200d);
    verify(context).saveMeasure(resource4, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource4, CoreMetrics.DUPLICATED_BLOCKS, 1d);
    verify(context).saveMeasure(eq(resource4), measureCaptor.capture());

    measure = measureCaptor.getValue();
    assertThat(measure.getMetric(), is(CoreMetrics.DUPLICATIONS_DATA));
    assertThat(measure.getData(), is("<duplications>"
      + "<g>"
      + "<b s=\"10\" l=\"200\" r=\"key:foo.Coffee\" />"
      + "<b s=\"5\" l=\"200\" r=\"key:foo.Foo\" />"
      + "</g>"
      + "<g>"
      + "<b s=\"10\" l=\"200\" r=\"key:foo.Coffee\" />"
      + "<b s=\"15\" l=\"200\" r=\"key:foo.Bar\" />"
      + "</g>"
      + "<g>"
      + "<b s=\"10\" l=\"200\" r=\"key:foo.Coffee\" />"
      + "<b s=\"7\" l=\"200\" r=\"key:foo.Hotel\" />"
      + "</g>"
      + "</duplications>"));
  }

  @Test
  public void testDuplicationOnSameFile() throws Exception {
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getSourceDirs()).thenReturn(Collections.<File> emptyList());
    File file1 = new File("target/tmp/file1.ext");

    Project project = new Project("key").setFileSystem(fileSystem);

    SensorContext context = mock(SensorContext.class);

    CpdMapping cpdMapping = mock(CpdMapping.class);
    Resource resource1 = new JavaFile("foo.Foo");
    when(cpdMapping.createResource((File) anyObject(), anyList())).thenReturn(resource1).thenReturn(resource1);

    Match match1 = new Match(304, new TokenEntry(null, file1.getAbsolutePath(), 5), new TokenEntry(null, file1.getAbsolutePath(), 215));
    match1.setLineCount(200);

    CpdAnalyser cpdAnalyser = new CpdAnalyser(project, context, cpdMapping);
    cpdAnalyser.analyse(Arrays.asList(match1).iterator());

    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);

    verify(context).saveMeasure(resource1, CoreMetrics.DUPLICATED_FILES, 1d);
    verify(context).saveMeasure(resource1, CoreMetrics.DUPLICATED_LINES, 400d);
    verify(context).saveMeasure(resource1, CoreMetrics.DUPLICATED_BLOCKS, 2d);
    verify(context).saveMeasure(eq(resource1), measureCaptor.capture());

    Measure measure = measureCaptor.getValue();
    assertThat(measure.getMetric(), is(CoreMetrics.DUPLICATIONS_DATA));
    // FIXME in fact should be only one group - see SONAR-3131
    assertThat(measure.getData(), is("<duplications>"
      + "<g>"
      + "<b s=\"5\" l=\"200\" r=\"key:foo.Foo\" />"
      + "<b s=\"215\" l=\"200\" r=\"key:foo.Foo\" />"
      + "</g>"
      + "<g>"
      + "<b s=\"215\" l=\"200\" r=\"key:foo.Foo\" />"
      + "<b s=\"5\" l=\"200\" r=\"key:foo.Foo\" />"
      + "</g>"
      + "</duplications>"));
  }

  private static TokenEntry createTokenEntry(String sourceId, int line) {
    TokenEntry entry = new TokenEntry(null, sourceId, line);
    entry.setHashCode(sourceId.hashCode() + line);
    return entry;
  }
}
