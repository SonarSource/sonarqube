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
package org.sonar.plugins.pmd;

import com.google.common.base.Charsets;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.test.TestUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PmdExecutorTest {
  PmdExecutor pmdExecutor;

  Project project = mock(Project.class);
  ProjectFileSystem projectFileSystem = mock(ProjectFileSystem.class);
  RulesProfile rulesProfile = mock(RulesProfile.class);
  PmdProfileExporter pmdProfileExporter = mock(PmdProfileExporter.class);
  PmdConfiguration pmdConfiguration = mock(PmdConfiguration.class);
  PmdTemplate pmdTemplate = mock(PmdTemplate.class);

  @Before
  public void setUpPmdExecutor() {
    pmdExecutor = Mockito.spy(new PmdExecutor(project, projectFileSystem, rulesProfile, pmdProfileExporter, pmdConfiguration));

    doReturn(pmdTemplate).when(pmdExecutor).createPmdTemplate();
  }

  @Test
  public void should_execute_pmd_on_source_files_and_test_files() {
    InputFile srcFile = file("src/Class.java");
    InputFile tstFile = file("test/ClassTest.java");
    when(pmdProfileExporter.exportProfile(PmdConstants.REPOSITORY_KEY, rulesProfile)).thenReturn(TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    when(pmdProfileExporter.exportProfile(PmdConstants.TEST_REPOSITORY_KEY, rulesProfile)).thenReturn(TestUtils.getResourceContent("/org/sonar/plugins/pmd/junit.xml"));
    when(projectFileSystem.getSourceCharset()).thenReturn(Charsets.UTF_8);
    when(projectFileSystem.mainFiles(Java.KEY)).thenReturn(Arrays.asList(srcFile));
    when(projectFileSystem.testFiles(Java.KEY)).thenReturn(Arrays.asList(tstFile));

    Report report = pmdExecutor.execute();

    verify(pmdTemplate).process(eq(new File("src/Class.java")), eq(Charsets.UTF_8), any(RuleSets.class), any(RuleContext.class));
    verify(pmdTemplate).process(eq(new File("test/ClassTest.java")), eq(Charsets.UTF_8), any(RuleSets.class), any(RuleContext.class));
    assertThat(report).isNotNull();
  }

  @Test
  public void should_dump_configuration_as_xml() {
    when(pmdProfileExporter.exportProfile(PmdConstants.REPOSITORY_KEY, rulesProfile)).thenReturn(TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    when(pmdProfileExporter.exportProfile(PmdConstants.TEST_REPOSITORY_KEY, rulesProfile)).thenReturn(TestUtils.getResourceContent("/org/sonar/plugins/pmd/junit.xml"));

    Report report = pmdExecutor.execute();

    verify(pmdConfiguration).dumpXmlReport(report);
  }

  @Test
  public void should_dump_ruleset_as_xml() {
    InputFile srcFile = file("src/Class.java");
    InputFile tstFile = file("test/ClassTest.java");
    when(pmdProfileExporter.exportProfile(PmdConstants.REPOSITORY_KEY, rulesProfile)).thenReturn(TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    when(pmdProfileExporter.exportProfile(PmdConstants.TEST_REPOSITORY_KEY, rulesProfile)).thenReturn(TestUtils.getResourceContent("/org/sonar/plugins/pmd/junit.xml"));
    when(projectFileSystem.mainFiles(Java.KEY)).thenReturn(Arrays.asList(srcFile));
    when(projectFileSystem.testFiles(Java.KEY)).thenReturn(Arrays.asList(tstFile));

    pmdExecutor.execute();

    verify(pmdConfiguration).dumpXmlRuleSet(PmdConstants.REPOSITORY_KEY, TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    verify(pmdConfiguration).dumpXmlRuleSet(PmdConstants.TEST_REPOSITORY_KEY, TestUtils.getResourceContent("/org/sonar/plugins/pmd/junit.xml"));
  }

  @Test
  public void should_ignore_empty_test_dir() {
    InputFile srcFile = file("src/Class.java");
    doReturn(pmdTemplate).when(pmdExecutor).createPmdTemplate();
    when(pmdProfileExporter.exportProfile(PmdConstants.REPOSITORY_KEY, rulesProfile)).thenReturn(TestUtils.getResourceContent("/org/sonar/plugins/pmd/simple.xml"));
    when(projectFileSystem.getSourceCharset()).thenReturn(Charsets.UTF_8);
    when(projectFileSystem.mainFiles(Java.KEY)).thenReturn(Arrays.asList(srcFile));
    when(projectFileSystem.testFiles(Java.KEY)).thenReturn(Collections.<InputFile> emptyList());

    pmdExecutor.execute();

    verify(pmdTemplate).process(eq(new File("src/Class.java")), eq(Charsets.UTF_8), any(RuleSets.class), any(RuleContext.class));
    verifyNoMoreInteractions(pmdTemplate);
  }

  static InputFile file(String path) {
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.getFile()).thenReturn(new File(path));
    return inputFile;
  }
}
