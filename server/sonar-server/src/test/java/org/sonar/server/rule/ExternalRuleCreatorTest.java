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
package org.sonar.server.rule;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.computation.task.projectanalysis.issue.NewExternalRule;
import org.sonar.server.computation.task.projectanalysis.issue.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rules.RuleType.BUG;

public class ExternalRuleCreatorTest {

  @org.junit.Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private ExternalRuleCreator underTest = new ExternalRuleCreator(dbTester.getDbClient(), System2.INSTANCE);
  private DbSession dbSession = dbTester.getSession();

  @Test
  public void create_external_rule() {

    RuleKey ruleKey = RuleKey.of("eslint", "no-cond-assign");
    NewExternalRule externalRule = new NewExternalRule.Builder()
      .setKey(ruleKey)
      .setPluginKey("eslint")
      .setName("disallow assignment operators in conditional statements (no-cond-assign)")
      .setDescriptionUrl("https://eslint.org/docs/rules/no-cond-assign")
      .setSeverity(BLOCKER)
      .setType(BUG)
      .build();

    Rule rule1 = underTest.create(dbSession, externalRule);

    assertThat(rule1).isNotNull();
    assertThat(rule1.isExternal()).isTrue();
    assertThat(rule1.getId()).isGreaterThan(0);
    assertThat(rule1.getKey()).isEqualTo(ruleKey);
    assertThat(rule1.getPluginKey()).isEqualTo("eslint");
    assertThat(rule1.getName()).isEqualTo("disallow assignment operators in conditional statements (no-cond-assign)");
    assertThat(rule1.getType()).isEqualTo(BUG);

  }

}
