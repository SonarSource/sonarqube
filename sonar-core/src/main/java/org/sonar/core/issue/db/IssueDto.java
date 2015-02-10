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
package org.sonar.core.issue.db;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.rule.RuleDto;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import static org.sonar.api.utils.DateUtils.dateToLong;
import static org.sonar.api.utils.DateUtils.longToDate;

/**
 * @since 3.6
 */
public final class IssueDto implements Serializable {

  private static final char TAGS_SEPARATOR = ',';
  private static final Joiner TAGS_JOINER = Joiner.on(TAGS_SEPARATOR).skipNulls();
  private static final Splitter TAGS_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private Long id;
  private String kee;
  private String componentUuid;
  private String projectUuid;
  private Integer ruleId;
  private String severity;
  private boolean manualSeverity;
  private String message;
  private Integer line;
  private Double effortToFix;
  private Long debt;
  private String status;
  private String resolution;
  private String checksum;
  private String reporter;
  private String assignee;
  private String authorLogin;
  private String actionPlanKey;
  private String issueAttributes;
  private long createdAt;
  private long updatedAt;

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
  private String language;
  private String componentKey;
  private String moduleUuid;
  private String moduleUuidPath;
  private String projectKey;
  private String filePath;
  private String tags;

  /**
   * On batch side, component keys and uuid are useless
   */
  public static IssueDto toDtoForComputationInsert(DefaultIssue issue, int ruleId, long now) {
    return new IssueDto()
      .setKee(issue.key())
      .setLine(issue.line())
      .setMessage(issue.message())
      .setEffortToFix(issue.effortToFix())
      .setDebt(issue.debtInMinutes())
      .setResolution(issue.resolution())
      .setStatus(issue.status())
      .setSeverity(issue.severity())
      .setManualSeverity(issue.manualSeverity())
      .setChecksum(issue.checksum())
      .setReporter(issue.reporter())
      .setAssignee(issue.assignee())
      .setRuleId(ruleId)
      .setRuleKey(issue.ruleKey().repository(), issue.ruleKey().rule())
      .setTags(issue.tags())
      .setComponentUuid(issue.componentUuid())
      .setComponentKey(issue.componentKey())
      .setModuleUuid(issue.moduleUuid())
      .setModuleUuidPath(issue.moduleUuidPath())
      .setComponentUuid(issue.componentUuid())
      .setProjectUuid(issue.projectUuid())
      .setProjectKey(issue.projectKey())
      .setActionPlanKey(issue.actionPlanKey())
      .setIssueAttributes(KeyValueFormat.format(issue.attributes()))
      .setAuthorLogin(issue.authorLogin())
      .setIssueCreationDate(issue.creationDate())
      .setIssueCloseDate(issue.closeDate())
      .setIssueUpdateDate(issue.updateDate())
      .setSelectedAt(issue.selectedAt())

      // technical dates
      .setCreatedAt(now)
      .setUpdatedAt(now);
  }

  /**
   * On server side, we need component keys and uuid
   */
  public static IssueDto toDtoForServerInsert(DefaultIssue issue, ComponentDto component, ComponentDto project, int ruleId, long now) {
    return toDtoForComputationInsert(issue, ruleId, now)
      .setComponent(component)
      .setProject(project);
  }

  public static IssueDto toDtoForUpdate(DefaultIssue issue, long now) {
    // Invariant fields, like key and rule, can't be updated
    return new IssueDto()
      .setKee(issue.key())
      .setLine(issue.line())
      .setMessage(issue.message())
      .setEffortToFix(issue.effortToFix())
      .setDebt(issue.debtInMinutes())
      .setResolution(issue.resolution())
      .setStatus(issue.status())
      .setSeverity(issue.severity())
      .setChecksum(issue.checksum())
      .setManualSeverity(issue.manualSeverity())
      .setReporter(issue.reporter())
      .setAssignee(issue.assignee())
      .setActionPlanKey(issue.actionPlanKey())
      .setIssueAttributes(KeyValueFormat.format(issue.attributes()))
      .setAuthorLogin(issue.authorLogin())
      .setRuleKey(issue.ruleKey().repository(), issue.ruleKey().rule())
      .setTags(issue.tags())
      .setComponentUuid(issue.componentUuid())
      .setComponentKey(issue.componentKey())
      .setModuleUuid(issue.moduleUuid())
      .setModuleUuidPath(issue.moduleUuidPath())
      .setProjectUuid(issue.projectUuid())
      .setProjectKey(issue.projectKey())
      .setIssueCreationDate(issue.creationDate())
      .setIssueCloseDate(issue.closeDate())
      .setIssueUpdateDate(issue.updateDate())
      .setSelectedAt(issue.selectedAt())

      // technical date
      .setUpdatedAt(now);
  }

  public static IssueDto createFor(Project project, RuleDto rule) {
    return new IssueDto()
      .setProjectUuid(project.getUuid())
      .setRuleId(rule.getId())
      .setKee(Uuids.create());
  }

  public String getKey() {
    return getKee();
  }

  public Long getId() {
    return id;
  }

  public IssueDto setId(@Nullable Long id) {
    this.id = id;
    return this;
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
    this.moduleUuid = component.moduleUuid();
    this.moduleUuidPath = component.moduleUuidPath();
    this.filePath = component.path();
    return this;
  }

  public IssueDto setProject(ComponentDto project) {
    this.projectKey = project.getKey();
    this.projectUuid = project.uuid();
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  /**
   * please use setRule(RuleDto rule)
   */
  public IssueDto setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  @CheckForNull
  public String getActionPlanKey() {
    return actionPlanKey;
  }

  public IssueDto setActionPlanKey(@Nullable String s) {
    this.actionPlanKey = s;
    return this;
  }

  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  public IssueDto setSeverity(@Nullable String severity) {
    this.severity = severity;
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
    this.message = s;
    return this;
  }

  @CheckForNull
  public Integer getLine() {
    return line;
  }

  public IssueDto setLine(@Nullable Integer line) {
    this.line = line;
    return this;
  }

  @CheckForNull
  public Double getEffortToFix() {
    return effortToFix;
  }

  public IssueDto setEffortToFix(@Nullable Double d) {
    this.effortToFix = d;
    return this;
  }

  @CheckForNull
  public Long getDebt() {
    return debt;
  }

  public IssueDto setDebt(@Nullable Long debt) {
    this.debt = debt;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public IssueDto setStatus(@Nullable String status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public String getResolution() {
    return resolution;
  }

  public IssueDto setResolution(@Nullable String s) {
    this.resolution = s;
    return this;
  }

  @CheckForNull
  public String getChecksum() {
    return checksum;
  }

  public IssueDto setChecksum(@Nullable String checksum) {
    this.checksum = checksum;
    return this;
  }

  @CheckForNull
  public String getReporter() {
    return reporter;
  }

  public IssueDto setReporter(@Nullable String s) {
    this.reporter = s;
    return this;
  }

  public String getAssignee() {
    return assignee;
  }

  public IssueDto setAssignee(@Nullable String s) {
    this.assignee = s;
    return this;
  }

  public String getAuthorLogin() {
    return authorLogin;
  }

  public IssueDto setAuthorLogin(@Nullable String authorLogin) {
    this.authorLogin = authorLogin;
    return this;
  }

  public String getIssueAttributes() {
    return issueAttributes;
  }

  public IssueDto setIssueAttributes(@Nullable String s) {
    Preconditions.checkArgument(s == null || s.length() <= 4000,
      "Issue attributes must not exceed 4000 characters: " + s);
    this.issueAttributes = s;
    return this;
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
    Preconditions.checkNotNull(rule.getId(), "Rule must be persisted.");
    this.ruleId = rule.getId();
    this.ruleKey = rule.getRuleKey();
    this.ruleRepo = rule.getRepositoryKey();
    this.language = rule.getLanguage();
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
   * Please use {@link #setRule(org.sonar.core.rule.RuleDto)} instead
   */
  public IssueDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  public String getComponentKey() {
    return componentKey;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setComponent(org.sonar.core.component.ComponentDto)} instead
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
   * Please use {@link #setComponent(org.sonar.core.component.ComponentDto)} instead
   */
  public IssueDto setComponentUuid(@Nullable String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  @CheckForNull
  public String getModuleUuid() {
    return moduleUuid;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setComponent(org.sonar.core.component.ComponentDto)} instead
   */
  public IssueDto setModuleUuid(@Nullable String moduleUuid) {
    this.moduleUuid = moduleUuid;
    return this;
  }

  @CheckForNull
  public String getModuleUuidPath() {
    return moduleUuidPath;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setComponent(org.sonar.core.component.ComponentDto)} instead
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
   * Please use {@link #setProject(org.sonar.core.component.ComponentDto)} instead
   */
  public IssueDto setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  /**
   * Can be null on Views or Devs
   */
  @CheckForNull
  public String getProjectUuid() {
    return projectUuid;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setProject(org.sonar.core.component.ComponentDto)} instead
   */
  public IssueDto setProjectUuid(@Nullable String projectUuid) {
    this.projectUuid = projectUuid;
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
   * Please use {@link #setRule(org.sonar.core.rule.RuleDto)} instead
   */
  public IssueDto setRuleKey(String repo, String rule) {
    this.ruleRepo = repo;
    this.ruleKey = rule;
    return this;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setProject(org.sonar.core.component.ComponentDto)} instead
   */
  public String getFilePath() {
    return filePath;
  }

  /**
   * Should only be used to persist in E/S
   * <p/>
   * Please use {@link #setProject(org.sonar.core.component.ComponentDto)} instead
   */
  public IssueDto setFilePath(String filePath) {
    this.filePath = filePath;
    return this;
  }

  public Collection<String> getTags() {
    return ImmutableSet.copyOf(TAGS_SPLITTER.split(tags == null ? "" : tags));
  }

  public IssueDto setTags(Collection<String> tags) {
    if (tags.isEmpty()) {
      this.tags = null;
    } else {
      this.tags = TAGS_JOINER.join(tags);
    }
    return this;
  }

  public String getTagsString() {
    return tags;
  }

  public IssueDto setTagsString(String tags) {
    this.tags = tags;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public DefaultIssue toDefaultIssue() {
    DefaultIssue issue = new DefaultIssue();
    issue.setKey(kee);
    issue.setStatus(status);
    issue.setResolution(resolution);
    issue.setMessage(message);
    issue.setEffortToFix(effortToFix);
    issue.setDebt(debt != null ? Duration.create(debt) : null);
    issue.setLine(line);
    issue.setSeverity(severity);
    issue.setReporter(reporter);
    issue.setAssignee(assignee);
    issue.setAttributes(KeyValueFormat.parse(Objects.firstNonNull(issueAttributes, "")));
    issue.setComponentKey(componentKey);
    issue.setComponentUuid(componentUuid);
    issue.setModuleUuid(moduleUuid);
    issue.setModuleUuidPath(moduleUuidPath);
    issue.setProjectUuid(projectUuid);
    issue.setProjectKey(projectKey);
    issue.setManualSeverity(manualSeverity);
    issue.setRuleKey(getRuleKey());
    issue.setTags(getTags());
    issue.setLanguage(language);
    issue.setActionPlanKey(actionPlanKey);
    issue.setAuthorLogin(authorLogin);
    issue.setNew(false);
    issue.setCreationDate(longToDate(issueCreationDate));
    issue.setCloseDate(longToDate(issueCloseDate));
    issue.setUpdateDate(longToDate(issueUpdateDate));
    issue.setSelectedAt(selectedAt);
    return issue;
  }
}
