/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.Issue.Flow;
import org.sonar.api.batch.sensor.issue.MessageFormatting;
import org.sonar.api.batch.sensor.issue.NewIssue.FlowType;
import org.sonar.api.batch.sensor.issue.fix.NewQuickFix;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultIssueTest {
  private static final RuleKey RULE_KEY = RuleKey.of("repo", "rule");
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final SensorStorage storage = mock(SensorStorage.class);
  private final DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.php")
    .initMetadata("Foo\nBar\n")
    .build();
  private DefaultInputProject project;

  private final NewQuickFix quickFix = mock(NewQuickFix.class);

  @Before
  public void prepare() throws IOException {
    project = new DefaultInputProject(ProjectDefinition.create()
      .setKey("foo")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder()));
  }

  @Test
  public void build_file_issue() {
    DefaultIssue issue = new DefaultIssue(project, storage)
      .at(new DefaultIssueLocation()
        .on(inputFile)
        .at(inputFile.selectLine(1))
        .message("Wrong way!"))
      .forRule(RULE_KEY)
      .gap(10.0)
      .setRuleDescriptionContextKey("spring")
      .setCodeVariants(List.of("variant1", "variant2"));

    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(inputFile);
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.primaryLocation().textRange().start().line()).isOne();
    assertThat(issue.gap()).isEqualTo(10.0);
    assertThat(issue.primaryLocation().message()).isEqualTo("Wrong way!");
    assertThat(issue.ruleDescriptionContextKey()).contains("spring");
    assertThat(issue.codeVariants()).containsOnly("variant1", "variant2");

    issue.save();

    verify(storage).store(issue);
  }

  @Test
  public void build_issue_with_flows() {
    TextRange range1 = new DefaultTextRange(new DefaultTextPointer(1, 1), new DefaultTextPointer(1, 2));
    TextRange range2 = new DefaultTextRange(new DefaultTextPointer(2, 1), new DefaultTextPointer(2, 2));

    DefaultIssue issue = new DefaultIssue(project, storage)
      .at(new DefaultIssueLocation().on(inputFile))
      .addFlow(List.of(new DefaultIssueLocation().message("loc1").on(inputFile)), FlowType.DATA, "desc")
      .addFlow(List.of(new DefaultIssueLocation().message("loc1").on(inputFile).at(range1), new DefaultIssueLocation().message("loc1").on(inputFile).at(range2)))
      .forRule(RULE_KEY);

    assertThat(issue.flows())
      .extracting(Flow::type, Flow::description)
      .containsExactly(tuple(FlowType.DATA, "desc"), tuple(FlowType.UNDEFINED, null));

    assertThat(issue.flows().get(0).locations()).hasSize(1);
    assertThat(issue.flows().get(1).locations()).hasSize(2);
  }

  @Test
  public void build_issue_with_secondary_locations() {
    TextRange range1 = new DefaultTextRange(new DefaultTextPointer(1, 1), new DefaultTextPointer(1, 2));
    TextRange range2 = new DefaultTextRange(new DefaultTextPointer(2, 1), new DefaultTextPointer(2, 2));

    DefaultIssue issue = new DefaultIssue(project, storage)
      .at(new DefaultIssueLocation().on(inputFile))
      .addLocation(new DefaultIssueLocation().on(inputFile).at(range1).message("loc1"))
      .addLocation(new DefaultIssueLocation().on(inputFile).at(range2).message("loc2"))
      .forRule(RULE_KEY);

    assertThat(issue.flows())
      .extracting(Flow::type, Flow::description)
      .containsExactly(tuple(FlowType.UNDEFINED, null), tuple(FlowType.UNDEFINED, null));

    assertThat(issue.flows().get(0).locations()).hasSize(1);
    assertThat(issue.flows().get(1).locations()).hasSize(1);
  }

  @Test
  public void move_directory_issue_to_project_root() {
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

    DefaultIssue issue = new DefaultIssue(project, storage)
      .at(new DefaultIssueLocation()
        .on(subModule)
        .message("Wrong way! with code snippet", List.of(new DefaultMessageFormatting().start(16).end(27).type(MessageFormatting.Type.CODE))))
      .forRule(RULE_KEY)
      .overrideSeverity(Severity.BLOCKER);

    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(project);
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.primaryLocation().textRange()).isNull();
    assertThat(issue.primaryLocation().message()).isEqualTo("[bar] Wrong way! with code snippet");
    assertThat(issue.overriddenSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(issue.primaryLocation().messageFormattings().get(0)).extracting(MessageFormatting::start,
      MessageFormatting::end, MessageFormatting::type)
      .as("Formatting ranges are padded with the new message")
      .containsExactly(22, 33, MessageFormatting.Type.CODE);
    issue.save();

    verify(storage).store(issue);
  }

  @Test
  public void build_project_issue() throws IOException {
    DefaultInputModule inputModule = new DefaultInputModule(ProjectDefinition.create().setKey("foo").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    DefaultIssue issue = new DefaultIssue(project, storage)
      .at(new DefaultIssueLocation()
        .on(inputModule)
        .message("Wrong way!"))
      .forRule(RULE_KEY)
      .gap(10.0);

    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(inputModule);
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(issue.primaryLocation().textRange()).isNull();
    assertThat(issue.gap()).isEqualTo(10.0);
    assertThat(issue.primaryLocation().message()).isEqualTo("Wrong way!");

    issue.save();

    verify(storage).store(issue);
  }

  @Test
  public void at_fails_if_called_twice() {
    DefaultIssueLocation loc = new DefaultIssueLocation().on(inputFile);
    DefaultIssue issue = new DefaultIssue(project, storage).at(loc);
    assertThatThrownBy(() -> issue.at(loc)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void at_fails_if_location_is_null() {
    DefaultIssue issue = new DefaultIssue(project, storage);
    assertThatThrownBy(() -> issue.at(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void default_issue_has_no_quickfix() {
    DefaultIssue issue = new DefaultIssue(project, storage);

    assertThat(issue.isQuickFixAvailable()).isFalse();
  }

  @Test
  public void issue_can_have_quickfix() {
    DefaultIssue issue = new DefaultIssue(project, storage).setQuickFixAvailable(true);

    assertThat(issue.isQuickFixAvailable()).isTrue();
  }

  @Test
  public void issue_can_override_impacts() {
    DefaultIssue issue = new DefaultIssue(project, storage).overrideImpact(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW);

    assertThat(issue.overridenImpacts()).containsEntry(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW);
  }

  @Test
  public void quickfix_only_sets_flag_to_true() {
    DefaultIssue issue = new DefaultIssue(project);

    NewQuickFix newQuickFix = issue.newQuickFix();
    assertThat(newQuickFix).isInstanceOf(NoOpNewQuickFix.class);

    assertThat(issue.isQuickFixAvailable()).isFalse();
    issue.addQuickFix(newQuickFix);
    assertThat(issue.isQuickFixAvailable()).isTrue();
  }
}
