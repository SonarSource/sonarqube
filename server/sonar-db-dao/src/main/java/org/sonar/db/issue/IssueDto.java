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
package org.sonar.db.issue;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.api.utils.DateUtils.dateToLong;
import static org.sonar.api.utils.DateUtils.longToDate;

public final class IssueDto implements Serializable {

  public static final int AUTHOR_MAX_SIZE = 255;
  private static final char TAGS_SEPARATOR = ',';
  private static final Joiner TAGS_JOINER = Joiner.on(TAGS_SEPARATOR).skipNulls();
  private static final Splitter TAGS_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private int type;
  private String kee;
  private String componentUuid;
  private String projectUuid;
  private String ruleUuid;
  private String severity;
  private boolean manualSeverity;
  private String message;
  private byte[] messageFormattings;
  private Integer line;
  private Double gap;
  private Long effort;
  private String status;
  private String resolution;
  private String checksum;
  private String assigneeUuid;
  private String assigneeLogin;
  private String authorLogin;
  private String securityStandards;
  private byte[] locations;
  private long createdAt;
  private long updatedAt;
  private boolean quickFixAvailable;
  private boolean isNewCodeReferenceIssue;
  private String ruleDescriptionContextKey;

  // functional dates stored as Long
  private Long issueCreationDate;
  private Long issueUpdateDate;
  private Long issueCloseDate;

  /**
   * Temporary date used only during scan
   */
  private Long selectedAt;

  // joins
  private String ruleKey;
  private String ruleRepo;
  private boolean isExternal;
  private String language;
  private String componentKey;
  private String moduleUuidPath;
  private String projectKey;
  private String filePath;
  private String tags;
  // populate only when retrieving closed issue for issue tracking
  private String closedChangeData;

  public IssueDto() {
    // nothing to do
  }

  /**
   * On batch side, component keys and uuid are useless
   */
  public static IssueDto toDtoForComputationInsert(DefaultIssue issue, String ruleUuid, long now) {
    return new IssueDto()
      .setKee(issue.key())
      .setType(issue.type())
      .setLine(issue.line())
      .setLocations((DbIssues.Locations) issue.getLocations())
      .setMessage(issue.message())
      .setMessageFormattings((DbIssues.MessageFormattings) issue.getMessageFormattings())
      .setGap(issue.gap())
      .setEffort(issue.effortInMinutes())
      .setResolution(issue.resolution())
      .setStatus(issue.status())
      .setSeverity(issue.severity())
      .setManualSeverity(issue.manualSeverity())
      .setChecksum(issue.checksum())
      .setAssigneeUuid(issue.assignee())
      .setRuleUuid(ruleUuid)
      .setRuleKey(issue.ruleKey().repository(), issue.ruleKey().rule())
      .setExternal(issue.isFromExternalRuleEngine())
      .setTags(issue.tags())
      .setRuleDescriptionContextKey(issue.getRuleDescriptionContextKey().orElse(null))
      .setComponentUuid(issue.componentUuid())
      .setComponentKey(issue.componentKey())
      .setModuleUuidPath(issue.moduleUuidPath())
      .setProjectUuid(issue.projectUuid())
      .setProjectKey(issue.projectKey())
      .setAuthorLogin(issue.authorLogin())
      .setIssueCreationDate(issue.creationDate())
      .setIssueCloseDate(issue.closeDate())
      .setIssueUpdateDate(issue.updateDate())
      .setSelectedAt(issue.selectedAt())
      .setQuickFixAvailable(issue.isQuickFixAvailable())
      .setIsNewCodeReferenceIssue(issue.isNewCodeReferenceIssue())

      // technical dates
      .setCreatedAt(now)
      .setUpdatedAt(now);
  }

  /**
   * On server side, we need component keys and uuid
   */
  public static IssueDto toDtoForServerInsert(DefaultIssue issue, ComponentDto component, ComponentDto project, String ruleUuid, long now) {
    return toDtoForComputationInsert(issue, ruleUuid, now)
      .setComponent(component)
      .setProject(project);
  }

  public static IssueDto toDtoForUpdate(DefaultIssue issue, long now) {
    // Invariant fields, like key and rule, can't be updated
    return new IssueDto()
      .setKee(issue.key())
      .setType(issue.type())
      .setLine(issue.line())
      .setLocations((DbIssues.Locations) issue.getLocations())
      .setMessage(issue.message())
      .setMessageFormattings((DbIssues.MessageFormattings) issue.getMessageFormattings())
      .setGap(issue.gap())
      .setEffort(issue.effortInMinutes())
      .setResolution(issue.resolution())
      .setStatus(issue.status())
      .setSeverity(issue.severity())
      .setChecksum(issue.checksum())
      .setManualSeverity(issue.manualSeverity())
      .setAssigneeUuid(issue.assignee())
      .setAuthorLogin(issue.authorLogin())
      .setRuleKey(issue.ruleKey().repository(), issue.ruleKey().rule())
      .setExternal(issue.isFromExternalRuleEngine())
      .setTags(issue.tags())
      .setRuleDescriptionContextKey(issue.getRuleDescriptionContextKey().orElse(null))
      .setComponentUuid(issue.componentUuid())
      .setComponentKey(issue.componentKey())
      .setModuleUuidPath(issue.moduleUuidPath())
      .setProjectUuid(issue.projectUuid())
      .setProjectKey(issue.projectKey())
      .setIssueCreationDate(issue.creationDate())
      .setIssueCloseDate(issue.closeDate())
      .setIssueUpdateDate(issue.updateDate())
      .setSelectedAt(issue.selectedAt())
      .setQuickFixAvailable(issue.isQuickFixAvailable())
      .setIsNewCodeReferenceIssue(issue.isNewCodeReferenceIssue())

      // technical date
      .setUpdatedAt(now);
  }

  public String getKey() {
    return getKee();
  }

  public String getKee() {
    return kee;
  }

  public IssueDto setKee(String s) {
    this.kee = s;
    return this;
  }

  public IssueDto setComponent(ComponentDto component) {
    this.componentKey = component.getKey();
    this.componentUuid = component.uuid();
    this.moduleUuidPath = component.moduleUuidPath();
    this.filePath = component.path();
    return this;
  }

  public IssueDto setProject(ComponentDto project) {
    this.projectKey = project.getKey();
    this.projectUuid = project.uuid();
    return this;
  }

  public String getRuleUuid() {
    return ruleUuid;
  }

  /**
   * please use setRule(RuleDto rule)
   */
  public IssueDto setRuleUuid(String ruleUuid) {
    this.ruleUuid = ruleUuid;
    return this;
  }

  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  public IssueDto setSeverity(@Nullable String s) {
    checkArgument(s == null || s.length() <= 10, "Value is too long for issue severity: %s", s);
    this.severity = s;
    return this;
  }

  public boolean isManualSeverity() {
    return manualSeverity;
  }

  public IssueDto setManualSeverity(boolean manualSeverity) {
    this.manualSeverity = manualSeverity;
    return this;
  }

  @CheckForNull
  public String getMessage() {
    return message;
  }

  public IssueDto setMessage(@Nullable String s) {
    checkArgument(s == null || s.length() <= 4000, "Value is too long for issue message: %s", s);
    this.message = s;
    return this;
  }

  public byte[] getMessageFormattings() {
    return messageFormattings;
  }

  public IssueDto setMessageFormattings(byte[] messageFormattings) {
    this.messageFormattings = messageFormattings;
    return this;
  }

  public IssueDto setMessageFormattings(@Nullable DbIssues.MessageFormattings messageFormattings) {
    if (messageFormattings == null) {
      this.messageFormattings = null;
    } else {
      this.messageFormattings = messageFormattings.toByteArray();
    }
    return this;
  }

  @CheckForNull
  public DbIssues.MessageFormattings parseMessageFormattings() {
    if (messageFormattings != null) {
      try {
        return DbIssues.MessageFormattings.parseFrom(messageFormattings);
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalStateException(String.format("Fail to read ISSUES.MESSAGE_FORMATTINGS [KEE=%s]", kee), e);
      }
    }
    return null;
  }

  @CheckForNull
  public Integer getLine() {
    return line;
  }

  public IssueDto setLine(@Nullable Integer i) {
    checkArgument(i == null || i >= 0, "Value of issue line must be positive: %d", i);
    this.line = i;
    return this;
  }

  @CheckForNull
  public Double getGap() {
    return gap;
  }

  public IssueDto setGap(@Nullable Double d) {
    checkArgument(d == null || d >= 0, "Value of issue gap must be positive: %d", d);
    this.gap = d;
    return this;
  }

  @CheckForNull
  public Long getEffort() {
    return effort;
  }

  public IssueDto setEffort(@Nullable Long l) {
    checkArgument(l == null || l >= 0, "Value of issue effort must be positive: %d", l);
    this.effort = l;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public IssueDto setStatus(@Nullable String s) {
    checkArgument(s == null || s.length() <= 20, "Value is too long for issue status: %s", s);
    this.status = s;
    return this;
  }

  @CheckForNull
  public String getResolution() {
    return resolution;
  }

  public IssueDto setResolution(@Nullable String s) {
    checkArgument(s == null || s.length() <= 20, "Value is too long for issue resolution: %s", s);
    this.resolution = s;
    return this;
  }

  @CheckForNull
  public String getChecksum() {
    return checksum;
  }

  public IssueDto setChecksum(@Nullable String s) {
    checkArgument(s == null || s.length() <= 1000, "Value is too long for issue checksum: %s", s);
    this.checksum = s;
    return this;
  }

  @CheckForNull
  public String getAssigneeUuid() {
    return assigneeUuid;
  }

  public IssueDto setAssigneeUuid(@Nullable String s) {
    checkArgument(s == null || s.length() <= 255, "Value is too long for issue assigneeUuid: %s", s);
    this.assigneeUuid = s;
    return this;
  }

  @CheckForNull
  public String getAssigneeLogin() {
    return assigneeLogin;
  }

  public IssueDto setAssigneeLogin(@Nullable String s) {
    this.assigneeLogin = s;
    return this;
  }

  @CheckForNull
  public String getAuthorLogin() {
    return authorLogin;
  }

  public IssueDto setAuthorLogin(@Nullable String s) {
    checkArgument(s == null || s.length() <= AUTHOR_MAX_SIZE, "Value is too long for issue author login: %s", s);
    this.authorLogin = s;
    return this;
  }

  public IssueDto setSecurityStandards(@Nullable String s) {
    this.securityStandards = s;
    return this;
  }

  public Set<String> getSecurityStandards() {
    return RuleDto.deserializeSecurityStandardsString(securityStandards);
  }

  /**
   * Technical date
   */
  public long getCreatedAt() {
    return createdAt;
  }

  public IssueDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Technical date
   */
  public long getUpdatedAt() {
    return updatedAt;
  }

  public IssueDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Long getIssueCreationTime() {
    return issueCreationDate;
  }

  public IssueDto setIssueCreationTime(Long time) {
    this.issueCreationDate = time;
    return this;
  }

  public Date getIssueCreationDate() {
    return longToDate(issueCreationDate);
  }

  public IssueDto setIssueCreationDate(@Nullable Date d) {
    this.issueCreationDate = dateToLong(d);
    return this;
  }

  public Long getIssueUpdateTime() {
    return issueUpdateDate;
  }

  public IssueDto setIssueUpdateTime(Long time) {
    this.issueUpdateDate = time;
    return this;
  }

  public Date getIssueUpdateDate() {
    return longToDate(issueUpdateDate);
  }

  public IssueDto setIssueUpdateDate(@Nullable Date d) {
    this.issueUpdateDate = dateToLong(d);
    return this;
  }

  public Long getIssueCloseTime() {
    return issueCloseDate;
  }

  public IssueDto setIssueCloseTime(Long time) {
    this.issueCloseDate = time;
    return this;
  }

  public Date getIssueCloseDate() {
    return longToDate(issueCloseDate);
  }

  public IssueDto setIssueCloseDate(@Nullable Date d) {
    this.issueCloseDate = dateToLong(d);
    return this;
  }

  public String getRule() {
    return ruleKey;
  }

  public IssueDto setRule(RuleDto rule) {
    Preconditions.checkNotNull(rule.getUuid(), "Rule must be persisted.");
    this.ruleUuid = rule.getUuid();
    this.ruleKey = rule.getRuleKey();
    this.ruleRepo = rule.getRepositoryKey();
    this.language = rule.getLanguage();
    this.isExternal = rule.isExternal();
    return this;
  }

  public String getRuleRepo() {
    return ruleRepo;
  }

  public RuleKey getRuleKey() {
    return RuleKey.of(ruleRepo, ruleKey);
  }

  public String getLanguage() {
    return language;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setRule(RuleDto)} instead
   */
  public IssueDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  public boolean isExternal() {
    return isExternal;
  }

  public IssueDto setExternal(boolean external) {
    isExternal = external;
    return this;
  }

  public String getComponentKey() {
    return componentKey;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setComponent(ComponentDto)} instead
   */
  public IssueDto setComponentKey(String componentKey) {
    this.componentKey = componentKey;
    return this;
  }

  /**
   * Can be null on Views or Devs
   */
  @CheckForNull
  public String getComponentUuid() {
    return componentUuid;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setComponent(ComponentDto)} instead
   */
  public IssueDto setComponentUuid(@Nullable String s) {
    checkArgument(s == null || s.length() <= 50, "Value is too long for column ISSUES.COMPONENT_UUID: %s", s);
    this.componentUuid = s;
    return this;
  }

  @CheckForNull
  public String getModuleUuidPath() {
    return moduleUuidPath;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setComponent(ComponentDto)} instead
   */
  public IssueDto setModuleUuidPath(@Nullable String moduleUuidPath) {
    this.moduleUuidPath = moduleUuidPath;
    return this;
  }

  /**
   * Used by the issue tracking mechanism, but it should used the component uuid instead
   */
  public String getProjectKey() {
    return projectKey;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setProject(ComponentDto)} instead
   */
  public IssueDto setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setProject(ComponentDto)} instead
   */
  public IssueDto setProjectUuid(String s) {
    checkArgument(s.length() <= 50, "Value is too long for column ISSUES.PROJECT_UUID: %s", s);
    this.projectUuid = s;
    return this;
  }

  @CheckForNull
  public Long getSelectedAt() {
    return selectedAt;
  }

  public IssueDto setSelectedAt(@Nullable Long d) {
    this.selectedAt = d;
    return this;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setRule(RuleDto)} instead
   */
  public IssueDto setRuleKey(String repo, String rule) {
    this.ruleRepo = repo;
    this.ruleKey = rule;
    return this;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setProject(ComponentDto)} instead
   */
  public String getFilePath() {
    return filePath;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setProject(ComponentDto)} instead
   */
  public IssueDto setFilePath(String filePath) {
    this.filePath = filePath;
    return this;
  }

  public Set<String> getTags() {
    return ImmutableSet.copyOf(TAGS_SPLITTER.split(tags == null ? "" : tags));
  }

  public IssueDto setTags(@Nullable Collection<String> tags) {
    if (tags == null || tags.isEmpty()) {
      setTagsString(null);
    } else {
      setTagsString(TAGS_JOINER.join(tags));
    }
    return this;
  }

  public IssueDto setTagsString(@Nullable String s) {
    checkArgument(s == null || s.length() <= 4000, "Value is too long for column ISSUES.TAGS: %s", s);
    this.tags = s;
    return this;
  }

  public String getTagsString() {
    return tags;
  }

  @CheckForNull
  public byte[] getLocations() {
    return locations;
  }

  @CheckForNull
  public DbIssues.Locations parseLocations() {
    if (locations != null) {
      try {
        return DbIssues.Locations.parseFrom(locations);
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalStateException(String.format("Fail to read ISSUES.LOCATIONS [KEE=%s]", kee), e);
      }
    }
    return null;
  }

  public IssueDto setLocations(@Nullable byte[] locations) {
    this.locations = locations;
    return this;
  }

  public IssueDto setLocations(@Nullable DbIssues.Locations locations) {
    if (locations == null) {
      this.locations = null;
    } else {
      this.locations = locations.toByteArray();
    }
    return this;
  }

  public boolean isQuickFixAvailable() {
    return quickFixAvailable;
  }

  public IssueDto setQuickFixAvailable(boolean quickFixAvailable) {
    this.quickFixAvailable = quickFixAvailable;
    return this;
  }

  public boolean isNewCodeReferenceIssue() {
    return isNewCodeReferenceIssue;
  }

  public IssueDto setIsNewCodeReferenceIssue(boolean isNewCodeReferenceIssue) {
    this.isNewCodeReferenceIssue = isNewCodeReferenceIssue;
    return this;
  }

  public int getType() {
    return type;
  }

  public IssueDto setType(int type) {
    this.type = type;
    return this;
  }

  public IssueDto setType(RuleType type) {
    this.type = type.getDbConstant();
    return this;
  }

  public Optional<String> getClosedChangeData() {
    return Optional.ofNullable(closedChangeData);
  }

  public Optional<String> getOptionalRuleDescriptionContextKey() {
    return Optional.ofNullable(ruleDescriptionContextKey);
  }

  public IssueDto setRuleDescriptionContextKey(@Nullable String ruleDescriptionContextKey) {
    this.ruleDescriptionContextKey = ruleDescriptionContextKey;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public DefaultIssue toDefaultIssue() {
    DefaultIssue issue = new DefaultIssue();
    issue.setKey(kee);
    issue.setType(RuleType.valueOf(type));
    issue.setStatus(status);
    issue.setResolution(resolution);
    issue.setMessage(message);
    issue.setMessageFormattings(parseMessageFormattings());
    issue.setGap(gap);
    issue.setEffort(effort != null ? Duration.create(effort) : null);
    issue.setLine(line);
    issue.setChecksum(checksum);
    issue.setSeverity(severity);
    issue.setAssigneeUuid(assigneeUuid);
    issue.setComponentKey(componentKey);
    issue.setComponentUuid(componentUuid);
    issue.setModuleUuidPath(moduleUuidPath);
    issue.setProjectUuid(projectUuid);
    issue.setProjectKey(projectKey);
    issue.setManualSeverity(manualSeverity);
    issue.setRuleKey(getRuleKey());
    issue.setTags(getTags());
    issue.setRuleDescriptionContextKey(ruleDescriptionContextKey);
    issue.setLanguage(language);
    issue.setAuthorLogin(authorLogin);
    issue.setNew(false);
    issue.setCreationDate(longToDate(issueCreationDate));
    issue.setCloseDate(longToDate(issueCloseDate));
    issue.setUpdateDate(longToDate(issueUpdateDate));
    issue.setSelectedAt(selectedAt);
    issue.setLocations(parseLocations());
    issue.setIsFromExternalRuleEngine(isExternal);
    issue.setQuickFixAvailable(quickFixAvailable);
    issue.setIsNewCodeReferenceIssue(isNewCodeReferenceIssue);
    return issue;
  }
}
