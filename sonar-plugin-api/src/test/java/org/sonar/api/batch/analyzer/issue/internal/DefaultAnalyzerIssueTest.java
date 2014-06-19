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
package org.sonar.api.batch.analyzer.issue.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.analyzer.issue.AnalyzerIssue;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.rule.RuleKey;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultAnalyzerIssueTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void build_file_issue() {
    AnalyzerIssue issue = new DefaultAnalyzerIssueBuilder()
      .onFile(new DefaultInputFile("src/Foo.php"))
      .ruleKey(RuleKey.of("repo", "rule"))
      .atLine(1)
      .effortToFix(10.0)
      .message("Wrong way!")
      .build();

    assertThat(issue.inputFile()).isEqualTo(new DefaultInputFile("src/Foo.php"));
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.line()).isEqualTo(1);
    assertThat(issue.effortToFix()).isEqualTo(10.0);
    assertThat(issue.message()).isEqualTo("Wrong way!");
  }

  @Test
  public void build_project_issue() {
    AnalyzerIssue issue = new DefaultAnalyzerIssueBuilder()
      .onProject()
      .ruleKey(RuleKey.of("repo", "rule"))
      .atLine(1)
      .effortToFix(10.0)
      .message("Wrong way!")
      .build();

    assertThat(issue.inputFile()).isNull();
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.line()).isEqualTo(1);
    assertThat(issue.effortToFix()).isEqualTo(10.0);
    assertThat(issue.message()).isEqualTo("Wrong way!");
  }

  @Test
  public void not_allowed_to_call_onFile_and_onProject() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("onFile or onProject can be called only once");
    new DefaultAnalyzerIssueBuilder()
      .onProject()
      .onFile(new DefaultInputFile("src/Foo.php"))
      .ruleKey(RuleKey.of("repo", "rule"))
      .atLine(1)
      .effortToFix(10.0)
      .message("Wrong way!")
      .build();

  }

}
