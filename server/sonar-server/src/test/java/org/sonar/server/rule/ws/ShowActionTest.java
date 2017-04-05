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
import java.util.function.Consumer;
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
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonar.server.rule.ws.ShowAction.PARAM_KEY;
import static org.sonar.server.rule.ws.ShowAction.PARAM_ORGANIZATION;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;

public class ShowActionTest {

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
  private ActiveRuleCompleter activeRuleCompleter = mock(ActiveRuleCompleter.class);
  private WsAction underTest = new ShowAction(dbClient, mapper, activeRuleCompleter, defaultOrganizationProvider);
  private WsActionTester actionTester = new WsActionTester(underTest);

  private RuleIndexer ruleIndexer = new RuleIndexer(esClient, dbClient);

  @Before
  public void before() {
    doReturn("interpreted").when(macroInterpreter).interpret(anyString());
  }

  @Test
  public void should_show_rule_key() throws IOException {
    RuleDefinitionDto rule = insertRule();

    Rules.ShowResponse result = Rules.ShowResponse.parseFrom(
      actionTester.newRequest()
        .setMediaType(PROTOBUF)
        .setParam(PARAM_KEY, rule.getKey().toString())
        .execute()
        .getInputStream());
    assertThat(result.getRule()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
  }

  @Test
  public void should_show_rule_tags_in_default_organization() throws IOException {
    RuleDefinitionDto rule = insertRule();
    RuleMetadataDto metadata = insertMetadata(dbTester.getDefaultOrganization(), rule, setTags("tag1", "tag2"));

    Rules.ShowResponse result = Rules.ShowResponse.parseFrom(
      actionTester.newRequest()
        .setMediaType(PROTOBUF)
        .setParam(PARAM_KEY, rule.getKey().toString())
        .execute()
        .getInputStream());
    assertThat(result.getRule().getTags().getTagsList())
      .containsExactly(metadata.getTags().toArray(new String[0]));
  }

  @Test
  public void should_show_rule_tags_in_specific_organization() throws IOException {
    RuleDefinitionDto rule = insertRule();
    OrganizationDto organization = dbTester.organizations().insert();
    RuleMetadataDto metadata = insertMetadata(organization, rule, setTags("tag1", "tag2"));

    Rules.ShowResponse result = Rules.ShowResponse.parseFrom(
      actionTester.newRequest()
        .setMediaType(PROTOBUF)
        .setParam(PARAM_KEY, rule.getKey().toString())
        .setParam(PARAM_ORGANIZATION, organization.getKey())
        .execute()
        .getInputStream());
    assertThat(result.getRule().getTags().getTagsList())
      .containsExactly(metadata.getTags().toArray(new String[0]));
  }

  private RuleDefinitionDto insertRule() {
    RuleDefinitionDto rule = dbTester.rules().insert();
    ruleIndexer.indexRuleDefinition(rule.getKey());
    return rule;
  }

  @SafeVarargs
  private final RuleMetadataDto insertMetadata(OrganizationDto organization, RuleDefinitionDto rule, Consumer<RuleMetadataDto>... populaters) {
    RuleMetadataDto metadata = dbTester.rules().insertOrUpdateMetadata(rule, organization, populaters);
    ruleIndexer.indexRuleExtension(organization, rule.getKey());
    return metadata;
  }
}
