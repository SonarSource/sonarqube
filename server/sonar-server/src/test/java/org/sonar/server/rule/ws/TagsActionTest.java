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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.rule.RuleTesting.setSystemTags;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonar.test.JsonAssert.assertJson;

public class TagsActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public EsTester esTester = new EsTester(new RuleIndexDefinition(new MapSettings().asConfig()));

  private DbClient dbClient = dbTester.getDbClient();
  private EsClient esClient = esTester.client();
  private RuleIndex ruleIndex = new RuleIndex(esClient);
  private RuleIndexer ruleIndexer = new RuleIndexer(esClient, dbClient);

  private WsActionTester tester = new WsActionTester(new org.sonar.server.rule.ws.TagsAction(ruleIndex, dbClient, TestDefaultOrganizationProvider.from(dbTester)));
  private OrganizationDto organization;

  @Before
  public void before() {
    organization = dbTester.organizations().insert();
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(3);

    WebService.Param query = action.param("q");
    assertThat(query).isNotNull();
    assertThat(query.isRequired()).isFalse();
    assertThat(query.description()).isNotEmpty();
    assertThat(query.exampleValue()).isNotEmpty();

    WebService.Param pageSize = action.param("ps");
    assertThat(pageSize).isNotNull();
    assertThat(pageSize.isRequired()).isFalse();
    assertThat(pageSize.defaultValue()).isEqualTo("0");
    assertThat(pageSize.description()).isNotEmpty();
    assertThat(pageSize.exampleValue()).isNotEmpty();

    WebService.Param organization = action.param("organization");
    assertThat(organization).isNotNull();
    assertThat(organization.isRequired()).isFalse();
    assertThat(organization.isInternal()).isTrue();
    assertThat(organization.description()).isNotEmpty();
    assertThat(organization.exampleValue()).isNotEmpty();
    assertThat(organization.since()).isEqualTo("6.4");
  }

  @Test
  public void return_system_tag() throws Exception {
    RuleDefinitionDto r = dbTester.rules().insert(setSystemTags("tag"));
    ruleIndexer.commitAndIndex(dbTester.getSession(), r.getKey());

    String result = tester.newRequest().execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag\"]}");
  }

  @Test
  public void return_tag() throws Exception {
    RuleDefinitionDto r = dbTester.rules().insert(setSystemTags());
    ruleIndexer.commitAndIndex(dbTester.getSession(), r.getKey());
    dbTester.rules().insertOrUpdateMetadata(r, organization, setTags("tag"));
    ruleIndexer.commitAndIndex(dbTester.getSession(), r.getKey(), organization);

    String result = tester.newRequest().setParam("organization", organization.getKey()).execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag\"]}");
  }
}
