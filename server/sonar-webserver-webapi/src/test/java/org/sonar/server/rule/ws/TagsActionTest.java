/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.rule.index.RuleIndex;
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
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final EsClient esClient = es.client();
  private final RuleIndex ruleIndex = new RuleIndex(esClient, System2.INSTANCE);
  private final RuleIndexer ruleIndexer = new RuleIndexer(esClient, dbClient);

  private final WsActionTester ws = new WsActionTester(new org.sonar.server.rule.ws.TagsAction(ruleIndex));

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(2);

    WebService.Param query = action.param("q");
    assertThat(query).isNotNull();
    assertThat(query.isRequired()).isFalse();
    assertThat(query.description()).isNotEmpty();
    assertThat(query.exampleValue()).isNotEmpty();

    WebService.Param pageSize = action.param("ps");
    assertThat(pageSize).isNotNull();
    assertThat(pageSize.isRequired()).isFalse();
    assertThat(pageSize.defaultValue()).isEqualTo("10");
    assertThat(pageSize.description()).isNotEmpty();
    assertThat(pageSize.exampleValue()).isNotEmpty();
  }

  @Test
  public void system_tag() {
    RuleDto r = db.rules().insert(setSystemTags("tag"), setTags());
    ruleIndexer.commitAndIndex(db.getSession(), r.getUuid());

    String result = ws.newRequest().execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag\"]}");
  }

  @Test
  public void tag() {
    RuleDto r = db.rules().insert(setSystemTags(), setTags("tag"));
    ruleIndexer.commitAndIndex(db.getSession(), r.getUuid());
    ruleIndexer.commitAndIndex(db.getSession(), r.getUuid());

    String result = ws.newRequest().execute().getInput();
    assertJson(result).isSimilarTo("{\"tags\":[\"tag\"]}");
  }
}
