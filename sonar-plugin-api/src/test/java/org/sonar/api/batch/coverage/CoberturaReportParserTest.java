/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.batch.coverage;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.test.IsResource;

import java.io.File;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoberturaReportParserTest {

  private SensorContext context;
  private ModuleFileSystem fs;
  private CoberturaReportParser parser;

  @Before
  public void setUp() {
    context = mock(SensorContext.class);
    fs = mock(ModuleFileSystem.class);
    parser = new CoberturaReportParser(fs);
  }

  @Test
  public void collectFileLineCoverage() throws URISyntaxException {
    InputFile inputFile = mock(InputFile.class);
    when(fs.inputFile(eq(new File("/Users/simon/projects/commons-chain/src/java", "org/apache/commons/chain/config/ConfigParser.java")))).thenReturn(inputFile);
    parser.parseReport(getCoverageReport(), context);

    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 30.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 5.0)));
  }

  @Test
  public void collectFileBranchCoverage() throws URISyntaxException {
    InputFile inputFile = mock(InputFile.class);
    when(fs.inputFile(eq(new File("/Users/simon/projects/commons-chain/src/java", "org/apache/commons/chain/config/ConfigParser.java")))).thenReturn(inputFile);
    parser.parseReport(getCoverageReport(), context);

    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER, 6.0)));
    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS, 2.0)));
  }

  @Test
  public void testDoNotSaveMeasureOnResourceWhichDoesntExistInTheFileSystem() throws URISyntaxException {
    when(fs.inputFile(any(File.class))).thenReturn(null);
    parser.parseReport(getCoverageReport(), context);
    verify(context, never()).saveMeasure(any(InputFile.class), any(Measure.class));
  }

  @Test
  public void javaInterfaceHasNoCoverage() throws URISyntaxException {
    InputFile inputFile = mock(InputFile.class);
    when(fs.inputFile(eq(new File("/Users/simon/projects/commons-chain/src/java", "org/apache/commons/chain/Chain.java")))).thenReturn(inputFile);
    parser.parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.COVERAGE)));

    verify(context, never()).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.LINE_COVERAGE)));
    verify(context, never()).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER)));
    verify(context, never()).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES)));

    verify(context, never()).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.BRANCH_COVERAGE)));
    verify(context, never()).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER)));
    verify(context, never()).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS)));
  }

  @Test
  public void shouldInsertCoverageAtFileLevel() throws URISyntaxException {
    File coverage = new File(getClass().getResource(
      "/org/sonar/api/utils/CoberturaReportParserUtilsTest/shouldInsertCoverageAtFileLevel/coverage.xml").toURI());

    InputFile innerClass = mock(InputFile.class);
    when(fs.inputFile(eq(new File("/Users/simon/projects/sonar/trunk/tests/integration/reference-projects/reference/src/main/java",
      "org/sonar/samples/InnerClass.java")))).thenReturn(innerClass);
    parser.parseReport(coverage, context);

    verify(context).saveMeasure(eq(innerClass), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 35.0)));
    verify(context).saveMeasure(eq(innerClass), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 22.0)));

    verify(context).saveMeasure(eq(innerClass), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER, 4.0)));
    verify(context).saveMeasure(eq(innerClass), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS, 3.0)));

    verify(context, never()).saveMeasure(
      argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, "org.sonar.samples.InnerClass$InnerClassInside")),
      argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER)));
    verify(context, never()).saveMeasure(
      argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, "org.sonar.samples.InnerClass$InnerClassInside")),
      argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER)));
    verify(context, never()).saveMeasure(
      argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, "org.sonar.samples.InnerClass$InnerClassInside")),
      argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS)));
    verify(context, never()).saveMeasure(
      argThat(new IsResource(Scopes.FILE, Qualifiers.FILE, "org.sonar.samples.InnerClass$InnerClassInside")),
      argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES)));

    verify(fs, never()).inputFile(eq(new File("/Users/simon/projects/sonar/trunk/tests/integration/reference-projects/reference/src/main/java",
      "org/sonar/samples/PrivateClass.java")));

    verify(context)
      .saveMeasure(
        eq(innerClass),
        argThat(new IsMeasure(
          CoreMetrics.COVERAGE_LINE_HITS_DATA,
          "22=2;25=0;26=0;29=0;30=0;31=0;34=1;35=1;36=1;37=0;39=1;41=1;44=2;46=1;47=1;50=0;51=0;52=0;53=0;55=0;57=0;60=0;61=0;64=1;71=1;73=1;76=0;77=0;80=0;81=0;85=0;87=0;91=0;93=0;96=1")));
  }

  @Test
  public void collectFileLineHitsData() throws URISyntaxException {
    InputFile inputFile = mock(InputFile.class);
    when(fs.inputFile(eq(new File("/Users/simon/projects/commons-chain/src/java", "org/apache/commons/chain/impl/CatalogBase.java")))).thenReturn(inputFile);
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));
    parser.parseReport(getCoverageReport(), context);
    verify(context).saveMeasure(
      eq(inputFile),
      argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA,
        "48=117;56=234;66=0;67=0;68=0;84=999;86=999;98=318;111=18;121=0;122=0;125=0;126=0;127=0;128=0;131=0;133=0")));
  }

  @Test
  public void shouldNotCountTwiceAnonymousClasses() throws URISyntaxException {
    File coverage = new File(getClass().getResource("/org/sonar/api/utils/CoberturaReportParserUtilsTest/shouldNotCountTwiceAnonymousClasses.xml").toURI());
    InputFile inputFile = mock(InputFile.class);
    when(fs.inputFile(eq(new File("/Users/simon/projects/sonar/trunk/tests/integration/reference-projects/reference/src/main/java",
      "org/sonar/samples/MyFile.java")))).thenReturn(inputFile);
    parser.parseReport(coverage, context);

    verify(context).saveMeasure(eq(inputFile), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 5.0))); // do not count line 26 twice
  }

  private File getCoverageReport() throws URISyntaxException {
    return new File(getClass().getResource("/org/sonar/api/utils/CoberturaReportParserUtilsTest/commons-chain-coverage.xml").toURI());
  }
}
