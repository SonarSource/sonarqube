/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.slf4j.Logger;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.duplication.DuplicationDao;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class IndexFactoryTest {

  Project project;
  Settings settings;
  IndexFactory factory;
  Logger logger;

  @Before
  public void setUp() {
    project = new Project("foo");
    settings = new Settings();
    factory = new IndexFactory(settings, mock(ResourcePersister.class), mock(DuplicationDao.class));
    logger = mock(Logger.class);
  }

  @Test
  public void crossProjectEnabled() {
    settings.setProperty(CoreProperties.CPD_CROSS_RPOJECT, "true");
    assertThat(factory.verifyCrossProject(project, logger)).isTrue();
    verify(logger).info("Cross-project analysis enabled");
  }

  @Test
  public void noCrossProjectWithBranch() {
    settings.setProperty(CoreProperties.CPD_CROSS_RPOJECT, "true");
    project.setBranch("branch");
    assertThat(factory.verifyCrossProject(project, logger)).isFalse();
    verify(logger).info("Cross-project analysis disabled. Not supported on project branches.");
  }

  @Test
  public void cross_project_should_be_disabled_on_dry_run() {
    settings.setProperty(CoreProperties.CPD_CROSS_RPOJECT, "true");
    settings.setProperty(CoreProperties.DRY_RUN, "true");
    assertThat(factory.verifyCrossProject(project, logger)).isFalse();
    verify(logger).info("Cross-project analysis disabled. Not supported on dry runs.");
  }

  @Test
  public void crossProjectDisabled() {
    settings.setProperty(CoreProperties.CPD_CROSS_RPOJECT, "false");
    assertThat(factory.verifyCrossProject(project, logger)).isFalse();
    verify(logger).info("Cross-project analysis disabled");
  }

}
