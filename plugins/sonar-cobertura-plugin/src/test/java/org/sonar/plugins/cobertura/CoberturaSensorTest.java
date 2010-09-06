/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.plugins.cobertura;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.IsMeasure;
import org.sonar.api.test.IsResource;
import org.sonar.api.test.MavenTestUtils;

public class CoberturaSensorTest {

  @Test
  public void shouldGetReportPathFromProperty() throws URISyntaxException {
    DefaultProjectFileSystem fileSystem = mock(DefaultProjectFileSystem.class);
    when(fileSystem.resolvePath("foo")).thenReturn(getCoverageReport());

    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(fileSystem);
    when(project.getProperty(CoreProperties.COBERTURA_REPORT_PATH_PROPERTY)).thenReturn("foo");

    File report = new CoberturaSensor(null, null).getReport(project);
    verify(fileSystem).resolvePath("foo");
    assertNotNull(report);
  }

  @Test
  public void doNotExecuteMavenPluginIfReuseReports() {
    Project project = mock(Project.class);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(new CoberturaSensor(null, new CoberturaMavenPluginHandler()).getMavenPluginHandler(project), nullValue());
  }

  @Test
  public void doNotExecuteMavenPluginIfStaticAnalysis() {
    Project project = mock(Project.class);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(new CoberturaSensor(null, new CoberturaMavenPluginHandler()).getMavenPluginHandler(project), nullValue());
  }

  @Test
  public void executeMavenPluginIfDynamicAnalysis() {
    Project project = mock(Project.class);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(new CoberturaSensor(null, new CoberturaMavenPluginHandler()).getMavenPluginHandler(project), not(nullValue()));
    assertThat(new CoberturaSensor(null, new CoberturaMavenPluginHandler()).getMavenPluginHandler(project).getArtifactId(),
        is("cobertura-maven-plugin"));
  }

  @Test
  public void shouldGetReportPathFromPom() {
    MavenProject pom = MavenTestUtils.loadPom("/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldGetReportPathFromPom/pom.xml");

    DefaultProjectFileSystem fileSystem = mock(DefaultProjectFileSystem.class);

    Project project = mock(Project.class);
    when(project.getPom()).thenReturn(pom);
    when(project.getFileSystem()).thenReturn(fileSystem);

    new CoberturaSensor(null, null).getReport(project);

    verify(fileSystem).resolvePath("overridden/dir");
  }

  @Test
  public void doNotCollectProjectCoverage() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    new CoberturaSensor(null, null).parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.COVERAGE), anyDouble());
  }

  @Test
  public void doNotCollectProjectLineCoverage() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    new CoberturaSensor(null, null).parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.LINE_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure(argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)));
  }

  @Test
  public void doNotCollectProjectBranchCoverage() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    new CoberturaSensor(null, null).parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.BRANCH_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure(argThat(new IsMeasure(CoreMetrics.BRANCH_COVERAGE_HITS_DATA)));
  }

  @Test
  public void collectPackageLineCoverage() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    new CoberturaSensor(null, null).parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.LINE_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.UNCOVERED_LINES), anyDouble());
  }

  @Test
  public void collectPackageBranchCoverage() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    new CoberturaSensor(null, null).parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.BRANCH_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.UNCOVERED_CONDITIONS), anyDouble());
  }

  @Test
  public void packageCoverageIsCalculatedLaterByDecorator() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    new CoberturaSensor(null, null).parseReport(getCoverageReport(), context);

    verify(context, never()).saveMeasure((Resource) argThat(is(JavaPackage.class)), eq(CoreMetrics.COVERAGE), anyDouble());
  }

  @Test
  public void collectFileLineCoverage() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));
    new CoberturaSensor(null, null).parseReport(getCoverageReport(), context);

    final JavaFile file = new JavaFile("org.apache.commons.chain.config.ConfigParser");
    // verify(context).saveMeasure(eq(file), argThat(new IsMeasure(CoreMetrics.LINE_COVERAGE, 83.3)));
    verify(context).saveMeasure(eq(file), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 30.0)));
    verify(context).saveMeasure(eq(file), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 5.0)));
  }

  @Test
  public void collectFileBranchCoverage() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));
    new CoberturaSensor(null, null).parseReport(getCoverageReport(), context);

    final JavaFile file = new JavaFile("org.apache.commons.chain.config.ConfigParser");
    verify(context).saveMeasure(eq(file), argThat(new IsMeasure(CoreMetrics.BRANCH_COVERAGE, 66.7)));
    verify(context).saveMeasure(eq(file), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER, 6.0)));
    verify(context).saveMeasure(eq(file), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS, 2.0)));
  }

  @Test
  public void testDoNotSaveMeasureOnResourceWhichDoesntExistInTheContext() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    when(context.getResource(any(Resource.class))).thenReturn(null);
    new CoberturaSensor(null, null).parseReport(getCoverageReport(), context);
    verify(context, never()).saveMeasure(any(Resource.class), any(Measure.class));
  }

  @Test
  public void javaInterfaceHasNoCoverage() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    new CoberturaSensor(null, null).parseReport(getCoverageReport(), context);

    final JavaFile interfaze = new JavaFile("org.apache.commons.chain.Chain");
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.COVERAGE)));

    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.LINE_COVERAGE)));
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER)));
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES)));

    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.BRANCH_COVERAGE)));
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER)));
    verify(context, never()).saveMeasure(eq(interfaze), argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS)));
  }

  @Test
  public void shouldInsertCoverageAtFileLevel() throws URISyntaxException {
    File coverage = new File(getClass().getResource(
        "/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldInsertCoverageAtFileLevel/coverage.xml").toURI());
    SensorContext context = mock(SensorContext.class);
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));
    new CoberturaSensor(null, null).parseReport(coverage, context);

    verify(context).saveMeasure(argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.InnerClass")),
        argThat(new IsMeasure(CoreMetrics.LINE_COVERAGE, 37.1)));
    verify(context).saveMeasure(argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.InnerClass")),
        argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 35.0)));
    verify(context).saveMeasure(argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.InnerClass")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 22.0)));

    verify(context).saveMeasure(argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.InnerClass")),
        argThat(new IsMeasure(CoreMetrics.BRANCH_COVERAGE, 25.0)));
    verify(context).saveMeasure(argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.InnerClass")),
        argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER, 4.0)));
    verify(context).saveMeasure(argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.InnerClass")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS, 3.0)));

    verify(context, never()).saveMeasure(
        argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.LINE_COVERAGE)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.BRANCH_COVERAGE)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.InnerClass$InnerClassInside")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES)));

    verify(context, never()).saveMeasure(
        argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.LINE_COVERAGE)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.BRANCH_COVERAGE)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.CONDITIONS_TO_COVER)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_CONDITIONS)));
    verify(context, never()).saveMeasure(
        argThat(new IsResource(Resource.SCOPE_ENTITY, Resource.QUALIFIER_CLASS, "org.sonar.samples.PrivateClass")),
        argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES)));

    verify(context)
        .saveMeasure(
            eq(new JavaFile("org.sonar.samples.InnerClass")),
            argThat(new IsMeasure(
                CoreMetrics.COVERAGE_LINE_HITS_DATA,
                "22=2;25=0;26=0;29=0;30=0;31=0;34=1;35=1;36=1;37=0;39=1;41=1;44=2;46=1;47=1;50=0;51=0;52=0;53=0;55=0;57=0;60=0;61=0;64=1;71=1;73=1;76=0;77=0;80=0;81=0;85=0;87=0;91=0;93=0;96=1")));
  }

  @Test
  public void collectFileLineHitsData() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));
    new CoberturaSensor(null, null).parseReport(getCoverageReport(), context);
    verify(context).saveMeasure(
        eq(new JavaFile("org.apache.commons.chain.impl.CatalogBase")),
        argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA,
            "111=18;121=0;122=0;125=0;126=0;127=0;128=0;131=0;133=0;48=117;56=234;66=0;67=0;68=0;84=999;86=999;98=318")));
  }

  @Test
  public void collectFileBranchHitsData() throws URISyntaxException {
    SensorContext context = mock(SensorContext.class);
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));
    new CoberturaSensor(null, null).parseReport(getCoverageReport(), context);

    // no conditions
    verify(context, never()).saveMeasure(
        eq(new JavaFile("org.apache.commons.chain.config.ConfigRuleSet")),
        argThat(new IsMeasure(CoreMetrics.BRANCH_COVERAGE_HITS_DATA)));

    verify(context).saveMeasure(
        eq(new JavaFile("org.apache.commons.chain.config.ConfigParser")),
        argThat(new IsMeasure(CoreMetrics.BRANCH_COVERAGE_HITS_DATA, "73=50%;76=50%;93=100%")));

    verify(context).saveMeasure(
        eq(new JavaFile("org.apache.commons.chain.generic.CopyCommand")),
        argThat(new IsMeasure(CoreMetrics.BRANCH_COVERAGE_HITS_DATA, "132=0%;136=0%")));
  }

  private File getCoverageReport() throws URISyntaxException {
    return new File(getClass().getResource("/org/sonar/plugins/cobertura/CoberturaSensorTest/commons-chain-coverage.xml").toURI());
  }
}
