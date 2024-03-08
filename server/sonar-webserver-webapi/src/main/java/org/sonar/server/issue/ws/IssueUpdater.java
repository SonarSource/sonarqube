/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue.ws;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.IssueChangePostProcessor;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Rule;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationSerializer;
import org.sonar.server.notification.NotificationManager;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.sonar.db.component.BranchType.PULL_REQUEST;

public class IssueUpdater {

  private final DbClient dbClient;
  private final WebIssueStorage issueStorage;
  private final NotificationManager notificationService;
  private final IssueChangePostProcessor issueChangePostProcessor;
  private final IssuesChangesNotificationSerializer notificationSerializer;

  public IssueUpdater(DbClient dbClient, WebIssueStorage issueStorage, NotificationManager notificationService,
    IssueChangePostProcessor issueChangePostProcessor, IssuesChangesNotificationSerializer notificationSerializer) {
    this.dbClient = dbClient;
    this.issueStorage = issueStorage;
    this.notificationService = notificationService;
    this.issueChangePostProcessor = issueChangePostProcessor;
    this.notificationSerializer = notificationSerializer;
  }

  public SearchResponseData saveIssueAndPreloadSearchResponseData(DbSession dbSession, IssueDto originalIssue, DefaultIssue newIssue, IssueChangeContext context) {
    BranchDto branch = getBranch(dbSession, newIssue);
    return saveIssueAndPreloadSearchResponseData(dbSession, originalIssue, newIssue, context, branch);
  }

  public SearchResponseData saveIssueAndPreloadSearchResponseData(DbSession dbSession, IssueDto originalIssue,
    DefaultIssue newIssue, IssueChangeContext context, BranchDto branch) {
    Optional<RuleDto> rule = getRuleByKey(dbSession, newIssue.getRuleKey());
    ComponentDto branchComponent = dbClient.componentDao().selectOrFailByUuid(dbSession, newIssue.projectUuid());
    ComponentDto component = getComponent(dbSession, newIssue, newIssue.componentUuid());
    IssueDto issueDto = doSaveIssue(dbSession, originalIssue, newIssue, context, rule.orElse(null), branchComponent, branch);

    SearchResponseData result = new SearchResponseData(issueDto);
    rule.ifPresent(r -> result.addRules(singletonList(r)));
    result.addComponents(singleton(branchComponent));
    result.addComponents(singleton(component));

    if (context.refreshMeasures()) {
      List<DefaultIssue> changedIssues = result.getIssues().stream().map(IssueDto::toDefaultIssue).toList();
      boolean isChangeFromWebhook = isNotEmpty(context.getWebhookSource());
      issueChangePostProcessor.process(dbSession, changedIssues, singleton(component), isChangeFromWebhook);
    }

    return result;
  }

  public BranchDto getBranch(DbSession dbSession, DefaultIssue issue) {
    String issueKey = issue.key();
    String componentUuid = issue.componentUuid();
    checkState(componentUuid != null, "Component uuid for issue key '%s' cannot be null", issueKey);
    Optional<ComponentDto> componentDto = dbClient.componentDao().selectByUuid(dbSession, componentUuid);
    checkState(componentDto.isPresent(), "Component not found for issue with key '%s'", issueKey);
    BranchDto branchDto = dbClient.branchDao().selectByUuid(dbSession, componentDto.get().branchUuid()).orElse(null);
    checkState(branchDto != null, "Branch not found for issue with key '%s'", issueKey);
    return branchDto;
  }

  private IssueDto doSaveIssue(DbSession session, IssueDto originalIssue, DefaultIssue issue, IssueChangeContext context,
    @Nullable RuleDto ruleDto, ComponentDto project, BranchDto branchDto) {
    IssueDto issueDto = issueStorage.save(session, singletonList(issue)).iterator().next();
    Date updateDate = issue.updateDate();
    if (
      // since this method is called after an update of the issue, date should never be null
      updateDate == null
      // name of rule is displayed in notification, rule must therefor be present
      || ruleDto == null
      // notification are not supported on PRs
      || !hasNotificationSupport(branchDto)
      || context.getWebhookSource() != null) {
      return issueDto;
    }

    Optional<UserDto> assignee = ofNullable(issue.assignee())
      .map(assigneeUuid -> dbClient.userDao().selectByUuid(session, assigneeUuid));
    UserDto author = ofNullable(context.userUuid())
      .map(authorUuid -> dbClient.userDao().selectByUuid(session, authorUuid))
      .orElseThrow(() -> new IllegalStateException("Can not find dto for change author " + context.userUuid()));
    IssuesChangesNotificationBuilder notificationBuilder = new IssuesChangesNotificationBuilder(singleton(
      new ChangedIssue.Builder(issue.key())
        .setNewStatus(issue.status())
        .setNewIssueStatus(IssueStatus.of(issue.status(), issue.resolution()))
        .setOldIssueStatus(originalIssue.getIssueStatus())
        .setAssignee(assignee.map(assigneeDto -> new User(assigneeDto.getUuid(), assigneeDto.getLogin(), assigneeDto.getName())).orElse(null))
        .setRule(new Rule(ruleDto.getKey(), RuleType.valueOfNullable(ruleDto.getType()), ruleDto.getName()))
        .setProject(new Project.Builder(project.uuid())
          .setKey(project.getKey())
          .setProjectName(project.name())
          .setBranchName(branchDto.isMain() ? null : branchDto.getKey())
          .build())
        .build()),
      new UserChange(updateDate.getTime(), new User(author.getUuid(), author.getLogin(), author.getName())));
    notificationService.scheduleForSending(notificationSerializer.serialize(notificationBuilder));
    return issueDto;
  }

  private static boolean hasNotificationSupport(BranchDto branch) {
    return branch.getBranchType() != PULL_REQUEST;
  }

  private ComponentDto getComponent(DbSession dbSession, DefaultIssue issue, @Nullable String componentUuid) {
    String issueKey = issue.key();
    checkState(componentUuid != null, "Issue '%s' has no component", issueKey);
    ComponentDto component = dbClient.componentDao().selectByUuid(dbSession, componentUuid).orElse(null);
    checkState(component != null, "Component uuid '%s' for issue key '%s' cannot be found", componentUuid, issueKey);
    return component;
  }

  private Optional<RuleDto> getRuleByKey(DbSession session, RuleKey ruleKey) {
    Optional<RuleDto> rule = dbClient.ruleDao().selectByKey(session, ruleKey);
    return (rule.isPresent() && rule.get().getStatus() != RuleStatus.REMOVED) ? rule : Optional.empty();
  }

}
