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

import com.google.protobuf.InvalidProtocolBufferException;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.Severity.MEDIUM;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;

class IssueDtoTest {

  private static final String TEST_CONTEXT_KEY = "test_context_key";

  private static final DbIssues.MessageFormattings EXAMPLE_MESSAGE_FORMATTINGS = DbIssues.MessageFormattings.newBuilder()
    .addMessageFormatting(DbIssues.MessageFormatting.newBuilder().setStart(0).setEnd(1).setType(DbIssues.MessageFormattingType.CODE)
      .build())
    .build();

  @Test
  void toDefaultIssue_ShouldSetIssueFields() throws InvalidProtocolBufferException {
    Date createdAt = DateUtils.addDays(new Date(), -5);
    Date updatedAt = DateUtils.addDays(new Date(), -3);
    Date closedAt = DateUtils.addDays(new Date(), -1);

    IssueDto dto = new IssueDto()
      .setKee("100")
      .setType(RuleType.VULNERABILITY)
      .setRuleUuid("rule-uuid-1")
      .setRuleKey("java", "AvoidCycle")
      .setLanguage("xoo")
      .setComponentKey("org.sonar.sample:Sample")
      .setComponentUuid("CDEF")
      .setProjectUuid("GHIJ")
      .setProjectKey("org.sonar.sample")
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FALSE_POSITIVE)
      .setGap(15.0)
      .setEffort(10L)
      .setLine(6)
      .setSeverity("BLOCKER")
      .setPrioritizedRule(true)
      .setFromSonarQubeUpdate(true)
      .setMessage("message")
      .setMessageFormattings(EXAMPLE_MESSAGE_FORMATTINGS)
      .setManualSeverity(true)
      .setAssigneeUuid("perceval")
      .setAuthorLogin("pierre")
      .setIssueCreationDate(createdAt)
      .setIssueUpdateDate(updatedAt)
      .setIssueCloseDate(closedAt)
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY)
      .setTags(List.of("tag1", "tag2"))
      .setInternalTags(List.of("internalTag1", "internalTag2"))
      .setCodeVariants(List.of("variant1", "variant2"))
      .addImpact(new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(HIGH).setManualSeverity(true))
      .addImpact(new ImpactDto().setSoftwareQuality(RELIABILITY).setSeverity(LOW).setManualSeverity(false));

    DefaultIssue expected = new DefaultIssue()
      .setKey("100")
      .setType(RuleType.VULNERABILITY)
      .setRuleKey(RuleKey.of("java", "AvoidCycle"))
      .setLanguage("xoo")
      .setComponentUuid("CDEF")
      .setProjectUuid("GHIJ")
      .setComponentKey("org.sonar.sample:Sample")
      .setProjectKey("org.sonar.sample")
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FALSE_POSITIVE)
      .setGap(15.0)
      .setEffort(Duration.create(10L))
      .setLine(6)
      .setSeverity("BLOCKER")
      .setPrioritizedRule(true)
      .setFromSonarQubeUpdate(true)
      .setMessage("message")
      .setMessageFormattings(DbIssues.MessageFormattings.parseFrom(EXAMPLE_MESSAGE_FORMATTINGS.toByteArray()))
      .setManualSeverity(true)
      .setAssigneeUuid("perceval")
      .setAuthorLogin("pierre")
      .setCreationDate(DateUtils.truncate(createdAt, Calendar.SECOND))
      .setUpdateDate(DateUtils.truncate(updatedAt, Calendar.SECOND))
      .setCloseDate(DateUtils.truncate(closedAt, Calendar.SECOND))
      .setNew(false)
      .setIsNewCodeReferenceIssue(false)
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY)
      .setTags(List.of("tag1", "tag2"))
      .setInternalTags(List.of("internalTag1", "internalTag2"))
      .setCodeVariants(List.of("variant1", "variant2"))
      .addImpact(MAINTAINABILITY, HIGH, true)
      .addImpact(RELIABILITY, LOW, false);

    DefaultIssue issue = dto.toDefaultIssue();

    assertThat(issue).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void set_rule() {
    IssueDto dto = new IssueDto()
      .setKee("100")
      .setRule(new RuleDto().setUuid("uuid-1").setRuleKey("AvoidCycle").setRepositoryKey("java").setIsExternal(true).setCleanCodeAttribute(CleanCodeAttribute.CLEAR))
      .setLanguage("xoo");

    assertThat(dto.getRuleUuid()).isEqualTo("uuid-1");
    assertThat(dto.getRuleRepo()).isEqualTo("java");
    assertThat(dto.getRule()).isEqualTo("AvoidCycle");
    assertThat(dto.getRuleKey()).hasToString("java:AvoidCycle");
    assertThat(dto.getEffectiveCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.CLEAR);
    assertThat(dto.getLanguage()).isEqualTo("xoo");
    assertThat(dto.isExternal()).isTrue();
  }


  @Test
  void getEffectiveImpacts_whenNoIssueImpactsOverridden_shouldReturnRuleImpacts() {
    IssueDto dto = new IssueDto();
    dto.getRuleDefaultImpacts().add(newImpactDto(MAINTAINABILITY, HIGH));
    dto.getRuleDefaultImpacts().add(newImpactDto(SECURITY, MEDIUM));
    dto.getRuleDefaultImpacts().add(newImpactDto(RELIABILITY, LOW));

    assertThat(dto.getEffectiveImpacts())
      .hasSize(3)
      .containsEntry(MAINTAINABILITY, HIGH)
      .containsEntry(SECURITY, MEDIUM)
      .containsEntry(RELIABILITY, LOW);
  }

  @Test
  void getEffectiveImpacts_whenIssueImpactsOverridden_shouldReturnIssueImpacts() {
    IssueDto dto = new IssueDto();
    dto.getRuleDefaultImpacts().add(newImpactDto(MAINTAINABILITY, HIGH));
    dto.getRuleDefaultImpacts().add(newImpactDto(SECURITY, MEDIUM));
    dto.getRuleDefaultImpacts().add(newImpactDto(RELIABILITY, LOW));

    dto.addImpact(newImpactDto(MAINTAINABILITY, LOW));
    dto.addImpact(newImpactDto(RELIABILITY, HIGH));

    assertThat(dto.getEffectiveImpacts())
      .hasSize(2)
      .containsEntry(MAINTAINABILITY, LOW)
      .containsEntry(RELIABILITY, HIGH);
  }


  @Test
  void toDtoForComputationInsert_givenDefaultIssueWithAllFields_returnFullIssueDto() {
    long now = System.currentTimeMillis();
    Date dateNow = Date.from(new Date(now).toInstant().truncatedTo(ChronoUnit.SECONDS));
    DefaultIssue defaultIssue = createExampleDefaultIssue(dateNow);

    IssueDto issueDto = IssueDto.toDtoForComputationInsert(defaultIssue, "ruleUuid", now);

    assertThat(issueDto).extracting(IssueDto::getKey, IssueDto::getType, IssueDto::getRuleKey).containsExactly("key",
      RuleType.BUG.getDbConstant(), RuleKey.of("repo", "rule"));

    assertThat(issueDto).extracting(IssueDto::getIssueCreationDate, IssueDto::getIssueCloseDate,
      IssueDto::getIssueUpdateDate, IssueDto::getSelectedAt, IssueDto::getUpdatedAt, IssueDto::getCreatedAt)
      .containsExactly(dateNow, dateNow, dateNow, dateNow.getTime(), now, now);

    assertThat(issueDto).extracting(IssueDto::getLine, IssueDto::getMessage,
      IssueDto::getGap, IssueDto::getEffort, IssueDto::getResolution, IssueDto::getStatus, IssueDto::getSeverity)
      .containsExactly(1, "message", 1.0, 1L, Issue.RESOLUTION_FALSE_POSITIVE, Issue.STATUS_CLOSED, "BLOCKER");

    assertThat(issueDto).extracting(IssueDto::getTags, IssueDto::getCodeVariants, IssueDto::getInternalTags, IssueDto::getAuthorLogin)
      .containsExactly(Set.of("todo"), Set.of("variant1", "variant2"), Set.of("internalTag1", "internalTag2"), "admin");

    assertThat(issueDto).extracting(IssueDto::isManualSeverity, IssueDto::getChecksum, IssueDto::getAssigneeUuid,
      IssueDto::isExternal, IssueDto::getComponentUuid, IssueDto::getComponentKey,
      IssueDto::getProjectUuid, IssueDto::getProjectKey, IssueDto::getRuleUuid)
      .containsExactly(true, "123", "123", true, "123", "componentKey", "123", "projectKey", "ruleUuid");

    assertThat(issueDto.isQuickFixAvailable()).isTrue();
    assertThat(issueDto.isNewCodeReferenceIssue()).isTrue();
    assertThat(issueDto.getOptionalRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
    assertThat(issueDto.getImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity, ImpactDto::isManualSeverity)
      .containsExactlyInAnyOrder(tuple(MAINTAINABILITY, HIGH, true), tuple(RELIABILITY, LOW, false));
    assertThat(issueDto.isPrioritizedRule()).isTrue();
    assertThat(issueDto.isFromSonarQubeUpdate()).isTrue();
  }

  @Test
  void toDtoForUpdate_givenDefaultIssueWithAllFields_returnFullIssueDto() {
    long now = System.currentTimeMillis();
    Date dateNow = Date.from(new Date(now).toInstant().truncatedTo(ChronoUnit.SECONDS));
    DefaultIssue defaultIssue = createExampleDefaultIssue(dateNow);

    IssueDto issueDto = IssueDto.toDtoForUpdate(defaultIssue, now);

    assertThat(issueDto).extracting(IssueDto::getKey, IssueDto::getType, IssueDto::getRuleKey).containsExactly("key",
      RuleType.BUG.getDbConstant(), RuleKey.of("repo", "rule"));

    assertThat(issueDto).extracting(IssueDto::getIssueCreationDate, IssueDto::getIssueCloseDate,
      IssueDto::getIssueUpdateDate, IssueDto::getSelectedAt, IssueDto::getUpdatedAt)
      .containsExactly(dateNow, dateNow, dateNow, dateNow.getTime(), now);

    assertThat(issueDto).extracting(IssueDto::getLine, IssueDto::getMessage,
      IssueDto::getGap, IssueDto::getEffort, IssueDto::getResolution, IssueDto::getStatus, IssueDto::getSeverity)
      .containsExactly(1, "message", 1.0, 1L, Issue.RESOLUTION_FALSE_POSITIVE, Issue.STATUS_CLOSED, "BLOCKER");

    assertThat(issueDto).extracting(IssueDto::getTags, IssueDto::getCodeVariants, IssueDto::getInternalTags, IssueDto::getAuthorLogin)
      .containsExactly(Set.of("todo"), Set.of("variant1", "variant2"), Set.of("internalTag1", "internalTag2"), "admin");

    assertThat(issueDto).extracting(IssueDto::isManualSeverity, IssueDto::getChecksum, IssueDto::getAssigneeUuid,
      IssueDto::isExternal, IssueDto::getComponentUuid, IssueDto::getComponentKey, IssueDto::getProjectUuid, IssueDto::getProjectKey)
      .containsExactly(true, "123", "123", true, "123", "componentKey", "123", "projectKey");

    assertThat(issueDto.isQuickFixAvailable()).isTrue();
    assertThat(issueDto.isNewCodeReferenceIssue()).isTrue();
    assertThat(issueDto.getOptionalRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
    assertThat(issueDto.getImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity, ImpactDto::isManualSeverity)
      .containsExactlyInAnyOrder(tuple(MAINTAINABILITY, HIGH, true), tuple(RELIABILITY, LOW, false));
    assertThat(issueDto.isPrioritizedRule()).isTrue();
    assertThat(issueDto.isFromSonarQubeUpdate()).isTrue();
  }

  private DefaultIssue createExampleDefaultIssue(Date dateNow) {
    DefaultIssue defaultIssue = new DefaultIssue();
    defaultIssue.setKey("key")
      .setType(RuleType.BUG)
      .setLine(1)
      .setMessage("message")
      .setGap(1.0)
      .setEffort(Duration.create(1))
      .setResolution(Issue.RESOLUTION_FALSE_POSITIVE)
      .setStatus(Issue.STATUS_CLOSED)
      .setSeverity("BLOCKER")
      .setPrioritizedRule(true)
      .setFromSonarQubeUpdate(true)
      .setManualSeverity(true)
      .setChecksum("123")
      .setAssigneeUuid("123")
      .setRuleKey(RuleKey.of("repo", "rule"))
      .setIsFromExternalRuleEngine(true)
      .setTags(List.of("todo"))
      .setComponentUuid("123")
      .setComponentKey("componentKey")
      .setProjectUuid("123")
      .setProjectKey("projectKey")
      .setAuthorLogin("admin")
      .setCreationDate(dateNow)
      .setCloseDate(dateNow)
      .setUpdateDate(dateNow)
      .setSelectedAt(dateNow.getTime())
      .setQuickFixAvailable(true)
      .setIsNewCodeReferenceIssue(true)
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY)
      .setCodeVariants(List.of("variant1", "variant2"))
      .setInternalTags(List.of("internalTag1", "internalTag2"))
      .setPrioritizedRule(true)
      .addImpact(MAINTAINABILITY, HIGH, true)
      .addImpact(RELIABILITY, LOW, false);
    return defaultIssue;
  }

  private static ImpactDto newImpactDto(SoftwareQuality softwareQuality, Severity severity) {
    return new ImpactDto()
      .setSoftwareQuality(softwareQuality)
      .setSeverity(severity);
  }

}

