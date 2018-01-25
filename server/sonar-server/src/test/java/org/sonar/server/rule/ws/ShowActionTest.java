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
package org.sonar.server.rule.ws;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Format;
import org.sonar.db.rule.RuleDto.Scope;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.NewCustomRule;
import org.sonar.server.rule.RuleCreator;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.Rule;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonar.server.rule.ws.ShowAction.PARAM_KEY;
import static org.sonar.server.rule.ws.ShowAction.PARAM_ORGANIZATION;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;

public class ShowActionTest {

  public static final String INTERPRETED = "interpreted";

  @org.junit.Rule
  public DbTester dbTester = DbTester.create();
  @org.junit.Rule
  public EsTester esTester = new EsTester(
    new RuleIndexDefinition(new MapSettings().asConfig()));
  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private EsClient esClient = esTester.client();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  private Languages languages = new Languages(LanguageTesting.newLanguage("xoo", "Xoo"));
  private RuleMapper mapper = new RuleMapper(languages, macroInterpreter);
  private ActiveRuleCompleter activeRuleCompleter = mock(ActiveRuleCompleter.class);
  private WsAction underTest = new ShowAction(dbClient, mapper, activeRuleCompleter, defaultOrganizationProvider);
  private WsActionTester actionTester = new WsActionTester(underTest);

  private RuleIndexer ruleIndexer = new RuleIndexer(esClient, dbClient);

  @Before
  public void before() {
    doReturn(INTERPRETED).when(macroInterpreter).interpret(anyString());
  }

  @Test
  public void should_show_rule_key() {
    RuleDefinitionDto rule = insertRule();

    Rules.ShowResponse result = actionTester.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(Rules.ShowResponse.class);
    assertThat(result.getRule()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
  }

  @Test
  public void should_show_rule_tags_in_default_organization() {
    RuleDefinitionDto rule = insertRule();
    RuleMetadataDto metadata = insertMetadata(dbTester.getDefaultOrganization(), rule, setTags("tag1", "tag2"));

    Rules.ShowResponse result = actionTester.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(Rules.ShowResponse.class);
    assertThat(result.getRule().getTags().getTagsList())
      .containsExactly(metadata.getTags().toArray(new String[0]));
  }

  @Test
  public void should_show_rule_tags_in_specific_organization() {
    RuleDefinitionDto rule = insertRule();
    OrganizationDto organization = dbTester.organizations().insert();
    RuleMetadataDto metadata = insertMetadata(organization, rule, setTags("tag1", "tag2"));

    Rules.ShowResponse result = actionTester.newRequest()
        .setParam(PARAM_KEY, rule.getKey().toString())
        .setParam(PARAM_ORGANIZATION, organization.getKey())
        .executeProtobuf(Rules.ShowResponse.class);
    assertThat(result.getRule().getTags().getTagsList())
      .containsExactly(metadata.getTags().toArray(new String[0]));
  }

  @Test
  public void show_rule_with_activation() {
    OrganizationDto organization = dbTester.organizations().insert();

    QProfileDto profile = QProfileTesting.newXooP1(organization);
    dbClient.qualityProfileDao().insert(dbTester.getSession(), profile);
    dbTester.commit();

    RuleDefinitionDto rule = insertRule();
    RuleMetadataDto ruleMetadata = dbTester.rules().insertOrUpdateMetadata(rule, organization);

    ArgumentCaptor<OrganizationDto> orgCaptor = ArgumentCaptor.forClass(OrganizationDto.class);
    ArgumentCaptor<RuleDefinitionDto> ruleCaptor = ArgumentCaptor.forClass(RuleDefinitionDto.class);
    Rules.Active active = Rules.Active.newBuilder()
      .setQProfile(randomAlphanumeric(5))
      .setInherit(randomAlphanumeric(5))
      .setSeverity(randomAlphanumeric(5))
      .build();
    Mockito.doReturn(singletonList(active)).when(activeRuleCompleter).completeShow(any(DbSession.class), orgCaptor.capture(), ruleCaptor.capture());

    ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(dbClient, esClient);
    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    TestResponse response = actionTester.newRequest().setMethod("GET")
      .setMediaType(PROTOBUF)
      .setParam(ShowAction.PARAM_KEY, rule.getKey().toString())
      .setParam(ShowAction.PARAM_ACTIVES, "true")
      .setParam(ShowAction.PARAM_ORGANIZATION, organization.getKey())
      .execute();

    assertThat(orgCaptor.getValue().getUuid()).isEqualTo(organization.getUuid());
    assertThat(ruleCaptor.getValue().getKey()).isEqualTo(rule.getKey());

    Rules.ShowResponse result = response.getInputObject(Rules.ShowResponse.class);
    Rule resultRule = result.getRule();
    assertEqual(rule, ruleMetadata, resultRule);

    List<Rules.Active> actives = result.getActivesList();
    assertThat(actives).extracting(Rules.Active::getQProfile).containsExactly(active.getQProfile());
    assertThat(actives).extracting(Rules.Active::getInherit).containsExactly(active.getInherit());
    assertThat(actives).extracting(Rules.Active::getSeverity).containsExactly(active.getSeverity());
  }

  @Test
  public void show_rule_without_activation() {
    OrganizationDto organization = dbTester.organizations().insert();

    QProfileDto profile = QProfileTesting.newXooP1(organization);
    dbClient.qualityProfileDao().insert(dbTester.getSession(), profile);
    dbTester.commit();

    RuleDefinitionDto rule = insertRule();
    RuleMetadataDto ruleMetadata = dbTester.rules().insertOrUpdateMetadata(rule, organization);

    dbTester.qualityProfiles().activateRule(profile, rule, a -> a.setSeverity("BLOCKER"));
    ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(dbClient, esClient);
    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    TestResponse response = actionTester.newRequest().setMethod("GET")
      .setParam(ShowAction.PARAM_KEY, rule.getKey().toString())
      .setParam(ShowAction.PARAM_ORGANIZATION, organization.getKey())
      .setMediaType(PROTOBUF)
      .execute();

    Rules.ShowResponse result = response.getInputObject(Rules.ShowResponse.class);
    Rule resultRule = result.getRule();
    assertEqual(rule, ruleMetadata, resultRule);

    List<Rules.Active> actives = result.getActivesList();
    assertThat(actives).isEmpty();
  }

  @Test
  public void throw_NotFoundException_if_organization_cannot_be_found() {
    RuleDefinitionDto rule = dbTester.rules().insert();

    thrown.expect(NotFoundException.class);

    actionTester.newRequest().setMethod("POST")
      .setParam("key", rule.getKey().toString())
      .setParam("organization", "foo")
      .execute();
  }

  @Test
  public void show_rule() throws Exception {
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("java", "S001"), dbTester.getDefaultOrganization())
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setDescriptionFormat(Format.HTML)
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setConfigKey("InternalKeyS001")
      .setLanguage("xoo")
      .setTags(newHashSet("tag1", "tag2"))
      .setSystemTags(newHashSet("systag1", "systag2"))
      .setType(RuleType.BUG)
      .setScope(Scope.ALL);
    RuleDefinitionDto definition = ruleDto.getDefinition();
    RuleDao ruleDao = dbClient.ruleDao();
    DbSession session = dbTester.getSession();
    ruleDao.insert(session, definition);
    ruleDao.insertOrUpdate(session, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    RuleParamDto param = RuleParamDto.createFor(definition).setName("regex").setType("STRING").setDescription("Reg *exp*").setDefaultValue(".*");
    ruleDao.insertRuleParam(session, definition, param);
    session.commit();
    session.clearCache();

    actionTester.newRequest()
      .setParam("key", ruleDto.getKey().toString())
      .execute().assertJson(getClass(), "show_rule.json");
  }

  @Test
  public void show_rule_with_default_debt_infos() throws Exception {
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("java", "S001"), dbTester.getDefaultOrganization())
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setConfigKey("InternalKeyS001")
      .setLanguage("xoo")
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setRemediationFunction(null)
      .setRemediationGapMultiplier(null)
      .setRemediationBaseEffort(null)
      .setScope(Scope.ALL);
    RuleDao ruleDao = dbClient.ruleDao();
    DbSession session = dbTester.getSession();
    ruleDao.insert(session, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(session, ruleDto.getMetadata());
    session.commit();
    session.clearCache();

    actionTester.newRequest()
      .setParam("key", ruleDto.getKey().toString())
      .execute()
      .assertJson(getClass(), "show_rule_with_default_debt_infos.json");
  }

  @Test
  public void show_rule_with_overridden_debt() throws Exception {
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("java", "S001"), dbTester.getDefaultOrganization())
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setConfigKey("InternalKeyS001")
      .setLanguage("xoo")
      .setDefRemediationFunction(null)
      .setDefRemediationGapMultiplier(null)
      .setDefRemediationBaseEffort(null)
      .setRemediationFunction("LINEAR_OFFSET")
      .setRemediationGapMultiplier("5d")
      .setRemediationBaseEffort("10h")
      .setScope(Scope.ALL);
    RuleDao ruleDao = dbClient.ruleDao();
    DbSession session = dbTester.getSession();
    ruleDao.insert(session, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(session, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    session.commit();
    session.clearCache();

    actionTester.newRequest()
      .setParam("key", ruleDto.getKey().toString())
      .execute().assertJson(getClass(), "show_rule_with_overridden_debt_infos.json");
  }

  @Test
  public void show_rule_with_default_and_overridden_debt_infos() throws Exception {
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("java", "S001"), dbTester.getDefaultOrganization())
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setConfigKey("InternalKeyS001")
      .setLanguage("xoo")
      .setDefRemediationFunction("LINEAR")
      .setDefRemediationGapMultiplier("5min")
      .setDefRemediationBaseEffort(null)
      .setRemediationFunction("LINEAR_OFFSET")
      .setRemediationGapMultiplier("5d")
      .setRemediationBaseEffort("10h")
      .setScope(Scope.ALL);
    RuleDao ruleDao = dbClient.ruleDao();
    DbSession session = dbTester.getSession();
    ruleDao.insert(session, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(session, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    session.commit();
    session.clearCache();

    actionTester.newRequest()
      .setParam("key", ruleDto.getKey().toString())
      .execute().assertJson(getClass(), "show_rule_with_default_and_overridden_debt_infos.json");
  }

  @Test
  public void show_rule_with_no_default_and_no_overridden_debt() throws Exception {
    RuleDefinitionDto ruleDto = RuleTesting.newRule(RuleKey.of("java", "S001"))
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setDescriptionFormat(Format.HTML)
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setConfigKey("InternalKeyS001")
      .setLanguage("xoo")
      .setDefRemediationFunction(null)
      .setDefRemediationGapMultiplier(null)
      .setDefRemediationBaseEffort(null)
      .setScope(Scope.ALL);
    RuleDao ruleDao = dbClient.ruleDao();
    DbSession session = dbTester.getSession();
    ruleDao.insert(session, ruleDto);
    session.commit();
    session.clearCache();

    actionTester.newRequest()
      .setParam("key", ruleDto.getKey().toString())
      .execute().assertJson(getClass(), "show_rule_with_no_default_and_no_overridden_debt.json");
  }

  @Test
  public void encode_html_description_of_custom_rule() {
    // Template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    RuleDao ruleDao = dbClient.ruleDao();
    DbSession session = dbTester.getSession();
    ruleDao.insert(session, templateRule.getDefinition());
    session.commit();

    // Custom rule
    NewCustomRule customRule = NewCustomRule.createForCustomRule("MY_CUSTOM", templateRule.getKey())
      .setName("My custom")
      .setSeverity(MINOR)
      .setStatus(RuleStatus.READY)
      .setMarkdownDescription("<div>line1\nline2</div>");
    RuleKey customRuleKey = new RuleCreator(System2.INSTANCE, ruleIndexer, dbClient, new TypeValidations(asList()), TestDefaultOrganizationProvider.from(dbTester)).create(session, customRule);
    session.clearCache();

    doReturn("&lt;div&gt;line1<br/>line2&lt;/div&gt;").when(macroInterpreter).interpret("<div>line1\nline2</div>");

    Rules.ShowResponse result = actionTester.newRequest()
      .setParam("key", customRuleKey.toString())
      .executeProtobuf(Rules.ShowResponse.class);

    Mockito.verify(macroInterpreter).interpret("&lt;div&gt;line1<br/>line2&lt;/div&gt;");

    assertThat(result.getRule().getKey()).isEqualTo("java:MY_CUSTOM");
    assertThat(result.getRule().getHtmlDesc()).isEqualTo(INTERPRETED);
    assertThat(result.getRule().getTemplateKey()).isEqualTo("java:S001");
  }

  @Test
  public void show_deprecated_rule_rem_function_fields() throws Exception {
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("java", "S001"), dbTester.getDefaultOrganization())
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setConfigKey("InternalKeyS001")
      .setLanguage("xoo")
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setDefRemediationGapMultiplier("6d")
      .setDefRemediationBaseEffort("11h")
      .setRemediationFunction("LINEAR_OFFSET")
      .setRemediationGapMultiplier("5d")
      .setRemediationBaseEffort("10h")
      .setScope(Scope.ALL);
    RuleDao ruleDao = dbClient.ruleDao();
    DbSession session = dbTester.getSession();
    ruleDao.insert(session, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(session, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    session.commit();

    actionTester.newRequest()
      .setParam("key", ruleDto.getKey().toString())
      .execute().assertJson(getClass(), "show_deprecated_rule_rem_function_fields.json");
  }

  @Test
  public void show_rule_when_activated() throws Exception {
    RuleDefinitionDto ruleDto = RuleTesting.newRule(RuleKey.of("java", "S001"))
      .setName("Rule S001")
      .setDescription("Rule S001 <b>description</b>")
      .setDescriptionFormat(Format.HTML)
      .setSeverity(MINOR)
      .setStatus(RuleStatus.BETA)
      .setLanguage("xoo")
      .setType(RuleType.BUG)
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime())
      .setScope(Scope.ALL);
    RuleDao ruleDao = dbClient.ruleDao();
    DbSession session = dbTester.getSession();
    ruleDao.insert(session, ruleDto);
    session.commit();
    ruleIndexer.commitAndIndex(session, ruleDto.getId());
    RuleParamDto regexParam = RuleParamDto.createFor(ruleDto).setName("regex").setType("STRING").setDescription("Reg *exp*").setDefaultValue(".*");
    ruleDao.insertRuleParam(session, ruleDto, regexParam);

    QProfileDto profile = new QProfileDto()
      .setRulesProfileUuid("profile")
      .setKee("profile")
      .setOrganizationUuid(defaultOrganizationProvider.get().getUuid())
      .setName("Profile")
      .setLanguage("xoo");
    dbClient.qualityProfileDao().insert(session, profile);
    ActiveRuleDto activeRuleDto = new ActiveRuleDto()
      .setProfileId(profile.getId())
      .setRuleId(ruleDto.getId())
      .setSeverity(MINOR)
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime());
    dbClient.activeRuleDao().insert(session, activeRuleDto);
    dbClient.activeRuleDao().insertParam(session, activeRuleDto, new ActiveRuleParamDto()
      .setRulesParameterId(regexParam.getId())
      .setKey(regexParam.getName())
      .setValue(".*?"));
    session.commit();

    StartupIndexer activeRuleIndexer = new ActiveRuleIndexer(dbClient, esClient);
    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    ActiveRuleCompleter activeRuleCompleter = new ActiveRuleCompleter(dbClient, languages);
    WsAction underTest = new ShowAction(dbClient, mapper, activeRuleCompleter, defaultOrganizationProvider);
    WsActionTester actionTester = new WsActionTester(underTest);

    actionTester.newRequest()
      .setParam("key", ruleDto.getKey().toString())
      .setParam("actives", "true")
      .execute().assertJson(getClass(), "show_rule_when_activated.json");
  }

  private void assertEqual(RuleDefinitionDto rule, RuleMetadataDto ruleMetadata, Rule resultRule) {
    assertThat(resultRule.getKey()).isEqualTo(rule.getKey().toString());
    assertThat(resultRule.getRepo()).isEqualTo(rule.getRepositoryKey());
    assertThat(resultRule.getName()).isEqualTo(rule.getName());
    assertThat(resultRule.getSeverity()).isEqualTo(rule.getSeverityString());
    assertThat(resultRule.getStatus().toString()).isEqualTo(rule.getStatus().toString());
    assertThat(resultRule.getInternalKey()).isEqualTo(rule.getConfigKey());
    assertThat(resultRule.getIsTemplate()).isEqualTo(rule.isTemplate());
    assertThat(resultRule.getTags().getTagsList()).containsExactlyInAnyOrder(ruleMetadata.getTags().toArray(new String[0]));
    assertThat(resultRule.getSysTags().getSysTagsList()).containsExactlyInAnyOrder(rule.getSystemTags().toArray(new String[0]));
    assertThat(resultRule.getLang()).isEqualTo(rule.getLanguage());
    assertThat(resultRule.getParams().getParamsList()).isEmpty();
  }

  private RuleDefinitionDto insertRule() {
    RuleDefinitionDto rule = dbTester.rules().insert();
    ruleIndexer.commitAndIndex(dbTester.getSession(), rule.getId());
    return rule;
  }

  @SafeVarargs
  private final RuleMetadataDto insertMetadata(OrganizationDto organization, RuleDefinitionDto rule, Consumer<RuleMetadataDto>... populaters) {
    RuleMetadataDto metadata = dbTester.rules().insertOrUpdateMetadata(rule, organization, populaters);
    ruleIndexer.commitAndIndex(dbTester.getSession(), rule.getId(), organization);
    return metadata;
  }
}
