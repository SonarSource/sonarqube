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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.i18n.I18n;
import org.sonar.server.issue.notification.FPOrWontFixNotification.FpOrWontFix;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.AnalysisChange;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Change;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Rule;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;
import org.sonar.test.html.HtmlFragmentAssert;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.issue.notification.FPOrWontFixNotification.FpOrWontFix.FP;
import static org.sonar.server.issue.notification.FPOrWontFixNotification.FpOrWontFix.WONT_FIX;

@RunWith(DataProviderRunner.class)
public class FpOrWontFixEmailTemplateTest {
  private I18n i18n = mock(I18n.class);
  private EmailSettings emailSettings = mock(EmailSettings.class);
  private FpOrWontFixEmailTemplate underTest = new FpOrWontFixEmailTemplate(i18n, emailSettings);

  @Test
  public void format_returns_null_on_Notification() {
    EmailMessage emailMessage = underTest.format(mock(Notification.class));

    assertThat(emailMessage).isNull();
  }

  @Test
  public void format_sets_message_id_specific_to_fp() {
    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(mock(Change.class), Collections.emptySet(), FP));

    assertThat(emailMessage.getMessageId()).isEqualTo("fp-issue-changes");
  }

  @Test
  public void format_sets_message_id_specific_to_wont_fix() {
    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(mock(Change.class), Collections.emptySet(), WONT_FIX));

    assertThat(emailMessage.getMessageId()).isEqualTo("wontfix-issue-changes");
  }

  @Test
  public void format_sets_subject_specific_to_fp() {
    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(mock(Change.class), Collections.emptySet(), FP));

    assertThat(emailMessage.getSubject()).isEqualTo("Issues marked as False Positive");
  }

  @Test
  public void format_sets_subject_specific_to_wont_fix() {
    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(mock(Change.class), Collections.emptySet(), WONT_FIX));

    assertThat(emailMessage.getSubject()).isEqualTo("Issues marked as Won't Fix");
  }

  @Test
  public void format_sets_from_to_name_of_author_change_when_available() {
    UserChange change = new UserChange(new Random().nextLong(), new User(randomAlphabetic(5), randomAlphabetic(6), randomAlphabetic(7)));
    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(change, Collections.emptySet(), WONT_FIX));

    assertThat(emailMessage.getFrom()).isEqualTo(change.getUser().getName().get());
  }

  @Test
  public void format_sets_from_to_login_of_author_change_when_name_is_not_available() {
    UserChange change = new UserChange(new Random().nextLong(), new User(randomAlphabetic(5), randomAlphabetic(6), null));
    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(change, Collections.emptySet(), WONT_FIX));

    assertThat(emailMessage.getFrom()).isEqualTo(change.getUser().getLogin());
  }

  @Test
  public void format_sets_from_to_null_when_analysisChange() {
    AnalysisChange change = new AnalysisChange(new Random().nextLong());
    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(change, Collections.emptySet(), WONT_FIX));

    assertThat(emailMessage.getFrom()).isNull();
  }

  @Test
  @UseDataProvider("userOrAnalysisChange")
  public void formats_returns_html_message_with_only_footer_and_header_when_no_issue_for_FPs(Change change) {
    formats_returns_html_message_with_only_footer_and_header_when_no_issue(change, FP, "False Positive");
  }

  @Test
  @UseDataProvider("userOrAnalysisChange")
  public void formats_returns_html_message_with_only_footer_and_header_when_no_issue_for_Wont_fixs(Change change) {
    formats_returns_html_message_with_only_footer_and_header_when_no_issue(change, WONT_FIX, "Won't Fix");
  }

  public void formats_returns_html_message_with_only_footer_and_header_when_no_issue(Change change, FpOrWontFix fpOrWontFix, String fpOrWontFixLabel) {
    String wordingNotification = randomAlphabetic(20);
    String host = randomAlphabetic(15);
    when(i18n.message(Locale.ENGLISH, "notification.dispatcher.NewFalsePositiveIssue", "notification.dispatcher.NewFalsePositiveIssue"))
      .thenReturn(wordingNotification);
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(change, Collections.emptySet(), fpOrWontFix));

    String footerText = "You received this email because you are subscribed to \"" + wordingNotification + "\" notifications from SonarQube."
      + " Click here to edit your email preferences.";
    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph("Hi,")
      .withoutLink()
      .hasParagraph("A manual change has resolved an issue as " + fpOrWontFixLabel + ":")
      .withoutLink()
      .hasEmptyParagraph()
      .hasParagraph(footerText)
      .withSmallOn(footerText)
      .withLink("here", host + "/account/notifications")
      .noMoreBlock();
  }

  @Test
  @UseDataProvider("fpOrWontFixValuesByUserOrAnalysisChange")
  public void formats_returns_html_message_for_single_issue_on_master(Change change, FpOrWontFix fpOrWontFix) {
    Project project = newProject("1");
    String ruleName = randomAlphabetic(8);
    String host = randomAlphabetic(15);
    ChangedIssue changedIssue = newChangedIssue("key", project, ruleName);
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(change, ImmutableSet.of(changedIssue), fpOrWontFix));

    HtmlFragmentAssert.assertThat(emailMessage.getMessage())
      .hasParagraph().hasParagraph() // skip header
      .hasParagraph(project.getProjectName())
      .hasList("Rule " + ruleName + " - See the single issue")
      .withLink("See the single issue", host + "/project/issues?id=" + project.getKey() + "&issues=" + changedIssue.getKey() + "&open=" + changedIssue.getKey())
      .hasParagraph().hasParagraph() // skip footer
      .noMoreBlock();
  }

  @Test
  @UseDataProvider("fpOrWontFixValuesByUserOrAnalysisChange")
  public void formats_returns_html_message_for_single_issue_on_branch(Change change, FpOrWontFix fpOrWontFix) {
    String branchName = randomAlphabetic(6);
    Project project = newBranch("1", branchName);
    String ruleName = randomAlphabetic(8);
    String host = randomAlphabetic(15);
    String key = "key";
    ChangedIssue changedIssue = newChangedIssue(key, project, ruleName);
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(change, ImmutableSet.of(changedIssue), fpOrWontFix));

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
  @UseDataProvider("fpOrWontFixValuesByUserOrAnalysisChange")
  public void formats_returns_html_message_for_multiple_issues_of_same_rule_on_same_project_on_master(Change change, FpOrWontFix fpOrWontFix) {
    Project project = newProject("1");
    String ruleName = randomAlphabetic(8);
    String host = randomAlphabetic(15);
    Rule rule = newRule(ruleName);
    List<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> newChangedIssue("issue_" + i, project, rule))
      .collect(toList());
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(change, ImmutableSet.copyOf(changedIssues), fpOrWontFix));

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
  @UseDataProvider("fpOrWontFixValuesByUserOrAnalysisChange")
  public void formats_returns_html_message_for_multiple_issues_of_same_rule_on_same_project_on_branch(Change change, FpOrWontFix fpOrWontFix) {
    String branchName = randomAlphabetic(19);
    Project project = newBranch("1", branchName);
    String ruleName = randomAlphabetic(8);
    String host = randomAlphabetic(15);
    Rule rule = newRule(ruleName);
    List<ChangedIssue> changedIssues = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(i -> newChangedIssue("issue_" + i, project, rule))
      .collect(toList());
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(change, ImmutableSet.copyOf(changedIssues), fpOrWontFix));

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
  @UseDataProvider("fpOrWontFixValuesByUserOrAnalysisChange")
  public void formats_returns_html_message_with_projects_ordered_by_name(Change change, FpOrWontFix fpOrWontFix) {
    Project project1 = newProject("1");
    Project project1Branch1 = newBranch("1", "a");
    Project project1Branch2 = newBranch("1", "b");
    Project project2 = newProject("B");
    Project project2Branch1 = newBranch("B", "a");
    Project project3 = newProject("C");
    String host = randomAlphabetic(15);
    List<ChangedIssue> changedIssues = Stream.of(project1, project1Branch1, project1Branch2, project2, project2Branch1, project3)
      .map(project -> newChangedIssue("issue_" + project.getUuid(), project, newRule(randomAlphabetic(2))))
      .collect(toList());
    Collections.shuffle(changedIssues);
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(change, ImmutableSet.copyOf(changedIssues), fpOrWontFix));

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
  @UseDataProvider("fpOrWontFixValuesByUserOrAnalysisChange")
  public void formats_returns_html_message_with_rules_ordered_by_name(Change change, FpOrWontFix fpOrWontFix) {
    Project project = newProject("1");
    Rule rule1 = newRule("1");
    Rule rule2 = newRule("a");
    Rule rule3 = newRule("b");
    Rule rule4 = newRule("X");
    String host = randomAlphabetic(15);
    List<ChangedIssue> changedIssues = Stream.of(rule1, rule2, rule3, rule4)
      .map(rule -> newChangedIssue("issue_" + rule.getName(), project, rule))
      .collect(toList());
    Collections.shuffle(changedIssues);
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(change, ImmutableSet.copyOf(changedIssues), fpOrWontFix));

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
  @UseDataProvider("fpOrWontFixValuesByUserOrAnalysisChange")
  public void formats_returns_html_message_with_multiple_links_by_rule_of_groups_of_up_to_40_issues(Change change, FpOrWontFix fpOrWontFix) {
    Project project1 = newProject("1");
    Project project2 = newProject("V");
    Project project2Branch = newBranch("V", "AB");
    Rule rule1 = newRule("1");
    Rule rule2 = newRule("a");
    String host = randomAlphabetic(15);
    List<ChangedIssue> changedIssues = Stream.of(
      IntStream.range(0, 39).mapToObj(i -> newChangedIssue("39_" + i, project1, rule1)),
      IntStream.range(0, 40).mapToObj(i -> newChangedIssue("40_" + i, project1, rule2)),
      IntStream.range(0, 81).mapToObj(i -> newChangedIssue("1-40_41-80_1_" + i, project2, rule2)),
      IntStream.range(0, 6).mapToObj(i -> newChangedIssue("6_" + i, project2Branch, rule1)))
      .flatMap(t -> t)
      .collect(toList());
    Collections.shuffle(changedIssues);
    when(emailSettings.getServerBaseURL()).thenReturn(host);

    EmailMessage emailMessage = underTest.format(new FPOrWontFixNotification(change, ImmutableSet.copyOf(changedIssues), fpOrWontFix));

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

  @DataProvider
  public static Object[][] userOrAnalysisChange() {
    AnalysisChange analysisChange = new AnalysisChange(new Random().nextLong());
    UserChange userChange = new UserChange(new Random().nextLong(), new User(randomAlphabetic(5), randomAlphabetic(6),
      new Random().nextBoolean() ? null : randomAlphabetic(7)));
    return new Object[][] {
      {analysisChange},
      {userChange}
    };
  }

  @DataProvider
  public static Object[][] fpOrWontFixValuesByUserOrAnalysisChange() {
    AnalysisChange analysisChange = new AnalysisChange(new Random().nextLong());
    UserChange userChange = new UserChange(new Random().nextLong(), new User(randomAlphabetic(5), randomAlphabetic(6),
      new Random().nextBoolean() ? null : randomAlphabetic(7)));
    return new Object[][] {
      {analysisChange, FP},
      {analysisChange, WONT_FIX},
      {userChange, FP},
      {userChange, WONT_FIX}
    };
  }

  private static ChangedIssue newChangedIssue(String key, Project project, String ruleName) {
    return newChangedIssue(key, project, newRule(ruleName));
  }

  private static ChangedIssue newChangedIssue(String key, Project project, Rule rule) {
    return new ChangedIssue.Builder(key)
      .setNewStatus(randomAlphabetic(19))
      .setProject(project)
      .setRule(rule)
      .build();
  }

  private static Rule newRule(String ruleName) {
    return new Rule(RuleKey.of(randomAlphabetic(6), randomAlphabetic(7)), ruleName);
  }

  private static Project newProject(String uuid) {
    return new Project.Builder(uuid).setProjectName(uuid + "_name").setKey(uuid + "_key").build();
  }

  private static Project newBranch(String uuid, String branchName) {
    return new Project.Builder(uuid).setProjectName(uuid + "_name").setKey(uuid + "_key").setBranchName(branchName).build();
  }
}
