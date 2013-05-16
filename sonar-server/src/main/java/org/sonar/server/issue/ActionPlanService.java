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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.ActionPlan;
import org.sonar.core.issue.ActionPlanStats;
import org.sonar.core.issue.db.ActionPlanDao;
import org.sonar.core.issue.db.ActionPlanDto;
import org.sonar.core.issue.db.ActionPlanStatsDao;
import org.sonar.core.issue.db.ActionPlanStatsDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class ActionPlanService implements ServerComponent {

  private final ActionPlanDao actionPlanDao;
  private final ActionPlanStatsDao actionPlanStatsDao;
  private final ResourceDao resourceDao;

  public ActionPlanService(ActionPlanDao actionPlanDao, ActionPlanStatsDao actionPlanStatsDao, ResourceDao resourceDao) {
    this.actionPlanDao = actionPlanDao;
    this.actionPlanStatsDao = actionPlanStatsDao;
    this.resourceDao = resourceDao;
  }

  public ActionPlan create(ActionPlan actionPlan){
    actionPlanDao.save(ActionPlanDto.toActionDto(actionPlan, findComponent(actionPlan.projectKey()).getId()));
    return actionPlan;
  }

  public ActionPlan update(ActionPlan actionPlan){
    actionPlanDao.update(ActionPlanDto.toActionDto(actionPlan, findComponent(actionPlan.projectKey()).getId()));
    return actionPlan;
  }

  public void delete(String key){
    actionPlanDao.delete(key);
  }

  public ActionPlan setStatus(String key, String status){
    ActionPlanDto actionPlanDto = actionPlanDao.findByKey(key);
    if (actionPlanDto == null) {
      throw new IllegalArgumentException("Action plan " + key + " has not been found.");
    }
    actionPlanDto.setStatus(status);
    actionPlanDto.setCreatedAt(new Date());
    actionPlanDao.update(actionPlanDto);
    return actionPlanDto.toActionPlan();
  }

  @CheckForNull
  public ActionPlan findByKey(String key) {
    ActionPlanDto actionPlanDto = actionPlanDao.findByKey(key);
    if (actionPlanDto == null) {
      return null;
    }
    return actionPlanDto.toActionPlan();
  }

  public Collection<ActionPlan> findByKeys(Collection<String> keys) {
    Collection<ActionPlanDto> actionPlanDtos = actionPlanDao.findByKeys(keys);
    return toActionPlans(actionPlanDtos);
  }

  public Collection<ActionPlan> findOpenByComponentKey(String componentKey) {
    Collection<ActionPlanDto> actionPlanDtos = actionPlanDao.findOpenByProjectId(findRootProject(componentKey).getId());
    return toActionPlans(actionPlanDtos);
  }

  public List<ActionPlanStats> findActionPlanStats(String projectKey) {
    Collection<ActionPlanStatsDto> actionPlanStatsDtos = actionPlanStatsDao.findByProjectId(findComponent(projectKey).getId());
    return newArrayList(Iterables.transform(actionPlanStatsDtos, new Function<ActionPlanStatsDto, ActionPlanStats>() {
      @Override
      public ActionPlanStats apply(ActionPlanStatsDto actionPlanStatsDto) {
        return actionPlanStatsDto.toActionPlanStat();
      }
    }));
  }

  public boolean isNameAlreadyUsedForProject(String name, String projectKey) {
    return !actionPlanDao.findByNameAndProjectId(name, findComponent(projectKey).getId()).isEmpty();
  }

  private Collection<ActionPlan> toActionPlans(Collection<ActionPlanDto> actionPlanDtos) {
    return newArrayList(Iterables.transform(actionPlanDtos, new Function<ActionPlanDto, ActionPlan>() {
      @Override
      public ActionPlan apply(ActionPlanDto actionPlanDto) {
        return actionPlanDto.toActionPlan();
      }
    }));
  }

  private ResourceDto findComponent(String componentKey){
    ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(componentKey));
    if (resourceDto == null) {
      throw new IllegalArgumentException("Component " + componentKey + " does not exists.");
    }
    return resourceDto;
  }

  private ResourceDto findRootProject(String componentKey){
    ResourceDto resourceDto = resourceDao.getRootProjectByComponentKey(componentKey);
    if (resourceDto == null) {
      throw new IllegalArgumentException("Component " + componentKey + " does not exists.");
    }
    return resourceDto;
  }
}
