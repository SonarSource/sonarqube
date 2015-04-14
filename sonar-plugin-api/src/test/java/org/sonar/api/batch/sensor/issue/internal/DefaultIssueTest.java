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
package org.sonar.api.batch.sensor.issue.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultIssueTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void build_file_issue() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue issue = new DefaultIssue(storage)
      .onFile(new DefaultInputFile("foo", "src/Foo.php").setLines(3))
      .forRule(RuleKey.of("repo", "rule"))
      .atLine(1)
      .effortToFix(10.0)
      .message("Wrong way!");

    assertThat(issue.inputPath()).isEqualTo(new DefaultInputFile("foo", "src/Foo.php"));
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.line()).isEqualTo(1);
    assertThat(issue.effortToFix()).isEqualTo(10.0);
    assertThat(issue.message()).isEqualTo("Wrong way!");

    issue.save();

    verify(storage).store(issue);
  }

  @Test
  public void build_directory_issue() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue issue = new DefaultIssue(storage)
      .onDir(new DefaultInputDir("foo", "src"))
      .forRule(RuleKey.of("repo", "rule"))
      .overrideSeverity(Severity.BLOCKER)
      .message("Wrong way!");

    assertThat(issue.inputPath()).isEqualTo(new DefaultInputDir("foo", "src"));
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.message()).isEqualTo("Wrong way!");
    assertThat(issue.overridenSeverity()).isEqualTo(Severity.BLOCKER);

    issue.save();

    verify(storage).store(issue);
  }

  @Test
  public void build_project_issue() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue issue = new DefaultIssue(storage)
      .onProject()
      .forRule(RuleKey.of("repo", "rule"))
      .effortToFix(10.0)
      .message("Wrong way!");

    assertThat(issue.inputPath()).isNull();
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.line()).isNull();
    assertThat(issue.effortToFix()).isEqualTo(10.0);
    assertThat(issue.message()).isEqualTo("Wrong way!");

    issue.save();

    verify(storage).store(issue);
  }

  @Test
  public void not_allowed_to_call_onFile_and_onProject() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("onProject already called");
    new DefaultIssue()
      .onProject()
      .onFile(new DefaultInputFile("foo", "src/Foo.php"))
      .forRule(RuleKey.of("repo", "rule"))
      .atLine(1)
      .effortToFix(10.0)
      .message("Wrong way!");
  }

  @Test
  public void line_is_positive() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("line starts at 1, invalid value 0.");
    new DefaultIssue()
      .onFile(new DefaultInputFile("foo", "src/Foo.php").setLines(3))
      .forRule(RuleKey.of("repo", "rule"))
      .atLine(0);
  }

  @Test
  public void not_allowed_to_create_issues_on_unexisting_line() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("File [moduleKey=foo, relative=src/Foo.php, basedir=null] has 3 lines. Unable to create issue at line 5.");
    new DefaultIssue()
      .onFile(new DefaultInputFile("foo", "src/Foo.php").setLines(3))
      .forRule(RuleKey.of("repo", "rule"))
      .atLine(5);
  }

}
