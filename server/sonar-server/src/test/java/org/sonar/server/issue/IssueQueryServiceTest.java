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

package org.sonar.server.issue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.assertj.core.api.Fail;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.AuthorDao;
import org.sonar.server.component.ComponentService;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.MockUserSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

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

  IssueQueryService issueQueryService;

  @Before
  public void setUp() throws Exception {
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

    issueQueryService = new IssueQueryService(dbClient, componentService);
  }

  @After
  public void tearDown() {
    MockUserSession.set();
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
    map.put("reporters", newArrayList("marilyn"));
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
    assertThat(query.reporters()).containsOnly("marilyn");
    assertThat(query.assignees()).containsOnly("joanna");
    assertThat(query.languages()).containsOnly("xoo");
    assertThat(query.tags()).containsOnly("tag1", "tag2");
    assertThat(query.onComponentOnly()).isFalse();
    assertThat(query.assigned()).isTrue();
    assertThat(query.planned()).isTrue();
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
  public void should_search_in_tree_with_component_root_uuids_but_unknown_qualifiers() throws Exception {
    Map<String, Object> map = newHashMap();
    map.put("componentRootUuids", newArrayList("ABCD"));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.<String>newHashSet());

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.onComponentOnly()).isFalse();
    assertThat(query.componentUuids()).contains("ABCD");
  }

  @Test
  public void should_search_in_tree_with_component_roots_but_different_qualifiers() throws Exception {
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
  public void should_search_in_tree_with_view() throws Exception {
    String viewUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentRootUuids", newArrayList(viewUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.VIEW));

    MockUserSession.set().addProjectUuidPermissions(UserRole.USER, viewUuid);

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.viewUuids()).containsExactly(viewUuid);
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void should_search_in_tree_with_subview_but_bad_permissions() throws Exception {
    String subViewUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentRootUuids", newArrayList(subViewUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.VIEW));

    MockUserSession.set();

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.viewUuids()).isNotEmpty().doesNotContain(subViewUuid);
  }

  @Test
  public void should_search_in_tree_with_project_uuid() throws Exception {
    String projectUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentUuids", newArrayList(projectUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.PROJECT));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.projectUuids()).containsExactly(projectUuid);
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void should_search_on_component_only_with_project_key() throws Exception {
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
  public void should_search_on_developer() throws Exception {
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
  public void should_override_authors_when_searching_on_developer() throws Exception {
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
  public void should_search_in_tree_with_module_uuid() throws Exception {
    String moduleUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentUuids", newArrayList(moduleUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.MODULE));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.moduleRootUuids()).containsExactly(moduleUuid);
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void should_search_in_tree_with_directory_uuid() throws Exception {
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
  public void should_search_on_source_file() throws Exception {
    String fileUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentUuids", newArrayList(fileUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.FILE));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.fileUuids()).containsExactly(fileUuid);
  }

  @Test
  public void should_search_on_test_file() throws Exception {
    String fileUuid = "ABCD";
    Map<String, Object> map = newHashMap();
    map.put("componentUuids", newArrayList(fileUuid));

    when(componentService.getDistinctQualifiers(isA(DbSession.class), anyCollection())).thenReturn(Sets.newHashSet(Qualifiers.UNIT_TEST_FILE));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.fileUuids()).containsExactly(fileUuid);
  }
}
