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
package org.sonar.server.computation.task.projectanalysis.issue;

import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.task.projectanalysis.component.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.rule.RuleTesting.XOO_X1;

public class RuleTypeCopierTest {

  DumbRule rule = new DumbRule(XOO_X1);

  @org.junit.Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule().add(rule);

  DefaultIssue issue = new DefaultIssue().setRuleKey(rule.getKey());
  RuleTypeCopier underTest = new RuleTypeCopier(ruleRepository);

  @Test
  public void copy_rule_type_if_missing() {
    rule.setType(RuleType.BUG);

    underTest.onIssue(mock(Component.class), issue);

    assertThat(issue.type()).isEqualTo(RuleType.BUG);
  }

  @Test
  public void do_not_copy_type_if_present() {
    rule.setType(RuleType.BUG);
    issue.setType(RuleType.VULNERABILITY);

    underTest.onIssue(mock(Component.class), issue);

    assertThat(issue.type()).isEqualTo(RuleType.VULNERABILITY);
  }
}
