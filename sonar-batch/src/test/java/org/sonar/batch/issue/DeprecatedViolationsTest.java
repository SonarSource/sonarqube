/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.issue;

import org.junit.Test;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.batch.index.ResourceCache;

import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeprecatedViolationsTest {

  IssueCache issueCache = mock(IssueCache.class);
  RuleFinder ruleFinder = mock(RuleFinder.class);
  ResourceCache resourceCache = mock(ResourceCache.class);
  DeprecatedViolations deprecatedViolations = new DeprecatedViolations(issueCache, ruleFinder, resourceCache);

  @Test
  public void test_toViolation() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycles");
    when(ruleFinder.findByKey(ruleKey)).thenReturn(new Rule("squid", "AvoidCycles"));
    when(resourceCache.get("org.apache:struts")).thenReturn(new Project("org.apache:struts"));

    DefaultIssue issue = newIssue(ruleKey);

    Violation violation = deprecatedViolations.toViolation(issue);
    assertThat(violation.getLineId()).isEqualTo(42);
    assertThat(violation.getSeverity()).isEqualTo(RulePriority.BLOCKER);
    assertThat(violation.isManual()).isTrue();
    assertThat(violation.getRule().getRepositoryKey()).isEqualTo("squid");
    assertThat(violation.getRule().getKey()).isEqualTo("AvoidCycles");
    assertThat(violation.getResource()).isNotNull();
    assertThat(violation.isSwitchedOff()).isFalse();
  }

  private DefaultIssue newIssue(RuleKey ruleKey) {
    DefaultIssue issue = new DefaultIssue();
    issue.setKey("ABCDE");
    issue.setRuleKey(ruleKey);
    issue.setComponentKey("org.apache:struts");
    issue.setLine(42);
    issue.setEffortToFix(3.14);
    issue.setReporter("leon");
    issue.setSeverity(Severity.BLOCKER);
    return issue;
  }

  @Test
  public void test_get() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycles");
    when(ruleFinder.findByKey(ruleKey)).thenReturn(new Rule("squid", "AvoidCycles"));
    when(resourceCache.get("org.apache:struts")).thenReturn(new Project("org.apache:struts"));
    when(issueCache.byComponent("org.apache:struts")).thenReturn(Arrays.asList(newIssue(ruleKey)));

    List<Violation> violations = deprecatedViolations.get("org.apache:struts");

    assertThat(violations).hasSize(1);
  }
}
