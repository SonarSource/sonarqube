/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.issue.ActionPlanStats;
import org.sonar.core.issue.db.ActionPlanStatsDao;
import org.sonar.core.issue.db.ActionPlanStatsDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;

import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ServerActionPlanStatsFinderTest {

  private ServerActionPlanStatsFinder actionPlanStatsFinder;

  private ActionPlanStatsDao actionPlanStatsDao = mock(ActionPlanStatsDao.class);
  private ResourceDao resourceDao = mock(ResourceDao.class);

  @Before
  public void before(){
    actionPlanStatsFinder = new ServerActionPlanStatsFinder(actionPlanStatsDao, resourceDao);
  }

  @Test
  public void should_find_action_plan_stats(){
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setId(1L).setKey("org.sonar.Sample"));
    when(actionPlanStatsDao.findByProjectId(1L)).thenReturn(newArrayList(new ActionPlanStatsDto()));

    Collection<ActionPlanStats> results = actionPlanStatsFinder.find("org.sonar.Sample");
    assertThat(results).hasSize(1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_throw_exception_if_project_not_found(){
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(null);

    actionPlanStatsFinder.find("org.sonar.Sample");
  }

}
