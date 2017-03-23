/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DeactivateRulesActionTest {

  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private RuleActivator ruleActivator = mock(RuleActivator.class);
  private QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSession, TestDefaultOrganizationProvider.from(dbTester));
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);
  private DeactivateRulesAction underTest = new DeactivateRulesAction(null, null);
  private WsActionTester wsActionTester = new WsActionTester(underTest);

  @Test
  public void define_bulk_deactivate_rule_action() {
    WebService.Action definition = wsActionTester.getDef();
    assertThat(definition).isNotNull();
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder(
      "types",
      "template_key",
      "languages",
      "is_template",
      "inheritance",
      "qprofile",
      "tags",
      "asc",
      "q",
      "active_severities",
      "s",
      "repositories",
      "profile_key",
      "statuses",
      "rule_key",
      "available_since",
      "activation",
      "severities"
    );
  }
}