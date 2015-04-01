/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.ws.WsTester;
import org.sonar.server.ws.WsTester.TestRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RepositoriesActionTest {

  private WsTester tester;

  @Mock
  private RuleRepositories repositories;

  @Before
  public void setUp() {
    tester = new WsTester(new RulesWebService(new RepositoriesAction(repositories)));

    RuleRepositories.Repository repo1 = mock(RuleRepositories.Repository.class);
    when(repo1.key()).thenReturn("xoo");
    when(repo1.name()).thenReturn("SonarQube");
    when(repo1.language()).thenReturn("xoo");

    RuleRepositories.Repository repo2 = mock(RuleRepositories.Repository.class);
    when(repo2.key()).thenReturn("squid");
    when(repo2.name()).thenReturn("SonarQube");
    when(repo2.language()).thenReturn("ws");

    RuleRepositories.Repository repo3 = mock(RuleRepositories.Repository.class);
    when(repo3.key()).thenReturn("common-ws");
    when(repo3.name()).thenReturn("SonarQube Common");
    when(repo3.language()).thenReturn("ws");

    when(repositories.repositories()).thenReturn(ImmutableList.of(repo1, repo2, repo3));
    when(repositories.repositoriesForLang("xoo")).thenReturn(ImmutableList.of(repo1));
    when(repositories.repositoriesForLang("ws")).thenReturn(ImmutableList.of(repo2, repo3));
  }

  @Test
  public void should_list_repositories() throws Exception {

    tester = new WsTester(new RulesWebService(new RepositoriesAction(repositories)));

    newRequest().execute().assertJson(this.getClass(), "repositories.json");
    newRequest().setParam("language", "xoo").execute().assertJson(this.getClass(), "repositories_xoo.json");
    newRequest().setParam("language", "ws").execute().assertJson(this.getClass(), "repositories_ws.json");
    newRequest().setParam("q", "common").execute().assertJson(this.getClass(), "repositories_common.json");
    newRequest().setParam("q", "squid").execute().assertJson(this.getClass(), "repositories_squid.json");
    newRequest().setParam("q", "sonar").execute().assertJson(this.getClass(), "repositories_sonar.json");
    newRequest().setParam("q", "manu").execute().assertJson(this.getClass(), "repositories_manual.json");
    newRequest().setParam("q", "sonar").setParam("ps", "2").execute().assertJson(this.getClass(), "repositories_limited.json");
    newRequest().setParam("ps", "4").execute().assertJson(this.getClass(), "repositories.json");
    newRequest().setParam("ps", "100").execute().assertJson(this.getClass(), "repositories.json");
  }

  protected TestRequest newRequest() {
    return tester.newGetRequest("api/rules", "repositories");
  }
}
