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

import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultIssueTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultInputProject project;

  private DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.php")
    .initMetadata("Foo\nBar\n")
    .build();

  @Before
  public void prepare() throws IOException {
    project = new DefaultInputProject(ProjectDefinition.create()
      .setKey("foo")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder()));
  }

  @Test
  public void build_file_issue() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue issue = new DefaultIssue(project, storage)
      .at(new DefaultIssueLocation()
        .on(inputFile)
        .at(inputFile.selectLine(1))
        .message("Wrong way!"))
      .forRule(RuleKey.of("repo", "rule"))
      .gap(10.0);

    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(inputFile);
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.primaryLocation().textRange().start().line()).isEqualTo(1);
    assertThat(issue.gap()).isEqualTo(10.0);
    assertThat(issue.primaryLocation().message()).isEqualTo("Wrong way!");

    issue.save();

    verify(storage).store(issue);
  }

  @Test
  public void move_directory_issue_to_project_root() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue issue = new DefaultIssue(project, storage)
      .at(new DefaultIssueLocation()
        .on(new DefaultInputDir("foo", "src/main").setModuleBaseDir(project.getBaseDir()))
        .message("Wrong way!"))
      .forRule(RuleKey.of("repo", "rule"))
      .overrideSeverity(Severity.BLOCKER);

    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(project);
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.primaryLocation().textRange()).isNull();
    assertThat(issue.primaryLocation().message()).isEqualTo("[src/main] Wrong way!");
    assertThat(issue.overriddenSeverity()).isEqualTo(Severity.BLOCKER);

    issue.save();

    verify(storage).store(issue);
  }

  @Test
  public void move_submodule_issue_to_project_root() {
    File subModuleDirectory = new File(project.getBaseDir().toString(), "bar");
    subModuleDirectory.mkdir();

    ProjectDefinition subModuleDefinition = ProjectDefinition.create()
      .setKey("foo/bar")
      .setBaseDir(subModuleDirectory)
      .setWorkDir(subModuleDirectory);
    project.definition().addSubProject(subModuleDefinition);
    DefaultInputModule subModule = new DefaultInputModule(subModuleDefinition);

    SensorStorage storage = mock(SensorStorage.class);
    DefaultIssue issue = new DefaultIssue(project, storage)
      .at(new DefaultIssueLocation()
        .on(subModule)
        .message("Wrong way!"))
      .forRule(RuleKey.of("repo", "rule"))
      .overrideSeverity(Severity.BLOCKER);

    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(project);
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.primaryLocation().textRange()).isNull();
    assertThat(issue.primaryLocation().message()).isEqualTo("[bar] Wrong way!");
    assertThat(issue.overriddenSeverity()).isEqualTo(Severity.BLOCKER);

    issue.save();

    verify(storage).store(issue);
  }

  @Test
  public void build_project_issue() throws IOException {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultInputModule inputModule = new DefaultInputModule(ProjectDefinition.create().setKey("foo").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    DefaultIssue issue = new DefaultIssue(project, storage)
      .at(new DefaultIssueLocation()
        .on(inputModule)
        .message("Wrong way!"))
      .forRule(RuleKey.of("repo", "rule"))
      .gap(10.0);

    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(inputModule);
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.primaryLocation().textRange()).isNull();
    assertThat(issue.gap()).isEqualTo(10.0);
    assertThat(issue.primaryLocation().message()).isEqualTo("Wrong way!");

    issue.save();

    verify(storage).store(issue);
  }

}
