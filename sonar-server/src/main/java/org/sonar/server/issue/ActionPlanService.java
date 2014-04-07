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

package org.sonar.server.issue;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.ActionPlanDeadlineComparator;
import org.sonar.core.issue.ActionPlanStats;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.*;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class ActionPlanService implements ServerComponent {

  private final ActionPlanDao actionPlanDao;
  private final ActionPlanStatsDao actionPlanStatsDao;
  private final ResourceDao resourceDao;
  private final IssueDao issueDao;
  private final IssueUpdater issueUpdater;
  private final IssueStorage issueStorage;

  public ActionPlanService(ActionPlanDao actionPlanDao, ActionPlanStatsDao actionPlanStatsDao, ResourceDao resourceDao,
                           IssueDao issueDao, IssueUpdater issueUpdater, IssueStorage issueStorage) {
    this.actionPlanDao = actionPlanDao;
    this.actionPlanStatsDao = actionPlanStatsDao;
    this.resourceDao = resourceDao;
    this.issueDao = issueDao;
    this.issueUpdater = issueUpdater;
    this.issueStorage = issueStorage;
  }

  public ActionPlan create(ActionPlan actionPlan, UserSession userSession) {
    ResourceDto project = findProject(actionPlan.projectKey());
    checkUserIsProjectAdministrator(project.getKey(), userSession);
    actionPlanDao.save(ActionPlanDto.toActionDto(actionPlan, project.getId()));
    return actionPlan;
  }

  public ActionPlan update(ActionPlan actionPlan, UserSession userSession) {
    ResourceDto project = findProject(actionPlan.projectKey());
    checkUserIsProjectAdministrator(project.getKey(), userSession);
    actionPlanDao.update(ActionPlanDto.toActionDto(actionPlan, project.getId()));
    return actionPlan;
  }

  public void delete(String actionPlanKey, UserSession userSession) {
    ActionPlanDto dto = findActionPlanDto(actionPlanKey);
    checkUserIsProjectAdministrator(dto.getProjectKey(), userSession);
    unplanIssues(dto.toActionPlan(), userSession);
    actionPlanDao.delete(actionPlanKey);
  }

  /**
   * Unplan all issues linked to an action plan
   */
  private void unplanIssues(DefaultActionPlan actionPlan, UserSession userSession) {
    // Get all issues linked to this plan (need to disable pagination and authorization check)
    IssueQuery query = IssueQuery.builder().actionPlans(Arrays.asList(actionPlan.key())).requiredRole(null).build();
    List<IssueDto> dtos = issueDao.selectIssues(query);
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    List<DefaultIssue> issues = newArrayList();
    for (IssueDto issueDto : dtos) {
      DefaultIssue issue = issueDto.toDefaultIssue();
      // Unplan issue
      if (issueUpdater.plan(issue, null, context)) {
        issues.add(issue);
      }
    }
    // Save all issues
    issueStorage.save(issues);
  }

  public ActionPlan setStatus(String actionPlanKey, String status, UserSession userSession) {
    ActionPlanDto actionPlanDto = findActionPlanDto(actionPlanKey);
    checkUserIsProjectAdministrator(actionPlanDto.getProjectKey(), userSession);

    actionPlanDto.setStatus(status);
    actionPlanDto.setCreatedAt(new Date());
    actionPlanDao.update(actionPlanDto);
    return actionPlanDto.toActionPlan();
  }

  @CheckForNull
  public ActionPlan findByKey(String key, UserSession userSession) {
    ActionPlanDto actionPlanDto = actionPlanDao.findByKey(key);
    if (actionPlanDto == null) {
      return null;
    }
    checkUserCanAccessProject(actionPlanDto.getProjectKey(), userSession);
    return actionPlanDto.toActionPlan();
  }

  public List<ActionPlan> findByKeys(Collection<String> keys) {
    List<ActionPlanDto> actionPlanDtos = actionPlanDao.findByKeys(keys);
    return toActionPlans(actionPlanDtos);
  }

  public Collection<ActionPlan> findOpenByProjectKey(String projectKey, UserSession userSession) {
    ResourceDto project = findProject(projectKey);
    checkUserCanAccessProject(project.getKey(), userSession);

    List<ActionPlanDto> dtos = actionPlanDao.findOpenByProjectId(project.getId());
    List<ActionPlan> plans = toActionPlans(dtos);
    Collections.sort(plans, new ActionPlanDeadlineComparator());
    return plans;
  }

  public List<ActionPlanStats> findActionPlanStats(String projectKey, UserSession userSession) {
    ResourceDto project = findProject(projectKey);
    checkUserCanAccessProject(project.getKey(), userSession);

    List<ActionPlanStatsDto> actionPlanStatsDtos = actionPlanStatsDao.findByProjectId(project.getId());
    List<ActionPlanStats> actionPlanStats = newArrayList(Iterables.transform(actionPlanStatsDtos, new Function<ActionPlanStatsDto, ActionPlanStats>() {
      @Override
      public ActionPlanStats apply(ActionPlanStatsDto actionPlanStatsDto) {
        return actionPlanStatsDto.toActionPlanStat();
      }
    }));
    Collections.sort(actionPlanStats, new ActionPlanDeadlineComparator());
    return actionPlanStats;
  }

  public boolean isNameAlreadyUsedForProject(String name, String projectKey) {
    return !actionPlanDao.findByNameAndProjectId(name, findProject(projectKey).getId()).isEmpty();
  }

  private List<ActionPlan> toActionPlans(List<ActionPlanDto> actionPlanDtos) {
    return newArrayList(Iterables.transform(actionPlanDtos, new Function<ActionPlanDto, ActionPlan>() {
      @Override
      public ActionPlan apply(ActionPlanDto actionPlanDto) {
        return actionPlanDto.toActionPlan();
      }
    }));
  }

  private ActionPlanDto findActionPlanDto(String actionPlanKey) {
    ActionPlanDto actionPlanDto = actionPlanDao.findByKey(actionPlanKey);
    if (actionPlanDto == null) {
      throw new NotFoundException("Action plan " + actionPlanKey + " has not been found.");
    }
    return actionPlanDto;
  }

  private ResourceDto findProject(String projectKey) {
    ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(projectKey));
    if (resourceDto == null) {
      throw new NotFoundException("Project " + projectKey + " does not exists.");
    }
    return resourceDto;
  }

  private void checkUserCanAccessProject(String projectKey, UserSession userSession) {
    userSession.checkProjectPermission(UserRole.USER, projectKey);
  }

  private void checkUserIsProjectAdministrator(String projectKey, UserSession userSession) {
    userSession.checkProjectPermission(UserRole.ADMIN, projectKey);
  }

}
