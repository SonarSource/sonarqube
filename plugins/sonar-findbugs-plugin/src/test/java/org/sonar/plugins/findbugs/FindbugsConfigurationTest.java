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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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

  @Before
  public void setup() {
    project = mock(Project.class);
    settings = new Settings();
    findbugsTempDir = tempFolder.newFolder("findbugs");
    when(project.getFileSystem()).thenReturn(new SimpleProjectFileSystem(findbugsTempDir));
  }

  @Test
  public void shouldSaveConfigFiles() throws Exception {
    FindbugsConfiguration conf = new FindbugsConfiguration(project, settings, RulesProfile.create(), new FindbugsProfileExporter(), null);

    conf.saveIncludeConfigXml();
    conf.saveExcludeConfigXml();

    File findbugsIncludeFile = new File(findbugsTempDir + "/target/sonar/findbugs-include.xml");
    File findbugsExcludeFile = new File(findbugsTempDir + "/target/sonar/findbugs-exclude.xml");
    assertThat(findbugsIncludeFile.exists()).isTrue();
    assertThat(findbugsExcludeFile.exists()).isTrue();
  }

  @Test
  public void shouldReturnExcludesFilters() {
    FindbugsConfiguration conf = new FindbugsConfiguration(project, settings, RulesProfile.create(), new FindbugsProfileExporter(), null);

    assertThat(conf.getExcludesFilters()).isEmpty();
    settings.setProperty(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY, " foo.xml , bar.xml,");
    assertThat(conf.getExcludesFilters()).hasSize(2);
  }

}
