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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.collect.Sets;
import java.util.Collections;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.rule.RuleTesting.XOO_X1;
import static org.sonar.db.rule.RuleTesting.XOO_X2;

public class RuleTagsCopierTest {

  DumbRule rule = new DumbRule(XOO_X1);
  DumbRule externalRule = new DumbRule(XOO_X2).setIsExternal(true);

  @org.junit.Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule().add(rule).add(externalRule);

  DefaultIssue issue = new DefaultIssue().setRuleKey(rule.getKey());
  DefaultIssue externalIssue = new DefaultIssue().setRuleKey(externalRule.getKey());
  RuleTagsCopier underTest = new RuleTagsCopier(ruleRepository);

  @Test
  public void copy_tags_if_new_issue() {
    rule.setTags(Sets.newHashSet("bug", "performance"));
    issue.setNew(true);

    underTest.onIssue(mock(Component.class), issue);

    assertThat(issue.tags()).containsExactly("bug", "performance");
  }

  @Test
  public void copy_tags_if_new_external_issue() {
    externalRule.setTags(Sets.newHashSet("es_lint", "java"));
    externalIssue.setNew(true);

    underTest.onIssue(mock(Component.class), externalIssue);

    assertThat(externalIssue.tags()).containsExactly("es_lint", "java");
  }

  @Test
  public void do_not_copy_tags_if_existing_issue() {
    rule.setTags(Sets.newHashSet("bug", "performance"));
    issue.setNew(false).setTags(asList("misra"));

    underTest.onIssue(mock(Component.class), issue);

    assertThat(issue.tags()).containsExactly("misra");
  }

  @Test
  public void do_not_copy_tags_if_existing_issue_without_tags() {
    rule.setTags(Sets.newHashSet("bug", "performance"));
    issue.setNew(false).setTags(Collections.emptyList());

    underTest.onIssue(mock(Component.class), issue);

    assertThat(issue.tags()).isEmpty();
  }
}
