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

import java.util.Date;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.protocol.Constants.Severity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class DeprecatedIssueAdapterForFilterTest {

  private static final String PROJECT_KEY = "foo";
  private static final Date ANALYSIS_DATE = new Date();
  private static final String COMPONENT_KEY = "foo:src/Foo.java";

  @Test
  public void improve_coverage() {
    DeprecatedIssueAdapterForFilter issue = new DeprecatedIssueAdapterForFilter(new Project(PROJECT_KEY).setAnalysisDate(ANALYSIS_DATE),
      org.sonar.batch.protocol.output.BatchReport.Issue.newBuilder()
        .setRuleRepository("repo")
        .setRuleKey("key")
        .setSeverity(Severity.BLOCKER)
        .setMsg("msg")
        .build(),
      COMPONENT_KEY);
    DeprecatedIssueAdapterForFilter issue2 = new DeprecatedIssueAdapterForFilter(new Project(PROJECT_KEY).setAnalysisDate(ANALYSIS_DATE),
      org.sonar.batch.protocol.output.BatchReport.Issue.newBuilder()
        .setRuleRepository("repo")
        .setRuleKey("key")
        .setSeverity(Severity.BLOCKER)
        .setMsg("msg")
        .setLine(1)
        .setEffortToFix(2.0)
        .build(),
      COMPONENT_KEY);

    try {
      issue.key();
      fail("Should be unsupported");
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(UnsupportedOperationException.class).hasMessage("Not available for issues filters");
    }

    assertThat(issue.componentKey()).isEqualTo(COMPONENT_KEY);
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "key"));

    try {
      issue.language();
      fail("Should be unsupported");
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(UnsupportedOperationException.class).hasMessage("Not available for issues filters");
    }

    assertThat(issue.severity()).isEqualTo("BLOCKER");
    assertThat(issue.message()).isEqualTo("msg");
    assertThat(issue.line()).isNull();
    assertThat(issue2.line()).isEqualTo(1);
    assertThat(issue.effortToFix()).isNull();
    assertThat(issue2.effortToFix()).isEqualTo(2.0);
    assertThat(issue.status()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(issue.resolution()).isNull();

    try {
      issue.reporter();
      fail("Should be unsupported");
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(UnsupportedOperationException.class).hasMessage("Not available for issues filters");
    }

    assertThat(issue.assignee()).isNull();
    assertThat(issue.creationDate()).isEqualTo(ANALYSIS_DATE);
    assertThat(issue.updateDate()).isNull();
    assertThat(issue.closeDate()).isNull();
    assertThat(issue.attribute(PROJECT_KEY)).isNull();

    try {
      issue.authorLogin();
      fail("Should be unsupported");
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(UnsupportedOperationException.class).hasMessage("Not available for issues filters");
    }

    try {
      issue.actionPlanKey();
      fail("Should be unsupported");
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(UnsupportedOperationException.class).hasMessage("Not available for issues filters");
    }

    try {
      issue.comments();
      fail("Should be unsupported");
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(UnsupportedOperationException.class).hasMessage("Not available for issues filters");
    }

    try {
      issue.isNew();
      fail("Should be unsupported");
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(UnsupportedOperationException.class).hasMessage("Not available for issues filters");
    }

    try {
      issue.debt();
      fail("Should be unsupported");
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(UnsupportedOperationException.class).hasMessage("Not available for issues filters");
    }

    assertThat(issue.projectKey()).isEqualTo(PROJECT_KEY);

    try {
      issue.projectUuid();
      fail("Should be unsupported");
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(UnsupportedOperationException.class).hasMessage("Not available for issues filters");
    }

    try {
      issue.componentUuid();
      fail("Should be unsupported");
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(UnsupportedOperationException.class).hasMessage("Not available for issues filters");
    }

    try {
      issue.tags();
      fail("Should be unsupported");
    } catch (Exception e) {
      assertThat(e).isExactlyInstanceOf(UnsupportedOperationException.class).hasMessage("Not available for issues filters");
    }
  }

}
