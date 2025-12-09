/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbClient;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.issue.SearchRequest;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueQuery;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.core.rule.RuleType.SECURITY_HOTSPOT;
import static org.sonar.test.JsonAssert.assertJson;

public class ComponentTagsActionTest {
  private static final String[] ISSUE_RULE_TYPES = Arrays.stream(RuleType.values())
    .filter(t -> t != SECURITY_HOTSPOT)
    .map(Enum::name)
    .toArray(String[]::new);

  private final IssueIndex service = mock(IssueIndex.class);
  private final IssueQueryFactory issueQueryFactory = mock(IssueQueryFactory.class, Mockito.RETURNS_DEEP_STUBS);
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = mock(IssueIndexSyncProgressChecker.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final ComponentDao componentDao = mock(ComponentDao.class);
  private final ComponentTagsAction underTest = new ComponentTagsAction(service, issueIndexSyncProgressChecker,
    issueQueryFactory, dbClient);
  private final WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void before() {
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(componentDao.selectByUuid(any(), any())).thenAnswer((Answer<Optional<ComponentDto>>) invocation -> {
      Object[] args = invocation.getArguments();
      return Optional.of(ComponentTesting.newPrivateProjectDto((String) args[1]));
    });

    when(componentDao.selectByUuid(any(), eq("not-exists")))
      .thenAnswer((Answer<Optional<ComponentDto>>) invocation -> Optional.empty());
  }

  @Test
  public void should_define() {
    Action action = tester.getDef();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.handler()).isEqualTo(underTest);
    assertThat(action.params()).hasSize(3);

    Param query = action.param("componentUuid");
    assertThat(query.isRequired()).isTrue();
    assertThat(query.description()).isNotEmpty();
    assertThat(query.exampleValue()).isNotEmpty();
    Param createdAfter = action.param("createdAfter");
    assertThat(createdAfter.isRequired()).isFalse();
    assertThat(createdAfter.description()).isNotEmpty();
    assertThat(createdAfter.exampleValue()).isNotEmpty();
    Param pageSize = action.param("ps");
    assertThat(pageSize.isRequired()).isFalse();
    assertThat(pageSize.defaultValue()).isEqualTo("10");
    assertThat(pageSize.description()).isNotEmpty();
    assertThat(pageSize.exampleValue()).isNotEmpty();
  }

  @Test
  public void should_return_empty_list() {
    TestResponse response = tester.newRequest()
      .setParam("componentUuid", "not-exists")
      .execute();
    assertJson(response.getInput()).isSimilarTo("{\"tags\":[]}");
    verify(issueIndexSyncProgressChecker).checkIfIssueSyncInProgress(any());
  }

  @Test
  public void should_return_tag_list() {
    Map<String, Long> tags = ImmutableMap.<String, Long>builder()
      .put("convention", 2771L)
      .put("brain-overload", 998L)
      .put("cwe", 89L)
      .put("bug", 32L)
      .put("cert", 2L)
      .build();
    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(issueQueryFactory.create(captor.capture())).thenReturn(mock(IssueQuery.class));
    when(service.countTags(any(IssueQuery.class), eq(5))).thenReturn(tags);

    TestResponse response = tester.newRequest()
      .setParam("componentUuid", "polop")
      .setParam("ps", "5")
      .execute();
    assertJson(response.getInput()).isSimilarTo(getClass().getResource("ComponentTagsActionTest/component-tags.json"));

    assertThat(captor.getValue().getTypes()).containsExactlyInAnyOrder(ISSUE_RULE_TYPES);
    assertThat(captor.getValue().getComponentUuids()).containsOnly("polop");
    assertThat(captor.getValue().getResolved()).isFalse();
    assertThat(captor.getValue().getCreatedAfter()).isNull();
    verify(issueIndexSyncProgressChecker).checkIfComponentNeedIssueSync(any(), eq("KEY_polop"));
  }

  @Test
  public void should_return_tag_list_with_created_after() {
    Map<String, Long> tags = ImmutableMap.<String, Long>builder()
      .put("convention", 2771L)
      .put("brain-overload", 998L)
      .put("cwe", 89L)
      .put("bug", 32L)
      .put("cert", 2L)
      .build();
    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    when(issueQueryFactory.create(captor.capture())).thenReturn(mock(IssueQuery.class));
    when(service.countTags(any(IssueQuery.class), eq(5))).thenReturn(tags);

    String componentUuid = "polop";
    String createdAfter = "2011-04-25";
    TestResponse response = tester.newRequest()
      .setParam("componentUuid", componentUuid)
      .setParam("createdAfter", createdAfter)
      .setParam("ps", "5")
      .execute();
    assertJson(response.getInput()).isSimilarTo(getClass().getResource("ComponentTagsActionTest/component-tags.json"));
    assertThat(captor.getValue().getTypes()).containsExactlyInAnyOrder(ISSUE_RULE_TYPES);
    assertThat(captor.getValue().getComponentUuids()).containsOnly(componentUuid);
    assertThat(captor.getValue().getResolved()).isFalse();
    assertThat(captor.getValue().getCreatedAfter()).isEqualTo(createdAfter);
    verify(issueIndexSyncProgressChecker).checkIfComponentNeedIssueSync(any(), eq("KEY_polop"));
  }
}
