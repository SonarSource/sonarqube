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
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FindbugsExecutorTest {

  @Test
  public void canGenerateXMLReport() throws Exception {
    FindbugsConfiguration conf = mockConf();
    File reportFile = new File("target/test-tmp/findbugs-report.xml");
    when(conf.getTargetXMLReport()).thenReturn(reportFile);

    new FindbugsExecutor(conf).execute();

    assertThat(reportFile.exists()).isTrue();
    String report = FileUtils.readFileToString(reportFile);
    assertThat(report).as("Report should contain bug instance").contains("<BugInstance");
    assertThat(report).as("Report should be generated with messages").contains("<Message>");
    assertThat(report).contains("synthetic=\"true\"");
  }

  @Test(expected = SonarException.class)
  public void shouldTerminateAfterTimeout() throws Exception {
    FindbugsConfiguration conf = mockConf();
    when(conf.getTimeout()).thenReturn(1L);

    new FindbugsExecutor(conf).execute();
  }

  @Test(expected = SonarException.class)
  public void shoulFailIfNoCompiledClasses() throws Exception {
    Project project = mock(Project.class);
    ProjectFileSystem fs = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(fs);
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.CORE_VIOLATION_LOCALE_PROPERTY, CoreProperties.CORE_VIOLATION_LOCALE_DEFAULT_VALUE);
    FindbugsConfiguration conf = new FindbugsConfiguration(project, settings, null, null, null);

    new FindbugsExecutor(conf).execute();
  }

  private FindbugsConfiguration mockConf() throws Exception {
    FindbugsConfiguration conf = mock(FindbugsConfiguration.class);
    edu.umd.cs.findbugs.Project project = new edu.umd.cs.findbugs.Project();
    project.addFile(new File("test-resources/classes").getCanonicalPath());
    project.addSourceDir(new File("test-resources/src").getCanonicalPath());
    project.setCurrentWorkingDirectory(new File("test-resources"));
    when(conf.getFindbugsProject()).thenReturn(project);
    when(conf.saveExcludeConfigXml()).thenReturn(new File("test-resources/findbugs-exclude.xml"));
    when(conf.saveIncludeConfigXml()).thenReturn(new File("test-resources/findbugs-include.xml"));
    when(conf.getExcludesFilters()).thenReturn(Lists.newArrayList(new File("test-resources/findbugs-exclude.xml"), new File("test-resources/fake-file.xml")));
    when(conf.getEffort()).thenReturn("default");
    when(conf.getTimeout()).thenReturn(CoreProperties.FINDBUGS_TIMEOUT_DEFAULT_VALUE);
    when(conf.getLocale()).thenReturn(Locale.ENGLISH);
    return conf;
  }

}
