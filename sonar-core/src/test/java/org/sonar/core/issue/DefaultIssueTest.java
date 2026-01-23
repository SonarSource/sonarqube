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
package org.sonar.core.issue;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.utils.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultIssueTest {

  private final DefaultIssue issue = new DefaultIssue();

  @Test
  void set_empty_dates() {
    issue
      .setCreationDate(null)
      .setUpdateDate(null)
      .setCloseDate(null)
      .setSelectedAt(null);

    assertThat(issue.creationDate()).isNull();
    assertThat(issue.updateDate()).isNull();
    assertThat(issue.closeDate()).isNull();
    assertThat(issue.selectedAt()).isNull();
  }

  @Test
  void fail_on_empty_status() {
    try {
      issue.setStatus("");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Status must be set");
    }
  }

  @Test
  void fail_on_bad_severity() {
    try {
      issue.setSeverity("FOO");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Not a valid severity: FOO");
    }
  }

  @Test
  void message_should_be_abbreviated_if_too_long() {
    issue.setMessage(StringUtils.repeat("a", 5_000));
    assertThat(issue.message()).hasSize(1_333);
  }

  @Test
  void message_could_be_null() {
    issue.setMessage(null);
    assertThat(issue.message()).isNull();
  }

  @Test
  void test_nullable_fields() {
    issue.setGap(null).setSeverity(null).setLine(null);
    assertThat(issue.gap()).isNull();
    assertThat(issue.severity()).isNull();
    assertThat(issue.line()).isNull();
  }

  @Test
  void test_equals_and_hashCode() {
    DefaultIssue a1 = new DefaultIssue().setKey("AAA");
    DefaultIssue a2 = new DefaultIssue().setKey("AAA");
    DefaultIssue b = new DefaultIssue().setKey("BBB");
    assertThat(a1)
      .isEqualTo(a1)
      .isEqualTo(a2)
      .isNotEqualTo(b)
      .hasSameHashCodeAs(a1);
  }

  @Test
  void comments_should_not_be_modifiable() {
    DefaultIssue defaultIssue = new DefaultIssue().setKey("AAA");

    List<DefaultIssueComment> comments = defaultIssue.defaultIssueComments();
    assertThat(comments).isEmpty();
    DefaultIssueComment defaultIssueComment = new DefaultIssueComment();
    try {
      comments.add(defaultIssueComment);
      fail();
    } catch (UnsupportedOperationException e) {
      // ok
    } catch (Exception e) {
      fail("Unexpected exception: " + e);
    }
  }

  @Test
  void all_changes_contain_current_change() {
    IssueChangeContext issueChangeContext = mock(IssueChangeContext.class);
    when(issueChangeContext.getExternalUser()).thenReturn("toto");
    when(issueChangeContext.getWebhookSource()).thenReturn("github");

    DefaultIssue defaultIssue = new DefaultIssue()
      .setKey("AAA")
      .setFieldChange(issueChangeContext, "actionPlan", "1.0", "1.1");

    assertThat(defaultIssue.changes()).hasSize(1);
    FieldDiffs actualDiffs = defaultIssue.changes().iterator().next();
    assertThat(actualDiffs.externalUser()).contains(issueChangeContext.getExternalUser());
    assertThat(actualDiffs.webhookSource()).contains(issueChangeContext.getWebhookSource());
  }

  @Test
  void setFieldChange_whenAddingChange_shouldUpdateCurrentChange() {
    IssueChangeContext issueChangeContext = mock(IssueChangeContext.class);
    DefaultIssue defaultIssue = new DefaultIssue().setKey("AAA");

    defaultIssue.setFieldChange(issueChangeContext, "actionPlan", "1.0", "1.1");
    assertThat(defaultIssue.changes()).hasSize(1);
    FieldDiffs currentChange = defaultIssue.currentChange();
    assertThat(currentChange).isNotNull();
    assertThat(currentChange.get("actionPlan")).isNotNull();
    assertThat(currentChange.get("authorLogin")).isNull();

    defaultIssue.setFieldChange(issueChangeContext, "authorLogin", null, "testuser");
    assertThat(defaultIssue.changes()).hasSize(1);
    assertThat(currentChange.get("actionPlan")).isNotNull();
    assertThat(currentChange.get("authorLogin")).isNotNull();
    assertThat(currentChange.get("authorLogin").newValue()).isEqualTo("testuser");
  }

  @Test
  void adding_null_change_has_no_effect() {
    DefaultIssue defaultIssue = new DefaultIssue();

    defaultIssue.addChange(null);

    assertThat(defaultIssue.changes()).isEmpty();
  }

  @Test
  void test_isToBeMigratedAsNewCodeReferenceIssue_is_correctly_calculated() {
    issue.setKey("ABCD")
      .setIsOnChangedLine(true)
      .setIsNewCodeReferenceIssue(false)
      .setIsNoLongerNewCodeReferenceIssue(false);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isTrue();

    issue.setKey("ABCD")
      .setIsOnChangedLine(false)
      .setIsNewCodeReferenceIssue(false)
      .setIsNoLongerNewCodeReferenceIssue(false);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isFalse();

    issue.setKey("ABCD")
      .setIsOnChangedLine(true)
      .setIsNewCodeReferenceIssue(true)
      .setIsNoLongerNewCodeReferenceIssue(false);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isFalse();

    issue.setKey("ABCD")
      .setIsOnChangedLine(false)
      .setIsNewCodeReferenceIssue(false)
      .setIsNoLongerNewCodeReferenceIssue(true);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isFalse();

    issue.setKey("ABCD")
      .setIsOnChangedLine(true)
      .setIsNewCodeReferenceIssue(true)
      .setIsNoLongerNewCodeReferenceIssue(true);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isFalse();

    issue.setKey("ABCD")
      .setIsOnChangedLine(false)
      .setIsNewCodeReferenceIssue(true)
      .setIsNoLongerNewCodeReferenceIssue(true);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isFalse();

    issue.setKey("ABCD")
      .setIsOnChangedLine(true)
      .setIsNewCodeReferenceIssue(false)
      .setIsNoLongerNewCodeReferenceIssue(true);

    assertThat(issue.isToBeMigratedAsNewCodeReferenceIssue()).isFalse();
  }

  @Test
  void isQuickFixAvailable_givenQuickFixAvailable_returnTrue() {
    DefaultIssue defaultIssue = new DefaultIssue();

    defaultIssue.setQuickFixAvailable(true);

    assertThat(defaultIssue.isQuickFixAvailable()).isTrue();

    defaultIssue.setQuickFixAvailable(false);

    assertThat(defaultIssue.isQuickFixAvailable()).isFalse();
  }

  @Test
  void setLine_whenLineIsNegative_shouldThrowException() {
    int anyNegativeValue = Integer.MIN_VALUE;
    assertThatThrownBy(() -> issue.setLine(anyNegativeValue))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Line must be null or greater than zero (got %s)", anyNegativeValue));
  }

  @Test
  void setLine_whenLineIsZero_shouldThrowException() {
    assertThatThrownBy(() -> issue.setLine(0))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Line must be null or greater than zero (got 0)");
  }

  @Test
  void setGap_whenGapIsNegative_shouldThrowException() {
    Double anyNegativeValue = -1.0;
    assertThatThrownBy(() -> issue.setGap(anyNegativeValue))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Gap must be greater than or equal 0 (got %s)", anyNegativeValue));
  }

  @Test
  void setGap_whenGapIsZero_shouldWork() {
    issue.setGap(0.0);
    assertThat(issue.gap()).isEqualTo(0.0);
  }

  @Test
  void effortInMinutes_shouldConvertEffortToMinutes() {
    issue.setEffort(Duration.create(60));
    assertThat(issue.effortInMinutes()).isEqualTo(60L);
  }

  @Test
  void effortInMinutes_whenNull_shouldReturnNull() {
    issue.setEffort(null);
    assertThat(issue.effortInMinutes()).isNull();
  }

  @Test
  void tags_whenNull_shouldReturnEmptySet() {
    assertThat(issue.tags()).isEmpty();
  }

  @Test
  void internalTags_whenNull_shouldReturnEmptySet() {
    assertThat(issue.internalTags()).isEmpty();
  }

  @Test
  void codeVariants_whenNull_shouldReturnEmptySet() {
    assertThat(issue.codeVariants()).isEmpty();
  }

  @Test
  void issueByDefault_shouldNotHaveAppliedAnticipatedTransitions() {
    DefaultIssue defaultIssue = new DefaultIssue();
    assertThat(defaultIssue.getAnticipatedTransitionUuid()).isNotPresent();
  }

  @Test
  void anticipatedTransitions_WhenSetTrue_shouldReturnTrue() {
    DefaultIssue defaultIssue = new DefaultIssue();
    defaultIssue.setAnticipatedTransitionUuid("uuid");
    assertThat(defaultIssue.getAnticipatedTransitionUuid()).isPresent();

  }

  @Test
  void getImpacts_whenAddingNewImpacts_shouldReturnListOfImpacts() {
    issue.addImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH);
    issue.addImpact(SoftwareQuality.RELIABILITY, Severity.LOW);

    assertThat(issue.impacts()).containsExactlyInAnyOrderEntriesOf(Map.of(SoftwareQuality.MAINTAINABILITY, Severity.HIGH,
      SoftwareQuality.RELIABILITY, Severity.LOW));
  }

  @Test
  void getIssueStatus_shouldReturnExpectedStatus() {
    issue.setStatus(Issue.STATUS_RESOLVED);
    issue.setResolution(Issue.RESOLUTION_FIXED);

    assertThat(issue.issueStatus()).isEqualTo(IssueStatus.FIXED);
  }

  @Test
  void replaceImpacts_shouldReplaceExistingImpacts() {
    issue.addImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH);
    issue.addImpact(SoftwareQuality.RELIABILITY, Severity.LOW);

    issue.replaceImpacts(Map.of(SoftwareQuality.SECURITY, Severity.LOW));

    assertThat(issue.impacts()).containsExactlyEntriesOf(Map.of(SoftwareQuality.SECURITY, Severity.LOW));
  }

  @Test
  void addImpact_shouldReplaceExistingSoftwareQuality() {
    issue.addImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH);
    issue.addImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW, true);
    assertThat(issue.getImpacts())
      .containsExactly(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW, true));
  }

  @Test
  void prioritizedRule_shouldHaveCorrectDefaultValue() {
    assertThat(issue.isPrioritizedRule()).isFalse();
    issue.setPrioritizedRule(true);
    assertThat(issue.isPrioritizedRule()).isTrue();
  }

  @Test
  void fromSonarQubeUpdate_shouldHaveCorrectDefaultValue() {
    assertThat(issue.isFromSonarQubeUpdate()).isFalse();
    issue.setFromSonarQubeUpdate(true);
    assertThat(issue.isFromSonarQubeUpdate()).isTrue();
  }
}
