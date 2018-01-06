/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.scanner.issue.tracking.TrackedIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultProjectIssuesTest {

  static final RuleKey SQUID_RULE_KEY = RuleKey.of("squid", "AvoidCycle");

  IssueCache cache = mock(IssueCache.class);
  DefaultProjectIssues projectIssues = new DefaultProjectIssues(cache);

  @Test
  public void should_get_all_issues() {
    DefaultIssue issueOnModule = new DefaultIssue().setKey("1").setRuleKey(SQUID_RULE_KEY).setComponentKey("org.apache:struts-core");
    DefaultIssue issueInModule = new DefaultIssue().setKey("2").setRuleKey(SQUID_RULE_KEY).setComponentKey("org.apache:struts-core:Action");
    DefaultIssue resolvedIssueInModule = new DefaultIssue().setKey("3").setRuleKey(SQUID_RULE_KEY).setComponentKey("org.apache:struts-core:Action")
      .setResolution(Issue.RESOLUTION_FIXED);

    DefaultIssue issueOnRoot = new DefaultIssue().setKey("4").setRuleKey(SQUID_RULE_KEY).setSeverity(Severity.CRITICAL).setComponentKey("org.apache:struts");
    DefaultIssue issueInRoot = new DefaultIssue().setKey("5").setRuleKey(SQUID_RULE_KEY).setSeverity(Severity.CRITICAL).setComponentKey("org.apache:struts:FileInRoot");
    when(cache.all()).thenReturn(Arrays.<TrackedIssue>asList(
      toTrackedIssue(issueOnRoot), toTrackedIssue(issueInRoot),
      toTrackedIssue(issueOnModule), toTrackedIssue(issueInModule), toTrackedIssue(resolvedIssueInModule)
      ));

    // unresolved issues
    List<Issue> issues = Lists.newArrayList(projectIssues.issues());
    assertThat(issues).containsOnly(issueOnRoot, issueInRoot, issueInModule, issueOnModule);

    List<Issue> resolvedIssues = Lists.newArrayList(projectIssues.resolvedIssues());
    assertThat(resolvedIssues).containsOnly(resolvedIssueInModule);
  }

  private TrackedIssue toTrackedIssue(DefaultIssue issue) {
    TrackedIssue trackedIssue = new TrackedIssue();

    trackedIssue.setKey(issue.key());
    trackedIssue.setRuleKey(issue.ruleKey());
    trackedIssue.setComponentKey(issue.componentKey());
    trackedIssue.setSeverity(issue.severity());
    trackedIssue.setResolution(issue.resolution());

    return trackedIssue;
  }
}
