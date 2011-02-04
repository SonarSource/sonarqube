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
package org.sonar.plugins.findbugs;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.test.SimpleProjectFileSystem;

public class FindbugsConfigurationTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private Project project;
  private File findbugsTempDir;

  @Before
  public void setup() {
    project = mock(Project.class);
    findbugsTempDir = tempFolder.newFolder("findbugs");
    when(project.getFileSystem()).thenReturn(new SimpleProjectFileSystem(findbugsTempDir));
  }

  @Test
  public void shouldSaveConfigFiles() throws Exception {
    FindbugsConfiguration conf = new FindbugsConfiguration(project, RulesProfile.create(), new FindbugsProfileExporter(), null);

    conf.saveIncludeConfigXml();
    conf.saveExcludeConfigXml();

    File findbugsIncludeFile = new File(findbugsTempDir + "/target/sonar/findbugs-include.xml");
    File findbugsExcludeFile = new File(findbugsTempDir + "/target/sonar/findbugs-exclude.xml");
    assertThat(findbugsIncludeFile.exists(), is(true));
    assertThat(findbugsExcludeFile.exists(), is(true));
  }

}
