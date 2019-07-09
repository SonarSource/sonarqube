/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.issue.notification;

import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.core.i18n.I18n;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.AnalysisChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Change;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Rule;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;
import org.sonar.test.html.HtmlFragmentAssert;
import org.sonar.test.html.HtmlListAssert;
import org.sonar.test.html.HtmlParagraphAssert;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.newBranch;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.newChangedIssue;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.newProject;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.newRule;

@RunWith(DataProviderRunner.class)
public class ChangesOnMyIssuesEmailTemplateTest {
  private static final String[] ISSUE_STATUSES = {STATUS_OPEN, STATUS_RESOLVED, STATUS_CONFIRMED, STATUS_REOPENED, STATUS_CLOSED};
  @org.junit.Rule
  public ExpectedException expectedException = ExpectedException.none();

  private I18n i18n = mock(I18n.class);
  private EmailSettings emailSettings = mock(EmailSettings.class);
  private ChangesOnMyIssuesEmailTemplate underTest = new ChangesOnMyIssuesEmailTemplate(i18n, emailSettings);

  @Test
  public void format_returns_null_on_Notification() {
    EmailMessage emailMessage = underTest.format(mock(Notification.class));

    assertThat(emailMessage).isNull();
  }

  @Test
  public void formats_fails_with_ISE_if_change_from_Analysis_and_no_issue() {
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("changedIssues can't be empty");

    underTest.format(new ChangesOnMyIssuesNotification(analysisChange, Collections.emptySet()));
  }

  @Test
  public void format_sets_message_id_with_project_key_of_first_issue_in_set_when_change_from_Analysis() {
    Set<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(4))
      .mapToObj(i -> newChangedIssue(i + "", randomValidStatus(), newProject("prj_" + i), newRule("rule_" + i)))
      .collect(toSet());
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, changedIssues));

    assertThat(emailMessage.getMessageId()).isEqualTo("changes-on-my-issues/" + changedIssues.iterator().next().getProject().getKey());
  }

  @Test
  public void format_sets_subject_with_project_name_of_first_issue_in_set_when_change_from_Analysis() {
    Set<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(4))
      .mapToObj(i -> newChangedIssue(i + "", randomValidStatus(), newProject("prj_" + i), newRule("rule_" + i)))
      .collect(toSet());
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, changedIssues));

    Project project = changedIssues.iterator().next().getProject();
    assertThat(emailMessage.getSubject()).isEqualTo("Analysis has changed some of your issues in " + project.getProjectName());
  }

  @Test
  public void format_sets_subject_with_project_name_and_branch_name_of_first_issue_in_set_when_change_from_Analysis() {
    Set<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(4))
      .mapToObj(i -> newChangedIssue(i + "", randomValidStatus(), newBranch("prj_" + i, "br_" + i), newRule("rule_" + i)))
      .collect(toSet());
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, changedIssues));

    Project project = changedIssues.iterator().next().getProject();
    assertThat(emailMessage.getSubject()).isEqualTo("Analysis has changed some of your issues in " + project.getProjectName() + ", " + project.getBranchName().get());
  }

  @Test
  public void format_set_html_message_with_header_dealing_with_plural_when_change_from_Analysis() {
    Set<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(4))
      .mapToObj(i -> newChangedIssue(i + "", randomValidStatus(), newProject("prj_" + i), newRule("rule_" + i)))
      .collect(toSet());
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();

    EmailMessage singleIssueMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, changedIssues.stream().limit(1).collect(toSet())));
    EmailMessage multiIssueMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, changedIssues));

    HtmlFragmentAssert.assertThat(singleIssueMessage.getMessage())
      .hasParagraph("Hi,")
      .hasParagraph("An analysis has updated an issue assigned to you:");
    HtmlFragmentAssert.assertThat(multiIssueMessage.getMessage())
      .hasParagraph("Hi,")
      .hasParagraph("An analysis has updated issues assigned to you:");
  }

  @Test
  public void format_sets_static_message_id_when_change_from_User() {
    Set<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(4))
      .mapToObj(i -> newChangedIssue(i + "", randomValidStatus(), newProject("prj_" + i), newRule("rule_" + i)))
      .collect(toSet());
    UserChange userChange = IssuesChangesNotificationBuilderTesting.newUserChange();

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(userChange, changedIssues));

    assertThat(emailMessage.getMessageId()).isEqualTo("changes-on-my-issues");
  }

  @Test
  public void format_sets_static_subject_when_change_from_User() {
    Set<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(4))
      .mapToObj(i -> newChangedIssue(i + "", randomValidStatus(), newProject("prj_" + i), newRule("rule_" + i)))
      .collect(toSet());
    UserChange userChange = IssuesChangesNotificationBuilderTesting.newUserChange();

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(userChange, changedIssues));

    assertThat(emailMessage.getSubject()).isEqualTo("A manual update has changed some of your issues");
  }

  @Test
  public void format_set_html_message_with_header_dealing_with_plural_when_change_from_User() {
    Set<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(4))
      .mapToObj(i -> newChangedIssue(i + "", randomValidStatus(), newProject("prj_" + i), newRule("rule_" + i)))
      .collect(toSet());
    UserChange userChange = IssuesChangesNotificationBuilderTesting.newUserChange();

    EmailMessage singleIssueMessage = underTest.format(new ChangesOnMyIssuesNotification(
      userChange, changedIssues.stream().limit(1).collect(toSet())));
    EmailMessage multiIssueMessage = underTest.format(new ChangesOnMyIssuesNotification(userChange, changedIssues));

    HtmlFragmentAssert.assertThat(singleIssueMessage.getMessage())
      .hasParagraph("Hi,")
      .withoutLink()
      .hasParagraph("A manual change has updated an issue assigned to you:")
      .withoutLink();
    HtmlFragmentAssert.assertThat(multiIssueMessage.getMessage())
      .hasParagraph("Hi,")
      .withoutLink()
      .hasParagraph("A manual change has updated issues assigned to you:")
      .withoutLink();
  }

  @Test
  @UseDataProvider("issueStatuses")
  public void format_set_html_message_with_footer_when_change_from_user(String issueStatus) {
    UserChange userChange = IssuesChangesNotificationBuilderTesting.newUserChange();
    format_set_html_message_with_footer(userChange, issueStatus, c -> c
      // skip content
      .hasParagraph() // open/closed issue
      .hasList() // rule list
    );
  }

  @Test
  @UseDataProvider("issueStatuses")
  public void format_set_html_message_with_footer_when_change_from_analysis(String issueStatus) {
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();
    format_set_html_message_with_footer(analysisChange, issueStatus, c -> c
      // skip content
      .hasParagraph() // status
      .hasList() // rule list
    );
  }

  @DataProvider
  public static Object[][] issueStatuses() {
    return Arrays.stream(ISSUE_STATUSES)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  private void format_set_html_message_with_footer(Change change, String issueStatus, Function<HtmlParagraphAssert, HtmlListAssert> skipContent) {
    String wordingNotification = randomAlphabetic(20);
    String host = randomAlphabetic(15);
    when(i18n.message(Locale.ENGLISH, "notification.dispatcher.ChangesOnMyIssue", "notification.dispatcher.ChangesOnMyIssue"))
      .thenReturn(wordingNotification);
    when(emailSettings.getServerBaseURL()).thenReturn(host);
    Project project = newProject("foo");
    Rule rule = newRule("bar");
    Set<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(4))
      .mapToObj(i -> newChangedIssue(i + "", issueStatus, project, rule))
      .collect(toSet());

    EmailMessage singleIssueMessage = underTest.format(new ChangesOnMyIssuesNotification(
      change, changedIssues.stream().limit(1).collect(toSet())));
    EmailMessage multiIssueMessage = underTest.format(new ChangesOnMyIssuesNotification(change, changedIssues));

    Stream.of(singleIssueMessage, multiIssueMessage)
      .forEach(issueMessage -> {
        HtmlParagraphAssert htmlAssert = HtmlFragmentAssert.assertThat(issueMessage.getMessage())
          .hasParagraph().hasParagraph(); // skip header
        // skip content
        HtmlListAssert htmlListAssert = skipContent.apply(htmlAssert);

        String footerText = "You received this email because you are subscribed to \"" + wordingNotification + "\" notifications from SonarQube."
          + " Click here to edit your email preferences.";
        htmlListAssert.hasEmptyParagraph()
          .hasParagraph(footerText)
          .withSmallOn(footerText)
          .withLink("here", host + "/account/notifications")
          .noMoreBlock();
      });
  }

  @Test
  public void format_set_html_message_with_issues_grouped_by_status_closed_or_any_other_when_change_from_analysis() {
    Project project = newProject("foo");
    Rule rule = newRule("bar");
    Set<ChangedIssue> changedIssues = Arrays.stream(ISSUE_STATUSES)
      .map(status -> newChangedIssue(status + "", status, project, rule))
      .collect(toSet());
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, changedIssues));

    HtmlListAssert htmlListAssert = HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph("Closed issue:")
      .withoutLink()
      .hasList("Rule " + rule.getName() + " - See the single issue")
      .withLinkOn("See the single issue")
      .hasParagraph("Open issues:")
      .withoutLink()
      .hasList("Rule " + rule.getName() + " - See all " + (ISSUE_STATUSES.length - 1) + " issues")
      .withLinkOn("See all " + (ISSUE_STATUSES.length - 1) + " issues");
    verifyEnd(htmlListAssert);
  }

  @Test
  public void format_set_html_message_with_status_title_handles_plural_when_change_from_analysis() {
    Project project = newProject("foo");
    Rule rule = newRule("bar");
    Set<ChangedIssue> closedIssues = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(status -> newChangedIssue(status + "", STATUS_CLOSED, project, rule))
      .collect(toSet());
    Set<ChangedIssue> openIssues = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(status -> newChangedIssue(status + "", STATUS_OPEN, project, rule))
      .collect(toSet());
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();

    EmailMessage closedIssuesMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, closedIssues));
    EmailMessage openIssuesMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, openIssues));

    HtmlListAssert htmlListAssert = HtmlFragmentAssert.assertThat(closedIssuesMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph("Closed issues:")
      .hasList();
    verifyEnd(htmlListAssert);
    htmlListAssert = HtmlFragmentAssert.assertThat(openIssuesMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph("Open issues:")
      .hasList();
    verifyEnd(htmlListAssert);
  }

  @Test
  public void formats_returns_html_message_for_single_issue_on_master_when_analysis_change() {
    Project project = newProject("1");
    String ruleName = randomAlphabetic(8);
    String host = randomAlphabetic(15);
    ChangedIssue changedIssue = newChangedIssue("key", randomValidStatus(), project, ruleName);
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, ImmutableSet.of(changedIssue)));

    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph()// skip title based on status
      .hasList("Rule " + ruleName + " - See the single issue")
      .withLink("See the single issue", host + "/project/issues?id=" + project.getKey() + "&issues=" + changedIssue.getKey() + "&open=" + changedIssue.getKey())
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  public void formats_returns_html_message_for_single_issue_on_master_when_user_change() {
    Project project = newProject("1");
    String ruleName = randomAlphabetic(8);
    String host = randomAlphabetic(15);
    ChangedIssue changedIssue = newChangedIssue("key", randomValidStatus(), project, ruleName);
    UserChange userChange = IssuesChangesNotificationBuilderTesting.newUserChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(userChange, ImmutableSet.of(changedIssue)));

    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph(project.getProjectName())
      .hasList("Rule " + ruleName + " - See the single issue")
      .withLink("See the single issue", host + "/project/issues?id=" + project.getKey() + "&issues=" + changedIssue.getKey() + "&open=" + changedIssue.getKey())
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  public void formats_returns_html_message_for_single_issue_on_branch_when_analysis_change() {
    String branchName = randomAlphabetic(6);
    Project project = newBranch("1", branchName);
    String ruleName = randomAlphabetic(8);
    String host = randomAlphabetic(15);
    String key = "key";
    ChangedIssue changedIssue = newChangedIssue(key, randomValidStatus(), project, ruleName);
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, ImmutableSet.of(changedIssue)));

    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph()// skip title based on status
      .hasList("Rule " + ruleName + " - See the single issue")
      .withLink("See the single issue",
        host + "/project/issues?id=" + project.getKey() + "&branch=" + branchName + "&issues=" + changedIssue.getKey() + "&open=" + changedIssue.getKey())
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  public void formats_returns_html_message_for_single_issue_on_branch_when_user_change() {
    String branchName = randomAlphabetic(6);
    Project project = newBranch("1", branchName);
    String ruleName = randomAlphabetic(8);
    String host = randomAlphabetic(15);
    String key = "key";
    ChangedIssue changedIssue = newChangedIssue(key, randomValidStatus(), project, ruleName);
    UserChange userChange = IssuesChangesNotificationBuilderTesting.newUserChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(userChange, ImmutableSet.of(changedIssue)));

    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph(project.getProjectName() + ", " + branchName)
      .hasList("Rule " + ruleName + " - See the single issue")
      .withLink("See the single issue",
        host + "/project/issues?id=" + project.getKey() + "&branch=" + branchName + "&issues=" + changedIssue.getKey() + "&open=" + changedIssue.getKey())
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  public void formats_returns_html_message_for_multiple_issues_of_same_rule_on_same_project_on_master_when_analysis_change() {
    Project project = newProject("1");
    String ruleName = randomAlphabetic(8);
    String host = randomAlphabetic(15);
    Rule rule = newRule(ruleName);
    String issueStatus = randomValidStatus();
    List<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> newChangedIssue("issue_" + i, issueStatus, project, rule))
      .collect(toList());
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, ImmutableSet.copyOf(changedIssues)));

    String expectedHref = host + "/project/issues?id=" + project.getKey()
      + "&issues=" + changedIssues.stream().map(ChangedIssue::getKey).collect(joining("%2C"));
    String expectedLinkText = "See all " + changedIssues.size() + " issues";
    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph() // skip title based on status
      .hasList("Rule " + ruleName + " - " + expectedLinkText)
      .withLink(expectedLinkText, expectedHref)
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  public void formats_returns_html_message_for_multiple_issues_of_same_rule_on_same_project_on_master_when_user_change() {
    Project project = newProject("1");
    String ruleName = randomAlphabetic(8);
    String host = randomAlphabetic(15);
    Rule rule = newRule(ruleName);
    List<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> newChangedIssue("issue_" + i, randomValidStatus(), project, rule))
      .collect(toList());
    UserChange userChange = IssuesChangesNotificationBuilderTesting.newUserChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(userChange, ImmutableSet.copyOf(changedIssues)));

    String expectedHref = host + "/project/issues?id=" + project.getKey()
      + "&issues=" + changedIssues.stream().map(ChangedIssue::getKey).collect(joining("%2C"));
    String expectedLinkText = "See all " + changedIssues.size() + " issues";
    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph(project.getProjectName())
      .hasList("Rule " + ruleName + " - " + expectedLinkText)
      .withLink(expectedLinkText, expectedHref)
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  public void formats_returns_html_message_for_multiple_issues_of_same_rule_on_same_project_on_branch_when_analysis_change() {
    String branchName = randomAlphabetic(19);
    Project project = newBranch("1", branchName);
    String ruleName = randomAlphabetic(8);
    String host = randomAlphabetic(15);
    Rule rule = newRule(ruleName);
    String status = randomValidStatus();
    List<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> newChangedIssue("issue_" + i, status, project, rule))
      .collect(toList());
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, ImmutableSet.copyOf(changedIssues)));

    String expectedHref = host + "/project/issues?id=" + project.getKey() + "&branch=" + branchName
      + "&issues=" + changedIssues.stream().map(ChangedIssue::getKey).collect(joining("%2C"));
    String expectedLinkText = "See all " + changedIssues.size() + " issues";
    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph()// skip title based on status
      .hasList("Rule " + ruleName + " - " + expectedLinkText)
      .withLink(expectedLinkText, expectedHref)
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  public void formats_returns_html_message_for_multiple_issues_of_same_rule_on_same_project_on_branch_when_user_change() {
    String branchName = randomAlphabetic(19);
    Project project = newBranch("1", branchName);
    String ruleName = randomAlphabetic(8);
    String host = randomAlphabetic(15);
    Rule rule = newRule(ruleName);
    List<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> newChangedIssue("issue_" + i, randomValidStatus(), project, rule))
      .collect(toList());
    UserChange userChange = IssuesChangesNotificationBuilderTesting.newUserChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(userChange, ImmutableSet.copyOf(changedIssues)));

    String expectedHref = host + "/project/issues?id=" + project.getKey() + "&branch=" + branchName
      + "&issues=" + changedIssues.stream().map(ChangedIssue::getKey).collect(joining("%2C"));
    String expectedLinkText = "See all " + changedIssues.size() + " issues";
    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph(project.getProjectName() + ", " + branchName)
      .hasList("Rule " + ruleName + " - " + expectedLinkText)
      .withLink(expectedLinkText, expectedHref)
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  public void formats_returns_html_message_with_projects_ordered_by_name_when_user_change() {
    Project project1 = newProject("1");
    Project project1Branch1 = newBranch("1", "a");
    Project project1Branch2 = newBranch("1", "b");
    Project project2 = newProject("B");
    Project project2Branch1 = newBranch("B", "a");
    Project project3 = newProject("C");
    String host = randomAlphabetic(15);
    List<ChangedIssue> changedIssues = Stream.of(project1, project1Branch1, project1Branch2, project2, project2Branch1, project3)
      .map(project -> newChangedIssue("issue_" + project.getUuid(), randomValidStatus(), project, newRule(randomAlphabetic(2))))
      .collect(toList());
    Collections.shuffle(changedIssues);
    UserChange userChange = IssuesChangesNotificationBuilderTesting.newUserChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(userChange, ImmutableSet.copyOf(changedIssues)));

    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph(project1.getProjectName())
      .hasList()
      .hasParagraph(project1Branch1.getProjectName() + ", " + project1Branch1.getBranchName().get())
      .hasList()
      .hasParagraph(project1Branch2.getProjectName() + ", " + project1Branch2.getBranchName().get())
      .hasList()
      .hasParagraph(project2.getProjectName())
      .hasList()
      .hasParagraph(project2Branch1.getProjectName() + ", " + project2Branch1.getBranchName().get())
      .hasList()
      .hasParagraph(project3.getProjectName())
      .hasList()
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  public void formats_returns_html_message_with_rules_ordered_by_name_when_analysis_change() {
    Project project = newProject("1");
    Rule rule1 = newRule("1");
    Rule rule2 = newRule("a");
    Rule rule3 = newRule("b");
    Rule rule4 = newRule("X");
    String host = randomAlphabetic(15);
    String issueStatus = randomValidStatus();
    List<ChangedIssue> changedIssues = Stream.of(rule1, rule2, rule3, rule4)
      .map(rule -> newChangedIssue("issue_" + rule.getName(), issueStatus, project, rule))
      .collect(toList());
    Collections.shuffle(changedIssues);
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, ImmutableSet.copyOf(changedIssues)));

    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph()// skip title based on status
      .hasList(
        "Rule " + rule1.getName() + " - See the single issue",
        "Rule " + rule2.getName() + " - See the single issue",
        "Rule " + rule3.getName() + " - See the single issue",
        "Rule " + rule4.getName() + " - See the single issue")
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  public void formats_returns_html_message_with_rules_ordered_by_name_when_analysis_change_when_user_analysis() {
    Project project = newProject("1");
    Rule rule1 = newRule("1");
    Rule rule2 = newRule("a");
    Rule rule3 = newRule("b");
    Rule rule4 = newRule("X");
    String host = randomAlphabetic(15);
    List<ChangedIssue> changedIssues = Stream.of(rule1, rule2, rule3, rule4)
      .map(rule -> newChangedIssue("issue_" + rule.getName(), randomValidStatus(), project, rule))
      .collect(toList());
    Collections.shuffle(changedIssues);
    UserChange userChange = IssuesChangesNotificationBuilderTesting.newUserChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(userChange, ImmutableSet.copyOf(changedIssues)));

    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph(project.getProjectName())
      .hasList(
        "Rule " + rule1.getName() + " - See the single issue",
        "Rule " + rule2.getName() + " - See the single issue",
        "Rule " + rule3.getName() + " - See the single issue",
        "Rule " + rule4.getName() + " - See the single issue")
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  public void formats_returns_html_message_with_multiple_links_by_rule_of_groups_of_up_to_40_issues_when_analysis_change() {
    Project project1 = newProject("1");
    Rule rule1 = newRule("1");
    Rule rule2 = newRule("a");
    String host = randomAlphabetic(15);
    String issueStatusClosed = STATUS_CLOSED;
    String otherIssueStatus = STATUS_RESOLVED;
    List<ChangedIssue> changedIssues = Stream.of(
      IntStream.range(0, 39).mapToObj(i -> newChangedIssue("39_" + i, issueStatusClosed, project1, rule1)),
      IntStream.range(0, 40).mapToObj(i -> newChangedIssue("40_" + i, issueStatusClosed, project1, rule2)),
      IntStream.range(0, 81).mapToObj(i -> newChangedIssue("1-40_41-80_1_" + i, otherIssueStatus, project1, rule2)),
      IntStream.range(0, 6).mapToObj(i -> newChangedIssue("6_" + i, otherIssueStatus, project1, rule1)))
      .flatMap(t -> t)
      .collect(toList());
    Collections.shuffle(changedIssues);
    AnalysisChange analysisChange = IssuesChangesNotificationBuilderTesting.newAnalysisChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(analysisChange, ImmutableSet.copyOf(changedIssues)));

    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph("Closed issues:") // skip title based on status
      .hasList(
        "Rule " + rule1.getName() + " - See all 39 issues",
        "Rule " + rule2.getName() + " - See all 40 issues")
      .withLink("See all 39 issues",
        host + "/project/issues?id=" + project1.getKey()
          + "&issues=" + IntStream.range(0, 39).mapToObj(i -> "39_" + i).sorted().collect(joining("%2C")))
      .withLink("See all 40 issues",
        host + "/project/issues?id=" + project1.getKey()
          + "&issues=" + IntStream.range(0, 40).mapToObj(i -> "40_" + i).sorted().collect(joining("%2C")))
      .hasParagraph("Open issues:")
      .hasList(
        "Rule " + rule2.getName() + " - See issues 1-40 41-80 81",
        "Rule " + rule1.getName() + " - See all 6 issues")
      .withLink("1-40",
        host + "/project/issues?id=" + project1.getKey()
          + "&issues=" + IntStream.range(0, 81).mapToObj(i -> "1-40_41-80_1_" + i).sorted().limit(40).collect(joining("%2C")))
      .withLink("41-80",
        host + "/project/issues?id=" + project1.getKey()
          + "&issues=" + IntStream.range(0, 81).mapToObj(i -> "1-40_41-80_1_" + i).sorted().skip(40).limit(40).collect(joining("%2C")))
      .withLink("81",
        host + "/project/issues?id=" + project1.getKey()
          + "&issues=" + "1-40_41-80_1_9" + "&open=" + "1-40_41-80_1_9")
      .withLink("See all 6 issues",
        host + "/project/issues?id=" + project1.getKey()
          + "&issues=" + IntStream.range(0, 6).mapToObj(i -> "6_" + i).sorted().collect(joining("%2C")))
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  public void formats_returns_html_message_with_multiple_links_by_rule_of_groups_of_up_to_40_issues_when_user_change() {
    Project project1 = newProject("1");
    Project project2 = newProject("V");
    Project project2Branch = newBranch("V", "AB");
    Rule rule1 = newRule("1");
    Rule rule2 = newRule("a");
    String status = randomValidStatus();
    String host = randomAlphabetic(15);
    List<ChangedIssue> changedIssues = Stream.of(
      IntStream.range(0, 39).mapToObj(i -> newChangedIssue("39_" + i, status, project1, rule1)),
      IntStream.range(0, 40).mapToObj(i -> newChangedIssue("40_" + i, status, project1, rule2)),
      IntStream.range(0, 81).mapToObj(i -> newChangedIssue("1-40_41-80_1_" + i, status, project2, rule2)),
      IntStream.range(0, 6).mapToObj(i -> newChangedIssue("6_" + i, status, project2Branch, rule1)))
      .flatMap(t -> t)
      .collect(toList());
    Collections.shuffle(changedIssues);
    UserChange userChange = IssuesChangesNotificationBuilderTesting.newUserChange();
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new ChangesOnMyIssuesNotification(userChange, ImmutableSet.copyOf(changedIssues)));

    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph(project1.getProjectName())
      .hasList()
      .withItemTexts(
        "Rule " + rule1.getName() + " - See all 39 issues",
        "Rule " + rule2.getName() + " - See all 40 issues")
      .withLink("See all 39 issues",
        host + "/project/issues?id=" + project1.getKey()
          + "&issues=" + IntStream.range(0, 39).mapToObj(i -> "39_" + i).sorted().collect(joining("%2C")))
      .withLink("See all 40 issues",
        host + "/project/issues?id=" + project1.getKey()
          + "&issues=" + IntStream.range(0, 40).mapToObj(i -> "40_" + i).sorted().collect(joining("%2C")))
      .hasParagraph(project2.getProjectName())
      .hasList("Rule " + rule2.getName() + " - See issues 1-40 41-80 81")
      .withLink("1-40",
        host + "/project/issues?id=" + project2.getKey()
          + "&issues=" + IntStream.range(0, 81).mapToObj(i -> "1-40_41-80_1_" + i).sorted().limit(40).collect(joining("%2C")))
      .withLink("41-80",
        host + "/project/issues?id=" + project2.getKey()
          + "&issues=" + IntStream.range(0, 81).mapToObj(i -> "1-40_41-80_1_" + i).sorted().skip(40).limit(40).collect(joining("%2C")))
      .withLink("81",
        host + "/project/issues?id=" + project2.getKey()
          + "&issues=" + "1-40_41-80_1_9" + "&open=" + "1-40_41-80_1_9")
      .hasParagraph(project2Branch.getProjectName() + ", " + project2Branch.getBranchName().get())
      .hasList("Rule " + rule1.getName() + " - See all 6 issues")
      .withLink("See all 6 issues",
        host + "/project/issues?id=" + project2Branch.getKey() + "&branch=" + project2Branch.getBranchName().get()
          + "&issues=" + IntStream.range(0, 6).mapToObj(i -> "6_" + i).sorted().collect(joining("%2C")))
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  private static String randomValidStatus() {
    return ISSUE_STATUSES[new Random().nextInt(ISSUE_STATUSES.length)];
  }

  private void verifyEnd(HtmlListAssert htmlListAssert) {
    htmlListAssert
      .hasEmptyParagraph()
      .hasParagraph()
      .noMoreBlock();
  }

}
