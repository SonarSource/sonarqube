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
package org.sonar.server.computation.task.projectanalysis.issue.filter;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

public class IssueFilterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  static final RuleKey XOO_X1 = RuleKey.of("xoo", "x1");
  static final RuleKey XOO_X2 = RuleKey.of("xoo", "x2");
  static final RuleKey XOO_X3 = RuleKey.of("xoo", "x3");

  static final String PATH1 = "src/main/xoo/File1.xoo";
  static final String PATH2 = "src/main/xoo/File2.xoo";
  static final String PATH3 = "src/main/xoo/File3.xoo";

  static final Component PROJECT = builder(Component.Type.PROJECT, 10).build();

  static final Component COMPONENT_1 = builder(FILE, 1).setKey("File1").setPath(PATH1).build();
  static final Component COMPONENT_2 = builder(FILE, 2).setKey("File2").setPath(PATH2).build();
  static final Component COMPONENT_3 = builder(FILE, 3).setKey("File3").setPath(PATH3).build();

  static final DefaultIssue ISSUE_1 = new DefaultIssue().setRuleKey(XOO_X1);
  static final DefaultIssue ISSUE_2 = new DefaultIssue().setRuleKey(XOO_X2);
  static final DefaultIssue ISSUE_3 = new DefaultIssue().setRuleKey(XOO_X3);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(PROJECT);

  ConfigurationRepository settingsRepository = mock(ConfigurationRepository.class);

  @Test
  public void accept_everything_when_no_filter_properties() {
    IssueFilter underTest = newIssueFilter(new MapSettings());

    assertThat(underTest.accept(ISSUE_1, COMPONENT_1)).isTrue();
    assertThat(underTest.accept(ISSUE_2, COMPONENT_2)).isTrue();
    assertThat(underTest.accept(ISSUE_3, COMPONENT_3)).isTrue();
  }

  @Test
  public void ignore_all() {
    IssueFilter underTest = newIssueFilter(newSettings(asList("*", "**"), Collections.emptyList()));

    assertThat(underTest.accept(ISSUE_1, COMPONENT_1)).isFalse();
    assertThat(underTest.accept(ISSUE_2, COMPONENT_1)).isFalse();
    assertThat(underTest.accept(ISSUE_3, COMPONENT_1)).isFalse();
  }

  @Test
  public void ignore_some_rule_and_component() {
    IssueFilter underTest = newIssueFilter(newSettings(asList("xoo:x1", "**/xoo/File1*"), Collections.emptyList()));

    assertThat(underTest.accept(ISSUE_1, COMPONENT_1)).isFalse();
    assertThat(underTest.accept(ISSUE_1, COMPONENT_2)).isTrue();
    assertThat(underTest.accept(ISSUE_2, COMPONENT_1)).isTrue();
    assertThat(underTest.accept(ISSUE_2, COMPONENT_2)).isTrue();
  }

  @Test
  public void ignore_many_rules() {
    IssueFilter underTest = newIssueFilter(newSettings(
      asList("xoo:x1", "**/xoo/File1*", "xoo:x2", "**/xoo/File1*"),
      Collections.emptyList()));

    assertThat(underTest.accept(ISSUE_1, COMPONENT_1)).isFalse();
    assertThat(underTest.accept(ISSUE_1, COMPONENT_2)).isTrue();
    assertThat(underTest.accept(ISSUE_2, COMPONENT_1)).isFalse();
    assertThat(underTest.accept(ISSUE_2, COMPONENT_2)).isTrue();
  }

  @Test
  public void include_all() {
    IssueFilter underTest = newIssueFilter(newSettings(Collections.emptyList(), asList("*", "**")));

    assertThat(underTest.accept(ISSUE_1, COMPONENT_1)).isTrue();
    assertThat(underTest.accept(ISSUE_2, COMPONENT_1)).isTrue();
    assertThat(underTest.accept(ISSUE_3, COMPONENT_1)).isTrue();
  }

  @Test
  public void include_some_rule_and_component() {
    IssueFilter underTest = newIssueFilter(newSettings(Collections.emptyList(), asList("xoo:x1", "**/xoo/File1*")));

    assertThat(underTest.accept(ISSUE_1, COMPONENT_1)).isTrue();
    assertThat(underTest.accept(ISSUE_1, COMPONENT_2)).isFalse();
    // Issues on other rule are accepted
    assertThat(underTest.accept(ISSUE_2, COMPONENT_1)).isTrue();
    assertThat(underTest.accept(ISSUE_2, COMPONENT_2)).isTrue();
  }

  @Test
  public void ignore_and_include_same_rule_and_component() {
    IssueFilter underTest = newIssueFilter(newSettings(
      asList("xoo:x1", "**/xoo/File1*"),
      asList("xoo:x1", "**/xoo/File1*")));

    assertThat(underTest.accept(ISSUE_1, COMPONENT_1)).isFalse();
    assertThat(underTest.accept(ISSUE_1, COMPONENT_2)).isFalse();
    // Issues on other rule are accepted
    assertThat(underTest.accept(ISSUE_2, COMPONENT_1)).isTrue();
    assertThat(underTest.accept(ISSUE_2, COMPONENT_2)).isTrue();
  }

  @Test
  public void include_many_rules() {
    IssueFilter underTest = newIssueFilter(newSettings(
      Collections.emptyList(),
      asList("xoo:x1", "**/xoo/File1*", "xoo:x2", "**/xoo/File1*")));

    assertThat(underTest.accept(ISSUE_1, COMPONENT_1)).isTrue();
    assertThat(underTest.accept(ISSUE_1, COMPONENT_2)).isFalse();
    assertThat(underTest.accept(ISSUE_2, COMPONENT_1)).isTrue();
    assertThat(underTest.accept(ISSUE_2, COMPONENT_2)).isFalse();
  }

  @Test
  public void accept_project_issues() {
    IssueFilter underTest = newIssueFilter(newSettings(
      asList("xoo:x1", "**/xoo/File1*"),
      asList("xoo:x1", "**/xoo/File1*")));

    assertThat(underTest.accept(ISSUE_1, PROJECT)).isTrue();
    assertThat(underTest.accept(ISSUE_2, PROJECT)).isTrue();
  }

  @Test
  public void fail_when_only_rule_key_parameter() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("File path pattern cannot be empty. Please check 'sonar.issue.ignore.multicriteria' settings");

    newIssueFilter(newSettings(asList("xoo:x1", ""), Collections.emptyList()));
  }

  @Test
  public void fail_when_only_path_parameter() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Rule key pattern cannot be empty. Please check 'sonar.issue.enforce.multicriteria' settings");

    newIssueFilter(newSettings(Collections.emptyList(), asList("", "**")));
  }

  private IssueFilter newIssueFilter(MapSettings settings) {
    when(settingsRepository.getConfiguration()).thenReturn(settings.asConfig());
    return new IssueFilter(settingsRepository);
  }

  private static MapSettings newSettings(List<String> exclusionsProperties, List<String> inclusionsProperties) {
    MapSettings settings = new MapSettings();
    if (!exclusionsProperties.isEmpty()) {
      addProperties(exclusionsProperties, "ignore", settings);
    }
    if (!inclusionsProperties.isEmpty()) {
      addProperties(inclusionsProperties, "enforce", settings);
    }
    return settings;
  }

  private static void addProperties(List<String> properties, String property, Settings settings) {
    if (!properties.isEmpty()) {
      List<Integer> indexes = new ArrayList<>();
      int index = 1;
      for (int i = 0; i < properties.size(); i += 2) {
        settings.setProperty("sonar.issue." + property + ".multicriteria." + index + ".ruleKey", properties.get(i));
        settings.setProperty("sonar.issue." + property + ".multicriteria." + index + ".resourceKey", properties.get(i + 1));
        indexes.add(index);
        index++;
      }
      settings.setProperty("sonar.issue." + property + ".multicriteria", Joiner.on(",").join(indexes));
    }
  }

}
