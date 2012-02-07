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
package org.sonar.plugins.cpd.index;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.duplication.DuplicationDao;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class IndexFactoryTest {

  private Project project;
  private Settings settings;

  @Before
  public void setUp() {
    project = new Project("foo");
    settings = new Settings();
  }

  @Test
  public void crossProjectEnabled() {
    settings.setProperty(CoreProperties.CPD_CROSS_RPOJECT, "true");
    IndexFactory factory = new IndexFactory(settings, mock(ResourcePersister.class), mock(DuplicationDao.class));
    assertThat(factory.isCrossProject(project), is(true));
  }

  @Test
  public void noCrossProjectWithBranch() {
    settings.setProperty(CoreProperties.CPD_CROSS_RPOJECT, "true");
    IndexFactory factory = new IndexFactory(settings, mock(ResourcePersister.class), mock(DuplicationDao.class));
    project.setBranch("branch");
    assertThat(factory.isCrossProject(project), is(false));
  }

  @Test
  public void noCrossProjectWithoutDatabase() {
    settings.setProperty(CoreProperties.CPD_CROSS_RPOJECT, "true");
    IndexFactory factory = new IndexFactory(settings);
    assertThat(factory.isCrossProject(project), is(false));
  }

  @Test
  public void crossProjectDisabled() {
    settings.setProperty(CoreProperties.CPD_CROSS_RPOJECT, "false");
    IndexFactory factory = new IndexFactory(settings, mock(ResourcePersister.class), mock(DuplicationDao.class));
    assertThat(factory.isCrossProject(project), is(false));
  }

}
