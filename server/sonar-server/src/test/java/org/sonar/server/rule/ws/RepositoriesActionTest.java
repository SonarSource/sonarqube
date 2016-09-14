/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.server.ws.WsTester;
import org.sonar.server.ws.WsTester.TestRequest;

import static java.util.Arrays.asList;

public class RepositoriesActionTest {

  private WsTester wsTester;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Before
  public void setUp() {
    wsTester = new WsTester(new RulesWs(new RepositoriesAction(dbTester.getDbClient())));
  }

  @Test
  public void should_list_repositories() throws Exception {
    DbSession dbSession = dbTester.getSession();
    RuleRepositoryDto repo1 = new RuleRepositoryDto("xoo", "xoo", "SonarQube");
    RuleRepositoryDto repo2 = new RuleRepositoryDto("squid", "ws", "SonarQube");
    RuleRepositoryDto repo3 = new RuleRepositoryDto("common-ws", "ws", "SonarQube Common");
    dbTester.getDbClient().ruleRepositoryDao().insert(dbSession, asList(repo1, repo2, repo3));
    dbSession.commit();

    wsTester = new WsTester(new RulesWs(new RepositoriesAction(dbTester.getDbClient())));

    newRequest().execute().assertJson(this.getClass(), "repositories.json");
    newRequest().setParam("language", "xoo").execute().assertJson(this.getClass(), "repositories_xoo.json");
    newRequest().setParam("language", "ws").execute().assertJson(this.getClass(), "repositories_ws.json");
    newRequest().setParam("q", "common").execute().assertJson(this.getClass(), "repositories_common.json");
    newRequest().setParam("q", "squid").execute().assertJson(this.getClass(), "repositories_squid.json");
    newRequest().setParam("q", "sonar").execute().assertJson(this.getClass(), "repositories_sonar.json");
    newRequest().setParam("ps", "4").execute().assertJson(this.getClass(), "repositories.json");
    newRequest().setParam("ps", "100").execute().assertJson(this.getClass(), "repositories.json");
  }

  protected TestRequest newRequest() {
    return wsTester.newGetRequest("api/rules", "repositories");
  }
}
