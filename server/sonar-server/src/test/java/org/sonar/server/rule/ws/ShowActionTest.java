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

public class ShowActionTest {
//
//  @org.junit.Rule
//  public DbTester dbTester = DbTester.create();
//  @org.junit.Rule
//  public EsTester esTester = new EsTester(
//    new RuleIndexDefinition(new MapSettings()));
//  @org.junit.Rule
//  public ExpectedException thrown = ExpectedException.none();
//
//  private DbClient dbClient = dbTester.getDbClient();
//  private EsClient esClient = esTester.client();
//
//  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
//  private MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
//  private Languages languages = new Languages();
//  private RuleMapper mapper = new RuleMapper(languages, macroInterpreter);
//  private ActiveRuleCompleter activeRuleCompleter = mock(ActiveRuleCompleter.class);
//  private WsAction underTest = new ShowAction(dbClient, mapper, activeRuleCompleter, defaultOrganizationProvider);
//  private WsActionTester actionTester = new WsActionTester(underTest);
//
//  private RuleIndexer ruleIndexer = new RuleIndexer(esClient, dbClient);
//
//  @Before
//  public void before() {
//    doReturn("interpreted").when(macroInterpreter).interpret(anyString());
//  }
//
//  @Test
//  public void should_show_rule_key() throws IOException {
//    RuleDefinitionDto rule = insertRule();
//
//    Rules.ShowResponse result = actionTester.newRequest()
//      .setParam(PARAM_KEY, rule.getKey().toString())
//      .executeProtobuf(Rules.ShowResponse.class);
//    assertThat(result.getRule()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
//  }
//
//  @Test
//  public void should_show_rule_tags_in_default_organization() throws IOException {
//    RuleDefinitionDto rule = insertRule();
//    RuleMetadataDto metadata = insertMetadata(dbTester.getDefaultOrganization(), rule, setTags("tag1", "tag2"));
//
//    Rules.ShowResponse result = actionTester.newRequest()
//      .setParam(PARAM_KEY, rule.getKey().toString())
//      .executeProtobuf(Rules.ShowResponse.class);
//    assertThat(result.getRule().getTags().getTagsList())
//      .containsExactly(metadata.getTags().toArray(new String[0]));
//  }
//
//  @Test
//  public void should_show_rule_tags_in_specific_organization() throws IOException {
//    RuleDefinitionDto rule = insertRule();
//    OrganizationDto organization = dbTester.organizations().insert();
//    RuleMetadataDto metadata = insertMetadata(organization, rule, setTags("tag1", "tag2"));
//
//    Rules.ShowResponse result = actionTester.newRequest()
//        .setParam(PARAM_KEY, rule.getKey().toString())
//        .setParam(PARAM_ORGANIZATION, organization.getKey())
//        .executeProtobuf(Rules.ShowResponse.class);
//    assertThat(result.getRule().getTags().getTagsList())
//      .containsExactly(metadata.getTags().toArray(new String[0]));
//  }
//
//  @Test
//  public void show_rule_with_activation() throws Exception {
//    OrganizationDto organization = dbTester.organizations().insert();
//
//    QProfileDto profile = QProfileTesting.newXooP1(organization);
//    dbClient.qualityProfileDao().insert(dbTester.getSession(), profile);
//    dbTester.commit();
//
//    RuleDefinitionDto rule = insertRule();
//    RuleMetadataDto ruleMetadata = dbTester.rules().insertOrUpdateMetadata(rule, organization);
//
//    ArgumentCaptor<OrganizationDto> orgCaptor = ArgumentCaptor.forClass(OrganizationDto.class);
//    ArgumentCaptor<RuleDefinitionDto> ruleCaptor = ArgumentCaptor.forClass(RuleDefinitionDto.class);
//    Rules.Active active = Rules.Active.newBuilder()
//      .setQProfile(randomAlphanumeric(5))
//      .setInherit(randomAlphanumeric(5))
//      .setSeverity(randomAlphanumeric(5))
//      .build();
//    Mockito.doReturn(singletonList(active)).when(activeRuleCompleter).completeShow(any(DbSession.class), orgCaptor.capture(), ruleCaptor.capture());

//    new ActiveRuleIndexer(System2.INSTANCE, dbClient, esClient).index();
//
//    TestResponse response = actionTester.newRequest().setMethod("GET")
//      .setMediaType(PROTOBUF)
//      .setParam(ShowAction.PARAM_KEY, rule.getKey().toString())
//      .setParam(ShowAction.PARAM_ACTIVES, "true")
//      .setParam(ShowAction.PARAM_ORGANIZATION, organization.getKey())
//      .execute();
//
//    assertThat(orgCaptor.getValue().getUuid()).isEqualTo(organization.getUuid());
//    assertThat(ruleCaptor.getValue().getKey()).isEqualTo(rule.getKey());
//
//    Rules.ShowResponse result = response.getInputObject(Rules.ShowResponse.class);
//    Rule resultRule = result.getRule();
//    assertEqual(rule, ruleMetadata, resultRule);
//
//    List<Rules.Active> actives = result.getActivesList();
//    assertThat(actives).extracting(Rules.Active::getQProfile).containsExactly(active.getQProfile());
//    assertThat(actives).extracting(Rules.Active::getInherit).containsExactly(active.getInherit());
//    assertThat(actives).extracting(Rules.Active::getSeverity).containsExactly(active.getSeverity());
//  }
//
//  @Test
//  public void show_rule_without_activation() throws Exception {
//    OrganizationDto organization = dbTester.organizations().insert();
//
//    QProfileDto profile = QProfileTesting.newXooP1(organization);
//    dbClient.qualityProfileDao().insert(dbTester.getSession(), profile);
//    dbTester.commit();
//
//    RuleDefinitionDto rule = insertRule();
//    RuleMetadataDto ruleMetadata = dbTester.rules().insertOrUpdateMetadata(rule, organization);
//
//    dbTester.qualityProfiles().activateRule(profile, rule, a -> a.setSeverity("BLOCKER"));
//    new ActiveRuleIndexer(dbClient, esClient).index();
//
//    TestResponse response = actionTester.newRequest().setMethod("GET")
//      .setParam(ShowAction.PARAM_KEY, rule.getKey().toString())
//      .setParam(ShowAction.PARAM_ORGANIZATION, organization.getKey())
//      .setMediaType(PROTOBUF)
//      .execute();
//
//    Rules.ShowResponse result = response.getInputObject(Rules.ShowResponse.class);
//    Rule resultRule = result.getRule();
//    assertEqual(rule, ruleMetadata, resultRule);
//
//    List<Rules.Active> actives = result.getActivesList();
//    assertThat(actives).isEmpty();
//  }
//
//  @Test
//  public void throw_NotFoundException_if_organization_cannot_be_found() throws Exception {
//    RuleDefinitionDto rule = dbTester.rules().insert();
//
//    thrown.expect(NotFoundException.class);
//
//    actionTester.newRequest().setMethod("POST")
//      .setParam("key", rule.getKey().toString())
//      .setParam("organization", "foo")
//      .execute();
//  }
//
//  private void assertEqual(RuleDefinitionDto rule, RuleMetadataDto ruleMetadata, Rule resultRule) {
//    assertThat(resultRule.getKey()).isEqualTo(rule.getKey().toString());
//    assertThat(resultRule.getRepo()).isEqualTo(rule.getRepositoryKey());
//    assertThat(resultRule.getName()).isEqualTo(rule.getName());
//    assertThat(resultRule.getSeverity()).isEqualTo(rule.getSeverityString());
//    assertThat(resultRule.getStatus().toString()).isEqualTo(rule.getStatus().toString());
//    assertThat(resultRule.getInternalKey()).isEqualTo(rule.getConfigKey());
//    assertThat(resultRule.getIsTemplate()).isEqualTo(rule.isTemplate());
//    assertThat(resultRule.getTags().getTagsList()).containsExactlyInAnyOrder(ruleMetadata.getTags().toArray(new String[0]));
//    assertThat(resultRule.getSysTags().getSysTagsList()).containsExactlyInAnyOrder(rule.getSystemTags().toArray(new String[0]));
//    assertThat(resultRule.getLang()).isEqualTo(rule.getLanguage());
//    assertThat(resultRule.getParams().getParamsList()).isEmpty();
//  }
//
//  private RuleDefinitionDto insertRule() {
//    RuleDefinitionDto rule = dbTester.rules().insert();
//    ruleIndexer.indexRuleDefinition(rule.getKey());
//    return rule;
//  }
//
//  @SafeVarargs
//  private final RuleMetadataDto insertMetadata(OrganizationDto organization, RuleDefinitionDto rule, Consumer<RuleMetadataDto>... populaters) {
//    RuleMetadataDto metadata = dbTester.rules().insertOrUpdateMetadata(rule, organization, populaters);
//    ruleIndexer.indexRuleExtension(organization, rule.getKey());
//    return metadata;
//  }
}
