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
import org.sonar.core.resource.ResourceIndexerDao;

import static org.mockito.Mockito.*;

public class ComponentIndexationInDatabaseStepTest {

  private ComponentIndexationInDatabaseStep sut;
  private ResourceIndexerDao resourceIndexerDao;

  @Before
  public void before() {
    this.resourceIndexerDao = mock(ResourceIndexerDao.class);
    this.sut = new ComponentIndexationInDatabaseStep(resourceIndexerDao);
  }

  @Test
  public void call_indexProject_of_dao() {
    ComponentDto project = mock(ComponentDto.class);
    when(project.getId()).thenReturn(123L);

    sut.execute(mock(DbSession.class), mock(AnalysisReportDto.class), project);

    verify(resourceIndexerDao).indexProject(123);
  }

}
