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
package org.sonar.server.rule.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Rules;

import static org.assertj.core.api.Assertions.assertThat;

public class ListActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  ListAction underTest = new ListAction(dbTester.getDbClient());

  WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void define() {
    WebService.Action def = tester.getDef();
    assertThat(def.params()).isEmpty();
  }

  @Test
  public void return_rules_in_protobuf() {
    dbTester.rules().insert(RuleTesting.newRule(RuleKey.of("java", "S001")).setConfigKey(null).setName(null));
    dbTester.rules().insert(RuleTesting.newRule(RuleKey.of("java", "S002")).setConfigKey("I002").setName("Rule Two"));
    dbTester.getSession().commit();

    Rules.ListResponse listResponse = tester.newRequest()
      .executeProtobuf(Rules.ListResponse.class);

    assertThat(listResponse.getRulesCount()).isEqualTo(2);

    assertThat(listResponse.getRules(0).getKey()).isEqualTo("S001");
    assertThat(listResponse.getRules(0).getInternalKey()).isEqualTo("");
    assertThat(listResponse.getRules(0).getName()).isEqualTo("");
    assertThat(listResponse.getRules(1).getKey()).isEqualTo("S002");
    assertThat(listResponse.getRules(1).getInternalKey()).isEqualTo("I002");
    assertThat(listResponse.getRules(1).getName()).isEqualTo("Rule Two");
  }
}
