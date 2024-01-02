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
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.test.JsonAssert.assertJson;

public class AppActionTest {

  private static final Language LANG1 = LanguageTesting.newLanguage("xoo", "Xoo");
  private static final Language LANG2 = LanguageTesting.newLanguage("ws", "Whitespace");

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private Languages languages = new Languages(LANG1, LANG2);
  private AppAction underTest = new AppAction(languages, db.getDbClient(), userSession);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.key()).isEqualTo("app");
    assertThat(definition.params()).isEmpty();
  }

  @Test
  public void response_contains_rule_repositories() {
    insertRules();

    String json = ws.newRequest().execute().getInput();
    assertJson(json).isSimilarTo("{" +
      "\"repositories\": [" +
      "    {" +
      "      \"key\": \"xoo\"," +
      "      \"name\": \"SonarQube\"," +
      "      \"language\": \"xoo\"" +
      "    }," +
      "    {" +
      "      \"key\": \"java\"," +
      "      \"name\": \"SonarQube\"," +
      "      \"language\": \"ws\"" +
      "    }" +
      "  ]" +
      "}");
  }

  @Test
  public void response_contains_languages() {
    String json = ws.newRequest().execute().getInput();

    assertJson(json).isSimilarTo("{" +
      "\"languages\": {" +
      "    \"xoo\": \"Xoo\"," +
      "    \"ws\": \"Whitespace\"" +
      "  }" +
      "}");
  }

  @Test
  public void canWrite_is_true_if_user_is_profile_administrator() {
    userSession.addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);

    String json = ws.newRequest().execute().getInput();

    assertJson(json).isSimilarTo("{ \"canWrite\": true }");
  }

  @Test
  public void canWrite_is_false_if_user_is_not_profile_administrator() {
    userSession.addPermission(GlobalPermission.SCAN);
    String json = ws.newRequest().execute().getInput();

    assertJson(json).isSimilarTo("{ \"canWrite\": false }");
  }

  private void insertRules() {
    RuleRepositoryDto repo1 = new RuleRepositoryDto("xoo", "xoo", "SonarQube");
    RuleRepositoryDto repo2 = new RuleRepositoryDto("java", "ws", "SonarQube");
    db.getDbClient().ruleRepositoryDao().insert(db.getSession(), asList(repo1, repo2));
    db.getSession().commit();
  }

}
