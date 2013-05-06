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

package org.sonar.core.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.ActionPlan;
import org.sonar.core.issue.db.ActionPlanDao;
import org.sonar.core.issue.db.ActionPlanDto;
import org.sonar.core.issue.db.ActionPlanStatsDao;
import org.sonar.core.issue.db.ActionPlanStatsDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;

import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActionPlanFinderTest {

  private ActionPlanDao actionPlanDao = mock(ActionPlanDao.class);
  private ActionPlanStatsDao actionPlanStatsDao = mock(ActionPlanStatsDao.class);
  private ResourceDao resourceDao = mock(ResourceDao.class);
  private ActionPlanFinder actionPlanFinder;

  @Before
  public void before() {
    actionPlanFinder = new ActionPlanFinder(actionPlanDao, actionPlanStatsDao, resourceDao);
  }

  @Test
  public void should_find_by_keys() {
    when(actionPlanDao.findByKeys(newArrayList("ABCD"))).thenReturn(newArrayList(new ActionPlanDto().setKey("ABCD")));
    Collection<ActionPlan> results = actionPlanFinder.findByKeys(newArrayList("ABCD"));
    assertThat(results).hasSize(1);
    assertThat(results.iterator().next().key()).isEqualTo("ABCD");
  }

  @Test
  public void should_find_open_by_project_key() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setKey("org.sonar.Sample").setId(1l));
    when(actionPlanDao.findOpenByProjectId(1l)).thenReturn(newArrayList(new ActionPlanDto().setKey("ABCD")));
    Collection<ActionPlan> results = actionPlanFinder.findOpenByProjectKey("org.sonar.Sample");
    assertThat(results).hasSize(1);
    assertThat(results.iterator().next().key()).isEqualTo("ABCD");
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_throw_exception_if_projecT_not_found_when_find_open_by_project_key() {
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(null);
    actionPlanFinder.findOpenByProjectKey("<Unkown>");
  }

  @Test
  public void should_find_action_plan_stats(){
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(new ResourceDto().setId(1L).setKey("org.sonar.Sample"));
    when(actionPlanStatsDao.findByProjectId(1L)).thenReturn(newArrayList(new ActionPlanStatsDto()));

    Collection<ActionPlanStats> results = actionPlanFinder.findActionPlanStats("org.sonar.Sample");
    assertThat(results).hasSize(1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_throw_exception_if_project_not_found_when_find_action_plan_stats(){
    when(resourceDao.getResource(any(ResourceQuery.class))).thenReturn(null);

    actionPlanFinder.findActionPlanStats("org.sonar.Sample");
  }

}
