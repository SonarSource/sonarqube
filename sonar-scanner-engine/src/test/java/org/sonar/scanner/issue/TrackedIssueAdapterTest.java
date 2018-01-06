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

import java.util.Date;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.scanner.issue.tracking.TrackedIssue;

import static org.assertj.core.api.Assertions.assertThat;

public class TrackedIssueAdapterTest {

  @Test
  public void improve_coverage() {
    Date creationDate = new Date();
    TrackedIssue trackedIssue = new TrackedIssue()
      .setKey("XYZ123")
      .setComponentKey("foo")
      .setRuleKey(RuleKey.of("repo", "rule"))
      .setSeverity("MAJOR")
      .setMessage("msg")
      .setStartLine(1)
      .setGap(2.0)
      .setStatus("RESOLVED")
      .setResolution("FIXED")
      .setAssignee("tata")
      .setNew(true)
      .setCreationDate(creationDate);
    Issue issue = new TrackedIssueAdapter(trackedIssue);
    assertThat(issue.key()).isEqualTo("XYZ123");
    assertThat(issue.componentKey()).isEqualTo("foo");
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.severity()).isEqualTo("MAJOR");
    assertThat(issue.message()).isEqualTo("msg");
    assertThat(issue.line()).isEqualTo(1);
    assertThat(issue.gap()).isEqualTo(2.0);
    assertThat(issue.status()).isEqualTo("RESOLVED");
    assertThat(issue.resolution()).isEqualTo("FIXED");
    assertThat(issue.assignee()).isEqualTo("tata");
    assertThat(issue.isNew()).isTrue();
    assertThat(issue.attribute("foo")).isNull();
    assertThat(issue.creationDate()).isEqualTo(creationDate);
    assertThat(issue.language()).isNull();
    assertThat(issue.updateDate()).isNull();
    assertThat(issue.closeDate()).isNull();
    assertThat(issue.authorLogin()).isNull();
    assertThat(issue.comments()).isEmpty();
    assertThat(issue.effort()).isNull();
    assertThat(issue.projectKey()).isNull();
    assertThat(issue.projectUuid()).isNull();
    assertThat(issue.componentUuid()).isNull();
    assertThat(issue.tags()).isEmpty();

    assertThat(issue).isNotEqualTo(null);
    assertThat(issue).isNotEqualTo("Foo");
    assertThat(issue).isEqualTo(new TrackedIssueAdapter(trackedIssue));
    assertThat(issue.hashCode()).isEqualTo(trackedIssue.key().hashCode());
    assertThat(issue).isNotEqualTo(new TrackedIssueAdapter(new TrackedIssue()
      .setKey("another")));
  }

}
