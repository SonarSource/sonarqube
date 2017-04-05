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
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.notification.NotificationManager;

public class IssueUpdater {

  private final DbClient dbClient;
  private final IssueStorage issueStorage;
  private final NotificationManager notificationService;

  public IssueUpdater(DbClient dbClient, IssueStorage issueStorage, NotificationManager notificationService) {
    this.dbClient = dbClient;
    this.issueStorage = issueStorage;
    this.notificationService = notificationService;
  }

  public void saveIssue(DbSession session, DefaultIssue issue, IssueChangeContext context, @Nullable String comment) {
    issueStorage.save(session, issue);
    Optional<RuleDefinitionDto> rule = getRuleByKey(session, issue.getRuleKey());
    ComponentDto project = dbClient.componentDao().selectOrFailByUuid(session, issue.projectUuid());
    notificationService.scheduleForSending(new IssueChangeNotification()
      .setIssue(issue)
      .setChangeAuthorLogin(context.login())
      .setRuleName(rule.isPresent() ? rule.get().getName() : null)
      .setProject(project.getKey(), project.name())
      .setComponent(dbClient.componentDao().selectOrFailByUuid(session, issue.componentUuid()))
      .setComment(comment));
  }

  private Optional<RuleDefinitionDto> getRuleByKey(DbSession session, RuleKey ruleKey) {
    Optional<RuleDefinitionDto> rule = Optional.ofNullable(dbClient.ruleDao().selectDefinitionByKey(session, ruleKey).orElse(null));
    return (rule.isPresent() && rule.get().getStatus() != RuleStatus.REMOVED) ? rule : Optional.empty();
  }
}
