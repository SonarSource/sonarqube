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

package org.sonar.server.computation;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetAndSetProjectStepTest {

  private GetAndSetProjectStep sut;
  private DbClient dbClient;
  private ComponentDto project;
  private DbSession session;

  @Before
  public void before() {
    this.dbClient = mock(DbClient.class);
    this.session = mock(DbSession.class);
    this.project = ComponentTesting.newProjectDto();

    ComponentDao componentDao = mock(ComponentDao.class);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(componentDao.getByKey(any(DbSession.class), anyString())).thenReturn(project);

    this.sut = new GetAndSetProjectStep(dbClient);
  }

  @Test
  public void set_project_return_by_dbclient() {
    AnalysisReportDto report = new AnalysisReportDto().setProjectKey("123-456-789");

    sut.execute(session, report);

    assertThat(report.getProject()).isEqualTo(project);
  }

}
