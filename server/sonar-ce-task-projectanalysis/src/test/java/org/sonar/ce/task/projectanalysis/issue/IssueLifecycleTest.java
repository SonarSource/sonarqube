/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Date;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.core.issue.DefaultImpact;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.component.BranchType;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.workflow.IssueWorkflow;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.db.rule.RuleTesting.XOO_X1;

public class IssueLifecycleTest {
  private static final Date DEFAULT_DATE = new Date();
  private static final Duration DEFAULT_DURATION = Duration.create(10);
  private static final String TEST_CONTEXT_KEY = "test_context_key";

  private final DumbRule rule = new DumbRule(XOO_X1);

  @Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule().add(rule);
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private final IssueChangeContext issueChangeContext = issueChangeContextByUserBuilder(DEFAULT_DATE, "default_user_uuid").build();
  private final IssueWorkflow workflow = mock(IssueWorkflow.class);
  private final IssueFieldsSetter updater = mock(IssueFieldsSetter.class);
  private final DebtCalculator debtCalculator = mock(DebtCalculator.class);
  private final IssueLifecycle underTest = new IssueLifecycle(analysisMetadataHolder, issueChangeContext, workflow, updater, debtCalculator, ruleRepository);

  @Test
  public void initNewOpenIssue() {
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(XOO_X1);
    when(debtCalculator.calculate(issue)).thenReturn(DEFAULT_DURATION);

    underTest.initNewOpenIssue(issue);

    assertThat(issue.key()).isNotNull();
    assertThat(issue.creationDate()).isNotNull();
    assertThat(issue.updateDate()).isNotNull();
    assertThat(issue.status()).isEqualTo(STATUS_OPEN);
    assertThat(issue.effort()).isEqualTo(DEFAULT_DURATION);
    assertThat(issue.isNew()).isTrue();
    assertThat(issue.isCopied()).isFalse();
    assertThat(issue.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.CONVENTIONAL);
  }

  @Test
  public void initNewOpenHotspot() {
    rule.setType(RuleType.SECURITY_HOTSPOT);
    DefaultIssue issue = new DefaultIssue()
      .setRuleKey(XOO_X1);
    when(debtCalculator.calculate(issue)).thenReturn(DEFAULT_DURATION);

    underTest.initNewOpenIssue(issue);

    assertThat(issue.key()).isNotNull();
    assertThat(issue.creationDate()).isNotNull();
    assertThat(issue.updateDate()).isNotNull();
    assertThat(issue.status()).isEqualTo(STATUS_TO_REVIEW);
    assertThat(issue.resolution()).isNull();
    assertThat(issue.effort()).isEqualTo(DEFAULT_DURATION);
    assertThat(issue.isNew()).isTrue();
    assertThat(issue.isCopied()).isFalse();
  }

  @Test
  public void mergeIssueFromPRIntoBranch() {
    DefaultIssue raw = new DefaultIssue()
      .setKey("raw");
    DefaultIssue fromShort = new DefaultIssue()
      .setKey("short")
      .setIsNewCodeReferenceIssue(true);
    fromShort.setResolution("resolution");
    fromShort.setStatus("status");
    fromShort.setCleanCodeAttribute(CleanCodeAttribute.COMPLETE);

    Date commentDate = new Date();
    fromShort.addComment(new DefaultIssueComment()
      .setIssueKey("short")
      .setCreatedAt(commentDate)
      .setUserUuid("user_uuid")
      .setMarkdownText("A comment"));

    Date diffDate = new Date();
    // file diff alone
    fromShort.addChange(new FieldDiffs()
      .setCreationDate(diffDate)
      .setIssueKey("short")
      .setUserUuid("user_uuid")
      .setDiff("file", "uuidA1", "uuidB1"));
    // file diff with another field
    fromShort.addChange(new FieldDiffs()
      .setCreationDate(diffDate)
      .setIssueKey("short")
      .setUserUuid("user_uuid")
      .setDiff("severity", "MINOR", "MAJOR")
      .setDiff("file", "uuidA2", "uuidB2"));

    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn("master");
    analysisMetadataHolder.setBranch(branch);

    underTest.mergeConfirmedOrResolvedFromPrOrBranch(raw, fromShort, BranchType.PULL_REQUEST, "2");

    assertThat(raw.resolution()).isEqualTo("resolution");
    assertThat(raw.status()).isEqualTo("status");
    assertThat(raw.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.COMPLETE);
    assertThat(raw.defaultIssueComments())
      .extracting(DefaultIssueComment::issueKey, DefaultIssueComment::createdAt, DefaultIssueComment::userUuid, DefaultIssueComment::markdownText)
      .containsOnly(tuple("raw", commentDate, "user_uuid", "A comment"));
    assertThat(raw.changes()).hasSize(2);
    assertThat(raw.changes().get(0).creationDate()).isEqualTo(diffDate);
    assertThat(raw.changes().get(0).userUuid()).contains("user_uuid");
    assertThat(raw.changes().get(0).issueKey()).contains("raw");
    assertThat(raw.changes().get(0).diffs()).containsOnlyKeys("severity");
    assertThat(raw.changes().get(1).userUuid()).contains("default_user_uuid");
    assertThat(raw.changes().get(1).diffs()).containsOnlyKeys(IssueFieldsSetter.FROM_BRANCH);
    assertThat(raw.changes().get(1).get(IssueFieldsSetter.FROM_BRANCH).oldValue()).isEqualTo("#2");
    assertThat(raw.changes().get(1).get(IssueFieldsSetter.FROM_BRANCH).newValue()).isEqualTo("master");
    assertThat(raw.isNewCodeReferenceIssue()).isTrue();
  }

  @Test
  public void copyExistingIssuesFromSourceBranchOfPullRequest() {
    String pullRequestKey = "1";
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(branch.getName()).thenReturn("sourceBranch-1");
    when(branch.getPullRequestKey()).thenReturn(pullRequestKey);
    analysisMetadataHolder.setBranch(branch);
    analysisMetadataHolder.setPullRequestKey(pullRequestKey);
    DefaultIssue raw = new DefaultIssue()
      .setKey("raw");
    DefaultIssue fromShort = new DefaultIssue()
      .setKey("short");
    fromShort.setResolution("resolution");
    fromShort.setStatus("status");
    fromShort.setCleanCodeAttribute(CleanCodeAttribute.DISTINCT);

    Date commentDate = new Date();
    fromShort.addComment(new DefaultIssueComment()
      .setIssueKey("short")
      .setCreatedAt(commentDate)
      .setUserUuid("user_uuid")
      .setMarkdownText("A comment"));

    Date diffDate = new Date();
    // file diff alone
    fromShort.addChange(new FieldDiffs()
      .setCreationDate(diffDate)
      .setIssueKey("short")
      .setUserUuid("user_uuid")
      .setDiff("file", "uuidA1", "uuidB1"));
    // file diff with another field
    fromShort.addChange(new FieldDiffs()
      .setCreationDate(diffDate)
      .setIssueKey("short")
      .setUserUuid("user_uuid")
      .setDiff("severity", "MINOR", "MAJOR")
      .setDiff("file", "uuidA2", "uuidB2"));

    underTest.copyExistingIssueFromSourceBranchToPullRequest(raw, fromShort);

    assertThat(raw.resolution()).isEqualTo("resolution");
    assertThat(raw.status()).isEqualTo("status");
    assertThat(raw.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.DISTINCT);
    assertThat(raw.defaultIssueComments())
      .extracting(DefaultIssueComment::issueKey, DefaultIssueComment::createdAt, DefaultIssueComment::userUuid, DefaultIssueComment::markdownText)
      .containsOnly(tuple("raw", commentDate, "user_uuid", "A comment"));
    assertThat(raw.changes()).hasSize(2);
    assertThat(raw.changes().get(0).creationDate()).isEqualTo(diffDate);
    assertThat(raw.changes().get(0).userUuid()).contains("user_uuid");
    assertThat(raw.changes().get(0).issueKey()).contains("raw");
    assertThat(raw.changes().get(0).diffs()).containsOnlyKeys("severity");
    assertThat(raw.changes().get(1).userUuid()).contains("default_user_uuid");
    assertThat(raw.changes().get(1).diffs()).containsOnlyKeys(IssueFieldsSetter.FROM_BRANCH);
    assertThat(raw.changes().get(1).get(IssueFieldsSetter.FROM_BRANCH).oldValue()).isEqualTo("sourceBranch-1");
    assertThat(raw.changes().get(1).get(IssueFieldsSetter.FROM_BRANCH).newValue()).isEqualTo("#1");
  }

  @Test
  public void copyExistingIssuesFromSourceBranchOfPullRequest_copyFieldDiffsCorrectly() {
    String pullRequestKey = "1";
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(branch.getName()).thenReturn("sourceBranch-1");
    when(branch.getPullRequestKey()).thenReturn(pullRequestKey);
    analysisMetadataHolder.setBranch(branch);
    analysisMetadataHolder.setPullRequestKey(pullRequestKey);
    DefaultIssue destIssue = new DefaultIssue()
      .setKey("raw");
    DefaultIssue sourceIssue = new DefaultIssue()
      .setKey("issue");
    sourceIssue.setResolution("resolution");
    sourceIssue.setStatus("status");

    FieldDiffs sourceFieldDiffs = new FieldDiffs();
    sourceIssue.addChange(sourceFieldDiffs
      .setCreationDate(new Date())
      .setIssueKey("short")
      .setUserUuid("user_uuid")
      .setExternalUser("toto")
      .setWebhookSource("github")
      .setDiff("severity", "MINOR", "MAJOR"));

    underTest.copyExistingIssueFromSourceBranchToPullRequest(destIssue, sourceIssue);

    FieldDiffs actualFieldDiffs = destIssue.changes().iterator().next();
    assertThat(actualFieldDiffs.issueKey()).contains(destIssue.key());
    assertThat(actualFieldDiffs).usingRecursiveComparison().ignoringFields("issueKey").isEqualTo(sourceFieldDiffs);
  }

  @Test
  public void copyExistingIssuesFromSourceBranchOfPullRequest_only_works_for_pull_requests() {
    DefaultIssue raw = new DefaultIssue()
      .setKey("raw");
    DefaultIssue from = new DefaultIssue()
      .setKey("short");
    from.setResolution("resolution");
    from.setStatus("status");
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.BRANCH);
    analysisMetadataHolder.setBranch(branch);

    assertThatThrownBy(() -> underTest.copyExistingIssueFromSourceBranchToPullRequest(raw, from))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("This operation should be done only on pull request analysis");
  }

  @Test
  public void copiedIssue() {
    DefaultIssue raw = new DefaultIssue()
      .setNew(true)
      .setKey("RAW_KEY")
      .setCreationDate(parseDate("2015-10-01"))
      .setUpdateDate(parseDate("2015-10-02"))
      .setCloseDate(parseDate("2015-10-03"))
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY);

    DbIssues.Locations issueLocations = DbIssues.Locations.newBuilder()
      .setTextRange(DbCommons.TextRange.newBuilder()
        .setStartLine(10)
        .setEndLine(12)
        .build())
      .build();
    DefaultIssue base = new DefaultIssue()
      .setKey("BASE_KEY")
      .setCreationDate(parseDate("2015-01-01"))
      .setUpdateDate(parseDate("2015-01-02"))
      .setCloseDate(parseDate("2015-01-03"))
      .setCleanCodeAttribute(CleanCodeAttribute.FOCUSED)
      .setResolution(RESOLUTION_FIXED)
      .setStatus(STATUS_CLOSED)
      .setSeverity(BLOCKER)
      .setAssigneeUuid("base assignee uuid")
      .setAssigneeLogin("base assignee login")
      .setAuthorLogin("base author")
      .setTags(newArrayList("base tag"))
      .setOnDisabledRule(true)
      .setSelectedAt(1000L)
      .setLine(10)
      .setMessage("message")
      .setGap(15d)
      .setEffort(Duration.create(15L))
      .setManualSeverity(false)
      .setLocations(issueLocations);

    when(debtCalculator.calculate(raw)).thenReturn(DEFAULT_DURATION);

    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn("release-2.x");
    analysisMetadataHolder.setBranch(branch);

    underTest.copyExistingOpenIssueFromBranch(raw, base, "master");

    assertThat(raw.isNew()).isFalse();
    assertThat(raw.isCopied()).isTrue();
    assertThat(raw.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.FOCUSED);
    assertThat(raw.key()).isNotNull();
    assertThat(raw.key()).isNotEqualTo(base.key());
    assertThat(raw.creationDate()).isEqualTo(base.creationDate());
    assertThat(raw.updateDate()).isEqualTo(base.updateDate());
    assertThat(raw.closeDate()).isEqualTo(base.closeDate());
    assertThat(raw.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(raw.status()).isEqualTo(STATUS_CLOSED);
    assertThat(raw.assignee()).isEqualTo("base assignee uuid");
    assertThat(raw.assigneeLogin()).isEqualTo("base assignee login");
    assertThat(raw.authorLogin()).isEqualTo("base author");
    assertThat(raw.tags()).containsOnly("base tag");
    assertThat(raw.effort()).isEqualTo(DEFAULT_DURATION);
    assertThat(raw.isOnDisabledRule()).isTrue();
    assertThat(raw.selectedAt()).isEqualTo(1000L);
    assertThat(raw.changes().get(0).get(IssueFieldsSetter.FROM_BRANCH).oldValue()).isEqualTo("master");
    assertThat(raw.changes().get(0).get(IssueFieldsSetter.FROM_BRANCH).newValue()).isEqualTo("release-2.x");
    assertThat(raw.getRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);

    verifyNoInteractions(updater);
  }

  @Test
  public void doAutomaticTransition() {
    DefaultIssue issue = new DefaultIssue();

    underTest.doAutomaticTransition(issue);

    verify(workflow).doAutomaticTransition(issue, issueChangeContext);
  }

  @Test
  public void doManualTransition() {
    DefaultIssue issue = new DefaultIssue();
    String transitionKey = "transitionKey";
    String userUuid = "userUuid";

    underTest.doManualTransition(issue, transitionKey, userUuid);

    verify(workflow).doManualTransition(issue, transitionKey, getIssueChangeContextWithUser(userUuid));
  }

  @Test
  public void addComment() {
    DefaultIssue issue = new DefaultIssue();
    String comment = "comment";
    String userUuid = "userUuid";

    underTest.addComment(issue, comment, userUuid);

    verify(updater).addComment(issue, comment, getIssueChangeContextWithUser(userUuid));
  }

  private IssueChangeContext getIssueChangeContextWithUser(String userUuid) {
    return IssueChangeContext.newBuilder()
      .setDate(issueChangeContext.date())
      .setWebhookSource(issueChangeContext.getWebhookSource())
      .setUserUuid(userUuid).build();
  }

  @Test
  public void mergeExistingOpenIssue() {
    DefaultIssue raw = new DefaultIssue()
      .setNew(true)
      .setKey("RAW_KEY")
      .setRuleKey(XOO_X1)
      .setRuleDescriptionContextKey("spring")
      .setCleanCodeAttribute(CleanCodeAttribute.IDENTIFIABLE)
      .setCodeVariants(Set.of("foo", "bar"))
      .addImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW)
      .addImpact(SoftwareQuality.RELIABILITY, Severity.HIGH)
      .setCreationDate(parseDate("2015-10-01"))
      .setUpdateDate(parseDate("2015-10-02"))
      .setCloseDate(parseDate("2015-10-03"));

    DbIssues.Locations issueLocations = DbIssues.Locations.newBuilder()
      .setTextRange(DbCommons.TextRange.newBuilder()
        .setStartLine(10)
        .setEndLine(12)
        .build())
      .build();

    DbIssues.MessageFormattings messageFormattings = DbIssues.MessageFormattings.newBuilder()
      .addMessageFormatting(DbIssues.MessageFormatting
        .newBuilder()
        .setStart(13)
        .setEnd(17)
        .setType(DbIssues.MessageFormattingType.CODE)
        .build())
      .build();

    DefaultIssue base = new DefaultIssue()
      .setKey("BASE_KEY")
      .setCreationDate(parseDate("2015-01-01"))
      .setUpdateDate(parseDate("2015-01-02"))
      .setCleanCodeAttribute(CleanCodeAttribute.FOCUSED)
      .setResolution(RESOLUTION_FALSE_POSITIVE)
      .setStatus(STATUS_RESOLVED)
      .setSeverity(BLOCKER)
      .setAssigneeUuid("base assignee uuid")
      .setAssigneeLogin("base assignee login")
      .setAuthorLogin("base author")
      .setTags(newArrayList("base tag"))
      .setOnDisabledRule(true)
      .setSelectedAt(1000L)
      .setLine(10)
      .setMessage("message with code")
      .setMessageFormattings(messageFormattings)
      .setGap(15d)
      .setRuleDescriptionContextKey("hibernate")
      .setCodeVariants(Set.of("donut"))
      .addImpact(SoftwareQuality.RELIABILITY, Severity.LOW, true)
      .setEffort(Duration.create(15L))
      .setManualSeverity(false)
      .setLocations(issueLocations)
      .addChange(new FieldDiffs().setDiff("foo", "bar", "donut"))
      .addChange(new FieldDiffs().setDiff("file", "A", "B"));

    when(debtCalculator.calculate(raw)).thenReturn(DEFAULT_DURATION);

    underTest.mergeExistingOpenIssue(raw, base);

    assertThat(raw.isNew()).isFalse();
    assertThat(raw.key()).isEqualTo("BASE_KEY");
    assertThat(raw.creationDate()).isEqualTo(base.creationDate());
    assertThat(raw.updateDate()).isEqualTo(base.updateDate());
    assertThat(raw.resolution()).isEqualTo(RESOLUTION_FALSE_POSITIVE);
    assertThat(raw.status()).isEqualTo(STATUS_RESOLVED);
    assertThat(raw.assignee()).isEqualTo("base assignee uuid");
    assertThat(raw.assigneeLogin()).isEqualTo("base assignee login");
    assertThat(raw.authorLogin()).isEqualTo("base author");
    assertThat(raw.tags()).containsOnly("base tag");
    assertThat(raw.codeVariants()).containsOnly("foo", "bar");
    assertThat(raw.effort()).isEqualTo(DEFAULT_DURATION);
    assertThat(raw.isOnDisabledRule()).isTrue();
    assertThat(raw.selectedAt()).isEqualTo(1000L);
    assertThat(raw.isChanged()).isFalse();
    assertThat(raw.changes()).hasSize(2);
    assertThat(raw.changes().get(0).diffs())
      .containsOnly(entry("foo", new FieldDiffs.Diff<>("bar", "donut")));
    assertThat(raw.changes().get(1).diffs())
      .containsOnly(entry("file", new FieldDiffs.Diff<>("A", "B")));
    verifyUpdater(raw, messageFormattings, issueLocations);
  }

  private void verifyUpdater(DefaultIssue raw, DbIssues.MessageFormattings messageFormattings, DbIssues.Locations issueLocations) {
    verify(updater).setPastSeverity(raw, BLOCKER, issueChangeContext);
    verify(updater).setPastLine(raw, 10);
    verify(updater).setRuleDescriptionContextKey(raw, "hibernate");
    verify(updater).setCodeVariants(raw, Set.of("donut"), issueChangeContext);
    verify(updater).setPastMessage(raw, "message with code", messageFormattings, issueChangeContext);
    verify(updater).setPastEffort(raw, Duration.create(15L), issueChangeContext);
    verify(updater).setPastLocations(raw, issueLocations);
    verify(updater).setCleanCodeAttribute(raw, CleanCodeAttribute.FOCUSED, issueChangeContext);
    verify(updater).setImpacts(raw, Set.of(new DefaultImpact(SoftwareQuality.RELIABILITY, Severity.LOW, true)), issueChangeContext);
  }

  @Test
  public void mergeExistingOpenIssue_with_manual_severity() {
    DefaultIssue raw = new DefaultIssue()
      .setNew(true)
      .setKey("RAW_KEY")
      .setRuleKey(XOO_X1);
    DefaultIssue base = new DefaultIssue()
      .setKey("BASE_KEY")
      .setResolution(RESOLUTION_FIXED)
      .setStatus(STATUS_CLOSED)
      .setSeverity(BLOCKER)
      .setManualSeverity(true);

    underTest.mergeExistingOpenIssue(raw, base);

    assertThat(raw.manualSeverity()).isTrue();
    assertThat(raw.severity()).isEqualTo(BLOCKER);

    verify(updater, never()).setPastSeverity(raw, BLOCKER, issueChangeContext);
  }

  @Test
  public void mergeExistingOpenIssue_with_base_changed() {
    DefaultIssue raw = new DefaultIssue()
      .setNew(true)
      .setKey("RAW_KEY")
      .setRuleKey(XOO_X1);
    DefaultIssue base = new DefaultIssue()
      .setChanged(true)
      .setKey("BASE_KEY")
      .setResolution(RESOLUTION_FALSE_POSITIVE)
      .setStatus(STATUS_RESOLVED);

    underTest.mergeExistingOpenIssue(raw, base);

    assertThat(raw.isChanged()).isTrue();
  }

  @Test
  public void mergeExistingOpenIssue_with_rule_description_context_key_added() {
    DefaultIssue raw = new DefaultIssue()
      .setNew(true)
      .setKey("RAW_KEY")
      .setRuleKey(XOO_X1)
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY);
    DefaultIssue base = new DefaultIssue()
      .setChanged(true)
      .setKey("RAW_KEY")
      .setResolution(RESOLUTION_FALSE_POSITIVE)
      .setStatus(STATUS_RESOLVED)
      .setRuleDescriptionContextKey(null);

    underTest.mergeExistingOpenIssue(raw, base);

    assertThat(raw.isChanged()).isTrue();
    assertThat(raw.getRuleDescriptionContextKey()).isEqualTo(raw.getRuleDescriptionContextKey());
  }

  @Test
  public void mergeExistingOpenIssue_with_rule_description_context_key_removed() {
    DefaultIssue raw = new DefaultIssue()
      .setNew(true)
      .setKey("RAW_KEY")
      .setRuleKey(XOO_X1)
      .setRuleDescriptionContextKey(null);
    DefaultIssue base = new DefaultIssue()
      .setChanged(true)
      .setKey("RAW_KEY")
      .setResolution(RESOLUTION_FALSE_POSITIVE)
      .setStatus(STATUS_RESOLVED)
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY);

    underTest.mergeExistingOpenIssue(raw, base);

    assertThat(raw.isChanged()).isTrue();
    assertThat(raw.getRuleDescriptionContextKey()).isEmpty();
  }
}
