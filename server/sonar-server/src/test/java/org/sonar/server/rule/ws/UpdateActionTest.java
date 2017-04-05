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

package org.sonar.server.rule.ws;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.RuleUpdater;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.sonar.api.server.debt.DebtRemediationFunction.Type.LINEAR;
import static org.sonar.api.server.debt.DebtRemediationFunction.Type.LINEAR_OFFSET;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.rule.ws.UpdateAction.DEPRECATED_PARAM_REMEDIATION_FN_COEFF;
import static org.sonar.server.rule.ws.UpdateAction.DEPRECATED_PARAM_REMEDIATION_FN_OFFSET;
import static org.sonar.server.rule.ws.UpdateAction.DEPRECATED_PARAM_REMEDIATION_FN_TYPE;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_KEY;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_REMEDIATION_FN_BASE_EFFORT;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_REMEDIATION_FN_GAP_MULTIPLIER;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_REMEDIATION_FN_TYPE;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;

public class UpdateActionTest {

  @org.junit.Rule
  public DbTester dbTester = DbTester.create();
  @org.junit.Rule
  public EsTester esTester = new EsTester(
    new RuleIndexDefinition(new MapSettings()));
  @org.junit.Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();
  private EsClient esClient = esTester.client();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  private Languages languages = new Languages();
  private RuleMapper mapper = new RuleMapper(languages, macroInterpreter);
  private RuleIndexer ruleIndexer = new RuleIndexer(esClient, dbClient);
  private RuleUpdater ruleUpdater = new RuleUpdater(dbClient, ruleIndexer, System2.INSTANCE, defaultOrganizationProvider);
  private RuleWsSupport ruleWsSupport = new RuleWsSupport(dbClient, userSession, defaultOrganizationProvider);
  private WsAction underTest = new UpdateAction(dbClient, ruleUpdater, mapper, userSession, ruleWsSupport, defaultOrganizationProvider);
  private WsActionTester actionTester = new WsActionTester(underTest);
  private OrganizationDto defaultOrganization;

  private RuleIndex ruleIndex = new RuleIndex(esClient);

  @Before
  public void setUp() {
    defaultOrganization = dbTester.getDefaultOrganization();
    logInAsQProfileAdministrator();
  }

  @Test
  public void update_rule_remediation_function() throws IOException {
    doReturn("interpreted").when(macroInterpreter).interpret(anyString());

    RuleDefinitionDto rule = dbTester.rules().insert(
      r -> r.setDefRemediationFunction(LINEAR.toString()),
      r -> r.setDefRemediationGapMultiplier("10d"),
      r -> r.setDefRemediationBaseEffort(null));

    String newOffset = LINEAR_OFFSET.toString();
    String newMultiplier = "15d";
    String newEffort = "5min";

    TestRequest request = actionTester.newRequest().setMethod("POST")
      .setMediaType(PROTOBUF)
      .setParam("key", rule.getKey().toString())
      .setParam(PARAM_REMEDIATION_FN_TYPE, newOffset)
      .setParam(PARAM_REMEDIATION_FN_GAP_MULTIPLIER, newMultiplier)
      .setParam(PARAM_REMEDIATION_FN_BASE_EFFORT, newEffort);
    TestResponse response = request.execute();
    Rules.UpdateResponse result = Rules.UpdateResponse.parseFrom(response.getInputStream());

    Rules.Rule updatedRule = result.getRule();
    assertThat(updatedRule).isNotNull();

    assertThat(updatedRule.getKey()).isEqualTo(rule.getKey().toString());
    assertThat(updatedRule.getDefaultRemFnType()).isEqualTo(rule.getDefRemediationFunction());
    assertThat(updatedRule.getDefaultRemFnGapMultiplier()).isEqualTo(rule.getDefRemediationGapMultiplier());
    assertThat(updatedRule.getDefaultRemFnBaseEffort()).isEqualTo("");
    assertThat(updatedRule.getGapDescription()).isEqualTo(rule.getGapDescription());

    assertThat(updatedRule.getRemFnType()).isEqualTo(newOffset);
    assertThat(updatedRule.getRemFnGapMultiplier()).isEqualTo(newMultiplier);
    assertThat(updatedRule.getRemFnBaseEffort()).isEqualTo(newEffort);
  }

  @Test
  public void update_custom_rule_with_deprecated_remediation_function_parameters() throws Exception {
    doReturn("interpreted").when(macroInterpreter).interpret(anyString());

    RuleDefinitionDto rule = RuleTesting.newRule()
      .setDefRemediationFunction(LINEAR_OFFSET.toString())
      .setDefRemediationGapMultiplier("10d")
      .setDefRemediationBaseEffort("5min");
    dbTester.rules().insert(rule);

    String newType = LINEAR_OFFSET.toString();
    String newCoeff = "11d";
    String newOffset = "6min";

    TestRequest request = actionTester.newRequest().setMethod("POST")
      .setMediaType(PROTOBUF)
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(DEPRECATED_PARAM_REMEDIATION_FN_TYPE, newType)
      .setParam(DEPRECATED_PARAM_REMEDIATION_FN_COEFF, newCoeff)
      .setParam(DEPRECATED_PARAM_REMEDIATION_FN_OFFSET, newOffset);
    TestResponse response = request.execute();
    Rules.UpdateResponse result = Rules.UpdateResponse.parseFrom(response.getInputStream());

    Rules.Rule updatedRule = result.getRule();
    assertThat(updatedRule).isNotNull();

    assertThat(updatedRule.getKey()).isEqualTo(rule.getKey().toString());
    assertThat(updatedRule.getDefaultRemFnType()).isEqualTo(rule.getDefRemediationFunction());
    assertThat(updatedRule.getDefaultRemFnGapMultiplier()).isEqualTo(rule.getDefRemediationGapMultiplier());
    assertThat(updatedRule.getDefaultRemFnBaseEffort()).isEqualTo(rule.getDefRemediationBaseEffort());
    assertThat(updatedRule.getEffortToFixDescription()).isEqualTo(rule.getGapDescription());

    assertThat(updatedRule.getRemFnType()).isEqualTo(newType);
    assertThat(updatedRule.getDebtRemFnCoeff()).isEqualTo(newCoeff);
    assertThat(updatedRule.getDebtRemFnOffset()).isEqualTo(newOffset);

    assertThat(updatedRule.getRemFnType()).isEqualTo(newType);
    assertThat(updatedRule.getRemFnGapMultiplier()).isEqualTo(newCoeff);
    assertThat(updatedRule.getRemFnBaseEffort()).isEqualTo(newOffset);
    assertThat(updatedRule.getGapDescription()).isEqualTo(rule.getGapDescription());
  }

  private void logInAsQProfileAdministrator() {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganization.getUuid());
  }
}
