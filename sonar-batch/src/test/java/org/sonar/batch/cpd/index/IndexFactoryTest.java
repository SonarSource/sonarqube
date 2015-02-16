/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.cpd.index;

import org.sonar.batch.cpd.index.IndexFactory;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.resources.Project;
import org.sonar.batch.bootstrap.DefaultAnalysisMode;
import org.sonar.batch.index.ResourceCache;
import org.sonar.core.duplication.DuplicationDao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexFactoryTest {

  Project project;
  Settings settings;
  IndexFactory factory;
  Logger logger;
  private DefaultAnalysisMode analysisMode;

  @Before
  public void setUp() {
    project = new Project("foo");
    settings = new Settings();
    analysisMode = mock(DefaultAnalysisMode.class);
    factory = new IndexFactory(analysisMode, settings, mock(DuplicationDao.class), mock(DatabaseSession.class), new ResourceCache());
    logger = mock(Logger.class);
  }

  @Test
  public void crossProjectEnabled() {
    settings.setProperty(CoreProperties.CPD_CROSS_PROJECT, "true");
    assertThat(factory.verifyCrossProject(project, logger)).isTrue();
    verify(logger).info("Cross-project analysis enabled");
  }

  @Test
  public void noCrossProjectWithBranch() {
    settings.setProperty(CoreProperties.CPD_CROSS_PROJECT, "true");
    settings.setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "branch");
    assertThat(factory.verifyCrossProject(project, logger)).isFalse();
    verify(logger).info("Cross-project analysis disabled. Not supported on project branches.");
  }

  @Test
  public void cross_project_should_be_disabled_on_preview() {
    when(analysisMode.isPreview()).thenReturn(true);
    settings.setProperty(CoreProperties.CPD_CROSS_PROJECT, "true");
    assertThat(factory.verifyCrossProject(project, logger)).isFalse();
    verify(logger).info("Cross-project analysis disabled. Not supported in preview mode.");
  }

  @Test
  public void crossProjectDisabled() {
    settings.setProperty(CoreProperties.CPD_CROSS_PROJECT, "false");
    assertThat(factory.verifyCrossProject(project, logger)).isFalse();
    verify(logger).info("Cross-project analysis disabled");
  }

}
