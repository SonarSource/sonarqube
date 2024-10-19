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
package org.sonar.server.issue.notification;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.core.i18n.I18n;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.AnalysisChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;

import static com.google.common.base.Preconditions.checkState;
import static java.util.function.Function.identity;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.server.issue.notification.RuleGroup.ISSUES;
import static org.sonar.server.issue.notification.RuleGroup.SECURITY_HOTSPOTS;
import static org.sonar.server.issue.notification.RuleGroup.resolveGroup;

/**
 * Creates email message for notification "Changes on my issues".
 */
public class ChangesOnMyIssuesEmailTemplate extends IssueChangesEmailTemplate {
  private static final String NOTIFICATION_NAME_I18N_KEY = "notification.dispatcher.ChangesOnMyIssue";

  public ChangesOnMyIssuesEmailTemplate(I18n i18n, EmailSettings settings) {
    super(i18n, settings);
  }

  @Override
  @CheckForNull
  public EmailMessage format(Notification notif) {
    if (!(notif instanceof ChangesOnMyIssuesNotification)) {
      return null;
    }

    ChangesOnMyIssuesNotification notification = (ChangesOnMyIssuesNotification) notif;

    if (notification.getChange() instanceof AnalysisChange) {
      checkState(!notification.getChangedIssues().isEmpty(), "changedIssues can't be empty");
      return formatAnalysisNotification(notification.getChangedIssues().keySet().iterator().next(), notification);
    }
    return formatMultiProject(notification);
  }

  private EmailMessage formatAnalysisNotification(Project project, ChangesOnMyIssuesNotification notification) {
    return new EmailMessage()
      .setMessageId("changes-on-my-issues/" + project.getKey())
      .setSubject(buildAnalysisSubject(project))
      .setHtmlMessage(buildAnalysisMessage(project, notification));
  }

  private static String buildAnalysisSubject(Project project) {
    return "Analysis has changed some of your issues in " + toSubject(project);
  }

  private String buildAnalysisMessage(Project project, ChangesOnMyIssuesNotification notification) {
    String projectParams = toUrlParams(project);

    StringBuilder sb = new StringBuilder();
    paragraph(sb, s -> s.append("Hi,"));
    paragraph(sb, s -> s.append("An analysis has updated ").append(issuesOrAnIssue(notification.getChangedIssues()))
      .append(" assigned to you:"));

    ListMultimap<String, ChangedIssue> issuesByNewStatus = notification.getChangedIssues().values().stream()
      .collect(index(changedIssue -> STATUS_CLOSED.equals(changedIssue.getNewStatus()) ? STATUS_CLOSED : STATUS_OPEN, t -> t));

    List<ChangedIssue> closedIssues = issuesByNewStatus.get(STATUS_CLOSED);
    if (!closedIssues.isEmpty()) {
      paragraph(sb, s -> s.append("Closed ").append(issueOrIssues(closedIssues)).append(":"));
      addIssuesByRule(sb, closedIssues, projectIssuePageHref(projectParams));
    }
    List<ChangedIssue> openIssues = issuesByNewStatus.get(STATUS_OPEN);
    if (!openIssues.isEmpty()) {
      paragraph(sb, s -> s.append("Open ").append(issueOrIssues(openIssues)).append(":"));
      addIssuesByRule(sb, openIssues, projectIssuePageHref(projectParams));
    }

    addFooter(sb, NOTIFICATION_NAME_I18N_KEY);

    return sb.toString();
  }

  private EmailMessage formatMultiProject(ChangesOnMyIssuesNotification notification) {
    User user = ((UserChange) notification.getChange()).getUser();
    return new EmailMessage()
      .setFrom(user.getName().orElse(user.getLogin()))
      .setMessageId("changes-on-my-issues")
      .setSubject("A manual update has changed some of your issues/hotspots")
      .setHtmlMessage(buildMultiProjectMessage(notification));
  }

  private String buildMultiProjectMessage(ChangesOnMyIssuesNotification notification) {
    ListMultimap<RuleGroup, ChangedIssue> issuesAndHotspots = notification.getChangedIssues().values().stream()
      .collect(index(changedIssue -> resolveGroup(changedIssue.getRule().getRuleType()), identity()));

    List<ChangedIssue> issues = issuesAndHotspots.get(ISSUES);
    List<ChangedIssue> hotspots = issuesAndHotspots.get(SECURITY_HOTSPOTS);

    StringBuilder sb = new StringBuilder();
    paragraph(sb, s -> s.append("Hi,"));
    paragraph(sb, s -> s.append("A manual change has updated ").append(RuleGroup.formatIssuesOrHotspots(issues, hotspots)).append(" assigned to you:"));

    addIssuesAndHotspotsByProjectThenRule(sb, notification.getChangedIssues());

    addFooter(sb, NOTIFICATION_NAME_I18N_KEY);

    return sb.toString();
  }

  private static String issuesOrAnIssue(SetMultimap<Project, ChangedIssue> changedIssues) {
    if (changedIssues.size() > 1) {
      return "issues";
    }
    return "an issue";
  }

}
