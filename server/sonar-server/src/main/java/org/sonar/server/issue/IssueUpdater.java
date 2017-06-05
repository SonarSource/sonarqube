/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.issue;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.ws.SearchResponseData;
import org.sonar.server.notification.NotificationManager;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

public class IssueUpdater {

  private final DbClient dbClient;
  private final IssueStorage issueStorage;
  private final NotificationManager notificationService;

  public IssueUpdater(DbClient dbClient, IssueStorage issueStorage, NotificationManager notificationService) {
    this.dbClient = dbClient;
    this.issueStorage = issueStorage;
    this.notificationService = notificationService;
  }

  /**
   * Same as {@link #saveIssue(DbSession, DefaultIssue, IssueChangeContext, String)} but populates the specified
   * {@link SearchResponseData} with the DTOs (rule and components) retrieved from DB to save the issue.
   */
  public SearchResponseData saveIssueAndPreloadSearchResponseData(DbSession session, DefaultIssue issue, IssueChangeContext context, @Nullable String comment) {
    Optional<RuleDefinitionDto> rule = getRuleByKey(session, issue.getRuleKey());
    ComponentDto project = dbClient.componentDao().selectOrFailByUuid(session, issue.projectUuid());
    ComponentDto component = dbClient.componentDao().selectOrFailByUuid(session, issue.componentUuid());
    IssueDto issueDto = saveIssue(session, issue, context, comment, rule, project, component);

    SearchResponseData preloadedSearchResponseData = new SearchResponseData(issueDto);
    rule.ifPresent(r -> preloadedSearchResponseData.setRules(singletonList(r)));
    preloadedSearchResponseData.addComponents(singleton(project));
    preloadedSearchResponseData.addComponents(singleton(component));
    return preloadedSearchResponseData;
  }

  public IssueDto saveIssue(DbSession session, DefaultIssue issue, IssueChangeContext context, @Nullable String comment) {
    Optional<RuleDefinitionDto> rule = getRuleByKey(session, issue.getRuleKey());
    ComponentDto project = dbClient.componentDao().selectOrFailByUuid(session, issue.projectUuid());
    ComponentDto component = dbClient.componentDao().selectOrFailByUuid(session, issue.componentUuid());
    return saveIssue(session, issue, context, comment, rule, project, component);
  }

  private IssueDto saveIssue(DbSession session, DefaultIssue issue, IssueChangeContext context, @Nullable String comment,
    Optional<RuleDefinitionDto> rule, ComponentDto project, ComponentDto component) {
    IssueDto issueDto = issueStorage.save(session, issue);
    notificationService.scheduleForSending(new IssueChangeNotification()
      .setIssue(issue)
      .setChangeAuthorLogin(context.login())
      .setRuleName(rule.map(RuleDefinitionDto::getName).orElse(null))
      .setProject(project.getKey(), project.name())
      .setComponent(component)
      .setComment(comment));
    return issueDto;
  }

  private Optional<RuleDefinitionDto> getRuleByKey(DbSession session, RuleKey ruleKey) {
    Optional<RuleDefinitionDto> rule = Optional.ofNullable(dbClient.ruleDao().selectDefinitionByKey(session, ruleKey).orElse(null));
    return (rule.isPresent() && rule.get().getStatus() != RuleStatus.REMOVED) ? rule : Optional.empty();
  }

}
