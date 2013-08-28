/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.component;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.core.resource.ResourceDao;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DefaultRubyComponentServiceTest {

  private ResourceDao resourceDao;
  private DefaultComponentFinder finder;
  private DefaultRubyComponentService componentService;

  @Before
  public void before() {
    resourceDao = mock(ResourceDao.class);
    finder = mock(DefaultComponentFinder.class);
    componentService = new DefaultRubyComponentService(resourceDao, finder);
  }

  @Test
  public void should_find_by_key() {
    Component component = mock(Component.class);
    when(resourceDao.findByKey("struts")).thenReturn(component);

    assertThat(componentService.findByKey("struts")).isEqualTo(component);
  }

  @Test
  public void should_find() {
    List<String> qualifiers = newArrayList("TRK");

    Map<String, Object> map = newHashMap();
    map.put("keys", newArrayList("org.codehaus.sonar"));
    map.put("names", newArrayList("Sonar"));
    map.put("qualifiers", qualifiers);
    map.put("pageSize", 10l);
    map.put("pageIndex", 50);
    map.put("sort", "NAME");
    map.put("asc", true);

    componentService.find(map);
    verify(resourceDao).selectProjectsByQualifiers(anyListOf(String.class));
    verify(finder).find(any(ComponentQuery.class), anyListOf(Component.class));
  }

  @Test
  public void should_find_with_uncomplete_projects() {
    List<String> qualifiers = newArrayList("TRK");

    Map<String, Object> map = newHashMap();
    map.put("keys", newArrayList("org.codehaus.sonar"));
    map.put("names", newArrayList("Sonar"));
    map.put("qualifiers", qualifiers);
    map.put("pageSize", 10l);
    map.put("pageIndex", 50);
    map.put("sort", "NAME");
    map.put("asc", true);

    componentService.findWithUncompleteProjects(map);
    verify(resourceDao).selectProjectsIncludingNotCompletedOnesByQualifiers(anyListOf(String.class));
    verify(finder).find(any(ComponentQuery.class), anyListOf(Component.class));
  }

  @Test
  public void should_find_ghosts_projects() {
    List<String> qualifiers = newArrayList("TRK");

    Map<String, Object> map = newHashMap();
    map.put("keys", newArrayList("org.codehaus.sonar"));
    map.put("names", newArrayList("Sonar"));
    map.put("qualifiers", qualifiers);
    map.put("pageSize", 10l);
    map.put("pageIndex", 50);
    map.put("sort", "NAME");
    map.put("asc", true);

    componentService.findGhostsProjects(map);
    verify(resourceDao).selectGhostsProjects(anyListOf(String.class));
    verify(finder).find(any(ComponentQuery.class), anyListOf(Component.class));
  }

  @Test
  public void should_create_query_from_parameters() {
    Map<String, Object> map = newHashMap();
    map.put("keys", newArrayList("org.codehaus.sonar"));
    map.put("names", newArrayList("Sonar"));
    map.put("qualifiers", newArrayList("TRK"));
    map.put("pageSize", 10l);
    map.put("pageIndex", 50);
    map.put("sort", "NAME");
    map.put("asc", true);

    ComponentQuery query = DefaultRubyComponentService.toQuery(map);
    assertThat(query.keys()).containsOnly("org.codehaus.sonar");
    assertThat(query.names()).containsOnly("Sonar");
    assertThat(query.qualifiers()).containsOnly("TRK");
    assertThat(query.pageSize()).isEqualTo(10);
    assertThat(query.pageIndex()).isEqualTo(50);
    assertThat(query.sort()).isEqualTo(ComponentQuery.SORT_BY_NAME);
    assertThat(query.asc()).isTrue();
  }

  @Test
  public void should_create_query_with_default_paging_from_parameters() {
    Map<String, Object> map = newHashMap();
    map.put("keys", newArrayList("org.codehaus.sonar"));
    map.put("names", newArrayList("Sonar"));
    map.put("qualifiers", newArrayList("TRK"));

    ComponentQuery query = DefaultRubyComponentService.toQuery(map);
    assertThat(query.keys()).containsOnly("org.codehaus.sonar");
    assertThat(query.names()).containsOnly("Sonar");
    assertThat(query.qualifiers()).containsOnly("TRK");
    assertThat(query.pageSize()).isEqualTo(100);
    assertThat(query.pageIndex()).isEqualTo(1);
    assertThat(query.sort()).isEqualTo(ComponentQuery.SORT_BY_NAME);
    assertThat(query.asc()).isTrue();
  }
}
