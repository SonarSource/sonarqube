/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.text.MacroInterpreter;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.rule.RuleUpdater;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.sonar.api.server.debt.DebtRemediationFunction.Type.LINEAR;
import static org.sonar.api.server.debt.DebtRemediationFunction.Type.LINEAR_OFFSET;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.db.rule.RuleTesting.setSystemTags;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_KEY;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_MARKDOWN_NOTE;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_REMEDIATION_FN_BASE_EFFORT;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_REMEDIATION_FN_GAP_MULTIPLIER;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_REMEDIATION_FN_TYPE;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_TAGS;
import static org.sonar.test.JsonAssert.assertJson;

public class UpdateActionIT {

  private static final long PAST = 10000L;

  @Rule
  public DbTester db = DbTester.create();

  @Rule
  public EsTester es = EsTester.create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final EsClient esClient = es.client();
  private final RuleDescriptionFormatter ruleDescriptionFormatter = new RuleDescriptionFormatter();

  private final Languages languages = new Languages();
  private final RuleMapper mapper = new RuleMapper(languages, createMacroInterpreter(), ruleDescriptionFormatter);
  private final RuleIndexer ruleIndexer = new RuleIndexer(esClient, dbClient);
  private final UuidFactoryFast uuidFactory = UuidFactoryFast.getInstance();

  private final RuleUpdater ruleUpdater = new RuleUpdater(dbClient, ruleIndexer, uuidFactory, System2.INSTANCE);
  private final WsAction underTest = new UpdateAction(dbClient, ruleUpdater, mapper, userSession, new RuleWsSupport(db.getDbClient(), userSession));
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void check_definition() {
    assertThat(ws.getDef().isPost()).isTrue();
    assertThat(ws.getDef().isInternal()).isFalse();
    assertThat(ws.getDef().responseExampleAsString()).isNotNull();
    assertThat(ws.getDef().description()).isNotNull();
  }

  @Test
  public void update_custom_rule() {
    logInAsQProfileAdministrator();
    RuleDto templateRule = db.rules().insert(
      r -> r.setRuleKey(RuleKey.of("java", "S001")),
      r -> r.setIsTemplate(true),
      r -> r.setNoteUserUuid(null),
      r -> r.setCreatedAt(PAST),
      r -> r.setUpdatedAt(PAST));
    db.rules().insertRuleParam(templateRule, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*"));

    RuleDto customRule = newRule(RuleKey.of("java", "MY_CUSTOM"), createRuleDescriptionSectionDto())
      .setName("Old custom")
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.BETA)
      .setTemplateUuid(templateRule.getUuid())
      .setLanguage("js")
      .setNoteUserUuid(null)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST);
    customRule = db.rules().insert(customRule);
    db.rules().insertRuleParam(customRule, param -> param.setName("regex").setType("a").setDescription("Reg ex"));

    TestResponse request = ws.newRequest().setMethod("POST")
      .setParam("key", customRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdownDescription", "Description")
      .setParam("severity", "MAJOR")
      .setParam("status", "BETA")
      .setParam("params", "regex=a.*")
      .execute();

    assertJson(request.getInput()).isSimilarTo("{\n" +
      "  \"rule\": {\n" +
      "    \"key\": \"java:MY_CUSTOM\",\n" +
      "    \"repo\": \"java\",\n" +
      "    \"name\": \"My custom rule\",\n" +
      "    \"htmlDesc\": \"Description\",\n" +
      "    \"severity\": \"MAJOR\",\n" +
      "    \"status\": \"BETA\",\n" +
      "    \"isTemplate\": false,\n" +
      "    \"templateKey\": \"java:S001\",\n" +
      "    \"params\": [\n" +
      "      {\n" +
      "        \"key\": \"regex\",\n" +
      "        \"htmlDesc\": \"Reg ex\",\n" +
      "        \"defaultValue\": \"a.*\"\n" +
      "      }\n" +
      "    ]\n" +
      "  }\n" +
      "}\n");
  }

  @Test
  public void update_tags() {
    logInAsQProfileAdministrator();

    RuleDto rule = db.rules().insert(setSystemTags("stag1", "stag2"), setTags("tag1", "tag2"), r -> r.setNoteData(null).setNoteUserUuid(null));

    Rules.UpdateResponse result = ws.newRequest().setMethod("POST")
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(PARAM_TAGS, "tag2,tag3")
      .executeProtobuf(Rules.UpdateResponse.class);

    Rules.Rule updatedRule = result.getRule();
    assertThat(updatedRule).isNotNull();

    assertThat(updatedRule.getKey()).isEqualTo(rule.getKey().toString());
    assertThat(updatedRule.getSysTags().getSysTagsList()).containsExactly(rule.getSystemTags().toArray(new String[0]));
    assertThat(updatedRule.getTags().getTagsList()).containsExactly("tag2", "tag3");
  }

  @Test
  public void update_rule_remediation_function() {
    logInAsQProfileAdministrator();

    RuleDto rule = db.rules().insert(
      r -> r.setDefRemediationFunction(LINEAR.toString()),
      r -> r.setDefRemediationGapMultiplier("10d"),
      r -> r.setDefRemediationBaseEffort(null),
      r -> r.setNoteUserUuid(null));

    String newOffset = LINEAR_OFFSET.toString();
    String newMultiplier = "15d";
    String newEffort = "5min";

    Rules.UpdateResponse result = ws.newRequest().setMethod("POST")
      .setParam("key", rule.getKey().toString())
      .setParam(PARAM_REMEDIATION_FN_TYPE, newOffset)
      .setParam(PARAM_REMEDIATION_FN_GAP_MULTIPLIER, newMultiplier)
      .setParam(PARAM_REMEDIATION_FN_BASE_EFFORT, newEffort)
      .executeProtobuf(Rules.UpdateResponse.class);

    Rules.Rule updatedRule = result.getRule();
    assertThat(updatedRule).isNotNull();

    assertThat(updatedRule.getKey()).isEqualTo(rule.getKey().toString());
    assertThat(updatedRule.getDefaultRemFnType()).isEqualTo(rule.getDefRemediationFunction());
    assertThat(updatedRule.getDefaultRemFnGapMultiplier()).isEqualTo(rule.getDefRemediationGapMultiplier());
    assertThat(updatedRule.getDefaultRemFnBaseEffort()).isEmpty();
    assertThat(updatedRule.getGapDescription()).isEqualTo(rule.getGapDescription());

    assertThat(updatedRule.getRemFnType()).isEqualTo(newOffset);
    assertThat(updatedRule.getRemFnGapMultiplier()).isEqualTo(newMultiplier);
    assertThat(updatedRule.getRemFnBaseEffort()).isEqualTo(newEffort);

    // check database
    RuleDto updatedRuleDto = db.getDbClient().ruleDao().selectByKey(db.getSession(), rule.getKey())
      .orElseThrow(() -> new IllegalStateException("Cannot load metadata"));
    assertThat(updatedRuleDto.getRemediationFunction()).isEqualTo(newOffset);
    assertThat(updatedRuleDto.getRemediationGapMultiplier()).isEqualTo(newMultiplier);
    assertThat(updatedRuleDto.getRemediationBaseEffort()).isEqualTo(newEffort);
  }

  @Test
  public void update_note() {
    UserDto userHavingUpdatingNote = db.users().insertUser();
    RuleDto rule = db.rules().insert(m -> m.setNoteData("old data").setNoteUserUuid(userHavingUpdatingNote.getUuid()));
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addPermission(ADMINISTER_QUALITY_PROFILES);

    Rules.UpdateResponse result = ws.newRequest().setMethod("POST")
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(PARAM_MARKDOWN_NOTE, "new data")
      .executeProtobuf(Rules.UpdateResponse.class);

    Rules.Rule updatedRule = result.getRule();

    // check response
    assertThat(updatedRule.getMdNote()).isEqualTo("new data");
    assertThat(updatedRule.getNoteLogin()).isEqualTo(userAuthenticated.getLogin());

    // check database
    RuleDto updatedRuleDto = db.getDbClient().ruleDao().selectByKey(db.getSession(), rule.getKey()).get();
    assertThat(updatedRuleDto.getNoteData()).isEqualTo("new data");
    assertThat(updatedRuleDto.getNoteUserUuid()).isEqualTo(userAuthenticated.getUuid());
  }

  @Test
  public void fail_to_update_custom_when_description_is_empty() {
    logInAsQProfileAdministrator();
    RuleDto templateRule = db.rules().insert(
      r -> r.setRuleKey(RuleKey.of("java", "S001")),
      r -> r.setIsTemplate(true),
      r -> r.setCreatedAt(PAST),
      r -> r.setUpdatedAt(PAST));

    RuleDto customRule = db.rules().insert(
      newRule(RuleKey.of("java", "MY_CUSTOM"), createRuleDescriptionSectionDto())
        .setRuleKey(RuleKey.of("java", "MY_CUSTOM"))
        .setName("Old custom")
        .setTemplateUuid(templateRule.getUuid())
        .setCreatedAt(PAST)
        .setUpdatedAt(PAST));

    assertThatThrownBy(() -> {
      ws.newRequest().setMethod("POST")
        .setParam("key", customRule.getKey().toString())
        .setParam("name", "My custom rule")
        .setParam("markdownDescription", "")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The description is missing");
  }

  @Test
  public void throw_IllegalArgumentException_if_trying_to_update_builtin_rule_description() {
    logInAsQProfileAdministrator();
    RuleDto rule = db.rules().insert();

    assertThatThrownBy(() -> {
      ws.newRequest().setMethod("POST")
        .setParam("key", rule.getKey().toString())
        .setParam("name", rule.getName())
        .setParam("markdown_description", "New description")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Not a custom rule");
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() {
    userSession.logIn();

    assertThatThrownBy(() -> {
      ws.newRequest().setMethod("POST").execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    assertThatThrownBy(() -> {
      ws.newRequest().setMethod("POST").execute();
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void returnRuleCleanCodeFields_whenEndpointIsCalled() {
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addPermission(ADMINISTER_QUALITY_PROFILES);

    RuleDto rule = db.rules()
      .insert();

    Rules.UpdateResponse updateResponse = ws.newRequest().setMethod("POST")
      .setParam("key", rule.getKey().toString())
      .executeProtobuf(Rules.UpdateResponse.class);

    // mandatory fields
    assertThat(updateResponse.getRule())
      .extracting(r -> r.getImpacts().getImpactsList().stream().findFirst().orElseThrow(() -> new IllegalStateException("Impact is a mandatory field in the response.")))
      .extracting(Common.Impact::getSoftwareQuality, Common.Impact::getSeverity)
      .containsExactly(Common.SoftwareQuality.MAINTAINABILITY, Common.ImpactSeverity.HIGH);

    // selected fields
    assertThat(updateResponse.getRule()).extracting(Rules.Rule::getCleanCodeAttribute).isEqualTo(Common.CleanCodeAttribute.CLEAR);
    assertThat(updateResponse.getRule()).extracting(Rules.Rule::getCleanCodeAttributeCategory).isEqualTo(Common.CleanCodeAttributeCategory.INTENTIONAL);
  }

  private void logInAsQProfileAdministrator() {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES);
  }

  private RuleDescriptionSectionDto createRuleDescriptionSectionDto() {
    return createDefaultRuleDescriptionSection(uuidFactory.create(), "Old description");
  }

  private static MacroInterpreter createMacroInterpreter() {
    MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
    doAnswer(returnsFirstArg()).when(macroInterpreter).interpret(anyString());
    return macroInterpreter;
  }
}
