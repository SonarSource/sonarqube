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
package org.sonar.server.issue.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.server.issue.IssueQueryFactory;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SearchActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private IssueIndex index = mock(IssueIndex.class);
  private IssueQueryFactory issueQueryFactory = mock(IssueQueryFactory.class);
  private SearchResponseLoader searchResponseLoader = mock(SearchResponseLoader.class);
  private SearchResponseFormat searchResponseFormat = mock(SearchResponseFormat.class);
  private SearchAction underTest = new SearchAction(userSession, index, issueQueryFactory, searchResponseLoader, searchResponseFormat);
  private WsActionTester wsTester = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action def = wsTester.getDef();
    assertThat(def.key()).isEqualTo("search");
    assertThat(def.isInternal()).isFalse();
    assertThat(def.isPost()).isFalse();
    assertThat(def.since()).isEqualTo("3.6");
    assertThat(def.responseExampleAsString()).isNotEmpty();

    assertThat(def.params()).extracting("key").containsExactlyInAnyOrder(
      "additionalFields", "asc", "assigned", "assignees", "authors", "componentKeys", "componentRootUuids", "componentRoots", "componentUuids", "components",
      "createdAfter", "createdAt", "createdBefore", "createdInLast", "directories", "facetMode", "facets", "fileUuids", "issues", "languages", "moduleUuids", "onComponentOnly",
      "organization", "p", "projectUuids", "projects", "ps", "resolutions", "resolved", "rules", "s", "severities", "sinceLeakPeriod",
      "statuses", "tags", "types");
    assertThat(def.param("organization"))
      .matches(Param::isInternal)
      .matches(p -> p.since().equals("6.4"));
  }

}
