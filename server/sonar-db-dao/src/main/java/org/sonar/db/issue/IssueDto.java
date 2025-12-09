/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.sonar.api.utils.DateUtils.longToDate;

public final class IssueDto extends IssueWithoutRuleInfoDto {

  private String securityStandards;

  // joins
  private String ruleKey;
  private String ruleRepo;
  private boolean isExternal;
  private String language;

  // non-persisted fields
  private Set<ImpactDto> ruleDefaultImpacts = new LinkedHashSet<>();
  private CleanCodeAttribute ruleCleanCodeAttribute;

  public IssueDto() {
    // nothing to do
  }

  /**
   * On batch side, component keys and uuid are useless
   */
  public static IssueDto toDtoForComputationInsert(DefaultIssue issue, String ruleUuid, long now) {
    IssueDto issueDto = new IssueDto()
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
      .setInternalTags(issue.internalTags())
      .setRuleDescriptionContextKey(issue.getRuleDescriptionContextKey().orElse(null))
      .setComponentUuid(issue.componentUuid())
      .setComponentKey(issue.componentKey())
      .setProjectUuid(issue.projectUuid())
      .setProjectKey(issue.projectKey())
      .setAuthorLogin(issue.authorLogin())
      .setIssueCreationDate(issue.creationDate())
      .setIssueCloseDate(issue.closeDate())
      .setIssueUpdateDate(issue.updateDate())
      .setSelectedAt(issue.selectedAt())
      .setQuickFixAvailable(issue.isQuickFixAvailable())
      .setIsNewCodeReferenceIssue(issue.isNewCodeReferenceIssue())
      .setCodeVariants(issue.codeVariants())
      .setCleanCodeAttribute(issue.getCleanCodeAttribute())
      .setPrioritizedRule(issue.isPrioritizedRule())
      .setFromSonarQubeUpdate(issue.isFromSonarQubeUpdate())
      // technical dates
      .setCreatedAt(now)
      .setUpdatedAt(now);

    issue.getImpacts().forEach(i -> issueDto.addImpact(new ImpactDto(i.softwareQuality(), i.severity(), i.manualSeverity())));
    return issueDto;
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
    IssueDto issueDto = new IssueDto()
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
      .setInternalTags(issue.internalTags())
      .setRuleDescriptionContextKey(issue.getRuleDescriptionContextKey().orElse(null))
      .setComponentUuid(issue.componentUuid())
      .setComponentKey(issue.componentKey())
      .setProjectUuid(issue.projectUuid())
      .setProjectKey(issue.projectKey())
      .setIssueCreationDate(issue.creationDate())
      .setIssueCloseDate(issue.closeDate())
      .setIssueUpdateDate(issue.updateDate())
      .setSelectedAt(issue.selectedAt())
      .setQuickFixAvailable(issue.isQuickFixAvailable())
      .setIsNewCodeReferenceIssue(issue.isNewCodeReferenceIssue())
      .setCodeVariants(issue.codeVariants())
      .setCleanCodeAttribute(issue.getCleanCodeAttribute())
      .setPrioritizedRule(issue.isPrioritizedRule())
      .setFromSonarQubeUpdate(issue.isFromSonarQubeUpdate())
      // technical date
      .setUpdatedAt(now);

    issue.getImpacts().forEach(i -> issueDto.addImpact(new ImpactDto(i.softwareQuality(), i.severity(), i.manualSeverity())));
    return issueDto;

  }

  @Override
  public IssueDto setKee(String s) {
    super.setKee(s);
    return this;
  }

  @Override
  public IssueDto setComponent(ComponentDto component) {
    super.setComponent(component);
    return this;
  }

  @Override
  public IssueDto setProject(ComponentDto branch) {
    super.setProject(branch);
    return this;
  }

  @Override
  public IssueDto setRuleUuid(String ruleUuid) {
    super.setRuleUuid(ruleUuid);
    return this;
  }

  @Override
  public IssueDto setSeverity(@Nullable String s) {
    super.setSeverity(s);
    return this;
  }

  @Override
  public IssueDto setManualSeverity(boolean manualSeverity) {
    super.setManualSeverity(manualSeverity);
    return this;
  }

  @Override
  public IssueDto setMessage(@Nullable String s) {
    super.setMessage(s);
    return this;
  }

  @Override
  public IssueDto setMessageFormattings(byte[] messageFormattings) {
    super.setMessageFormattings(messageFormattings);
    return this;
  }

  @Override
  public IssueDto setMessageFormattings(@Nullable DbIssues.MessageFormattings messageFormattings) {
    super.setMessageFormattings(messageFormattings);
    return this;
  }

  @Override
  public IssueDto setLine(@Nullable Integer i) {
    super.setLine(i);
    return this;
  }

  @Override
  public IssueDto setGap(@Nullable Double d) {
    super.setGap(d);
    return this;
  }

  @Override
  public IssueDto setEffort(@Nullable Long l) {
    super.setEffort(l);
    return this;
  }

  @Override
  public IssueDto setStatus(@Nullable String s) {
    super.setStatus(s);
    return this;
  }

  @Override
  public IssueDto setResolution(@Nullable String s) {
    super.setResolution(s);
    return this;
  }

  @Override
  public IssueDto setChecksum(@Nullable String s) {
    super.setChecksum(s);
    return this;
  }

  @Override
  public IssueDto setAssigneeUuid(@Nullable String s) {
    super.setAssigneeUuid(s);
    return this;
  }

  @Override
  public IssueDto setAssigneeLogin(@Nullable String s) {
    super.setAssigneeLogin(s);
    return this;
  }

  @Override
  public IssueDto setAuthorLogin(@Nullable String s) {
    super.setAuthorLogin(s);
    return this;
  }

  public IssueDto setSecurityStandards(@Nullable String s) {
    this.securityStandards = s;
    return this;
  }

  public Set<String> getSecurityStandards() {
    return RuleDto.deserializeSecurityStandardsString(securityStandards);
  }

  @Override
  public IssueDto setCreatedAt(long createdAt) {
    super.setCreatedAt(createdAt);
    return this;
  }

  @Override
  public IssueDto setUpdatedAt(long updatedAt) {
    super.setUpdatedAt(updatedAt);
    return this;
  }

  @Override
  public IssueDto setIssueCreationTime(Long time) {
    super.setIssueCreationTime(time);
    return this;
  }

  @Override
  public IssueDto setIssueCreationDate(@Nullable Date d) {
    super.setIssueCreationDate(d);
    return this;
  }

  @Override
  public IssueDto setIssueUpdateTime(Long time) {
    super.setIssueUpdateTime(time);
    return this;
  }

  @Override
  public IssueDto setIssueUpdateDate(@Nullable Date d) {
    super.setIssueUpdateDate(d);
    return this;
  }

  @Override
  public IssueDto setIssueCloseTime(Long time) {
    super.setIssueCloseTime(time);
    return this;
  }

  @Override
  public IssueDto setIssueCloseDate(@Nullable Date d) {
    super.setIssueCloseDate(d);
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
    this.cleanCodeAttribute = rule.getCleanCodeAttribute();
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

  @Override
  public IssueDto setComponentKey(String componentKey) {
    super.setComponentKey(componentKey);
    return this;
  }

  @Override
  public IssueDto setComponentUuid(@Nullable String s) {
    super.setComponentUuid(s);
    return this;
  }

  @Override
  public IssueDto setProjectKey(String projectKey) {
    super.setProjectKey(projectKey);
    return this;
  }

  @Override
  public IssueDto setProjectUuid(String s) {
    super.setProjectUuid(s);
    return this;
  }

  @Override
  public IssueDto setSelectedAt(@Nullable Long d) {
    super.setSelectedAt(d);
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

  @Override
  public IssueDto setFilePath(String filePath) {
    super.setFilePath(filePath);
    return this;
  }

  @Override
  public IssueDto setTags(@Nullable Collection<String> tags) {
    super.setTags(tags);
    return this;
  }

  @Override
  public IssueDto setTagsString(@Nullable String s) {
    super.setTagsString(s);
    return this;
  }

  @Override
  public IssueDto setInternalTags(@Nullable Collection<String> internalTags) {
    super.setInternalTags(internalTags);
    return this;
  }

  @Override
  public IssueDto setInternalTagsString(@Nullable String internalTags) {
    super.setInternalTagsString(internalTags);
    return this;
  }

  @Override
  public IssueDto setCodeVariants(@Nullable Collection<String> codeVariants) {
    super.setCodeVariants(codeVariants);
    return this;
  }

  @Override
  public IssueDto setCodeVariantsString(@Nullable String codeVariants) {
    super.setCodeVariantsString(codeVariants);
    return this;
  }

  @Override
  public IssueDto setLocations(@Nullable byte[] locations) {
    super.setLocations(locations);
    return this;
  }

  @Override
  public IssueDto setLocations(@Nullable DbIssues.Locations locations) {
    super.setLocations(locations);
    return this;
  }

  @Override
  public IssueDto setQuickFixAvailable(boolean quickFixAvailable) {
    super.setQuickFixAvailable(quickFixAvailable);
    return this;
  }

  @Override
  public IssueDto setIsNewCodeReferenceIssue(boolean isNewCodeReferenceIssue) {
    super.setIsNewCodeReferenceIssue(isNewCodeReferenceIssue);
    return this;
  }

  @Override
  public IssueDto setType(int type) {
    super.setType(type);
    return this;
  }

  @Override
  public IssueDto setType(RuleType type) {
    super.setType(type);
    return this;
  }

  @CheckForNull
  public CleanCodeAttribute getEffectiveCleanCodeAttribute() {
    if (cleanCodeAttribute != null) {
      return cleanCodeAttribute;
    }
    return ruleCleanCodeAttribute;
  }

  @Override
  public IssueDto setCleanCodeAttribute(CleanCodeAttribute cleanCodeAttribute) {
    super.setCleanCodeAttribute(cleanCodeAttribute);
    return this;
  }

  public IssueDto setRuleCleanCodeAttribute(CleanCodeAttribute ruleCleanCodeAttribute) {
    this.ruleCleanCodeAttribute = ruleCleanCodeAttribute;
    return this;
  }

  @Override
  public IssueDto setRuleDescriptionContextKey(@Nullable String ruleDescriptionContextKey) {
    super.setRuleDescriptionContextKey(ruleDescriptionContextKey);
    return this;
  }

  @Override
  public IssueDto addImpact(ImpactDto impact) {
    super.addImpact(impact);
    return this;
  }

  public IssueDto setRuleDefaultImpacts(Set<ImpactDto> ruleDefaultImpacts) {
    this.ruleDefaultImpacts = new HashSet<>(ruleDefaultImpacts);
    return this;
  }

  @Override
  public IssueDto replaceAllImpacts(Collection<ImpactDto> newImpacts) {
    super.replaceAllImpacts(newImpacts);
    return this;
  }

  Set<ImpactDto> getRuleDefaultImpacts() {
    return ruleDefaultImpacts;
  }

  /**
   * Returns effective impacts defined on this issue along with default ones.
   *
   * @return Unmodifiable Map of impacts
   */
  public Map<SoftwareQuality, Severity> getEffectiveImpacts() {
    return impacts.isEmpty() ? toImpactMap(ruleDefaultImpacts) : toImpactMap(impacts);
  }

  private static Map<SoftwareQuality, Severity> toImpactMap(Collection<ImpactDto> impacts) {
    return impacts.stream()
      .collect(toUnmodifiableMap(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity));
  }

  @Override
  public IssueDto setPrioritizedRule(boolean isBlockerRule) {
    super.setPrioritizedRule(isBlockerRule);
    return this;
  }

  @Override
  public IssueDto setLinkedTicketStatus(String linkedTicketStatus) {
    super.setLinkedTicketStatus(linkedTicketStatus);
    return this;
  }

  @Override
  public IssueDto setFromSonarQubeUpdate(boolean fromSonarQubeUpdate) {
    super.setFromSonarQubeUpdate(fromSonarQubeUpdate);
    return this;
  }

  public DefaultIssue toDefaultIssue() {
    DefaultIssue issue = new DefaultIssue();
    issue.setKey(kee);
    issue.setType(RuleType.fromDbConstant(type));
    issue.setStatus(status);
    issue.setResolution(resolution);
    issue.setMessage(message);
    issue.setMessageFormattings(parseMessageFormattings());
    issue.setGap(gap);
    issue.setEffort(effort != null ? Duration.create(effort) : null);
    issue.setLine(line);
    issue.setChecksum(checksum);
    issue.setSeverity(severity);
    issue.setPrioritizedRule(prioritizedRule);
    issue.setFromSonarQubeUpdate(fromSonarQubeUpdate);
    issue.setAssigneeUuid(assigneeUuid);
    issue.setAssigneeLogin(assigneeLogin);
    issue.setComponentKey(componentKey);
    issue.setComponentUuid(componentUuid);
    issue.setProjectUuid(projectUuid);
    issue.setProjectKey(projectKey);
    issue.setManualSeverity(manualSeverity);
    issue.setRuleKey(getRuleKey());
    issue.setTags(getTags());
    issue.setInternalTags(getInternalTags());
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
    issue.setCodeVariants(getCodeVariants());
    issue.setCleanCodeAttribute(cleanCodeAttribute);
    impacts.forEach(i -> issue.addImpact(i.getSoftwareQuality(), i.getSeverity(), i.isManualSeverity()));
    return issue;
  }
}
