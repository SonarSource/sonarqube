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
package org.sonar.scanner.issue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.rule.internal.RulesBuilder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultExternalIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssuePublisherTest {

  static final RuleKey SQUID_RULE_KEY = RuleKey.of("squid", "AvoidCycle");
  static final String SQUID_RULE_NAME = "Avoid Cycle";
  private static final RuleKey NOSONAR_RULE_KEY = RuleKey.of("squid", "NoSonarCheck");

  private DefaultInputProject project;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Mock
  IssueFilters filters;

  ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
  RulesBuilder ruleBuilder = new RulesBuilder();

  IssuePublisher moduleIssues;

  DefaultInputFile file = new TestInputFileBuilder("foo", "src/Foo.php").initMetadata("Foo\nBar\nBiz\n").build();
  ReportPublisher reportPublisher = mock(ReportPublisher.class, RETURNS_DEEP_STUBS);

  @Before
  public void prepare() throws IOException {
    project = new DefaultInputProject(ProjectDefinition.create()
      .setKey("foo")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder()));
  }

  @Test
  public void ignore_null_active_rule() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    initModuleIssues();
    DefaultIssue issue = new DefaultIssue(project)
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message("Foo"))
      .forRule(SQUID_RULE_KEY);
    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(reportPublisher);
  }

  @Test
  public void ignore_null_rule_of_active_rule() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    activeRulesBuilder.addRule(new NewActiveRule.Builder().setRuleKey(SQUID_RULE_KEY).setQProfileKey("qp-1").build());
    initModuleIssues();

    DefaultIssue issue = new DefaultIssue(project)
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message("Foo"))
      .forRule(SQUID_RULE_KEY);
    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(reportPublisher);
  }

  @Test
  public void add_issue_to_cache() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    activeRulesBuilder.addRule(new NewActiveRule.Builder()
      .setRuleKey(SQUID_RULE_KEY)
      .setSeverity(Severity.INFO)
      .setQProfileKey("qp-1")
      .build());
    initModuleIssues();

    DefaultIssue issue = new DefaultIssue(project)
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message("Foo"))
      .forRule(SQUID_RULE_KEY)
      .overrideSeverity(org.sonar.api.batch.rule.Severity.CRITICAL);

    when(filters.accept(any(InputComponent.class), any(ScannerReport.Issue.class))).thenReturn(true);

    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isTrue();
    ArgumentCaptor<ScannerReport.Issue> argument = ArgumentCaptor.forClass(ScannerReport.Issue.class);
    verify(reportPublisher.getWriter()).appendComponentIssue(eq(file.scannerId()), argument.capture());
    assertThat(argument.getValue().getSeverity()).isEqualTo(org.sonar.scanner.protocol.Constants.Severity.CRITICAL);
  }

  @Test
  public void add_external_issue_to_cache() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    initModuleIssues();

    DefaultExternalIssue issue = new DefaultExternalIssue(project)
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message("Foo"))
      .type(RuleType.BUG)
      .forRule(SQUID_RULE_KEY)
      .severity(org.sonar.api.batch.rule.Severity.CRITICAL);

    moduleIssues.initAndAddExternalIssue(issue);

    ArgumentCaptor<ScannerReport.ExternalIssue> argument = ArgumentCaptor.forClass(ScannerReport.ExternalIssue.class);
    verify(reportPublisher.getWriter()).appendComponentExternalIssue(eq(file.scannerId()), argument.capture());
    assertThat(argument.getValue().getSeverity()).isEqualTo(org.sonar.scanner.protocol.Constants.Severity.CRITICAL);
  }

  @Test
  public void use_severity_from_active_rule_if_no_severity_on_issue() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    activeRulesBuilder.addRule(new NewActiveRule.Builder()
      .setRuleKey(SQUID_RULE_KEY)
      .setSeverity(Severity.INFO)
      .setQProfileKey("qp-1")
      .build());
    initModuleIssues();

    DefaultIssue issue = new DefaultIssue(project)
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message("Foo"))
      .forRule(SQUID_RULE_KEY);
    when(filters.accept(any(InputComponent.class), any(ScannerReport.Issue.class))).thenReturn(true);
    moduleIssues.initAndAddIssue(issue);

    ArgumentCaptor<ScannerReport.Issue> argument = ArgumentCaptor.forClass(ScannerReport.Issue.class);
    verify(reportPublisher.getWriter()).appendComponentIssue(eq(file.scannerId()), argument.capture());
    assertThat(argument.getValue().getSeverity()).isEqualTo(org.sonar.scanner.protocol.Constants.Severity.INFO);
  }

  @Test
  public void filter_issue() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    activeRulesBuilder.addRule(new NewActiveRule.Builder()
      .setRuleKey(SQUID_RULE_KEY)
      .setSeverity(Severity.INFO)
      .setQProfileKey("qp-1")
      .build());
    initModuleIssues();

    DefaultIssue issue = new DefaultIssue(project)
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message(""))
      .forRule(SQUID_RULE_KEY);

    when(filters.accept(any(InputComponent.class), any(ScannerReport.Issue.class))).thenReturn(false);

    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(reportPublisher);
  }

  @Test
  public void should_ignore_lines_commented_with_nosonar() {
    ruleBuilder.add(SQUID_RULE_KEY).setName(SQUID_RULE_NAME);
    activeRulesBuilder.addRule(new NewActiveRule.Builder()
      .setRuleKey(SQUID_RULE_KEY)
      .setSeverity(Severity.INFO)
      .setQProfileKey("qp-1")
      .build());
    initModuleIssues();

    DefaultIssue issue = new DefaultIssue(project)
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message(""))
      .forRule(SQUID_RULE_KEY);

    file.noSonarAt(new HashSet<>(Collections.singletonList(3)));

    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isFalse();
    verifyZeroInteractions(reportPublisher);
  }

  @Test
  public void should_accept_issues_on_no_sonar_rules() {
    // The "No Sonar" rule logs violations on the lines that are flagged with "NOSONAR" !!
    ruleBuilder.add(NOSONAR_RULE_KEY).setName("No Sonar");
    activeRulesBuilder.addRule(new NewActiveRule.Builder()
      .setRuleKey(NOSONAR_RULE_KEY)
      .setSeverity(Severity.INFO)
      .setQProfileKey("qp-1")
      .build());
    initModuleIssues();

    file.noSonarAt(new HashSet<>(Collections.singletonList(3)));

    DefaultIssue issue = new DefaultIssue(project)
      .at(new DefaultIssueLocation().on(file).at(file.selectLine(3)).message(""))
      .forRule(NOSONAR_RULE_KEY);

    when(filters.accept(any(InputComponent.class), any(ScannerReport.Issue.class))).thenReturn(true);

    boolean added = moduleIssues.initAndAddIssue(issue);

    assertThat(added).isTrue();
    verify(reportPublisher.getWriter()).appendComponentIssue(eq(file.scannerId()), any());
  }

  /**
   * Every rules and active rules has to be added in builders before creating IssuePublisher
   */
  private void initModuleIssues() {
    moduleIssues = new IssuePublisher(activeRulesBuilder.build(), filters, reportPublisher);
  }

}
