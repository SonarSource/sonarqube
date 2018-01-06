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
package org.sonar.server.issue.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.test.JsonAssert.assertJson;

public class AuthorsActionTest {
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()));
  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new AuthorizationTypeSupport(userSession));

  private WsActionTester ws = new WsActionTester(new AuthorsAction(issueIndex));

  @Test
  public void json_example() {
    db.issues().insertIssue(issue -> issue.setAuthorLogin("luke.skywalker"));
    db.issues().insertIssue(issue -> issue.setAuthorLogin("leia.organa"));
    issueIndexer.indexOnStartup(emptySet());

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void search_by_query() {
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    db.issues().insertIssue(issue -> issue.setAuthorLogin(leia));
    db.issues().insertIssue(issue -> issue.setAuthorLogin(luke));
    issueIndexer.indexOnStartup(emptySet());

    String result = ws.newRequest()
      .setParam(TEXT_QUERY, "leia")
      .execute().getInput();

    assertThat(result).contains(leia).doesNotContain(luke);
  }

  @Test
  public void set_page_size() {
    String han = "han.solo";
    String leia = "leia.organa";
    String luke = "luke.skywalker";
    db.issues().insertIssue(issue -> issue.setAuthorLogin(han));
    db.issues().insertIssue(issue -> issue.setAuthorLogin(leia));
    db.issues().insertIssue(issue -> issue.setAuthorLogin(luke));
    issueIndexer.indexOnStartup(emptySet());

    String result = ws.newRequest()
      .setParam(PAGE_SIZE, "2")
      .execute().getInput();

    assertThat(result).contains(han, leia).doesNotContain(luke);
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("authors");
    assertThat(definition.since()).isEqualTo("5.1");
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("q", "ps");
  }
}
