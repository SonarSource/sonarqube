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
package org.sonar.server.issue.ws;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.time.Clock;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.AvatarResolverImpl;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ORGANIZATION;

public class SearchActionTestOnSonarCloud {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();

  private MapSettings mapSettings = new MapSettings().setProperty("sonar.sonarcloud.enabled", true);

  private DbClient dbClient = db.getDbClient();
  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new WebAuthorizationTypeSupport(userSession));
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient));
  private IssueQueryFactory issueQueryFactory = new IssueQueryFactory(dbClient, Clock.systemUTC(), userSession);
  private IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private IssueWorkflow issueWorkflow = new IssueWorkflow(new FunctionExecutor(issueFieldsSetter), issueFieldsSetter);
  private SearchResponseLoader searchResponseLoader = new SearchResponseLoader(userSession, dbClient, new TransitionService(userSession, issueWorkflow));
  private Languages languages = new Languages();
  private SearchResponseFormat searchResponseFormat = new SearchResponseFormat(new Durations(), languages, new AvatarResolverImpl());
  private PermissionIndexerTester permissionIndexer = new PermissionIndexerTester(es, issueIndexer);

  private SearchAction underTest = new SearchAction(userSession, issueIndex, issueQueryFactory, searchResponseLoader, searchResponseFormat,
    mapSettings.asConfig(), System2.INSTANCE, dbClient);
  private WsActionTester ws = new WsActionTester(underTest);

  private OrganizationDto organization;
  private UserDto user;
  private ComponentDto project;

  @Before
  public void setup() {
    underTest.start();
    organization = db.organizations().insert(o -> o.setKey("org-1"));
    user = db.users().insertUser();

    project = db.components().insertPublicProject(organization, p -> p.setDbKey("PK1"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setDbKey("FK1"));
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insert(rule, project, file, i -> i.setAuthorLogin("leia").setKee("2bd4eac2-b650-4037-80bc-7b112bd4eac2"));
    db.issues().insert(rule, project, file, i -> i.setAuthorLogin("luke@skywalker.name").setKee("82fd47d4-b650-4037-80bc-7b1182fd47d4"));
    db.commit();
    allowAnyoneOnProjects(project);
    indexIssues();
  }

  @Test
  public void authors_facet_is_hidden_if_organization_is_not_set() {
    db.organizations().addMember(organization, user);
    userSession
      .logIn(user)
      .addMembership(organization);

    String input = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, project.getKey())
      .setParam(FACETS, "authors")
      .execute()
      .getInput();

    JsonAssert.assertJson(input).isSimilarTo(this.getClass().getResource(this.getClass().getSimpleName() + "/no_authors_facet.json"));

    JsonElement gson = new JsonParser().parse(input);
    assertThat(gson.getAsJsonObject().get("facets").getAsJsonArray()).isEmpty();
  }

  @Test
  public void authors_facet_is_hidden_if_user_is_not_a_member_of_the_organization() {
    userSession
      .logIn(user);

    String input = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, project.getKey())
      .setParam(FACETS, "authors")
      .execute()
      .getInput();

    JsonAssert.assertJson(input).isSimilarTo(this.getClass().getResource(this.getClass().getSimpleName() + "/no_author_and_no_authors_facet.json"));

    JsonElement gson = new JsonParser().parse(input);
    assertThat(gson.getAsJsonObject().get("facets").getAsJsonArray()).isEmpty();

  }

  @Test
  public void authors_facet_is_shown_if_organization_is_set_and_user_is_member_of_the_organization() {
    db.organizations().addMember(organization, user);

    userSession
      .logIn(user)
      .addMembership(organization);

    ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, project.getKey())
      .setParam(FACETS, "authors")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute()
      .assertJson(this.getClass(), "with_authors_facet.json");
  }

  private void indexIssues() {
    issueIndexer.indexOnStartup(null);
  }

  private void allowAnyoneOnProjects(ComponentDto... projects) {
    userSession.registerComponents(projects);
    Arrays.stream(projects).forEach(p -> permissionIndexer.allowOnlyAnyone(p));
  }
}
