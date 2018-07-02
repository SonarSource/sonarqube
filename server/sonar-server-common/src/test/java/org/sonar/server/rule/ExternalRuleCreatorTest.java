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
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.rule.index.RuleIndexer;

import static org.assertj.core.api.Assertions.assertThat;

public class ExternalRuleCreatorTest {

  @org.junit.Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @org.junit.Rule
  public EsTester es = EsTester.create();

  private RuleIndexer indexer = new RuleIndexer(es.client(), dbTester.getDbClient());
  private ExternalRuleCreator underTest = new ExternalRuleCreator(dbTester.getDbClient(), System2.INSTANCE, indexer);
  private DbSession dbSession = dbTester.getSession();

  @Test
  public void create_external_rule() {
    RuleKey ruleKey = RuleKey.of("eslint", "no-cond-assign");
    NewExternalRule externalRule = new NewExternalRule.Builder()
      .setKey(ruleKey)
      .setPluginKey("eslint")
      .setName("name")
      .build();

    RuleDto rule1 = underTest.persistAndIndex(dbSession, externalRule);

    assertThat(rule1).isNotNull();
    assertThat(rule1.isExternal()).isTrue();
    assertThat(rule1.getId()).isGreaterThan(0);
    assertThat(rule1.getKey()).isEqualTo(ruleKey);
    assertThat(rule1.getPluginKey()).isEqualTo("eslint");
    assertThat(rule1.getName()).isEqualTo("name");
    assertThat(rule1.getType()).isEqualTo(0);
  }

}
