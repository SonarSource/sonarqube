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
package org.sonar.core.issue;

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultIssueBuilderTest {

  @Test
  public void build_new_issue() {
    String componentKey = "org.apache.struts:struts-core:Action.java";
    String projectKey = "org.apache.struts";
    DefaultIssue issue = (DefaultIssue) new DefaultIssueBuilder()
      .componentKey(componentKey)
      .projectKey(projectKey)
      .message("the message")
      .line(123)
      .effortToFix(10000.0)
      .ruleKey(RuleKey.of("squid", "NullDereference"))
      .severity(Severity.CRITICAL)
      .attribute("JIRA", "FOO-123")
      .attribute("YOUTRACK", "YT-123")
      .build();

    assertThat(issue).isNotNull();
    assertThat(issue.key()).isNotNull();
    assertThat(issue.effortToFix()).isEqualTo(10000.0);
    assertThat(issue.componentKey()).isEqualTo(componentKey);
    assertThat(issue.projectKey()).isEqualTo(projectKey);
    assertThat(issue.message()).isEqualTo("the message");
    assertThat(issue.line()).isEqualTo(123);
    assertThat(issue.ruleKey().repository()).isEqualTo("squid");
    assertThat(issue.ruleKey().rule()).isEqualTo("NullDereference");
    assertThat(issue.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issue.assignee()).isNull();
    assertThat(issue.isNew()).isTrue();
    assertThat(issue.resolution()).isNull();
    assertThat(issue.status()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(issue.attribute("JIRA")).isEqualTo("FOO-123");
    assertThat(issue.attribute("YOUTRACK")).isEqualTo("YT-123");
    assertThat(issue.attributes()).hasSize(2);
  }

  @Test
  public void not_set_default_severity() {
    DefaultIssue issue = (DefaultIssue) new DefaultIssueBuilder()
      .componentKey("Action.java")
      .projectKey("org.apache.struts")
      .ruleKey(RuleKey.of("squid", "NullDereference"))
      .build();

    assertThat(issue.severity()).isNull();
  }
}
