/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.clover;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.IsMeasure;
import org.sonar.test.TestUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.text.ParseException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class XmlReportParserTest {

  private XmlReportParser reportParser;
  private SensorContext context;
  private File xmlFile;

  @Before
  public void before() throws URISyntaxException {
    xmlFile = TestUtils.getResource(getClass(), "clover.xml");
    context = mock(SensorContext.class);
    reportParser = new XmlReportParser(context);
  }

  @Test
  public void collectProjectMeasures() throws Exception {
    reportParser.collect(xmlFile);
    verify(context).saveMeasure(null, CoreMetrics.COVERAGE, 5.0); // coveredelements / elements

    verify(context).saveMeasure(null, CoreMetrics.LINE_COVERAGE, 6.63); // covered methods + covered statements / methods + statements
    verify(context).saveMeasure(null, CoreMetrics.LINES_TO_COVER, 196.0);
    verify(context).saveMeasure(null, CoreMetrics.UNCOVERED_LINES, 183.0); // covered methods + covered statements

    verify(context).saveMeasure(null, CoreMetrics.BRANCH_COVERAGE, 0.0); // covered conditionals / conditionals
    verify(context).saveMeasure(null, CoreMetrics.CONDITIONS_TO_COVER, 64.0); // covered_conditionals
    verify(context).saveMeasure(null, CoreMetrics.UNCOVERED_CONDITIONS, 64.0);
  }

  @Test
  public void collectPackageMeasures() throws ParseException {
    reportParser.collect(xmlFile);
    final JavaPackage pac = new JavaPackage("org.sonar.samples");
    verify(context).saveMeasure(pac, CoreMetrics.COVERAGE, 28.89);

    // lines
    verify(context).saveMeasure(pac, CoreMetrics.LINE_COVERAGE, 28.89);
    verify(context).saveMeasure(pac, CoreMetrics.LINES_TO_COVER, 45.0);
    verify(context).saveMeasure(pac, CoreMetrics.UNCOVERED_LINES, 32.0);

    // no conditions
    verify(context, never()).saveMeasure(eq(pac), eq(CoreMetrics.BRANCH_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure(eq(pac), eq(CoreMetrics.CONDITIONS_TO_COVER), anyDouble());
    verify(context, never()).saveMeasure(eq(pac), eq(CoreMetrics.UNCOVERED_CONDITIONS), anyDouble());
  }

  @Test
  public void parseClaver232Format() throws ParseException, URISyntaxException {
    reportParser.collect(TestUtils.getResource(getClass(), "clover_2_3_2.xml"));
    verify(context).saveMeasure(new JavaPackage("org.sonar.squid.sensors"), CoreMetrics.COVERAGE, 94.87);
  }

//  @Test
//  public void doNotSaveInnerClassMeasures() throws ParseException, URISyntaxException {
//    collector.collect(xmlFile);
//
//    verify(context, never()).saveMeasure(
//        eq(new JavaFile("ch.hortis.sonar.model.MetricMetaInf.Classes")), eq(CoreMetrics.COVERAGE), anyDouble());
//    verify(context).saveMeasure(
//        eq(new JavaFile("ch.hortis.sonar.model.MetricMetaInf")), eq(CoreMetrics.COVERAGE), anyDouble());
//  }

  @Test
  public void collectFileMeasures() throws Exception {
    reportParser.collect(xmlFile);

    final JavaFile file = new JavaFile("org.sonar.samples.ClassUnderTest");
    verify(context).saveMeasure(file, CoreMetrics.COVERAGE, 100.0);

    verify(context).saveMeasure(file, CoreMetrics.LINE_COVERAGE, 100.0);
    verify(context).saveMeasure(file, CoreMetrics.LINES_TO_COVER, 5.0);
    verify(context).saveMeasure(file, CoreMetrics.UNCOVERED_LINES, 0.0);

    // no conditions
    verify(context, never()).saveMeasure(eq(file), eq(CoreMetrics.BRANCH_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure(eq(file), eq(CoreMetrics.CONDITIONS_TO_COVER), anyDouble());
    verify(context, never()).saveMeasure(eq(file), eq(CoreMetrics.UNCOVERED_CONDITIONS), anyDouble());
  }

  @Test
  public void collectFileHitsData() throws Exception {
    reportParser.collect(xmlFile);
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.samples.ClassUnderTest")), argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "4=1;5=1;6=1;8=1;9=1")));
  }

  @Test
  public void clover1FileNameContainsPath() {
    XmlReportParser reportParser = new XmlReportParser(context);
    assertEquals("SampleClass", reportParser.extractClassName("C:\\src\\main\\java\\org\\sonar\\samples\\SampleClass.java"));

    assertEquals("SampleClass", reportParser.extractClassName("/src/main/java/org/sonar/samples/SampleClass.java"));
  }

  @Test
  public void clover2FileNameDoesNotContainPath() {
    XmlReportParser reportParser = new XmlReportParser(context);
    assertEquals("SampleClass", reportParser.extractClassName("SampleClass.java"));
  }

  @Test
  public void coverageShouldBeZeroWhenNoElements() throws URISyntaxException {
    File xmlFile = TestUtils.getResource(getClass(), "coverageShouldBeZeroWhenNoElements/clover.xml");
    context = mock(SensorContext.class);
    XmlReportParser reportParser = new XmlReportParser(context);
    reportParser.collect(xmlFile);
    verify(context, never()).saveMeasure((Resource) anyObject(), eq(CoreMetrics.COVERAGE), anyDouble());
    verify(context, never()).saveMeasure((Resource) anyObject(), eq(CoreMetrics.LINE_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure((Resource) anyObject(), eq(CoreMetrics.BRANCH_COVERAGE), anyDouble());
  }
}
