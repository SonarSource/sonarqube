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
package org.sonar.plugins.surefire;

import org.apache.commons.lang.ObjectUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.*;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.test.IsResource;
import org.sonar.api.test.MavenTestUtils;

import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class SurefireSensorTest {

  @Test
  public void shouldNotAnalyseIfStaticAnalysis() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Java.KEY);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertFalse(new SurefireSensor().shouldExecuteOnProject(project));
  }

  @Test
  public void shouldAnalyseIfReuseDynamicReports() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Java.KEY);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(new SurefireSensor().shouldExecuteOnProject(project), is(true));
  }

  @Test
  public void shouldNotFailIfReportsNotFound() {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "shouldNotFailIfReportsNotFound/pom.xml");
    new SurefireSensor().collect(project, mock(SensorContext.class), new File("unknown"));
  }


  private SensorContext mockContext() {
    SensorContext context = mock(SensorContext.class);
    when(context.isIndexed(any(Resource.class), eq(false))).thenReturn(true);
    return context;
  }

  @Test
  public void shouldHandleTestSuiteDetails() throws URISyntaxException {
    SensorContext context = mockContext();
    new SurefireSensor().collect(newJarProject(), context, new File(getClass().getResource(
        "/org/sonar/plugins/surefire/SurefireSensorTest/shouldHandleTestSuiteDetails/").toURI()));

    // 3 classes, 6 measures by class
    verify(context, times(3)).saveMeasure(argThat(new IsResource(JavaFile.SCOPE_ENTITY, Qualifiers.UNIT_TEST_FILE)),
        eq(CoreMetrics.SKIPPED_TESTS), anyDouble());
    verify(context, times(3)).saveMeasure(argThat(new IsResource(JavaFile.SCOPE_ENTITY, Qualifiers.UNIT_TEST_FILE)),
        eq(CoreMetrics.TESTS), anyDouble());
    verify(context, times(18)).saveMeasure(argThat(new IsResource(JavaFile.SCOPE_ENTITY, Qualifiers.UNIT_TEST_FILE)),
        (Metric) anyObject(), anyDouble());
    verify(context, times(3)).saveMeasure(argThat(new IsResource(JavaFile.SCOPE_ENTITY, Qualifiers.UNIT_TEST_FILE)),
        argThat(new IsMeasure(CoreMetrics.TEST_DATA)));

    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest", true)), eq(CoreMetrics.TESTS), eq(4d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest", true)), eq(CoreMetrics.TEST_EXECUTION_TIME),
        eq(111d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest", true)), eq(CoreMetrics.TEST_FAILURES), eq(1d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest", true)), eq(CoreMetrics.TEST_ERRORS), eq(1d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest", true)), eq(CoreMetrics.SKIPPED_TESTS), eq(0d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest", true)),
        argThat(getTestDetailsMatcher("shouldHandleTestSuiteDetails/ExtensionsFinderTest-expected-result.xml")));

    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest2", true)), eq(CoreMetrics.TESTS), eq(2d));
    verify(context)
        .saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest2", true)), eq(CoreMetrics.TEST_EXECUTION_TIME), eq(2d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest2", true)), eq(CoreMetrics.TEST_FAILURES), eq(0d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest2", true)), eq(CoreMetrics.TEST_ERRORS), eq(0d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest2", true)), eq(CoreMetrics.SKIPPED_TESTS), eq(0d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest2", true)),
        argThat(getTestDetailsMatcher("shouldHandleTestSuiteDetails/ExtensionsFinderTest2-expected-result.xml")));

    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest3", true)), eq(CoreMetrics.TESTS), eq(1d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest3", true)), eq(CoreMetrics.TEST_EXECUTION_TIME),
        eq(16d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest3", true)), eq(CoreMetrics.TEST_FAILURES), eq(0d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest3", true)), eq(CoreMetrics.TEST_ERRORS), eq(0d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest3", true)), eq(CoreMetrics.SKIPPED_TESTS), eq(1d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest3", true)),
        argThat(getTestDetailsMatcher("shouldHandleTestSuiteDetails/ExtensionsFinderTest3-expected-result.xml")));
  }

  @Test
  public void shouldSaveErrorsAndFailuresInXML() throws URISyntaxException {
    SensorContext context = mockContext();
    new SurefireSensor().collect(newJarProject(), context, new File(getClass().getResource(
        "/org/sonar/plugins/surefire/SurefireSensorTest/shouldSaveErrorsAndFailuresInXML/").toURI()));

    // 1 classes, 6 measures by class
    verify(context, times(1)).saveMeasure(argThat(new IsResource(JavaFile.SCOPE_ENTITY, Qualifiers.UNIT_TEST_FILE)),
        eq(CoreMetrics.SKIPPED_TESTS), anyDouble());

    verify(context, times(1)).saveMeasure(argThat(new IsResource(JavaFile.SCOPE_ENTITY, Qualifiers.UNIT_TEST_FILE)),
        eq(CoreMetrics.TESTS), anyDouble());
    verify(context, times(6)).saveMeasure(argThat(new IsResource(JavaFile.SCOPE_ENTITY, Qualifiers.UNIT_TEST_FILE)),
        (Metric) anyObject(), anyDouble());
    verify(context, times(1)).saveMeasure(argThat(new IsResource(JavaFile.SCOPE_ENTITY, Qualifiers.UNIT_TEST_FILE)),
        argThat(new IsMeasure(CoreMetrics.TEST_DATA)));

    verify(context).saveMeasure(eq(new JavaFile("org.sonar.core.ExtensionsFinderTest", true)),
        argThat(getTestDetailsMatcher("shouldSaveErrorsAndFailuresInXML/expected-test-details.xml")));
  }

  @Test
  public void shouldManageClassesWithDefaultPackage() throws URISyntaxException {
    SensorContext context = mockContext();
    new SurefireSensor().collect(newJarProject(), context, new File(getClass().getResource(
        "/org/sonar/plugins/surefire/SurefireSensorTest/shouldManageClassesWithDefaultPackage/").toURI()));

    verify(context).saveMeasure(new JavaFile("NoPackagesTest", true), CoreMetrics.TESTS, 2d);
  }

  @Test
  public void successRatioIsZeroWhenAllTestsFail() throws URISyntaxException {
    SensorContext context = mockContext();
    new SurefireSensor().collect(newJarProject(), context, new File(getClass().getResource(
        "/org/sonar/plugins/surefire/SurefireSensorTest/successRatioIsZeroWhenAllTestsFail/").toURI()));

    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.TESTS), eq(2d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.TEST_FAILURES), eq(1d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.TEST_ERRORS), eq(1d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.TEST_SUCCESS_DENSITY), eq(0d));
  }

  @Test
  public void measuresShouldNotIncludeSkippedTests() throws URISyntaxException {
    SensorContext context = mockContext();
    new SurefireSensor().collect(newJarProject(), context, new File(getClass().getResource(
        "/org/sonar/plugins/surefire/SurefireSensorTest/measuresShouldNotIncludeSkippedTests/").toURI()));

    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.TESTS), eq(2d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.TEST_FAILURES), eq(1d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.TEST_ERRORS), eq(0d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.SKIPPED_TESTS), eq(1d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.TEST_SUCCESS_DENSITY), eq(50d));
  }

  @Test
  public void noSuccessRatioIfNoTests() throws URISyntaxException {
    SensorContext context = mockContext();
    new SurefireSensor().collect(newJarProject(), context, new File(getClass().getResource(
        "/org/sonar/plugins/surefire/SurefireSensorTest/noSuccessRatioIfNoTests/").toURI()));

    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.TESTS), eq(0d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.TEST_FAILURES), eq(0d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.TEST_ERRORS), eq(0d));
    verify(context).saveMeasure(eq(new JavaFile("org.sonar.Foo", true)), eq(CoreMetrics.SKIPPED_TESTS), eq(2d));
    verify(context, never()).saveMeasure(eq(new JavaFile("org.sonar.Foo")), eq(CoreMetrics.TEST_SUCCESS_DENSITY), anyDouble());
  }

  @Test
  public void ignoreSuiteAsInnerClass() throws URISyntaxException {
    SensorContext context = mockContext();
    new SurefireSensor().collect(newJarProject(), context, new File(getClass().getResource(
        "/org/sonar/plugins/surefire/SurefireSensorTest/ignoreSuiteAsInnerClass/").toURI()));

    // ignore TestHandler$Input.xml
    verify(context).saveMeasure(eq(new JavaFile("org.apache.shindig.protocol.TestHandler")), eq(CoreMetrics.TESTS), eq(0.0));
    verify(context).saveMeasure(eq(new JavaFile("org.apache.shindig.protocol.TestHandler")), eq(CoreMetrics.SKIPPED_TESTS), eq(1.0));
  }

  private BaseMatcher<Measure> getTestDetailsMatcher(final String xmlBaseFile) {
    return new BaseMatcher<Measure>() {

      private Diff diff;

      public boolean matches(Object obj) {
        try {
          if (!ObjectUtils.equals(CoreMetrics.TEST_DATA, ((Measure) obj).getMetric())) {
            return false;
          }

          File expectedXML = new File(getClass().getResource("/org/sonar/plugins/surefire/SurefireSensorTest/" + xmlBaseFile).toURI());
          XMLUnit.setIgnoreWhitespace(true);
          XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
          XMLUnit.setNormalize(true);
          XMLUnit.setNormalizeWhitespace(true);
          diff = XMLUnit.compareXML(new FileReader(expectedXML), new StringReader(((Measure) obj).getData()));
          return diff.similar();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

      public void describeTo(Description d) {
        DetailedDiff dd = new DetailedDiff(diff);
        d.appendText("XML differences in " + xmlBaseFile + ": " + dd.getAllDifferences());
      }
    };
  }

  private static Project newJarProject() {
    return new Project("key").setPackaging("jar");
  }

  private static Project newPomProject() {
    return new Project("key").setPackaging("pom");
  }
}
