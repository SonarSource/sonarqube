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
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.Rule;
import org.sonarqube.ws.Rules.SearchResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.entry;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.sonar.db.rule.RuleTesting.setSystemTags;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;

public class SearchActionTest {

  @org.junit.Rule
  public DbTester dbTester = DbTester.create();
  @org.junit.Rule
  public EsTester esTester = new EsTester(
    new RuleIndexDefinition(new MapSettings()));

  private DbClient dbClient = dbTester.getDbClient();
  private EsClient esClient = esTester.client();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  private Languages languages = new Languages();
  private RuleMapper mapper = new RuleMapper(languages, macroInterpreter);
  private RuleQueryFactory ruleQueryFactory = new RuleQueryFactory(dbClient, new RuleWsSupport(dbClient, null, defaultOrganizationProvider));
  private ActiveRuleCompleter activeRuleCompleter = mock(ActiveRuleCompleter.class);
  private RuleIndex ruleIndex = new RuleIndex(esClient);
  private WsAction underTest = new SearchAction(ruleIndex, activeRuleCompleter, ruleQueryFactory, dbClient, mapper);
  private WsActionTester actionTester = new WsActionTester(underTest);

  private RuleIndexer ruleIndexer = new RuleIndexer(esClient, dbClient);

  @Before
  public void before() {
    doReturn("interpreted").when(macroInterpreter).interpret(anyString());
  }

  @Test
  public void should_find_rule() throws IOException {
    RuleDefinitionDto rule = insertRuleDefinition();

    doReturn("interpreted").when(macroInterpreter).interpret(anyString());

    SearchResponse result = SearchResponse.parseFrom(
      actionTester.newRequest()
        .setMediaType(PROTOBUF)
        .execute()
        .getInputStream());
    assertThat(result.getRulesList()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
  }

  @Test
  public void should_filter_on_organization_specific_tags() throws IOException {
    OrganizationDto organization = dbTester.organizations().insert();
    RuleDefinitionDto rule1 = insertRuleDefinition();
    RuleMetadataDto metadata1 = insertMetadata(organization, rule1, setTags("tag1", "tag2"));
    RuleDefinitionDto rule2 = insertRuleDefinition();
    RuleMetadataDto metadata2 = insertMetadata(organization, rule2);

    InputStream inputStream = actionTester.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("f", "repo,name")
      .setParam("tags", metadata1.getTags().stream().collect(Collectors.joining(",")))
      .setParam("organization", organization.getKey())
      .execute()
      .getInputStream();
    SearchResponse result = SearchResponse.parseFrom(inputStream);
    assertThat(result.getRulesList()).extracting(Rule::getKey).containsExactly(rule1.getKey().toString());
  }

  @Test
  public void should_list_tags_in_tags_facet() throws IOException {
    OrganizationDto organization = dbTester.organizations().insert();
    RuleDefinitionDto rule = insertRuleDefinition(setSystemTags("tag1", "tag3", "tag5", "tag7", "tag9", "x"));
    RuleMetadataDto metadata = insertMetadata(organization, rule, setTags("tag2", "tag4", "tag6", "tag8", "tagA"));

    InputStream inputStream = actionTester.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("facets", "tags")
      .setParam("organization", organization.getKey())
      .execute()
      .getInputStream();
    SearchResponse result = SearchResponse.parseFrom(inputStream);
    assertThat(result.getFacets().getFacets(0).getValuesList()).extracting(v -> entry(v.getVal(), v.getCount()))
      .containsExactly(entry("tag1", 1L), entry("tag2", 1L), entry("tag3", 1L), entry("tag4", 1L), entry("tag5", 1L), entry("tag6", 1L), entry("tag7", 1L), entry("tag8", 1L),
        entry("tag9", 1L), entry("tagA", 1L));
  }

  @Test
  public void should_include_selected_matching_tag_in_facet() throws IOException {
    insertRuleDefinition(setSystemTags("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tagA", "x"));

    InputStream inputStream = actionTester.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("facets", "tags")
      .setParam("tags", "x")
      .execute()
      .getInputStream();
    SearchResponse result = SearchResponse.parseFrom(inputStream);
    assertThat(result.getFacets().getFacets(0).getValuesList()).extracting(v -> entry(v.getVal(), v.getCount())).contains(entry("x", 1L));
  }

  @Test
  public void should_included_selected_non_matching_tag_in_facet() throws IOException {
    insertRuleDefinition(setSystemTags("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tagA"));

    InputStream inputStream = actionTester.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("facets", "tags")
      .setParam("tags", "x")
      .execute()
      .getInputStream();
    SearchResponse result = SearchResponse.parseFrom(inputStream);
    assertThat(result.getFacets().getFacets(0).getValuesList()).extracting(v -> entry(v.getVal(), v.getCount())).contains(entry("x", 0L));
  }

  @Test
  public void should_return_organization_specific_tags() throws IOException {
    OrganizationDto organization = dbTester.organizations().insert();
    RuleDefinitionDto rule = insertRuleDefinition();
    RuleMetadataDto metadata = insertMetadata(organization, rule, setTags("tag1", "tag2"));

    InputStream inputStream = actionTester.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("f", "tags")
      .setParam("organization", organization.getKey())
      .execute()
      .getInputStream();
    SearchResponse result = SearchResponse.parseFrom(inputStream);
    assertThat(result.getRulesList()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
    assertThat(result.getRulesList())
      .extracting(Rule::getTags).flatExtracting(Rules.Tags::getTagsList)
      .containsExactly(metadata.getTags().toArray(new String[0]));
  }

  @Test
  public void should_return_specified_fields() throws IOException {
    RuleDefinitionDto rule = insertRuleDefinition();

    checkField(rule, "repo", Rule::getRepo, rule.getRepositoryKey());
    checkField(rule, "name", Rule::getName, rule.getName());
    checkField(rule, "severity", Rule::getSeverity, rule.getSeverityString());
    checkField(rule, "status", r -> r.getStatus().toString(), rule.getStatus().toString());
    checkField(rule, "internalKey", Rule::getInternalKey, rule.getConfigKey());
    checkField(rule, "isTemplate", Rule::getIsTemplate, rule.isTemplate());
    checkField(rule, "sysTags",
      r -> r.getSysTags().getSysTagsList().stream().collect(Collectors.joining(",")),
      rule.getSystemTags().stream().collect(Collectors.joining(",")));
    checkField(rule, "lang", Rule::getLang, rule.getLanguage());
    checkField(rule, "langName", Rule::getLangName, rule.getLanguage());
    checkField(rule, "gapDescription", Rule::getGapDescription, rule.getGapDescription());
    // to be continued...
  }

  @SafeVarargs
  private final <T> void checkField(RuleDefinitionDto rule, String fieldName, Extractor<Rule, T> responseExtractor, T... expected) throws IOException {
    InputStream inputStream = actionTester.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("f", fieldName)
      .execute()
      .getInputStream();
    SearchResponse result = SearchResponse.parseFrom(inputStream);
    assertThat(result.getRulesList()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
    assertThat(result.getRulesList()).extracting(responseExtractor).containsExactly(expected);
  }

  @SafeVarargs
  private final RuleDefinitionDto insertRuleDefinition(Consumer<RuleDefinitionDto>... populaters) {
    RuleDefinitionDto ruleDefinitionDto = dbTester.rules().insert(populaters);
    ruleIndexer.indexRuleDefinition(ruleDefinitionDto.getKey());
    return ruleDefinitionDto;
  }

  @SafeVarargs
  private final RuleMetadataDto insertMetadata(OrganizationDto organization, RuleDefinitionDto rule, Consumer<RuleMetadataDto>... populaters) {
    RuleMetadataDto metadata = dbTester.rules().insertOrUpdateMetadata(rule, organization, populaters);
    ruleIndexer.indexRuleExtension(organization, rule.getKey());
    return metadata;
  }
}
