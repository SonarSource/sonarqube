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
package org.sonar.plugins.jacoco;

import com.google.common.io.Files;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.IsMeasure;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class JaCoCoOverallSensorTest {
  private final JacocoConfiguration configuration = mock(JacocoConfiguration.class);
  private final SensorContext context = mock(SensorContext.class);
  private final ProjectFileSystem pfs = mock(ProjectFileSystem.class);
  private final Project project = mock(Project.class);
  private final JaCoCoOverallSensor sensor = new JaCoCoOverallSensor(configuration);

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);
    when(configuration.getItReportPath()).thenReturn("target/it-jacoco.exec");
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC).thenReturn(Project.AnalysisType.REUSE_REPORTS);

    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void do_not_execute_when_report_path_not_specified() {
    Project project = mock(Project.class);
    when(configuration.getItReportPath()).thenReturn("");

    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_save_measures() throws IOException {
    File outputDir = TestUtils.getResource(JaCoCoOverallSensorTest.class, ".");
    Files.copy(TestUtils.getResource("HelloWorld.class.toCopy"), new File(outputDir, "HelloWorld.class"));

    JavaFile resource = new JavaFile("com.sonar.coverages.HelloWorld");

    when(project.getFileSystem()).thenReturn(pfs);
    when(context.getResource(any(Resource.class))).thenReturn(resource);
    when(configuration.getReportPath()).thenReturn("ut.exec");
    when(configuration.getItReportPath()).thenReturn("it.exec");
    when(pfs.getBuildOutputDir()).thenReturn(outputDir);
    when(pfs.resolvePath("ut.exec")).thenReturn(new File(outputDir, "ut.exec"));
    when(pfs.resolvePath("it.exec")).thenReturn(new File(outputDir, "it.exec"));
    when(pfs.resolvePath("target/sonar/jacoco-overall.exec")).thenReturn(new File("target/sonar/jacoco-overall.exec"));

    sensor.analyse(project, context);

    verify(context).getResource(resource);
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_LINES_TO_COVER, 12.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_LINES, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, "3=1;6=1;7=1;10=1;11=1;14=1;15=1;17=1;18=1;20=1;23=0;24=0")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_CONDITIONS_TO_COVER, 2.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, 0.0)));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_CONDITIONS_BY_LINE, "14=2")));
    verify(context).saveMeasure(eq(resource), argThat(new IsMeasure(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE, (String) null)));
    verifyNoMoreInteractions(context);
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("JaCoCoOverallSensor");
  }
}
