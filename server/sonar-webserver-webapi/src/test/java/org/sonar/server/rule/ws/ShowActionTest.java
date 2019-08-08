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

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.Rule;
import org.sonarqube.ws.Rules.ShowResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.organization.OrganizationDto.Subscription.PAID;
import static org.sonar.db.rule.RuleDto.Format.MARKDOWN;
import static org.sonar.db.rule.RuleTesting.newCustomRule;
import static org.sonar.db.rule.RuleTesting.newTemplateRule;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonar.server.language.LanguageTesting.newLanguage;
import static org.sonar.server.rule.ws.ShowAction.PARAM_ACTIVES;
import static org.sonar.server.rule.ws.ShowAction.PARAM_KEY;
import static org.sonar.server.rule.ws.ShowAction.PARAM_ORGANIZATION;
import static org.sonarqube.ws.Common.RuleType.UNKNOWN;
import static org.sonarqube.ws.Common.RuleType.VULNERABILITY;

public class ShowActionTest {

  private static final String INTERPRETED = "interpreted";

  @org.junit.Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @org.junit.Rule
  public DbTester db = DbTester.create();
  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  private MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  private Languages languages = new Languages(newLanguage("xoo", "Xoo"));

  private WsActionTester ws = new WsActionTester(
    new ShowAction(db.getDbClient(), new RuleMapper(languages, macroInterpreter),
      new ActiveRuleCompleter(db.getDbClient(), languages),
      new RuleWsSupport(db.getDbClient(), userSession, TestDefaultOrganizationProvider.from(db))));

  @Before
  public void before() {
    doReturn(INTERPRETED).when(macroInterpreter).interpret(anyString());
  }

  @Test
  public void show_rule_key() {
    RuleDefinitionDto rule = db.rules().insert();

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getRule()).extracting(Rule::getKey).isEqualTo(rule.getKey().toString());
  }

  @Test
  public void show_rule_with_basic_info() {
    RuleDefinitionDto rule = db.rules().insert();
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule);

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.getKey()).isEqualTo(rule.getKey().toString());
    assertThat(resultRule.getRepo()).isEqualTo(rule.getRepositoryKey());
    assertThat(resultRule.getName()).isEqualTo(rule.getName());
    assertThat(resultRule.getSeverity()).isEqualTo(rule.getSeverityString());
    assertThat(resultRule.getStatus().toString()).isEqualTo(rule.getStatus().toString());
    assertThat(resultRule.getInternalKey()).isEqualTo(rule.getConfigKey());
    assertThat(resultRule.getIsTemplate()).isEqualTo(rule.isTemplate());
    assertThat(resultRule.getLang()).isEqualTo(rule.getLanguage());
    assertThat(resultRule.getParams().getParamsList())
      .extracting(Rule.Param::getKey, Rule.Param::getHtmlDesc, Rule.Param::getDefaultValue)
      .containsExactlyInAnyOrder(tuple(ruleParam.getName(), ruleParam.getDescription(), ruleParam.getDefaultValue()));
  }

  @Test
  public void show_rule_tags_in_default_organization() {
    RuleDefinitionDto rule = db.rules().insert();
    RuleMetadataDto metadata = db.rules().insertOrUpdateMetadata(rule, db.getDefaultOrganization(), setTags("tag1", "tag2"), m -> m.setNoteData(null).setNoteUserUuid(null));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getRule().getTags().getTagsList())
      .containsExactly(metadata.getTags().toArray(new String[0]));
  }

  @Test
  public void show_rule_tags_in_specific_organization() {
    RuleDefinitionDto rule = db.rules().insert();
    OrganizationDto organization = db.organizations().insert();
    RuleMetadataDto metadata = db.rules().insertOrUpdateMetadata(rule, organization, setTags("tag1", "tag2"), m -> m.setNoteData(null).setNoteUserUuid(null));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getRule().getTags().getTagsList())
      .containsExactly(metadata.getTags().toArray(new String[0]));
  }

  @Test
  public void show_rule_with_note_login() {
    RuleDefinitionDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    OrganizationDto organization = db.organizations().insert();
    db.rules().insertOrUpdateMetadata(rule, user, organization);

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getRule().getNoteLogin()).isEqualTo(user.getLogin());
  }

  @Test
  public void show_rule_with_default_debt_infos() {
    RuleDefinitionDto rule = db.rules().insert(r -> r
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setGapDescription("gap desc"));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.getDefaultRemFnType()).isEqualTo("LINEAR_OFFSET");
    assertThat(resultRule.getDefaultRemFnGapMultiplier()).isEqualTo("5d");
    assertThat(resultRule.getDefaultRemFnBaseEffort()).isEqualTo("10h");
    assertThat(resultRule.getGapDescription()).isEqualTo("gap desc");
    assertThat(resultRule.getRemFnType()).isEqualTo("LINEAR_OFFSET");
    assertThat(resultRule.getRemFnGapMultiplier()).isEqualTo("5d");
    assertThat(resultRule.getRemFnBaseEffort()).isEqualTo("10h");
    assertThat(resultRule.getRemFnOverloaded()).isFalse();
  }

  @Test
  public void show_rule_with_only_overridden_debt() {
    RuleDefinitionDto rule = db.rules().insert(r -> r
      .setDefRemediationFunction(null)
      .setDefRemediationGapMultiplier(null)
      .setDefRemediationBaseEffort(null));
    db.rules().insertOrUpdateMetadata(rule, db.getDefaultOrganization(),
      m -> m.setNoteData(null).setNoteUserUuid(null),
      m -> m
        .setRemediationFunction("LINEAR_OFFSET")
        .setRemediationGapMultiplier("5d")
        .setRemediationBaseEffort("10h"));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.hasDefaultRemFnType()).isFalse();
    assertThat(resultRule.hasDefaultRemFnGapMultiplier()).isFalse();
    assertThat(resultRule.hasDefaultRemFnBaseEffort()).isFalse();
    assertThat(resultRule.getRemFnType()).isEqualTo("LINEAR_OFFSET");
    assertThat(resultRule.getRemFnGapMultiplier()).isEqualTo("5d");
    assertThat(resultRule.getRemFnBaseEffort()).isEqualTo("10h");
    assertThat(resultRule.getRemFnOverloaded()).isTrue();
  }

  @Test
  public void show_rule_with_default_and_overridden_debt_infos() {
    RuleDefinitionDto rule = db.rules().insert(r -> r
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h"));
    db.rules().insertOrUpdateMetadata(rule, db.getDefaultOrganization(), m -> m.setNoteData(null).setNoteUserUuid(null),
      m -> m
        .setRemediationFunction("CONSTANT_ISSUE")
        .setRemediationGapMultiplier(null)
        .setRemediationBaseEffort("15h"));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.getDefaultRemFnType()).isEqualTo("LINEAR_OFFSET");
    assertThat(resultRule.getDefaultRemFnType()).isEqualTo("LINEAR_OFFSET");
    assertThat(resultRule.getDefaultRemFnGapMultiplier()).isEqualTo("5d");
    assertThat(resultRule.getDefaultRemFnBaseEffort()).isEqualTo("10h");
    assertThat(resultRule.getRemFnType()).isEqualTo("CONSTANT_ISSUE");
    assertThat(resultRule.hasRemFnGapMultiplier()).isFalse();
    assertThat(resultRule.getRemFnBaseEffort()).isEqualTo("15h");
    assertThat(resultRule.getRemFnOverloaded()).isTrue();
  }

  @Test
  public void show_rule_with_no_default_and_no_overridden_debt() {
    RuleDefinitionDto rule = db.rules().insert(r -> r
      .setDefRemediationFunction(null)
      .setDefRemediationGapMultiplier(null)
      .setDefRemediationBaseEffort(null));
    db.rules().insertOrUpdateMetadata(rule, db.getDefaultOrganization(), m -> m.setNoteData(null).setNoteUserUuid(null),
      m -> m
        .setRemediationFunction(null)
        .setRemediationGapMultiplier(null)
        .setRemediationBaseEffort(null));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.hasDefaultRemFnType()).isFalse();
    assertThat(resultRule.hasDefaultRemFnGapMultiplier()).isFalse();
    assertThat(resultRule.hasDefaultRemFnBaseEffort()).isFalse();
    assertThat(resultRule.hasRemFnType()).isFalse();
    assertThat(resultRule.hasRemFnGapMultiplier()).isFalse();
    assertThat(resultRule.hasRemFnBaseEffort()).isFalse();
    assertThat(resultRule.getRemFnOverloaded()).isFalse();
  }

  @Test
  public void show_deprecated_rule_debt_fields() {
    RuleDefinitionDto rule = db.rules().insert(r -> r
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setGapDescription("gap desc"));
    db.rules().insertOrUpdateMetadata(rule, db.getDefaultOrganization(), m -> m.setNoteData(null).setNoteUserUuid(null),
      m -> m
        .setRemediationFunction("CONSTANT_ISSUE")
        .setRemediationGapMultiplier(null)
        .setRemediationBaseEffort("15h"));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.getDefaultRemFnType()).isEqualTo("LINEAR_OFFSET");
    assertThat(resultRule.getDefaultDebtRemFnCoeff()).isEqualTo("5d");
    assertThat(resultRule.getDefaultDebtRemFnOffset()).isEqualTo("10h");
    assertThat(resultRule.getEffortToFixDescription()).isEqualTo("gap desc");
    assertThat(resultRule.getDebtRemFnType()).isEqualTo("CONSTANT_ISSUE");
    assertThat(resultRule.hasDebtRemFnCoeff()).isFalse();
    assertThat(resultRule.getDebtRemFnOffset()).isEqualTo("15h");
    assertThat(resultRule.getDebtOverloaded()).isTrue();
  }

  @Test
  public void encode_html_description_of_custom_rule() {
    // Template rule
    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001"));
    db.rules().insert(templateRule.getDefinition());
    // Custom rule
    RuleDefinitionDto customRule = newCustomRule(templateRule.getDefinition())
      .setDescription("<div>line1\nline2</div>")
      .setDescriptionFormat(MARKDOWN);
    db.rules().insert(customRule);
    doReturn("&lt;div&gt;line1<br/>line2&lt;/div&gt;").when(macroInterpreter).interpret("<div>line1\nline2</div>");

    ShowResponse result = ws.newRequest()
      .setParam("key", customRule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getRule().getHtmlDesc()).isEqualTo(INTERPRETED);
    assertThat(result.getRule().getTemplateKey()).isEqualTo(templateRule.getKey().toString());
    verify(macroInterpreter).interpret("&lt;div&gt;line1<br/>line2&lt;/div&gt;");
  }

  @Test
  public void show_external_rule() {
    RuleDefinitionDto externalRule = db.rules().insert(r -> r
      .setIsExternal(true)
      .setName("ext rule name"));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, externalRule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.getName()).isEqualTo("ext rule name");
  }

  @Test
  public void show_adhoc_rule() {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto externalRule = db.rules().insert(r -> r
      .setIsExternal(true)
      .setIsAdHoc(true));
    RuleMetadataDto metadata = db.rules().insertOrUpdateMetadata(externalRule, organization, m -> m
      .setAdHocName("adhoc name")
      .setAdHocDescription("<div>desc</div>")
      .setAdHocSeverity(Severity.BLOCKER)
      .setAdHocType(RuleType.VULNERABILITY)
      .setNoteData(null)
      .setNoteUserUuid(null));
    doReturn("&lt;div&gt;desc2&lt;/div&gt;").when(macroInterpreter).interpret(metadata.getAdHocDescription());

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, externalRule.getKey().toString())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule)
      .extracting(Rule::getName, Rule::getHtmlDesc, Rule::getSeverity, Rule::getType)
      .containsExactlyInAnyOrder("adhoc name", "&lt;div&gt;desc2&lt;/div&gt;", Severity.BLOCKER, VULNERABILITY);
  }

  @Test
  public void ignore_predefined_info_on_adhoc_rule() {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto externalRule = db.rules().insert(r -> r
      .setIsExternal(true)
      .setIsAdHoc(true)
      .setName("predefined name")
      .setDescription("<div>predefined desc</div>")
      .setSeverity(Severity.BLOCKER)
      .setType(RuleType.VULNERABILITY));
    RuleMetadataDto metadata = db.rules().insertOrUpdateMetadata(externalRule, organization, m -> m
      .setAdHocName("adhoc name")
      .setAdHocDescription("<div>adhoc desc</div>")
      .setAdHocSeverity(Severity.MAJOR)
      .setAdHocType(RuleType.CODE_SMELL)
      .setNoteData(null)
      .setNoteUserUuid(null));
    doReturn("&lt;div&gt;adhoc desc&lt;/div&gt;").when(macroInterpreter).interpret(metadata.getAdHocDescription());

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, externalRule.getKey().toString())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule)
      .extracting(Rule::getName, Rule::getHtmlDesc, Rule::getSeverity, Rule::getType)
      .containsExactlyInAnyOrder("adhoc name", "&lt;div&gt;adhoc desc&lt;/div&gt;", Severity.MAJOR, Common.RuleType.CODE_SMELL);
  }

  @Test
  public void adhoc_info_are_empty_when_no_metadata() {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto externalRule = db.rules().insert(r -> r
      .setIsExternal(true)
      .setIsAdHoc(true)
      .setName(null)
      .setDescription(null)
      .setDescriptionFormat(null)
      .setSeverity((String) null)
      .setType(0));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, externalRule.getKey().toString())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule)
      .extracting(Rule::hasName, Rule::hasHtmlDesc, Rule::hasSeverity, Rule::getType)
      .containsExactlyInAnyOrder(false, false, false, UNKNOWN);
  }

  @Test
  public void show_rule_with_activation() {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto rule = db.rules().insert();
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setType("STRING").setDescription("Reg *exp*").setDefaultValue(".*"));
    RuleMetadataDto ruleMetadata = db.rules().insertOrUpdateMetadata(rule, organization, m -> m.setNoteData(null).setNoteUserUuid(null));
    QProfileDto qProfile = db.qualityProfiles().insert(organization);
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(qProfile, rule);
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRule, new ActiveRuleParamDto()
      .setRulesParameterId(ruleParam.getId())
      .setKey(ruleParam.getName())
      .setValue(".*?"));
    db.commit();

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(PARAM_ACTIVES, "true")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .executeProtobuf(ShowResponse.class);

    List<Rules.Active> actives = result.getActivesList();
    assertThat(actives).extracting(Rules.Active::getQProfile).containsExactly(qProfile.getKee());
    assertThat(actives).extracting(Rules.Active::getSeverity).containsExactly(activeRule.getSeverityString());
    assertThat(actives).extracting(Rules.Active::getInherit).containsExactly("NONE");
    assertThat(actives.get(0).getParamsList())
      .extracting(Rules.Active.Param::getKey, Rules.Active.Param::getValue)
      .containsExactlyInAnyOrder(tuple(ruleParam.getName(), ".*?"));
  }

  @Test
  public void show_rule_without_activation() {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto rule = db.rules().insert();
    RuleMetadataDto ruleMetadata = db.rules().insertOrUpdateMetadata(rule, organization, m -> m.setNoteData(null).setNoteUserUuid(null));
    QProfileDto qProfile = db.qualityProfiles().insert(organization);
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(qProfile, rule);

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(PARAM_ACTIVES, "false")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getActivesList()).isEmpty();
  }

  @Test
  public void active_rules_are_returned_when_member_of_paid_organization() {
    OrganizationDto organization = db.organizations().insert(o -> o.setSubscription(PAID));
    RuleDefinitionDto rule = db.rules().insert();
    QProfileDto qProfile = db.qualityProfiles().insert(organization);
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(qProfile, rule);
    userSession.logIn(db.users().insertUser()).addMembership(organization);

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(PARAM_ACTIVES, "true")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getActivesList()).isNotEmpty();
  }

  @Test
  public void active_rules_are_not_returned_when_not_member_of_paid_organization() {
    OrganizationDto organization = db.organizations().insert(o -> o.setSubscription(PAID));
    RuleDefinitionDto rule = db.rules().insert();
    QProfileDto qProfile = db.qualityProfiles().insert(organization);
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(qProfile, rule);

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(PARAM_ACTIVES, "true")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getActivesList()).isEmpty();
  }

  @Test
  public void throw_NotFoundException_if_organization_cannot_be_found() {
    RuleDefinitionDto rule = db.rules().insert();

    thrown.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("key", rule.getKey().toString())
      .setParam("organization", "foo")
      .execute();
  }

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.isPost()).isFalse();
    assertThat(def.since()).isEqualTo("4.2");
    assertThat(def.isInternal()).isFalse();
    assertThat(def.responseExampleAsString()).isNotEmpty();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("key", true),
        tuple("actives", false),
        tuple("organization", false));
  }

}
