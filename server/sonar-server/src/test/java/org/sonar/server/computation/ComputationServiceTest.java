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

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ComputationServiceTest {

  private ComputationService sut;

  private DbClient dbClient;
  private ComputationStepRegistry stepRegistry;
  private ActivityService activityService;

  @Before
  public void before() {
    this.stepRegistry = mock(ComputationStepRegistry.class);
    this.activityService = mock(ActivityService.class);
    this.dbClient = mock(DbClient.class);
    when(dbClient.openSession(anyBoolean())).thenReturn(mock(DbSession.class));
    ComponentDao componentDao = mock(ComponentDao.class);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(componentDao.getByKey(any(DbSession.class), anyString())).thenReturn(ComponentTesting.newProjectDto());

    this.sut = new ComputationService(dbClient, stepRegistry, activityService);
  }

  @Test
  public void call_execute_method_of_my_registry() {
    ComputationStep firstStep = mock(ComputationStep.class);
    ComputationStep secondStep = mock(ComputationStep.class);
    ComputationStep thirdStep = mock(ComputationStep.class);

    when(stepRegistry.steps()).thenReturn(Lists.newArrayList(firstStep, secondStep, thirdStep));

    sut.analyzeReport(AnalysisReportDto.newForTests(1L));

    InOrder order = inOrder(firstStep, secondStep, thirdStep);

    order.verify(firstStep).execute(any(DbSession.class), any(AnalysisReportDto.class), any(ComponentDto.class));
    order.verify(secondStep).execute(any(DbSession.class), any(AnalysisReportDto.class), any(ComponentDto.class));
    order.verify(thirdStep).execute(any(DbSession.class), any(AnalysisReportDto.class), any(ComponentDto.class));
  }
}
