/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.base.Joiner;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.rule.RuleTagFormat;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Updates issue fields and chooses if changes must be kept in history.
 */
@ServerSide
@ComputeEngineSide
public class IssueFieldsSetter {

  public static final String UNUSED = "";
  public static final String SEVERITY = "severity";
  public static final String TYPE = "type";
  public static final String ASSIGNEE = "assignee";
  public static final String RESOLUTION = "resolution";
  public static final String STATUS = "status";
  public static final String AUTHOR = "author";
  public static final String FILE = "file";
  public static final String FROM_LONG_BRANCH = "from_long_branch";
  public static final String FROM_SHORT_BRANCH = "from_short_branch";

  /**
   * It should be renamed to 'effort', but it hasn't been done to prevent a massive update in database
   */
  public static final String TECHNICAL_DEBT = "technicalDebt";
  public static final String LINE = "line";
  public static final String TAGS = "tags";

  private static final Joiner CHANGELOG_TAG_JOINER = Joiner.on(" ").skipNulls();

  public boolean setType(DefaultIssue issue, RuleType type, IssueChangeContext context) {
    if (!Objects.equals(type, issue.type())) {
      issue.setFieldChange(context, TYPE, issue.type(), type);
      issue.setType(type);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setSeverity(DefaultIssue issue, String severity, IssueChangeContext context) {
    checkState(!issue.manualSeverity(), "Severity can't be changed");
    if (!Objects.equals(severity, issue.severity())) {
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
    if (!issue.manualSeverity() || !Objects.equals(severity, issue.severity())) {
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

  public boolean assign(DefaultIssue issue, @Nullable UserDto user, IssueChangeContext context) {
    String assigneeUuid = user != null ? user.getUuid() : null;
    if (!Objects.equals(assigneeUuid, issue.assignee())) {
      String newAssigneeName = user == null ? null : user.getName();
      issue.setFieldChange(context, ASSIGNEE, UNUSED, newAssigneeName);
      issue.setAssigneeUuid(user != null ? user.getUuid() : null);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      issue.setSendNotifications(true);
      return true;
    }
    return false;
  }

  /**
   * Used to set the assignee when it was null
   */
  public boolean setNewAssignee(DefaultIssue issue, @Nullable String newAssigneeUuid, IssueChangeContext context) {
    if (newAssigneeUuid == null) {
      return false;
    }
    checkState(issue.assignee() == null, "It's not possible to update the assignee with this method, please use assign()");
    issue.setFieldChange(context, ASSIGNEE, UNUSED, newAssigneeUuid);
    issue.setAssigneeUuid(newAssigneeUuid);
    issue.setUpdateDate(context.date());
    issue.setChanged(true);
    issue.setSendNotifications(true);
    return true;
  }

  public boolean unsetLine(DefaultIssue issue, IssueChangeContext context) {
    Integer currentValue = issue.line();
    if (currentValue != null) {
      issue.setFieldChange(context, LINE, currentValue, "");
      issue.setLine(null);
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setPastLine(DefaultIssue issue, @Nullable Integer previousLine) {
    Integer currentLine = issue.line();
    issue.setLine(previousLine);
    if (!Objects.equals(currentLine, previousLine)) {
      issue.setLine(currentLine);
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setLocations(DefaultIssue issue, @Nullable Object locations) {
    if (!Objects.equals(locations, issue.getLocations())) {
      issue.setLocations(locations);
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setPastLocations(DefaultIssue issue, @Nullable Object previousLocations) {
    Object currentLocations = issue.getLocations();
    issue.setLocations(previousLocations);
    return setLocations(issue, currentLocations);

  }

  public boolean setResolution(DefaultIssue issue, @Nullable String resolution, IssueChangeContext context) {
    if (!Objects.equals(resolution, issue.resolution())) {
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
    if (!Objects.equals(status, issue.status())) {
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
    if (!Objects.equals(authorLogin, issue.authorLogin())) {
      issue.setFieldChange(context, AUTHOR, issue.authorLogin(), authorLogin);
      issue.setAuthorLogin(authorLogin);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      // do not send notifications to prevent spam when installing the developer cockpit plugin
      return true;
    }
    return false;
  }

  /**
   * Used to set the author when it was null
   */
  public boolean setNewAuthor(DefaultIssue issue, @Nullable String newAuthorLogin, IssueChangeContext context) {
    if (isNullOrEmpty(newAuthorLogin)) {
      return false;
    }
    checkState(issue.authorLogin() == null, "It's not possible to update the author with this method, please use setAuthorLogin()");
    issue.setFieldChange(context, AUTHOR, null, newAuthorLogin);
    issue.setAuthorLogin(newAuthorLogin);
    issue.setUpdateDate(context.date());
    issue.setChanged(true);
    // do not send notifications to prevent spam when installing the developer cockpit plugin
    return true;
  }

  public boolean setMessage(DefaultIssue issue, @Nullable String s, IssueChangeContext context) {
    if (!Objects.equals(s, issue.message())) {
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
    issue.addComment(DefaultIssueComment.create(issue.key(), context.userUuid(), text));
    issue.setUpdateDate(context.date());
    issue.setChanged(true);
  }

  public void setCloseDate(DefaultIssue issue, @Nullable Date d, IssueChangeContext context) {
    if (relevantDateDifference(d, issue.closeDate())) {
      issue.setCloseDate(d);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
    }
  }

  public void setCreationDate(DefaultIssue issue, Date d, IssueChangeContext context) {
    if (relevantDateDifference(d, issue.creationDate())) {
      issue.setCreationDate(d);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
    }
  }

  public boolean setGap(DefaultIssue issue, @Nullable Double d, IssueChangeContext context) {
    if (!Objects.equals(d, issue.gap())) {
      issue.setGap(d);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      // Do not send notifications to prevent spam when installing the SQALE plugin,
      // and do not complete the changelog (for the moment)
      return true;
    }
    return false;
  }

  public boolean setPastGap(DefaultIssue issue, @Nullable Double previousGap, IssueChangeContext context) {
    Double currentGap = issue.gap();
    issue.setGap(previousGap);
    return setGap(issue, currentGap, context);
  }

  public boolean setEffort(DefaultIssue issue, @Nullable Duration value, IssueChangeContext context) {
    Duration oldValue = issue.effort();
    if (!Objects.equals(value, oldValue)) {
      issue.setEffort(value);
      issue.setFieldChange(context, TECHNICAL_DEBT, oldValue != null ? oldValue.toMinutes() : null, value != null ? value.toMinutes() : null);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setPastEffort(DefaultIssue issue, @Nullable Duration previousEffort, IssueChangeContext context) {
    Duration currentEffort = issue.effort();
    issue.setEffort(previousEffort);
    return setEffort(issue, currentEffort, context);
  }

  public boolean setAttribute(DefaultIssue issue, String key, @Nullable String value, IssueChangeContext context) {
    String oldValue = issue.attribute(key);
    if (!Objects.equals(oldValue, value)) {
      issue.setFieldChange(context, key, oldValue, value);
      issue.setAttribute(key, value);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setTags(DefaultIssue issue, Collection<String> tags, IssueChangeContext context) {
    Set<String> newTags = RuleTagFormat.validate(tags);

    Set<String> oldTags = new HashSet<>(issue.tags());
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

  public boolean setIssueMoved(DefaultIssue issue, String newComponentUuid, IssueChangeContext context) {
    if (!Objects.equals(newComponentUuid, issue.componentUuid())) {
      issue.setFieldChange(context, FILE, issue.componentUuid(), newComponentUuid);
      issue.setComponentUuid(newComponentUuid);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  private static boolean relevantDateDifference(@Nullable Date left, @Nullable Date right) {
    return !Objects.equals(truncateMillis(left), truncateMillis(right));
  }

  private static Date truncateMillis(@Nullable Date d) {
    if (d == null) {
      return null;
    }
    return Date.from(d.toInstant().truncatedTo(ChronoUnit.SECONDS));
  }
}
