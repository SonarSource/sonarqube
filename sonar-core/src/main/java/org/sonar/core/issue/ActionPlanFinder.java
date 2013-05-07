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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.ActionPlan;
import org.sonar.core.issue.db.ActionPlanDao;
import org.sonar.core.issue.db.ActionPlanDto;
import org.sonar.core.issue.db.ActionPlanStatsDao;
import org.sonar.core.issue.db.ActionPlanStatsDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class ActionPlanFinder implements ServerComponent {

  private final ActionPlanDao actionPlanDao;
  private final ActionPlanStatsDao actionPlanStatsDao;
  private final ResourceDao resourceDao;

  public ActionPlanFinder(ActionPlanDao actionPlanDao, ActionPlanStatsDao actionPlanStatsDao, ResourceDao resourceDao) {
    this.actionPlanDao = actionPlanDao;
    this.actionPlanStatsDao = actionPlanStatsDao;
    this.resourceDao = resourceDao;
  }

  public Collection<ActionPlan> findByKeys(Collection<String> keys) {
    Collection<ActionPlanDto> actionPlanDtos = actionPlanDao.findByKeys(keys);
    return toActionPlans(actionPlanDtos);
  }

  public Collection<ActionPlan> findOpenByProjectKey(String projectKey) {
    ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(projectKey));
    if (resourceDto == null) {
      throw new IllegalArgumentException("Project " + projectKey + " has not been found.");
    }
    Collection<ActionPlanDto> actionPlanDtos = actionPlanDao.findOpenByProjectId(resourceDto.getId());
    return toActionPlans(actionPlanDtos);
  }

  public List<ActionPlanStats> findOpenActionPlanStats(String projectKey) {
    ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(projectKey));
    if (resourceDto == null) {
      throw new IllegalArgumentException("Project " + projectKey + " does not exists.");
    }
    Collection<ActionPlanStatsDto> actionPlanStatsDtos = actionPlanStatsDao.findOpenByProjectId(resourceDto.getId());
    return newArrayList(Iterables.transform(actionPlanStatsDtos, new Function<ActionPlanStatsDto, ActionPlanStats>() {
      @Override
      public ActionPlanStats apply(ActionPlanStatsDto actionPlanStatsDto) {
        return actionPlanStatsDto.toActionPlanStat();
      }
    }));
  }

  public List<ActionPlanStats> findClosedActionPlanStats(String projectKey) {
    ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(projectKey));
    if (resourceDto == null) {
      throw new IllegalArgumentException("Project " + projectKey + " does not exists.");
    }
    Collection<ActionPlanStatsDto> actionPlanStatsDtos = actionPlanStatsDao.findClosedByProjectId(resourceDto.getId());
    return newArrayList(Iterables.transform(actionPlanStatsDtos, new Function<ActionPlanStatsDto, ActionPlanStats>() {
      @Override
      public ActionPlanStats apply(ActionPlanStatsDto actionPlanStatsDto) {
        return actionPlanStatsDto.toActionPlanStat();
      }
    }));
  }

  private Collection<ActionPlan> toActionPlans(Collection<ActionPlanDto> actionPlanDtos) {
    return newArrayList(Iterables.transform(actionPlanDtos, new Function<ActionPlanDto, ActionPlan>() {
      @Override
      public ActionPlan apply(@Nullable ActionPlanDto actionPlanDto) {
        return actionPlanDto.toActionPlan();
      }
    }));
  }
}
