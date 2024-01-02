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
package org.sonar.core.issue;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.status.IssueStatus;
import org.sonar.core.issue.tracking.Trackable;

import static org.sonar.api.utils.DateUtils.truncateToSeconds;

public class DefaultIssue implements Issue, Trackable, org.sonar.api.ce.measure.Issue {

  private String key = null;
  private RuleType type = null;
  private String componentUuid = null;
  private String componentKey = null;

  private String projectUuid = null;
  private String projectKey = null;

  private RuleKey ruleKey = null;
  private String language = null;
  private String severity = null;
  private boolean manualSeverity = false;
  private String message = null;
  private Object messageFormattings = null;
  private Integer line = null;
  private Double gap = null;
  private Duration effort = null;
  private String status = null;
  private String resolution = null;
  private String assigneeUuid = null;
  private String assigneeLogin = null;
  private String checksum = null;
  private String authorLogin = null;
  private List<DefaultIssueComment> comments = null;
  private Set<String> tags = null;
  private Set<String> codeVariants = null;
  // temporarily an Object as long as DefaultIssue is used by sonar-batch
  private Object locations = null;

  private boolean isFromExternalRuleEngine = false;

  // FUNCTIONAL DATES
  private Date creationDate = null;
  private Date updateDate = null;
  private Date closeDate = null;

  // Current changes
  private FieldDiffs currentChange = null;

  // all changes
  // -- contains only current change (if any) on CE side unless reopening a closed issue or copying issue from base branch
  // when analyzing a branch from the first time
  private List<FieldDiffs> changes = null;

  // true if the issue did not exist in the previous scan.
  private boolean isNew = true;

  // true if the issue is on a changed line on a branch using the reference branch new code strategy
  private boolean isOnChangedLine = false;

  // true if the issue is being copied between branch
  private boolean isCopied = false;

  // true if any of the locations have changed (ignoring hashes)
  private boolean locationsChanged = false;

  // True if the issue did exist in the previous scan but not in the current one. That means
  // that this issue should be closed.
  private boolean beingClosed = false;

  private boolean onDisabledRule = false;

  // true if some fields have been changed since the previous scan
  private boolean isChanged = false;

  // true if notifications have to be sent
  private boolean sendNotifications = false;

  // Date when issue was loaded from db (only when isNew=false)
  private Long selectedAt = null;

  private boolean quickFixAvailable = false;
  private boolean isNewCodeReferenceIssue = false;

  // true if the issue is no longer new in its branch
  private boolean isNoLongerNewCodeReferenceIssue = false;

  private String ruleDescriptionContextKey = null;

  private String anticipatedTransitionUuid = null;

  private Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts = new EnumMap<>(SoftwareQuality.class);
  private CleanCodeAttribute cleanCodeAttribute = null;

  @Override
  public String key() {
    return key;
  }

  public DefaultIssue setKey(String key) {
    this.key = key;
    return this;
  }

  @Override
  public RuleType type() {
    return type;
  }

  @Override
  public Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts() {
    return impacts;
  }

  public DefaultIssue replaceImpacts(Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts) {
    this.impacts.clear();
    this.impacts.putAll(impacts);
    return this;
  }

  public DefaultIssue addImpact(SoftwareQuality softwareQuality, org.sonar.api.issue.impact.Severity severity) {
    impacts.put(softwareQuality, severity);
    return this;
  }

  public DefaultIssue setType(RuleType type) {
    this.type = type;
    return this;
  }

  /**
   * Can be null on Views or Devs
   */
  @Override
  @CheckForNull
  public String componentUuid() {
    return componentUuid;
  }

  public DefaultIssue setComponentUuid(@Nullable String s) {
    this.componentUuid = s;
    return this;
  }

  @Override
  public String componentKey() {
    return componentKey;
  }

  public DefaultIssue setComponentKey(String s) {
    this.componentKey = s;
    return this;
  }

  @Override
  public String projectUuid() {
    return projectUuid;
  }

  public DefaultIssue setProjectUuid(String s) {
    this.projectUuid = s;
    return this;
  }

  @Override
  public String projectKey() {
    return projectKey;
  }

  public DefaultIssue setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  @Override
  public RuleKey ruleKey() {
    return ruleKey;
  }

  public DefaultIssue setRuleKey(RuleKey k) {
    this.ruleKey = k;
    return this;
  }

  @Override
  @CheckForNull
  public String language() {
    return language;
  }

  public DefaultIssue setLanguage(@Nullable String l) {
    this.language = l;
    return this;
  }

  @Override
  public String severity() {
    return severity;
  }

  public DefaultIssue setSeverity(@Nullable String s) {
    Preconditions.checkArgument(s == null || Severity.ALL.contains(s), "Not a valid severity: " + s);
    this.severity = s;
    return this;
  }

  public boolean manualSeverity() {
    return manualSeverity;
  }

  public DefaultIssue setManualSeverity(boolean b) {
    this.manualSeverity = b;
    return this;
  }

  public boolean isFromExternalRuleEngine() {
    return isFromExternalRuleEngine;
  }

  public DefaultIssue setIsFromExternalRuleEngine(boolean isFromExternalRuleEngine) {
    this.isFromExternalRuleEngine = isFromExternalRuleEngine;
    return this;
  }

  @Override
  @CheckForNull
  public String message() {
    return message;
  }

  public DefaultIssue setMessage(@Nullable String s) {
    this.message = StringUtils.abbreviate(s, MESSAGE_MAX_SIZE);
    return this;
  }

  @CheckForNull
  public <T> T getMessageFormattings() {
    return (T) messageFormattings;
  }

  public DefaultIssue setMessageFormattings(@Nullable Object messageFormattings) {
    this.messageFormattings = messageFormattings;
    return this;
  }

  @Override
  @CheckForNull
  public Integer line() {
    return line;
  }

  public DefaultIssue setLine(@Nullable Integer l) {
    Preconditions.checkArgument(l == null || l > 0, "Line must be null or greater than zero (got %s)", l);
    this.line = l;
    return this;
  }

  @Override
  @CheckForNull
  public Double gap() {
    return gap;
  }

  public DefaultIssue setGap(@Nullable Double d) {
    Preconditions.checkArgument(d == null || d >= 0, "Gap must be greater than or equal 0 (got %s)", d);
    this.gap = d;
    return this;
  }

  /**
   * Elapsed time to fix the issue
   */
  @Override
  @CheckForNull
  public Duration effort() {
    return effort;
  }

  @CheckForNull
  public Long effortInMinutes() {
    return effort != null ? effort.toMinutes() : null;
  }

  public DefaultIssue setEffort(@Nullable Duration t) {
    this.effort = t;
    return this;
  }

  @Override
  public String status() {
    return status;
  }

  public DefaultIssue setStatus(String s) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(s), "Status must be set");
    this.status = s;
    return this;
  }

  @Nullable
  public IssueStatus getIssueStatus() {
    return IssueStatus.of(status, resolution);
  }

  @Override
  @CheckForNull
  public String resolution() {
    return resolution;
  }

  public DefaultIssue setResolution(@Nullable String s) {
    this.resolution = s;
    return this;
  }

  @Override
  @CheckForNull
  public String assignee() {
    return assigneeUuid;
  }

  public DefaultIssue setAssigneeUuid(@Nullable String s) {
    this.assigneeUuid = s;
    return this;
  }

  @CheckForNull
  public String assigneeLogin() {
    return assigneeLogin;
  }

  public DefaultIssue setAssigneeLogin(@Nullable String s) {
    this.assigneeLogin = s;
    return this;
  }

  @Override
  public Date creationDate() {
    return creationDate;
  }

  public DefaultIssue setCreationDate(Date d) {
    this.creationDate = truncateToSeconds(d);
    return this;
  }

  @Override
  @CheckForNull
  public Date updateDate() {
    return updateDate;
  }

  public DefaultIssue setUpdateDate(@Nullable Date d) {
    this.updateDate = truncateToSeconds(d);
    return this;
  }

  @Override
  @CheckForNull
  public Date closeDate() {
    return closeDate;
  }

  public DefaultIssue setCloseDate(@Nullable Date d) {
    this.closeDate = truncateToSeconds(d);
    return this;
  }

  @CheckForNull
  public String checksum() {
    return checksum;
  }

  public DefaultIssue setChecksum(@Nullable String s) {
    this.checksum = s;
    return this;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  public boolean isOnChangedLine() {
    return isOnChangedLine;
  }

  @Override
  public boolean isCopied() {
    return isCopied;
  }

  public DefaultIssue setCopied(boolean b) {
    isCopied = b;
    return this;
  }

  public boolean locationsChanged() {
    return locationsChanged;
  }

  public DefaultIssue setLocationsChanged(boolean locationsChanged) {
    this.locationsChanged = locationsChanged;
    return this;
  }

  public DefaultIssue setNew(boolean b) {
    isNew = b;
    return this;
  }

  public DefaultIssue setIsOnChangedLine(boolean b) {
    isOnChangedLine = b;
    return this;
  }

  /**
   * True when one of the following conditions is true :
   * <ul>
   * <li>the related component has been deleted or renamed</li>
   * <li>the rule has been deleted (eg. on plugin uninstall)</li>
   * <li>the rule has been disabled in the Quality profile</li>
   * </ul>
   */
  public boolean isBeingClosed() {
    return beingClosed;
  }

  public DefaultIssue setBeingClosed(boolean b) {
    beingClosed = b;
    return this;
  }

  public boolean isOnDisabledRule() {
    return onDisabledRule;
  }

  public DefaultIssue setOnDisabledRule(boolean b) {
    onDisabledRule = b;
    return this;
  }

  public boolean isChanged() {
    return isChanged;
  }

  public DefaultIssue setChanged(boolean b) {
    isChanged = b;
    return this;
  }

  public boolean mustSendNotifications() {
    return sendNotifications;
  }

  public DefaultIssue setSendNotifications(boolean b) {
    sendNotifications = b;
    return this;
  }

  /**
   * @deprecated since 9.4, attribute was already not returning any element since 5.2
   */
  @Deprecated
  @Override
  @CheckForNull
  public String attribute(String key) {
    return null;
  }

  /**
   * @deprecated since 9.4, attribute was already not returning any element since 5.2
   */
  @Deprecated
  @Override
  public Map<String, String> attributes() {
    return new HashMap<>();
  }

  @Override
  @CheckForNull
  public String authorLogin() {
    return authorLogin;
  }

  public DefaultIssue setAuthorLogin(@Nullable String s) {
    this.authorLogin = s;
    return this;
  }

  public DefaultIssue setFieldChange(IssueChangeContext context, String field, @Nullable Serializable oldValue, @Nullable Serializable newValue) {
    if (!Objects.equals(oldValue, newValue)) {
      if (currentChange == null) {
        currentChange = new FieldDiffs();
        currentChange.setUserUuid(context.userUuid());
        currentChange.setCreationDate(context.date());
        currentChange.setWebhookSource(context.getWebhookSource());
        currentChange.setExternalUser(context.getExternalUser());
        addChange(currentChange);
      }
      currentChange.setDiff(field, oldValue, newValue);
    }
    return this;
  }

  public DefaultIssue setCurrentChange(FieldDiffs currentChange) {
    this.currentChange = currentChange;
    addChange(currentChange);
    return this;
  }

  public DefaultIssue setCurrentChangeWithoutAddChange(@Nullable FieldDiffs currentChange) {
    this.currentChange = currentChange;
    return this;
  }

  public boolean isQuickFixAvailable() {
    return quickFixAvailable;
  }

  public DefaultIssue setQuickFixAvailable(boolean quickFixAvailable) {
    this.quickFixAvailable = quickFixAvailable;
    return this;
  }

  public boolean isNewCodeReferenceIssue() {
    return isNewCodeReferenceIssue;
  }

  public DefaultIssue setIsNewCodeReferenceIssue(boolean isNewCodeReferenceIssue) {
    this.isNewCodeReferenceIssue = isNewCodeReferenceIssue;
    return this;
  }

  public boolean isNoLongerNewCodeReferenceIssue() {
    return isNoLongerNewCodeReferenceIssue;
  }

  public DefaultIssue setIsNoLongerNewCodeReferenceIssue(boolean isNoLongerNewCodeReferenceIssue) {
    this.isNoLongerNewCodeReferenceIssue = isNoLongerNewCodeReferenceIssue;
    return this;
  }

  // true if the issue is new on a reference branch,
  // but it's not persisted as such due to being created before the SQ 9.3 migration
  public boolean isToBeMigratedAsNewCodeReferenceIssue() {
    return isOnChangedLine && !isNewCodeReferenceIssue && !isNoLongerNewCodeReferenceIssue;
  }

  @CheckForNull
  public FieldDiffs currentChange() {
    return currentChange;
  }

  public DefaultIssue addChange(@Nullable FieldDiffs change) {
    if (change == null) {
      return this;
    }
    if (changes == null) {
      changes = new ArrayList<>();
    }
    changes.add(change);
    return this;
  }

  public List<FieldDiffs> changes() {
    if (changes == null) {
      return Collections.emptyList();
    }
    return ImmutableList.copyOf(changes);
  }

  public DefaultIssue addComment(DefaultIssueComment comment) {
    if (comments == null) {
      comments = new ArrayList<>();
    }
    comments.add(comment);
    return this;
  }

  public List<DefaultIssueComment> defaultIssueComments() {
    if (comments == null) {
      return Collections.emptyList();
    }
    return ImmutableList.copyOf(comments);
  }

  @CheckForNull
  public Long selectedAt() {
    return selectedAt;
  }

  public DefaultIssue setSelectedAt(@Nullable Long d) {
    this.selectedAt = d;
    return this;
  }

  @CheckForNull
  public <T> T getLocations() {
    return (T) locations;
  }

  public DefaultIssue setLocations(@Nullable Object locations) {
    this.locations = locations;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultIssue that = (DefaultIssue) o;
    return Objects.equals(key, that.key);
  }

  @Override
  public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public Set<String> tags() {
    if (tags == null) {
      return Set.of();
    } else {
      return ImmutableSet.copyOf(tags);
    }
  }

  public DefaultIssue setTags(Collection<String> tags) {
    this.tags = new LinkedHashSet<>(tags);
    return this;
  }

  @Override
  public Set<String> codeVariants() {
    if (codeVariants == null) {
      return Set.of();
    } else {
      return ImmutableSet.copyOf(codeVariants);
    }
  }

  public DefaultIssue setCodeVariants(Collection<String> codeVariants) {
    this.codeVariants = new LinkedHashSet<>(codeVariants);
    return this;
  }

  public Optional<String> getRuleDescriptionContextKey() {
    return Optional.ofNullable(ruleDescriptionContextKey);
  }

  public DefaultIssue setRuleDescriptionContextKey(@Nullable String ruleDescriptionContextKey) {
    this.ruleDescriptionContextKey = ruleDescriptionContextKey;
    return this;
  }

  public Optional<String> getAnticipatedTransitionUuid() {
    return Optional.ofNullable(anticipatedTransitionUuid);
  }

  public DefaultIssue setAnticipatedTransitionUuid(@Nullable String anticipatedTransitionUuid) {
    this.anticipatedTransitionUuid = anticipatedTransitionUuid;
    return this;
  }

  public DefaultIssue setCleanCodeAttribute(@Nullable CleanCodeAttribute cleanCodeAttribute) {
    this.cleanCodeAttribute = cleanCodeAttribute;
    return this;
  }

  @Nullable
  public CleanCodeAttribute getCleanCodeAttribute() {
    return cleanCodeAttribute;
  }

  @Override
  public Integer getLine() {
    return line;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public String getLineHash() {
    return checksum;
  }

  @Override
  public RuleKey getRuleKey() {
    return ruleKey;
  }

  @Override
  public String getStatus() {
    return status;
  }

  @Override
  public Date getUpdateDate() {
    return updateDate;
  }
}
