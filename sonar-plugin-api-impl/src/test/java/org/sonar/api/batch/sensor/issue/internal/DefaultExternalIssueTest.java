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
package org.sonar.api.batch.sensor.issue.internal;

import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.internal.DefaultExternalIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultExternalIssueTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultInputProject project;

  @Before
  public void setup() throws IOException {
    project = new DefaultInputProject(ProjectDefinition.create()
      .setKey("foo")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder()));
  }

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.php")
    .initMetadata("Foo\nBar\n")
    .build();

  @Test
  public void build_file_issue() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultExternalIssue issue = new DefaultExternalIssue(project, storage)
      .at(new DefaultIssueLocation()
        .on(inputFile)
        .at(inputFile.selectLine(1))
        .message("Wrong way!"))
      .forRule(RuleKey.of("repo", "rule"))
      .remediationEffortMinutes(10l)
      .type(RuleType.BUG)
      .severity(Severity.BLOCKER);

    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(inputFile);
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("external_repo", "rule"));
    assertThat(issue.engineId()).isEqualTo("repo");
    assertThat(issue.ruleId()).isEqualTo("rule");
    assertThat(issue.primaryLocation().textRange().start().line()).isEqualTo(1);
    assertThat(issue.remediationEffort()).isEqualTo(10l);
    assertThat(issue.type()).isEqualTo(RuleType.BUG);
    assertThat(issue.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(issue.primaryLocation().message()).isEqualTo("Wrong way!");

    issue.save();

    verify(storage).store(issue);
  }

  @Test
  public void fail_to_store_if_no_type() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultExternalIssue issue = new DefaultExternalIssue(project, storage)
      .at(new DefaultIssueLocation()
        .on(inputFile)
        .at(inputFile.selectLine(1))
        .message("Wrong way!"))
      .forRule(RuleKey.of("repo", "rule"))
      .remediationEffortMinutes(10l)
      .severity(Severity.BLOCKER);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Type is mandatory");
    issue.save();
  }

  @Test
  public void fail_to_store_if_primary_location_is_not_a_file() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultExternalIssue issue = new DefaultExternalIssue(project, storage)
      .at(new DefaultIssueLocation()
        .on(mock(InputComponent.class))
        .message("Wrong way!"))
      .forRule(RuleKey.of("repo", "rule"))
      .remediationEffortMinutes(10l)
      .severity(Severity.BLOCKER);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("External issues must be located in files");
    issue.save();
  }

  @Test
  public void fail_to_store_if_primary_location_has_no_message() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultExternalIssue issue = new DefaultExternalIssue(project, storage)
      .at(new DefaultIssueLocation()
        .on(inputFile)
        .at(inputFile.selectLine(1)))
      .forRule(RuleKey.of("repo", "rule"))
      .remediationEffortMinutes(10l)
      .type(RuleType.BUG)
      .severity(Severity.BLOCKER);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("External issues must have a message");
    issue.save();
  }

  @Test
  public void fail_to_store_if_no_severity() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultExternalIssue issue = new DefaultExternalIssue(project, storage)
      .at(new DefaultIssueLocation()
        .on(inputFile)
        .at(inputFile.selectLine(1))
        .message("Wrong way!"))
      .forRule(RuleKey.of("repo", "rule"))
      .remediationEffortMinutes(10l)
      .type(RuleType.BUG);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Severity is mandatory");
    issue.save();
  }

}
