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
package org.sonar.core.issue;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.server.rule.RuleTagFormat;
import org.sonar.api.user.User;
import org.sonar.api.utils.Duration;

import javax.annotation.Nullable;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

/**
 * Updates issue fields and chooses if changes must be kept in history.
 */
@BatchSide
@ServerSide
public class IssueUpdater {

  public static final String UNUSED = "";
  public static final String SEVERITY = "severity";
  public static final String ASSIGNEE = "assignee";
  public static final String RESOLUTION = "resolution";
  public static final String STATUS = "status";
  public static final String AUTHOR = "author";
  public static final String ACTION_PLAN = "actionPlan";
  public static final String TECHNICAL_DEBT = "technicalDebt";
  public static final String TAGS = "tags";

  private static final Joiner CHANGELOG_TAG_JOINER = Joiner.on(" ").skipNulls();

  public boolean setSeverity(DefaultIssue issue, String severity, IssueChangeContext context) {
    if (issue.manualSeverity()) {
      throw new IllegalStateException("Severity can't be changed");
    }
    if (!Objects.equal(severity, issue.severity())) {
      issue.setFieldChange(context, SEVERITY, issue.severity(), severity);
      issue.setSeverity(severity);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setPastSeverity(DefaultIssue issue, @Nullable String previousSeverity, IssueChangeContext context) {
    String currentSeverity = issue.severity();
    issue.setSeverity(previousSeverity);
    return setSeverity(issue, currentSeverity, context);
  }

  public boolean setManualSeverity(DefaultIssue issue, String severity, IssueChangeContext context) {
    if (!issue.manualSeverity() || !Objects.equal(severity, issue.severity())) {
      issue.setFieldChange(context, SEVERITY, issue.severity(), severity);
      issue.setSeverity(severity);
      issue.setManualSeverity(true);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      issue.setSendNotifications(true);
      return true;
    }
    return false;
  }

  public boolean assign(DefaultIssue issue, @Nullable User user, IssueChangeContext context) {
    String sanitizedAssignee = null;
    if (user != null) {
      sanitizedAssignee = StringUtils.defaultIfBlank(user.login(), null);
    }
    if (!Objects.equal(sanitizedAssignee, issue.assignee())) {
      String newAssignee = user != null ? user.name() : null;
      issue.setFieldChange(context, ASSIGNEE, UNUSED, newAssignee);
      issue.setAssignee(sanitizedAssignee);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      issue.setSendNotifications(true);
      return true;
    }
    return false;
  }

  public boolean setLine(DefaultIssue issue, @Nullable Integer line) {
    if (!Objects.equal(line, issue.line())) {
      issue.setLine(line);
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setPastLine(DefaultIssue issue, @Nullable Integer previousLine) {
    Integer currentLine = issue.line();
    issue.setLine(previousLine);
    return setLine(issue, currentLine);
  }

  public boolean setResolution(DefaultIssue issue, @Nullable String resolution, IssueChangeContext context) {
    if (!Objects.equal(resolution, issue.resolution())) {
      issue.setFieldChange(context, RESOLUTION, issue.resolution(), resolution);
      issue.setResolution(resolution);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      issue.setSendNotifications(true);
      return true;
    }
    return false;
  }

  public boolean setStatus(DefaultIssue issue, String status, IssueChangeContext context) {
    if (!Objects.equal(status, issue.status())) {
      issue.setFieldChange(context, STATUS, issue.status(), status);
      issue.setStatus(status);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      issue.setSendNotifications(true);
      return true;
    }
    return false;
  }

  public boolean setAuthorLogin(DefaultIssue issue, @Nullable String authorLogin, IssueChangeContext context) {
    if (!Objects.equal(authorLogin, issue.authorLogin())) {
      issue.setFieldChange(context, AUTHOR, issue.authorLogin(), authorLogin);
      issue.setAuthorLogin(authorLogin);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      // do not send notifications to prevent spam when installing the developer cockpit plugin
      return true;
    }
    return false;
  }

  public boolean setMessage(DefaultIssue issue, @Nullable String s, IssueChangeContext context) {
    if (!Objects.equal(s, issue.message())) {
      issue.setMessage(s);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setPastMessage(DefaultIssue issue, @Nullable String previousMessage, IssueChangeContext context) {
    String currentMessage = issue.message();
    issue.setMessage(previousMessage);
    return setMessage(issue, currentMessage, context);
  }

  public void addComment(DefaultIssue issue, String text, IssueChangeContext context) {
    issue.addComment(DefaultIssueComment.create(issue.key(), context.login(), text));
    issue.setUpdateDate(context.date());
    issue.setChanged(true);
  }

  public void setCloseDate(DefaultIssue issue, @Nullable Date d, IssueChangeContext context) {
    Date dateWithoutMilliseconds = d == null ? null : DateUtils.truncate(d, Calendar.SECOND);
    if (!Objects.equal(dateWithoutMilliseconds, issue.closeDate())) {
      issue.setCloseDate(d);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
    }
  }

  public boolean setEffortToFix(DefaultIssue issue, @Nullable Double d, IssueChangeContext context) {
    if (!Objects.equal(d, issue.effortToFix())) {
      issue.setEffortToFix(d);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      // Do not send notifications to prevent spam when installing the SQALE plugin,
      // and do not complete the changelog (for the moment)
      return true;
    }
    return false;
  }

  public boolean setPastEffortToFix(DefaultIssue issue, @Nullable Double previousEffort, IssueChangeContext context) {
    Double currentEffort = issue.effortToFix();
    issue.setEffortToFix(previousEffort);
    return setEffortToFix(issue, currentEffort, context);
  }

  public boolean setTechnicalDebt(DefaultIssue issue, @Nullable Duration value, IssueChangeContext context) {
    Duration oldValue = issue.debt();
    if (!Objects.equal(value, oldValue)) {
      issue.setDebt(value != null ? value : null);
      issue.setFieldChange(context, TECHNICAL_DEBT, oldValue != null ? oldValue.toMinutes() : null, value != null ? value.toMinutes() : null);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setPastTechnicalDebt(DefaultIssue issue, @Nullable Duration previousTechnicalDebt, IssueChangeContext context) {
    Duration currentTechnicalDebt = issue.debt();
    issue.setDebt(previousTechnicalDebt);
    return setTechnicalDebt(issue, currentTechnicalDebt, context);
  }

  public boolean setAttribute(DefaultIssue issue, String key, @Nullable String value, IssueChangeContext context) {
    String oldValue = issue.attribute(key);
    if (!Objects.equal(oldValue, value)) {
      issue.setFieldChange(context, key, oldValue, value);
      issue.setAttribute(key, value);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean plan(DefaultIssue issue, @Nullable ActionPlan actionPlan, IssueChangeContext context) {
    String sanitizedActionPlanKey = null;
    if (actionPlan != null) {
      sanitizedActionPlanKey = StringUtils.defaultIfBlank(actionPlan.key(), null);
    }
    if (!Objects.equal(sanitizedActionPlanKey, issue.actionPlanKey())) {
      String newActionPlanName = actionPlan != null ? actionPlan.name() : null;
      issue.setFieldChange(context, ACTION_PLAN, UNUSED, newActionPlanName);
      issue.setActionPlanKey(sanitizedActionPlanKey);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      issue.setSendNotifications(true);
      return true;
    }
    return false;
  }

  public boolean setProject(DefaultIssue issue, String projectKey, IssueChangeContext context) {
    if (!Objects.equal(projectKey, issue.projectKey())) {
      issue.setProjectKey(projectKey);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setPastProject(DefaultIssue issue, String previousKey, IssueChangeContext context) {
    String currentProjectKey = issue.projectKey();
    issue.setProjectKey(previousKey);
    return setProject(issue, currentProjectKey, context);
  }

  public boolean setTags(DefaultIssue issue, Collection<String> tags, IssueChangeContext context) {
    Set<String> newTags = Sets.newHashSet(Collections2.transform(
      Collections2.filter(tags, new Predicate<String>() {
        @Override
        public boolean apply(String tag) {
          return tag != null && !tag.isEmpty();
        }
      }), new Function<String, String>() {
        @Override
        public String apply(String tag) {
          String lowerCaseTag = tag.toLowerCase();
          RuleTagFormat.validate(lowerCaseTag);
          return lowerCaseTag;
        }
      }));

    Set<String> oldTags = Sets.newHashSet(issue.tags());

    if (!oldTags.equals(newTags)) {
      issue.setFieldChange(context, TAGS,
        oldTags.isEmpty() ? null : CHANGELOG_TAG_JOINER.join(oldTags),
        newTags.isEmpty() ? null : CHANGELOG_TAG_JOINER.join(newTags));
      issue.setTags(newTags);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      issue.setSendNotifications(true);
      return true;
    }
    return false;
  }

}
