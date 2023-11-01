/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.rule.RuleTagFormat;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.status.SimpleStatus;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserIdDto;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Updates issue fields and chooses if changes must be kept in history.
 */
@ServerSide
@ComputeEngineSide
public class IssueFieldsSetter {

  public static final String UNUSED = "";
  public static final String SEVERITY = "severity";
  public static final String TYPE = "type";
  public static final String CLEAN_CODE_ATTRIBUTE = "cleanCodeAttribute";
  public static final String ASSIGNEE = "assignee";

  /**
   * @deprecated use {@link IssueFieldsSetter#SIMPLE_STATUS} instead
   */
  @Deprecated(since = "10.4")
  public static final String RESOLUTION = "resolution";
  /**
   * @deprecated use {@link IssueFieldsSetter#SIMPLE_STATUS} instead
   */
  @Deprecated(since = "10.4")
  public static final String STATUS = "status";
  public static final String SIMPLE_STATUS = "simpleStatus";
  public static final String AUTHOR = "author";
  public static final String FILE = "file";
  public static final String FROM_BRANCH = "from_branch";

  /**
   * It should be renamed to 'effort', but it hasn't been done to prevent a massive update in database
   */
  public static final String TECHNICAL_DEBT = "technicalDebt";
  public static final String LINE = "line";
  public static final String TAGS = "tags";
  public static final String CODE_VARIANTS = "code_variants";

  private static final Joiner CHANGELOG_LIST_JOINER = Joiner.on(" ").skipNulls();

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
  public boolean setNewAssignee(DefaultIssue issue, @Nullable UserIdDto userId, IssueChangeContext context) {
    if (userId == null) {
      return false;
    }
    checkState(issue.assignee() == null, "It's not possible to update the assignee with this method, please use assign()");
    issue.setFieldChange(context, ASSIGNEE, UNUSED, userId.getUuid());
    issue.setAssigneeUuid(userId.getUuid());
    issue.setAssigneeLogin(userId.getLogin());
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

  public boolean setRuleDescriptionContextKey(DefaultIssue issue, @Nullable String previousContextKey) {
    String currentContextKey = issue.getRuleDescriptionContextKey().orElse(null);
    issue.setRuleDescriptionContextKey(previousContextKey);
    if (!Objects.equals(currentContextKey, previousContextKey)) {
      issue.setRuleDescriptionContextKey(currentContextKey);
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  /**
   * New value will be set if the locations are different, ignoring the hashes. If that's the case, we mark the issue as changed,
   * and we also flag that the locations have changed, so that we calculate all the hashes later, in an efficient way.
   * WARNING: It is possible that the hashes changes without the text ranges changing, but for optimization we take that risk.
   *
   * @see ComputeLocationHashesVisitor
   */
  public boolean setLocations(DefaultIssue issue, @Nullable Object locations) {
    if (!locationsEqualsIgnoreHashes(locations, issue.getLocations())) {
      issue.setLocations(locations);
      issue.setChanged(true);
      issue.setLocationsChanged(true);
      return true;
    }
    return false;
  }

  private static boolean locationsEqualsIgnoreHashes(@Nullable Object l1, @Nullable DbIssues.Locations l2) {
    if (l1 == null && l2 == null) {
      return true;
    }

    if (l2 == null || !(l1 instanceof DbIssues.Locations)) {
      return false;
    }

    DbIssues.Locations l1c = (DbIssues.Locations) l1;
    if (!Objects.equals(l1c.getTextRange(), l2.getTextRange()) || l1c.getFlowCount() != l2.getFlowCount()) {
      return false;
    }

    for (int i = 0; i < l1c.getFlowCount(); i++) {
      if (l1c.getFlow(i).getLocationCount() != l2.getFlow(i).getLocationCount()) {
        return false;
      }
      for (int j = 0; j < l1c.getFlow(i).getLocationCount(); j++) {
        if (!locationEqualsIgnoreHashes(l1c.getFlow(i).getLocation(j), l2.getFlow(i).getLocation(j))) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean locationEqualsIgnoreHashes(DbIssues.Location l1, DbIssues.Location l2) {
    return Objects.equals(l1.getComponentId(), l2.getComponentId()) && Objects.equals(l1.getTextRange(), l2.getTextRange()) && Objects.equals(l1.getMsg(), l2.getMsg());
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

  public boolean setSimpleStatus(DefaultIssue issue, @Nullable SimpleStatus previousSimpleStatus, @Nullable SimpleStatus newSimpleStatus, IssueChangeContext context) {
    if (!Objects.equals(newSimpleStatus, previousSimpleStatus)) {
      //Currently, simple status is not persisted in database, but is considered as an issue change
      issue.setFieldChange(context, SIMPLE_STATUS, previousSimpleStatus, issue.getSimpleStatus());
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

  public boolean setMessageFormattings(DefaultIssue issue, @Nullable Object issueMessageFormattings, IssueChangeContext context) {
    if (!messageFormattingsEqualsIgnoreHashes(issueMessageFormattings, issue.getMessageFormattings())) {
      issue.setMessageFormattings(issueMessageFormattings);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  private static boolean messageFormattingsEqualsIgnoreHashes(@Nullable Object l1, @Nullable DbIssues.MessageFormattings l2) {
    if (l1 == null && l2 == null) {
      return true;
    }

    if (l2 == null || !(l1 instanceof DbIssues.MessageFormattings)) {
      return false;
    }

    DbIssues.MessageFormattings l1c = (DbIssues.MessageFormattings) l1;

    if (!Objects.equals(l1c.getMessageFormattingCount(), l2.getMessageFormattingCount())) {
      return false;
    }

    for (int i = 0; i < l1c.getMessageFormattingCount(); i++) {
      if (l1c.getMessageFormatting(i).getStart() != l2.getMessageFormatting(i).getStart()
          || l1c.getMessageFormatting(i).getEnd() != l2.getMessageFormatting(i).getEnd()
          || l1c.getMessageFormatting(i).getType() != l2.getMessageFormatting(i).getType()) {
        return false;
      }
    }
    return true;
  }

  public boolean setPastMessage(DefaultIssue issue, @Nullable String previousMessage, @Nullable Object previousMessageFormattings, IssueChangeContext context) {
    String currentMessage = issue.message();
    DbIssues.MessageFormattings currentMessageFormattings = issue.getMessageFormattings();
    issue.setMessage(previousMessage);
    issue.setMessageFormattings(previousMessageFormattings);
    boolean changed = setMessage(issue, currentMessage, context);
    return setMessageFormattings(issue, currentMessageFormattings, context) || changed;
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

  public boolean setTags(DefaultIssue issue, Collection<String> tags, IssueChangeContext context) {
    Set<String> newTags = RuleTagFormat.validate(tags);

    Set<String> oldTags = new HashSet<>(issue.tags());
    if (!oldTags.equals(newTags)) {
      issue.setFieldChange(context, TAGS,
        oldTags.isEmpty() ? null : CHANGELOG_LIST_JOINER.join(oldTags),
        newTags.isEmpty() ? null : CHANGELOG_LIST_JOINER.join(newTags));
      issue.setTags(newTags);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      issue.setSendNotifications(true);
      return true;
    }
    return false;
  }

  public boolean setCodeVariants(DefaultIssue issue, Set<String> currentCodeVariants, IssueChangeContext context) {
    Set<String> newCodeVariants = getNewCodeVariants(issue);
    if (!currentCodeVariants.equals(newCodeVariants)) {
      issue.setFieldChange(context, CODE_VARIANTS,
        currentCodeVariants.isEmpty() ? null : CHANGELOG_LIST_JOINER.join(currentCodeVariants),
        newCodeVariants.isEmpty() ? null : CHANGELOG_LIST_JOINER.join(newCodeVariants));
      issue.setCodeVariants(newCodeVariants);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      issue.setSendNotifications(true);
      return true;
    }
    return false;
  }

  public boolean setImpacts(DefaultIssue issue, Map<SoftwareQuality, Severity> previousImpacts, IssueChangeContext context) {
    Map<SoftwareQuality, Severity> currentImpacts = new EnumMap<>(issue.impacts());
    if (!previousImpacts.equals(currentImpacts)) {
      issue.replaceImpacts(currentImpacts);
      issue.setUpdateDate(context.date());
      issue.setChanged(true);
      return true;
    }
    return false;
  }

  public boolean setCleanCodeAttribute(DefaultIssue raw, @Nullable CleanCodeAttribute previousCleanCodeAttribute, IssueChangeContext changeContext) {
    CleanCodeAttribute newCleanCodeAttribute = requireNonNull(raw.getCleanCodeAttribute());
    if (Objects.equals(previousCleanCodeAttribute, newCleanCodeAttribute)) {
      return false;
    }
    raw.setFieldChange(changeContext, CLEAN_CODE_ATTRIBUTE, previousCleanCodeAttribute, newCleanCodeAttribute.name());
    raw.setCleanCodeAttribute(newCleanCodeAttribute);
    raw.setUpdateDate(changeContext.date());
    raw.setChanged(true);
    return true;

  }

  private static Set<String> getNewCodeVariants(DefaultIssue issue) {
    Set<String> issueCodeVariants = issue.codeVariants();
    if (issueCodeVariants == null) {
      return Set.of();
    }
    return issueCodeVariants.stream()
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .collect(Collectors.toSet());
  }

  public void setIssueComponent(DefaultIssue issue, String newComponentUuid, String newComponentKey, Date updateDate) {
    if (!Objects.equals(newComponentUuid, issue.componentUuid())) {
      issue.setComponentUuid(newComponentUuid);
      issue.setUpdateDate(updateDate);
      issue.setChanged(true);
    }

    // other fields (such as module, modulePath, componentKey) are read-only and set/reset for consistency only
    issue.setComponentKey(newComponentKey);
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
