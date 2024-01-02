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

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueDtoTest {

  private static final String TEST_CONTEXT_KEY = "test_context_key";

  private static final DbIssues.MessageFormattings EXAMPLE_MESSAGE_FORMATTINGS = DbIssues.MessageFormattings.newBuilder()
    .addMessageFormatting(DbIssues.MessageFormatting.newBuilder().setStart(0).setEnd(1).setType(DbIssues.MessageFormattingType.CODE)
      .build())
    .build();

  @Test
  public void toDefaultIssue_set_issue_fields() {
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
      .setModuleUuidPath("ABCD.BCDE.")
      .setProjectKey("org.sonar.sample")
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FALSE_POSITIVE)
      .setGap(15.0)
      .setEffort(10L)
      .setLine(6)
      .setSeverity("BLOCKER")
      .setMessage("message")
      .setMessageFormattings(EXAMPLE_MESSAGE_FORMATTINGS)
      .setManualSeverity(true)
      .setAssigneeUuid("perceval")
      .setAuthorLogin("pierre")
      .setIssueCreationDate(createdAt)
      .setIssueUpdateDate(updatedAt)
      .setIssueCloseDate(closedAt)
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY);

    DefaultIssue issue = dto.toDefaultIssue();
    assertThat(issue.key()).isEqualTo("100");
    assertThat(issue.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(issue.ruleKey()).hasToString("java:AvoidCycle");
    assertThat(issue.language()).isEqualTo("xoo");
    assertThat(issue.componentUuid()).isEqualTo("CDEF");
    assertThat(issue.projectUuid()).isEqualTo("GHIJ");
    assertThat(issue.componentKey()).isEqualTo("org.sonar.sample:Sample");
    assertThat(issue.moduleUuidPath()).isEqualTo("ABCD.BCDE.");
    assertThat(issue.projectKey()).isEqualTo("org.sonar.sample");
    assertThat(issue.status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(issue.resolution()).isEqualTo(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(issue.gap()).isEqualTo(15.0);
    assertThat(issue.effort()).isEqualTo(Duration.create(10L));
    assertThat(issue.line()).isEqualTo(6);
    assertThat(issue.severity()).isEqualTo("BLOCKER");
    assertThat(issue.message()).isEqualTo("message");
    assertThat((DbIssues.MessageFormattings) issue.getMessageFormattings()).isEqualTo(EXAMPLE_MESSAGE_FORMATTINGS);
    assertThat(issue.manualSeverity()).isTrue();
    assertThat(issue.assignee()).isEqualTo("perceval");
    assertThat(issue.authorLogin()).isEqualTo("pierre");
    assertThat(issue.creationDate()).isEqualTo(DateUtils.truncate(createdAt, Calendar.SECOND));
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(updatedAt, Calendar.SECOND));
    assertThat(issue.closeDate()).isEqualTo(DateUtils.truncate(closedAt, Calendar.SECOND));
    assertThat(issue.isNew()).isFalse();
    assertThat(issue.isNewCodeReferenceIssue()).isFalse();
    assertThat(issue.getRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
  }

  @Test
  public void set_rule() {
    IssueDto dto = new IssueDto()
      .setKee("100")
      .setRule(new RuleDto().setUuid("uuid-1").setRuleKey("AvoidCycle").setRepositoryKey("java").setIsExternal(true))
      .setLanguage("xoo");

    assertThat(dto.getRuleUuid()).isEqualTo("uuid-1");
    assertThat(dto.getRuleRepo()).isEqualTo("java");
    assertThat(dto.getRule()).isEqualTo("AvoidCycle");
    assertThat(dto.getRuleKey()).hasToString("java:AvoidCycle");
    assertThat(dto.getLanguage()).isEqualTo("xoo");
    assertThat(dto.isExternal()).isTrue();
  }

  @Test
  public void set_tags() {
    IssueDto dto = new IssueDto();
    assertThat(dto.getTags()).isEmpty();
    assertThat(dto.getTagsString()).isNull();

    dto.setTags(Arrays.asList("tag1", "tag2", "tag3"));
    assertThat(dto.getTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(dto.getTagsString()).isEqualTo("tag1,tag2,tag3");

    dto.setTags(Arrays.asList());
    assertThat(dto.getTags()).isEmpty();

    dto.setTagsString("tag1, tag2 ,,tag3");
    assertThat(dto.getTags()).containsOnly("tag1", "tag2", "tag3");

    dto.setTagsString(null);
    assertThat(dto.getTags()).isEmpty();

    dto.setTagsString("");
    assertThat(dto.getTags()).isEmpty();
  }

  @Test
  public void toDtoForComputationInsert_givenDefaultIssueWithAllFields_returnFullIssueDto() {
    long now = System.currentTimeMillis();
    Date dateNow = Date.from(new Date(now).toInstant().truncatedTo(ChronoUnit.SECONDS));
    DefaultIssue defaultIssue = createExampleDefaultIssue(dateNow);

    IssueDto issueDto = IssueDto.toDtoForComputationInsert(defaultIssue, "ruleUuid", now);

    assertThat(issueDto).extracting(IssueDto::getKey, IssueDto::getType, IssueDto::getRuleKey).containsExactly("key", RuleType.BUG.getDbConstant(), RuleKey.of("repo", "rule"));

    assertThat(issueDto).extracting(IssueDto::getIssueCreationDate, IssueDto::getIssueCloseDate,
      IssueDto::getIssueUpdateDate, IssueDto::getSelectedAt, IssueDto::getUpdatedAt, IssueDto::getCreatedAt)
      .containsExactly(dateNow, dateNow, dateNow, dateNow.getTime(), now, now);

    assertThat(issueDto).extracting(IssueDto::getLine, IssueDto::getMessage,
      IssueDto::getGap, IssueDto::getEffort, IssueDto::getResolution, IssueDto::getStatus, IssueDto::getSeverity)
      .containsExactly(1, "message", 1.0, 1L, Issue.RESOLUTION_FALSE_POSITIVE, Issue.STATUS_CLOSED, "BLOCKER");

    assertThat(issueDto).extracting(IssueDto::getTags, IssueDto::getAuthorLogin)
      .containsExactly(Set.of("todo"), "admin");

    assertThat(issueDto).extracting(IssueDto::isManualSeverity, IssueDto::getChecksum, IssueDto::getAssigneeUuid,
      IssueDto::isExternal, IssueDto::getComponentUuid, IssueDto::getComponentKey,
      IssueDto::getModuleUuidPath, IssueDto::getProjectUuid, IssueDto::getProjectKey,
      IssueDto::getRuleUuid)
      .containsExactly(true, "123", "123", true, "123", "componentKey",
        "path/to/module/uuid", "123", "projectKey", "ruleUuid");

    assertThat(issueDto.isQuickFixAvailable()).isTrue();
    assertThat(issueDto.isNewCodeReferenceIssue()).isTrue();
    assertThat(issueDto.getOptionalRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
  }

  @Test
  public void toDtoForUpdate_givenDefaultIssueWithAllFields_returnFullIssueDto() {
    long now = System.currentTimeMillis();
    Date dateNow = Date.from(new Date(now).toInstant().truncatedTo(ChronoUnit.SECONDS));
    DefaultIssue defaultIssue = createExampleDefaultIssue(dateNow);

    IssueDto issueDto = IssueDto.toDtoForUpdate(defaultIssue, now);

    assertThat(issueDto).extracting(IssueDto::getKey, IssueDto::getType, IssueDto::getRuleKey).containsExactly("key", RuleType.BUG.getDbConstant(), RuleKey.of("repo", "rule"));

    assertThat(issueDto).extracting(IssueDto::getIssueCreationDate, IssueDto::getIssueCloseDate,
      IssueDto::getIssueUpdateDate, IssueDto::getSelectedAt, IssueDto::getUpdatedAt)
      .containsExactly(dateNow, dateNow, dateNow, dateNow.getTime(), now);

    assertThat(issueDto).extracting(IssueDto::getLine, IssueDto::getMessage,
      IssueDto::getGap, IssueDto::getEffort, IssueDto::getResolution, IssueDto::getStatus, IssueDto::getSeverity)
      .containsExactly(1, "message", 1.0, 1L, Issue.RESOLUTION_FALSE_POSITIVE, Issue.STATUS_CLOSED, "BLOCKER");

    assertThat(issueDto).extracting(IssueDto::getTags, IssueDto::getAuthorLogin)
      .containsExactly(Set.of("todo"), "admin");

    assertThat(issueDto).extracting(IssueDto::isManualSeverity, IssueDto::getChecksum, IssueDto::getAssigneeUuid,
      IssueDto::isExternal, IssueDto::getComponentUuid, IssueDto::getComponentKey,
      IssueDto::getModuleUuidPath, IssueDto::getProjectUuid, IssueDto::getProjectKey)
      .containsExactly(true, "123", "123", true, "123", "componentKey",
        "path/to/module/uuid", "123", "projectKey");

    assertThat(issueDto.isQuickFixAvailable()).isTrue();
    assertThat(issueDto.isNewCodeReferenceIssue()).isTrue();
    assertThat(issueDto.getOptionalRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
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
      .setManualSeverity(true)
      .setChecksum("123")
      .setAssigneeUuid("123")
      .setRuleKey(RuleKey.of("repo", "rule"))
      .setIsFromExternalRuleEngine(true)
      .setTags(List.of("todo"))
      .setComponentUuid("123")
      .setComponentKey("componentKey")
      .setModuleUuidPath("path/to/module/uuid")
      .setProjectUuid("123")
      .setProjectKey("projectKey")
      .setAuthorLogin("admin")
      .setCreationDate(dateNow)
      .setCloseDate(dateNow)
      .setUpdateDate(dateNow)
      .setSelectedAt(dateNow.getTime())
      .setQuickFixAvailable(true)
      .setIsNewCodeReferenceIssue(true)
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY);
    return defaultIssue;
  }
}
