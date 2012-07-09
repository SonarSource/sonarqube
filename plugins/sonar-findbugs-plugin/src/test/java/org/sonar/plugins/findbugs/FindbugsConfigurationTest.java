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

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.test.SimpleProjectFileSystem;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FindbugsConfigurationTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private Project project;
  private Settings settings;
  private File findbugsTempDir;
  private FindbugsConfiguration conf;

  @Before
  public void setUp() {
    project = mock(Project.class);
    settings = new Settings(new PropertyDefinitions().addComponent(FindbugsPlugin.class));
    findbugsTempDir = tempFolder.newFolder("findbugs");
    when(project.getFileSystem()).thenReturn(new SimpleProjectFileSystem(findbugsTempDir));
    conf = new FindbugsConfiguration(project, settings, RulesProfile.create(), new FindbugsProfileExporter(), null);
  }

  @Test
  public void should_return_report_file() throws Exception {
    assertThat(conf.getTargetXMLReport()).isEqualTo(new File(findbugsTempDir, "target/sonar/findbugs-result.xml"));
  }

  @Test
  public void should_save_include_config() throws Exception {
    conf.saveIncludeConfigXml();
    File findbugsIncludeFile = new File(findbugsTempDir + "/target/sonar/findbugs-include.xml");
    assertThat(findbugsIncludeFile.exists()).isTrue();
  }

  @Test
  public void should_save_exclude_config() throws Exception {
    when(project.getExclusionPatterns()).thenReturn(new String[] {"dir/**/*.java"});
    conf.saveExcludeConfigXml();
    File findbugsExcludeFile = new File(findbugsTempDir + "/target/sonar/findbugs-exclude.xml");
    assertThat(findbugsExcludeFile.exists()).isTrue();
    String findbugsExclude = FileUtils.readFileToString(findbugsExcludeFile);
    assertThat(findbugsExclude).contains("Match");
  }

  @Test
  public void should_save_empty_exclude_config() throws Exception {
    conf.saveExcludeConfigXml();
    File findbugsExcludeFile = new File(findbugsTempDir + "/target/sonar/findbugs-exclude.xml");
    assertThat(findbugsExcludeFile.exists()).isTrue();
    String findbugsExclude = FileUtils.readFileToString(findbugsExcludeFile);
    assertThat(findbugsExclude).doesNotContain("Match");
  }

  @Test
  public void should_return_effort() {
    assertThat(conf.getEffort()).as("default effort").isEqualTo("default");
    settings.setProperty(CoreProperties.FINDBUGS_EFFORT_PROPERTY, "Max");
    assertThat(conf.getEffort()).isEqualTo("max");
  }

  @Test
  public void should_return_timeout() {
    assertThat(conf.getTimeout()).as("default timeout").isEqualTo(600000);
    settings.setProperty(CoreProperties.FINDBUGS_TIMEOUT_PROPERTY, 1);
    assertThat(conf.getTimeout()).isEqualTo(1);
  }

  @Test
  public void should_return_excludes_filters() {
    assertThat(conf.getExcludesFilters()).isEmpty();
    settings.setProperty(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY, " foo.xml , bar.xml,");
    assertThat(conf.getExcludesFilters()).hasSize(2);
  }

}
