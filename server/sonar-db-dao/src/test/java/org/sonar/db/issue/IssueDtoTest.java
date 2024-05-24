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

import com.google.protobuf.InvalidProtocolBufferException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
      .setMessage("message")
      .setMessageFormattings(EXAMPLE_MESSAGE_FORMATTINGS)
      .setManualSeverity(true)
      .setAssigneeUuid("perceval")
      .setAuthorLogin("pierre")
      .setIssueCreationDate(createdAt)
      .setIssueUpdateDate(updatedAt)
      .setIssueCloseDate(closedAt)
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY)
      .addImpact(new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(HIGH));

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
      .setCodeVariants(Set.of())
      .setTags(Set.of())
      .addImpact(MAINTAINABILITY, HIGH);

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
  void set_tags() {
    IssueDto dto = new IssueDto();
    assertThat(dto.getTags()).isEmpty();
    assertThat(dto.getTagsString()).isNull();

    dto.setTags(Arrays.asList("tag1", "tag2", "tag3"));
    assertThat(dto.getTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(dto.getTagsString()).isEqualTo("tag1,tag2,tag3");

    dto.setTags(List.of());
    assertThat(dto.getTags()).isEmpty();

    dto.setTagsString("tag1, tag2 ,,tag3");
    assertThat(dto.getTags()).containsOnly("tag1", "tag2", "tag3");

    dto.setTagsString(null);
    assertThat(dto.getTags()).isEmpty();

    dto.setTagsString("");
    assertThat(dto.getTags()).isEmpty();
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
  void addImpact_whenSoftwareQualityAlreadyDefined_shouldThrowISE() {
    IssueDto dto = new IssueDto();
    dto.addImpact(newImpactDto(MAINTAINABILITY, LOW));

    ImpactDto duplicatedImpact = newImpactDto(MAINTAINABILITY, HIGH);

    assertThatThrownBy(() -> dto.addImpact(duplicatedImpact))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Impact already defined on issue for Software Quality [MAINTAINABILITY]");
  }

  @Test
  void replaceAllImpacts_whenSoftwareQualityAlreadyDuplicated_shouldThrowISE() {
    IssueDto dto = new IssueDto();
    dto.addImpact(newImpactDto(MAINTAINABILITY, MEDIUM));
    dto.addImpact(newImpactDto(SECURITY, HIGH));
    dto.addImpact(newImpactDto(RELIABILITY, LOW));

    Set<ImpactDto> duplicatedImpacts = Set.of(
      newImpactDto(MAINTAINABILITY, HIGH),
      newImpactDto(MAINTAINABILITY, LOW));
    assertThatThrownBy(() -> dto.replaceAllImpacts(duplicatedImpacts))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Impacts must have unique Software Quality values");
  }

  @Test
  void replaceAllImpacts_shouldReplaceExistingImpacts() {
    IssueDto dto = new IssueDto();
    dto.addImpact(newImpactDto(MAINTAINABILITY, MEDIUM));
    dto.addImpact(newImpactDto(SECURITY, HIGH));
    dto.addImpact(newImpactDto(RELIABILITY, LOW));

    Set<ImpactDto> duplicatedImpacts = Set.of(
      newImpactDto(MAINTAINABILITY, HIGH),
      newImpactDto(SECURITY, LOW));

    dto.replaceAllImpacts(duplicatedImpacts);

    assertThat(dto.getImpacts())
      .extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactlyInAnyOrder(
        tuple(MAINTAINABILITY, HIGH),
        tuple(SECURITY, LOW));

  }

  @Test
  void setCodeVariants_shouldReturnCodeVariants() {
    IssueDto dto = new IssueDto();
    assertThat(dto.getCodeVariants()).isEmpty();
    assertThat(dto.getCodeVariantsString()).isNull();

    dto.setCodeVariants(Arrays.asList("variant1", "variant2", "variant3"));
    assertThat(dto.getCodeVariants()).containsOnly("variant1", "variant2", "variant3");
    assertThat(dto.getCodeVariantsString()).isEqualTo("variant1,variant2,variant3");

    dto.setCodeVariants(null);
    assertThat(dto.getCodeVariants()).isEmpty();
    assertThat(dto.getCodeVariantsString()).isNull();

    dto.setCodeVariants(List.of());
    assertThat(dto.getCodeVariants()).isEmpty();
    assertThat(dto.getCodeVariantsString()).isNull();
  }

  @Test
  void setCodeVariantsString_shouldReturnCodeVariants() {
    IssueDto dto = new IssueDto();

    dto.setCodeVariantsString("variant1, variant2 ,,variant4");
    assertThat(dto.getCodeVariants()).containsOnly("variant1", "variant2", "variant4");

    dto.setCodeVariantsString(null);
    assertThat(dto.getCodeVariants()).isEmpty();

    dto.setCodeVariantsString("");
    assertThat(dto.getCodeVariants()).isEmpty();
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

    assertThat(issueDto).extracting(IssueDto::getTags, IssueDto::getCodeVariants, IssueDto::getAuthorLogin)
      .containsExactly(Set.of("todo"), Set.of("variant1", "variant2"), "admin");

    assertThat(issueDto).extracting(IssueDto::isManualSeverity, IssueDto::getChecksum, IssueDto::getAssigneeUuid,
        IssueDto::isExternal, IssueDto::getComponentUuid, IssueDto::getComponentKey,
        IssueDto::getProjectUuid, IssueDto::getProjectKey, IssueDto::getRuleUuid)
      .containsExactly(true, "123", "123", true, "123", "componentKey", "123", "projectKey", "ruleUuid");

    assertThat(issueDto.isQuickFixAvailable()).isTrue();
    assertThat(issueDto.isNewCodeReferenceIssue()).isTrue();
    assertThat(issueDto.getOptionalRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
    assertThat(issueDto.getImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactlyInAnyOrder(tuple(MAINTAINABILITY, HIGH), tuple(RELIABILITY, LOW));
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

    assertThat(issueDto).extracting(IssueDto::getTags, IssueDto::getCodeVariants, IssueDto::getAuthorLogin)
      .containsExactly(Set.of("todo"), Set.of("variant1", "variant2"), "admin");

    assertThat(issueDto).extracting(IssueDto::isManualSeverity, IssueDto::getChecksum, IssueDto::getAssigneeUuid,
        IssueDto::isExternal, IssueDto::getComponentUuid, IssueDto::getComponentKey, IssueDto::getProjectUuid, IssueDto::getProjectKey)
      .containsExactly(true, "123", "123", true, "123", "componentKey", "123", "projectKey");

    assertThat(issueDto.isQuickFixAvailable()).isTrue();
    assertThat(issueDto.isNewCodeReferenceIssue()).isTrue();
    assertThat(issueDto.getOptionalRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
    assertThat(issueDto.getImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactlyInAnyOrder(tuple(MAINTAINABILITY, HIGH), tuple(RELIABILITY, LOW));
  }

  @Test
  void getIssueStatus_shouldReturnExpectedValueFromStatusAndResolution() {
    IssueDto dto = new IssueDto();
    dto.setStatus(Issue.STATUS_CLOSED);
    assertThat(dto.getIssueStatus()).isEqualTo(IssueStatus.FIXED);

    dto.setStatus(Issue.STATUS_RESOLVED);
    dto.setResolution(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(dto.getIssueStatus()).isEqualTo(IssueStatus.FALSE_POSITIVE);

    dto.setStatus(Issue.STATUS_RESOLVED);
    dto.setResolution(Issue.RESOLUTION_WONT_FIX);
    assertThat(dto.getIssueStatus()).isEqualTo(IssueStatus.ACCEPTED);
  }

  @Test
  void getIssueStatus_shouldReturnOpen_whenStatusIsNull() {
    IssueDto dto = new IssueDto();
    assertThat(dto.getIssueStatus())
      .isEqualTo(IssueStatus.OPEN);
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
      .addImpact(MAINTAINABILITY, HIGH)
      .addImpact(RELIABILITY, LOW);
    return defaultIssue;
  }

  private static ImpactDto newImpactDto(SoftwareQuality softwareQuality, Severity severity) {
    return new ImpactDto()
      .setSoftwareQuality(softwareQuality)
      .setSeverity(severity);
  }

}
