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
package org.sonar.server.issue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import org.assertj.core.api.Fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.AuthorDao;
import org.sonar.server.component.ComponentService;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.UserSessionRule;
import org.sonarqube.ws.client.issue.SearchWsRequest;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueQueryServiceTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  DbClient dbClient;

  @Mock
  DbSession session;

  @Mock
  ComponentDao componentDao;

  @Mock
  AuthorDao authorDao;

  @Mock
  ComponentService componentService;

  @Mock
  System2 system;

  IssueQueryService issueQueryService;

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.authorDao()).thenReturn(authorDao);

    when(componentService.componentUuids(any(DbSession.class), any(Collection.class), eq(true))).thenAnswer(new Answer<Collection<String>>() {
      @Override
      public Collection<String> answer(InvocationOnMock invocation) throws Throwable {
        Collection<String> componentKeys = (Collection<String>) invocation.getArguments()[1];
        return componentKeys == null ? Arrays.<String>asList() : componentKeys;
      }
    });

    issueQueryService = new IssueQueryService(dbClient, componentService, system, userSessionRule);
  }

  @Test
  public void create_query_from_parameters() {
    Map<String, Object> map = newHashMap();
    map.put("issues", newArrayList("ABCDE1234"));
    map.put("severities", newArrayList("MAJOR", "MINOR"));
    map.put("statuses", newArrayList("CLOSED"));
    map.put("resolutions", newArrayList("FALSE-POSITIVE"));
    map.put("resolved", true);
    ArrayList<String> projectKeys = newArrayList("org.apache");
    map.put("projectKeys", projectKeys);
    ArrayList<String> moduleUuids = newArrayList("BCDE");
    map.put("moduleUuids", moduleUuids);
    map.put("directories", newArrayList("/src/main/java/example"));
    ArrayList<String> fileUuids = newArrayList("CDEF");
    map.put("fileUuids", fileUuids);
    map.put("assignees", newArrayList("joanna"));
    map.put("languages", newArrayList("xoo"));
    map.put("tags", newArrayList("tag1", "tag2"));
    map.put("assigned", true);
    map.put("planned", true);
    map.put("hideRules", true);
    map.put("createdAfter", "2013-04-16T09:08:24+0200");
    map.put("createdBefore", "2013-04-17T09:08:24+0200");
    map.put("rules", "squid:AvoidCycle,findbugs:NullReference");
    map.put("sort", "CREATION_DATE");
    map.put("asc", true);

    when(componentService.componentUuids(eq(session), Matchers.anyCollection(), eq(true))).thenAnswer(new Answer<Collection<String>>() {
      @Override
      public Collection<String> answer(InvocationOnMock invocation) throws Throwable {
        Collection<String> components = (Collection<String>) invocation.getArguments()[1];
        if (components == null) {
          return newArrayList();
        }
        if (components.contains("org.apache")) {
          return newArrayList("ABCD");
        }
        return newArrayList();
      }
    });

    when(componentService.getDistinctQualifiers(eq(session), Matchers.anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.PROJECT));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.issueKeys()).containsOnly("ABCDE1234");
    assertThat(query.severities()).containsOnly("MAJOR", "MINOR");
    assertThat(query.statuses()).containsOnly("CLOSED");
    assertThat(query.resolutions()).containsOnly("FALSE-POSITIVE");
    assertThat(query.resolved()).isTrue();
    assertThat(query.projectUuids()).containsOnly("ABCD");
    assertThat(query.moduleUuids()).containsOnly("BCDE");
    assertThat(query.fileUuids()).containsOnly("CDEF");
    assertThat(query.assignees()).containsOnly("joanna");
    assertThat(query.languages()).containsOnly("xoo");
    assertThat(query.tags()).containsOnly("tag1", "tag2");
    assertThat(query.onComponentOnly()).isFalse();
    assertThat(query.assigned()).isTrue();
    assertThat(query.hideRules()).isTrue();
    assertThat(query.rules()).hasSize(2);
    assertThat(query.directories()).containsOnly("/src/main/java/example");
    assertThat(query.createdAfter()).isEqualTo(DateUtils.parseDateTime("2013-04-16T09:08:24+0200"));
    assertThat(query.createdBefore()).isEqualTo(DateUtils.parseDateTime("2013-04-17T09:08:24+0200"));
    assertThat(query.sort()).isEqualTo(IssueQuery.SORT_BY_CREATION_DATE);
    assertThat(query.asc()).isTrue();
  }

  @Test
  public void add_unknown_when_no_component_found() {
    Map<String, Object> map = newHashMap();
    ArrayList<String> componentKeys = newArrayList("unknown");
    map.put("components", componentKeys);

    when(componentService.componentUuids(eq(session), Matchers.anyCollection(), eq(true))).thenAnswer(new Answer<Collection<String>>() {
      @Override
      public Collection<String> answer(InvocationOnMock invocation) throws Throwable {
        return newArrayList();
      }
    });

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.componentUuids()).containsOnly("<UNKNOWN>");
  }

  @Test
  public void parse_list_of_rules() {
    assertThat(IssueQueryService.toRules(null)).isNull();
    assertThat(IssueQueryService.toRules("")).isEmpty();
    assertThat(IssueQueryService.toRules("squid:AvoidCycle")).containsOnly(RuleKey.of("squid", "AvoidCycle"));
    assertThat(IssueQueryService.toRules("squid:AvoidCycle,findbugs:NullRef")).containsOnly(RuleKey.of("squid", "AvoidCycle"), RuleKey.of("findbugs", "NullRef"));
    assertThat(IssueQueryService.toRules(asList("squid:AvoidCycle", "findbugs:NullRef"))).containsOnly(RuleKey.of("squid", "AvoidCycle"), RuleKey.of("findbugs", "NullRef"));
  }

  @Test
  public void fail_if_components_and_components_uuid_params_are_set_at_the_same_time() {
    Map<String, Object> map = newHashMap();
    ArrayList<String> componentKeys = newArrayList("org.apache");
    map.put("components", componentKeys);
    map.put("componentUuids", newArrayList("ABCD"));

    try {
      issueQueryService.createFromMap(map);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("components and componentUuids cannot be set simultaneously");
    }
  }

  @Test
  public void fail_if_projects_and_project_uuids_params_are_set_at_the_same_time() {
    Map<String, Object> map = newHashMap();
    map.put("projects", newArrayList("org.apache"));
    map.put("projectUuids", newArrayList("ABCD"));

    try {
      issueQueryService.createFromMap(map);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("projects and projectUuids cannot be set simultaneously");
    }
  }

  @Test
  public void fail_if_component_roots_and_component_root_uuids_params_are_set_at_the_same_time() {
    Map<String, Object> map = newHashMap();
    map.put("componentRoots", newArrayList("org.apache"));
    map.put("componentRootUuids", newArrayList("ABCD"));

    try {
      issueQueryService.createFromMap(map);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("componentRoots and componentRootUuids cannot be set simultaneously");
    }
  }

  @Test
  public void should_search_in_tree_with_component_root_uuids_but_unknown_qualifiers() {
    Map<String, Object> map = newHashMap();
    map.put("componentRootUuids", newArrayList("ABCD"));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.<String>newHashSet());

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.onComponentOnly()).isFalse();
    assertThat(query.componentUuids()).contains("ABCD");
  }

  @Test
  public void should_search_in_tree_with_component_roots_but_different_qualifiers() {
    Map<String, Object> map = newHashMap();
    map.put("componentRoots", newArrayList("org.apache.struts:struts", "org.codehaus.sonar:sonar-server"));

    when(componentService.componentUuids(isA(DbSession.class), anyCollection(), eq(true))).thenReturn(Sets.newHashSet("ABCD", "BCDE"));
    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newTreeSet(Arrays.asList("TRK", "BRC")));

    try {
      issueQueryService.createFromMap(map);
      Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException exception) {
      assertThat(exception).hasMessage("All components must have the same qualifier, found BRC,TRK");
    }
  }

  @Test
  public void should_search_in_tree_with_view() {
    String viewUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentRootUuids", newArrayList(viewUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.VIEW));

    userSessionRule.addProjectUuidPermissions(UserRole.USER, viewUuid);

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.viewUuids()).containsExactly(viewUuid);
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void should_search_in_tree_with_subview_but_bad_permissions() {
    String subViewUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentRootUuids", newArrayList(subViewUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.VIEW));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.viewUuids()).isNotEmpty().doesNotContain(subViewUuid);
  }

  @Test
  public void should_search_in_tree_with_project_uuid() {
    String projectUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentUuids", newArrayList(projectUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.PROJECT));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.projectUuids()).containsExactly(projectUuid);
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void should_search_on_component_only_with_project_key() {
    String projectKey = "org.apache.struts:struts";
    String projectUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentKeys", newArrayList(projectKey));
    map.put("onComponentOnly", true);

    when(componentService.componentUuids(isA(DbSession.class), anyCollection(), eq(true))).thenReturn(Sets.newHashSet(projectUuid));
    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.PROJECT));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.projectUuids()).isEmpty();
    assertThat(query.componentUuids()).containsExactly(projectUuid);
    assertThat(query.onComponentOnly()).isTrue();
  }

  @Test
  public void should_search_on_developer() {
    String devUuid = "DEV:anakin.skywalker";
    String login1 = "anakin@skywalker.name";
    String login2 = "darth.vader";
    Map<String, Object> map = newHashMap();
    map.put("componentUuids", newArrayList(devUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet("DEV"));
    when(authorDao.selectScmAccountsByDeveloperUuids(isA(DbSession.class), anyCollection())).thenReturn(Lists.newArrayList(login1, login2));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.authors()).containsExactly(login1, login2);
  }

  @Test
  public void should_override_authors_when_searching_on_developer() {
    String devUuid = "DEV:anakin.skywalker";
    String login = "anakin@skywalker.name";
    Map<String, Object> map = newHashMap();
    map.put("componentUuids", newArrayList(devUuid));
    map.put("authors", newArrayList(login));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet("DEV"));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.authors()).containsExactly(login);
  }

  @Test
  public void should_search_on_developer_technical_project() {
    String projectUuid = "sample1";
    String devUuid = "DEV:anakin.skywalker";
    String login1 = "anakin@skywalker.name";
    String login2 = "darth.vader";
    String copyProjectUuid = devUuid + ":" + projectUuid;

    long copyResourceId = 42L;
    ComponentDto technicalProject = new ComponentDto().setProjectUuid(devUuid).setCopyResourceId(copyResourceId);
    when(componentDao.selectByUuids(isA(DbSession.class), anyCollection())).thenReturn(Arrays.asList(technicalProject));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet("DEV_PRJ"));
    when(authorDao.selectScmAccountsByDeveloperUuids(isA(DbSession.class), anyCollection())).thenReturn(Lists.newArrayList(login1, login2));

    ComponentDto actualProject = new ComponentDto().setUuid(projectUuid);
    when(componentDao.selectByIds(isA(DbSession.class), anyCollection())).thenReturn(Arrays.asList(actualProject));

    Map<String, Object> map = newHashMap();
    map.put("componentUuids", newArrayList(copyProjectUuid));
    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.authors()).containsExactly(login1, login2);
    assertThat(query.projectUuids()).containsExactly(projectUuid);
  }

  @Test
  public void should_search_in_tree_with_module_uuid() {
    String moduleUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentUuids", newArrayList(moduleUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.MODULE));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.moduleRootUuids()).containsExactly(moduleUuid);
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void should_search_in_tree_with_directory_uuid() {
    String directoryUuid = "ABCD";
    String directoryPath = "/some/module/relative/path";
    String moduleUuid = "BCDE";
    Map<String, Object> map = newHashMap();
    map.put("componentUuids", newArrayList(directoryUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.DIRECTORY));
    when(componentService.getByUuids(isA(DbSession.class), anyCollection())).thenReturn(Arrays.asList(new ComponentDto().setModuleUuid(moduleUuid).setPath(directoryPath)));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.moduleUuids()).contains(moduleUuid);
    assertThat(query.directories()).containsExactly(directoryPath);
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void should_search_on_source_file() {
    String fileUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentUuids", newArrayList(fileUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.FILE));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.fileUuids()).containsExactly(fileUuid);
  }

  @Test
  public void should_search_on_test_file() {
    String fileUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentUuids", newArrayList(fileUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.UNIT_TEST_FILE));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.fileUuids()).containsExactly(fileUuid);
  }

  @Test
  public void fail_if_created_after_and_created_since_are_both_set() {
    Map<String, Object> map = newHashMap();
    map.put("createdAfter", "2013-07-25T07:35:00+0100");
    map.put("createdInLast", "palap");

    try {
      issueQueryService.createFromMap(map);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("createdAfter and createdInLast cannot be set simultaneously");
    }
  }

  @Test
  public void set_created_after_from_created_since() {
    Date now = DateUtils.parseDateTime("2013-07-25T07:35:00+0100");
    when(system.now()).thenReturn(now.getTime());
    Map<String, Object> map = newHashMap();

    map.put("createdInLast", "1y2m3w4d");
    assertThat(issueQueryService.createFromMap(map).createdAfter()).isEqualTo(DateUtils.parseDateTime("2012-04-30T07:35:00+0100"));
  }

  @Test
  public void fail_if_since_leak_period_and_created_after_set_at_the_same_time() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("'createdAfter' and 'sinceLeakPeriod' cannot be set simultaneously");

    issueQueryService.createFromRequest(new SearchWsRequest()
      .setSinceLeakPeriod(true)
      .setCreatedAfter("2013-07-25T07:35:00+0100"));
  }

  @Test
  public void fail_if_no_component_provided_with_since_leak_period() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("One and only one component must be provided when searching since leak period");

    issueQueryService.createFromRequest(new SearchWsRequest().setSinceLeakPeriod(true));
  }

  @Test
  public void fail_if_several_components_provided_with_since_leak_period() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("One and only one component must be provided when searching since leak period");

    issueQueryService.createFromRequest(new SearchWsRequest()
      .setSinceLeakPeriod(true)
      .setComponentUuids(Collections.singletonList("component-uuid"))
      .setProjectUuids(Collections.singletonList("project-uuid"))
      );
  }
}
