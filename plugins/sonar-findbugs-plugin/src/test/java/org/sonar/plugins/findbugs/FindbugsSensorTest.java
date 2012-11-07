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
package org.sonar.plugins.findbugs;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.*;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.*;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.api.test.IsViolation;

import java.util.ArrayList;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FindbugsSensorTest extends FindbugsTests {

  @Test
  public void shouldNotAnalyseIfNoJavaProject() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn("php");

    FindbugsSensor sensor = new FindbugsSensor(null, null, null);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void shouldNotAnalyseIfJavaProjectButNoSource() {
    Project project = mock(Project.class);
    ProjectFileSystem fs = mock(ProjectFileSystem.class);
    when(fs.mainFiles("java")).thenReturn(new ArrayList<InputFile>());
    when(project.getFileSystem()).thenReturn(fs);
    when(project.getLanguageKey()).thenReturn("java");

    FindbugsSensor sensor = new FindbugsSensor(null, null, null);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void shouldNotAnalyseIfJavaProjectButNoRules() {
    Project project = mock(Project.class);
    ProjectFileSystem fs = mock(ProjectFileSystem.class);
    when(fs.mainFiles("java")).thenReturn(Lists.newArrayList(InputFileUtils.create(null, "")));
    when(project.getFileSystem()).thenReturn(fs);
    when(project.getLanguageKey()).thenReturn("java");

    FindbugsSensor sensor = new FindbugsSensor(RulesProfile.create(), null, null);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void shouldAnalyse() {
    Project project = mock(Project.class);
    ProjectFileSystem fs = mock(ProjectFileSystem.class);
    when(fs.mainFiles("java")).thenReturn(Lists.newArrayList(InputFileUtils.create(null, "")));
    when(project.getFileSystem()).thenReturn(fs);
    when(project.getLanguageKey()).thenReturn("java");

    FindbugsSensor sensor = new FindbugsSensor(createRulesProfileWithActiveRules(), null, null);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void shouldExecuteFindbugsWhenNoReportProvided() throws Exception {
    Project project = createProject();
    FindbugsExecutor executor = mock(FindbugsExecutor.class);
    SensorContext context = mock(SensorContext.class);

    BugCollection collection = new SortedBugCollection();
    BugInstance bugInstance = new BugInstance("AM_CREATES_EMPTY_ZIP_FILE_ENTRY", 2);
    String className = "org.sonar.commons.ZipUtils";
    String sourceFile = "org/sonar/commons/ZipUtils.java";
    int startLine = 107;
    ClassAnnotation classAnnotation = new ClassAnnotation(className, sourceFile);
    bugInstance.add(classAnnotation);
    MethodAnnotation methodAnnotation = new MethodAnnotation(className, "_zip", "(Ljava/lang/String;Ljava/io/File;Ljava/util/zip/ZipOutputStream;)V", true);
    methodAnnotation.setSourceLines(new SourceLineAnnotation(className, sourceFile, startLine, 0, 0, 0));
    bugInstance.add(methodAnnotation);
    collection.add(bugInstance);
    when(executor.execute()).thenReturn(collection);

    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));

    FindbugsSensor analyser = new FindbugsSensor(createRulesProfileWithActiveRules(), new FakeRuleFinder(), executor);
    analyser.analyse(project, context);

    verify(executor).execute();
    verify(context, times(1)).saveViolation(any(Violation.class));

    Violation wanted = Violation.create((Rule) null, new JavaFile("org.sonar.commons.ZipUtils")).setMessage(
        "Empty zip file entry created in org.sonar.commons.ZipUtils._zip(String, File, ZipOutputStream)").setLineId(107);
    verify(context).saveViolation(argThat(new IsViolation(wanted)));
  }

  @Test
  public void shouldIgnoreNotActiveViolations() throws Exception {
    Project project = createProject();
    FindbugsExecutor executor = mock(FindbugsExecutor.class);
    SensorContext context = mock(SensorContext.class);
    when(context.getResource(any(Resource.class))).thenReturn(new JavaFile("org.sonar.MyClass"));

    BugCollection collection = new SortedBugCollection();
    BugInstance bugInstance = new BugInstance("UNKNOWN", 2);
    String className = "org.sonar.commons.ZipUtils";
    String sourceFile = "org/sonar/commons/ZipUtils.java";
    ClassAnnotation classAnnotation = new ClassAnnotation(className, sourceFile);
    bugInstance.add(classAnnotation);
    collection.add(bugInstance);
    when(executor.execute()).thenReturn(collection);

    FindbugsSensor analyser = new FindbugsSensor(createRulesProfileWithActiveRules(), new FakeRuleFinder(), executor);
    analyser.analyse(project, context);

    verify(context, never()).saveViolation(any(Violation.class));
  }

  private Project createProject() {
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.hasJavaSourceFiles()).thenReturn(Boolean.TRUE);

    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(fileSystem);
    return project;
  }

}
