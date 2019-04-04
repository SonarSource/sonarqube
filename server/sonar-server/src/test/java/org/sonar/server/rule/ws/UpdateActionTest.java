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
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.RuleUpdater;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.sonar.api.server.debt.DebtRemediationFunction.Type.LINEAR;
import static org.sonar.api.server.debt.DebtRemediationFunction.Type.LINEAR_OFFSET;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.rule.RuleTesting.setSystemTags;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_KEY;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_MARKDOWN_NOTE;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_ORGANIZATION;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_REMEDIATION_FN_BASE_EFFORT;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_REMEDIATION_FN_GAP_MULTIPLIER;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_REMEDIATION_FN_TYPE;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_TAGS;
import static org.sonar.test.JsonAssert.assertJson;

public class UpdateActionTest {

  private static final long PAST = 10000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create();

  @Rule
  public EsTester es = EsTester.create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();
  private EsClient esClient = es.client();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private Languages languages = new Languages();
  private RuleMapper mapper = new RuleMapper(languages, createMacroInterpreter());
  private RuleIndexer ruleIndexer = new RuleIndexer(esClient, dbClient);
  private RuleUpdater ruleUpdater = new RuleUpdater(dbClient, ruleIndexer, System2.INSTANCE);
  private WsAction underTest = new UpdateAction(dbClient, ruleUpdater, mapper, userSession, new RuleWsSupport(db.getDbClient(), userSession, defaultOrganizationProvider));
  private WsActionTester ws = new WsActionTester(underTest);

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
    RuleDefinitionDto templateRule = db.rules().insert(
      r -> r.setRuleKey(RuleKey.of("java", "S001")),
      r -> r.setIsTemplate(true),
      r -> r.setCreatedAt(PAST),
      r -> r.setUpdatedAt(PAST));
    db.rules().insertRuleParam(templateRule, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*"));
    RuleDefinitionDto customRule = db.rules().insert(
      r -> r.setRuleKey(RuleKey.of("java", "MY_CUSTOM")),
      r -> r.setName("Old custom"),
      r -> r.setDescription("Old description"),
      r -> r.setSeverity(Severity.MINOR),
      r -> r.setStatus(RuleStatus.BETA),
      r -> r.setTemplateId(templateRule.getId()),
      r -> r.setLanguage("js"),
      r -> r.setCreatedAt(PAST),
      r -> r.setUpdatedAt(PAST));
    db.rules().insertRuleParam(customRule, param -> param.setName("regex").setType("a").setDescription("Reg ex"));

    TestResponse request = ws.newRequest().setMethod("POST")
      .setParam("key", customRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdown_description", "Description")
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
  public void update_tags_for_default_organization() {
    logInAsQProfileAdministrator();

    RuleDefinitionDto rule = db.rules().insert(setSystemTags("stag1", "stag2"));
    db.rules().insertOrUpdateMetadata(rule, db.getDefaultOrganization(), setTags("tag1", "tag2"), m -> m.setNoteData(null).setNoteUserUuid(null));

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
  public void update_tags_for_specific_organization() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQProfileAdministrator(organization.getUuid());

    RuleDefinitionDto rule = db.rules().insert(setSystemTags("stag1", "stag2"));
    db.rules().insertOrUpdateMetadata(rule, organization, setTags("tagAlt1", "tagAlt2"), m -> m.setNoteData(null).setNoteUserUuid(null));

    Rules.UpdateResponse result = ws.newRequest().setMethod("POST")
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(PARAM_TAGS, "tag2,tag3")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .executeProtobuf(Rules.UpdateResponse.class);

    Rules.Rule updatedRule = result.getRule();
    assertThat(updatedRule).isNotNull();

    // check response
    assertThat(updatedRule.getKey()).isEqualTo(rule.getKey().toString());
    assertThat(updatedRule.getSysTags().getSysTagsList()).containsExactly(rule.getSystemTags().toArray(new String[0]));
    assertThat(updatedRule.getTags().getTagsList()).containsExactly("tag2", "tag3");

    // check database
    RuleMetadataDto metadataOfSpecificOrg = db.getDbClient().ruleDao().selectMetadataByKey(db.getSession(), rule.getKey(), organization)
      .orElseThrow(() -> new IllegalStateException("Cannot load metadata"));
    assertThat(metadataOfSpecificOrg.getTags()).containsExactly("tag2", "tag3");
  }

  @Test
  public void update_rule_remediation_function() {
    OrganizationDto organization = db.organizations().insert();
    logInAsQProfileAdministrator(organization.getUuid());

    RuleDefinitionDto rule = db.rules().insert(
      r -> r.setDefRemediationFunction(LINEAR.toString()),
      r -> r.setDefRemediationGapMultiplier("10d"),
      r -> r.setDefRemediationBaseEffort(null));

    String newOffset = LINEAR_OFFSET.toString();
    String newMultiplier = "15d";
    String newEffort = "5min";

    Rules.UpdateResponse result = ws.newRequest().setMethod("POST")
      .setParam("key", rule.getKey().toString())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_REMEDIATION_FN_TYPE, newOffset)
      .setParam(PARAM_REMEDIATION_FN_GAP_MULTIPLIER, newMultiplier)
      .setParam(PARAM_REMEDIATION_FN_BASE_EFFORT, newEffort)
      .executeProtobuf(Rules.UpdateResponse.class);

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

    // check database
    RuleMetadataDto metadataOfSpecificOrg = db.getDbClient().ruleDao().selectMetadataByKey(db.getSession(), rule.getKey(), organization)
      .orElseThrow(() -> new IllegalStateException("Cannot load metadata"));
    assertThat(metadataOfSpecificOrg.getRemediationFunction()).isEqualTo(newOffset);
    assertThat(metadataOfSpecificOrg.getRemediationGapMultiplier()).isEqualTo(newMultiplier);
    assertThat(metadataOfSpecificOrg.getRemediationBaseEffort()).isEqualTo(newEffort);
  }

  @Test
  public void update_note() {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto rule = db.rules().insert();
    UserDto userHavingUpdatingNote = db.users().insertUser();
    db.rules().insertOrUpdateMetadata(rule, userHavingUpdatingNote, organization, m -> m.setNoteData("old data"));
    UserDto userAuthenticated = db.users().insertUser();
    userSession.logIn(userAuthenticated).addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    Rules.UpdateResponse result = ws.newRequest().setMethod("POST")
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(PARAM_MARKDOWN_NOTE, "new data")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .executeProtobuf(Rules.UpdateResponse.class);

    Rules.Rule updatedRule = result.getRule();

    // check response
    assertThat(updatedRule.getMdNote()).isEqualTo("new data");
    assertThat(updatedRule.getNoteLogin()).isEqualTo(userAuthenticated.getLogin());

    // check database
    RuleMetadataDto metadataOfSpecificOrg = db.getDbClient().ruleDao().selectMetadataByKey(db.getSession(), rule.getKey(), organization).get();
    assertThat(metadataOfSpecificOrg.getNoteData()).isEqualTo("new data");
    assertThat(metadataOfSpecificOrg.getNoteUserUuid()).isEqualTo(userAuthenticated.getUuid());
  }

  @Test
  public void fail_to_update_custom_when_description_is_empty() {
    logInAsQProfileAdministrator();
    RuleDefinitionDto templateRule = db.rules().insert(
      r -> r.setRuleKey(RuleKey.of("java", "S001")),
      r -> r.setIsTemplate(true),
      r -> r.setCreatedAt(PAST),
      r -> r.setUpdatedAt(PAST));
    RuleDefinitionDto customRule = db.rules().insert(
      r -> r.setRuleKey(RuleKey.of("java", "MY_CUSTOM")),
      r -> r.setName("Old custom"),
      r -> r.setDescription("Old description"),
      r -> r.setTemplateId(templateRule.getId()),
      r -> r.setCreatedAt(PAST),
      r -> r.setUpdatedAt(PAST));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The description is missing");

    ws.newRequest().setMethod("POST")
      .setParam("key", customRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdown_description", "")
      .execute();
  }

  @Test
  public void throw_IllegalArgumentException_if_trying_to_update_builtin_rule_description() {
    logInAsQProfileAdministrator();
    RuleDefinitionDto rule = db.rules().insert();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Not a custom rule");

    ws.newRequest().setMethod("POST")
      .setParam("key", rule.getKey().toString())
      .setParam("name", rule.getName())
      .setParam("markdown_description", "New description")
      .execute();

  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    ws.newRequest().setMethod("POST").execute();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);

    ws.newRequest().setMethod("POST").execute();
  }

  @Test
  public void throw_NotFoundException_if_organization_cannot_be_found() {
    logInAsQProfileAdministrator();
    RuleDefinitionDto rule = db.rules().insert();

    expectedException.expect(NotFoundException.class);

    ws.newRequest().setMethod("POST")
      .setParam("key", rule.getKey().toString())
      .setParam("organization", "foo")
      .execute();
  }

  private void logInAsQProfileAdministrator() {
    logInAsQProfileAdministrator(db.getDefaultOrganization().getUuid());
  }

  private void logInAsQProfileAdministrator(String orgUuid) {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, orgUuid);
  }

  private static MacroInterpreter createMacroInterpreter() {
    MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
    doAnswer(returnsFirstArg()).when(macroInterpreter).interpret(anyString());
    return macroInterpreter;
  }
}
