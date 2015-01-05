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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.ComponentService;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
  ComponentService componentService;

  IssueQueryService issueQueryService;

  @Before
  public void setUp() throws Exception {
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.componentDao()).thenReturn(componentDao);

    when(componentService.componentUuids(any(DbSession.class), any(Collection.class), eq(true))).thenAnswer(new Answer<Collection<String>>() {
      @Override
      public Collection<String> answer(InvocationOnMock invocation) throws Throwable {
        Collection<String> componentKeys = (Collection<String>) invocation.getArguments()[1];
        return componentKeys == null ? Arrays.<String>asList() : componentKeys;
      }
    });

    issueQueryService = new IssueQueryService(dbClient, componentService);
  }

  @Test
  public void create_query_from_parameters() {
    Map<String, Object> map = newHashMap();
    map.put("issues", newArrayList("ABCDE1234"));
    map.put("severities", newArrayList("MAJOR", "MINOR"));
    map.put("statuses", newArrayList("CLOSED"));
    map.put("resolutions", newArrayList("FALSE-POSITIVE"));
    map.put("resolved", true);
    ArrayList<String> componentKeys = newArrayList("org.apache");
    map.put("components", componentKeys);
    ArrayList<String> moduleKeys = newArrayList("org.sonar");
    map.put("componentRoots", moduleKeys);
    map.put("reporters", newArrayList("marilyn"));
    map.put("assignees", newArrayList("joanna"));
    map.put("languages", newArrayList("xoo"));
    map.put("tags", newArrayList("tag1", "tag2"));
    map.put("onComponentOnly", true);
    map.put("assigned", true);
    map.put("planned", true);
    map.put("hideRules", true);
    map.put("createdAfter", "2013-04-16T09:08:24+0200");
    map.put("createdBefore", "2013-04-17T09:08:24+0200");
    map.put("rules", "squid:AvoidCycle,findbugs:NullReference");
    map.put("sort", "CREATION_DATE");
    map.put("asc", true);

    when(componentService.componentUuids(session, componentKeys, true)).thenReturn(newArrayList("ABCD"));
    when(componentService.componentUuids(session, moduleKeys, true)).thenReturn(newArrayList("BCDE"));

    IssueQuery query = issueQueryService.createFromMap(map);
    assertThat(query.issueKeys()).containsOnly("ABCDE1234");
    assertThat(query.severities()).containsOnly("MAJOR", "MINOR");
    assertThat(query.statuses()).containsOnly("CLOSED");
    assertThat(query.resolutions()).containsOnly("FALSE-POSITIVE");
    assertThat(query.resolved()).isTrue();
    assertThat(query.componentUuids()).containsOnly("ABCD");
    assertThat(query.moduleUuids()).containsOnly("BCDE");
    assertThat(query.reporters()).containsOnly("marilyn");
    assertThat(query.assignees()).containsOnly("joanna");
    assertThat(query.languages()).containsOnly("xoo");
    assertThat(query.tags()).containsOnly("tag1", "tag2");
    assertThat(query.onComponentOnly()).isTrue();
    assertThat(query.assigned()).isTrue();
    assertThat(query.planned()).isTrue();
    assertThat(query.hideRules()).isTrue();
    assertThat(query.rules()).hasSize(2);
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

    when(componentService.componentUuids(any(DbSession.class), eq(componentKeys), eq(true))).thenReturn(Arrays.<String>asList());

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

}
