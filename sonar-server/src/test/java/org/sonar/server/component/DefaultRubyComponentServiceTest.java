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

import org.sonar.server.exceptions.NotFoundException;

import org.sonar.server.exceptions.BadRequestException;
import org.sonar.core.resource.ResourceDto;
import org.mockito.ArgumentCaptor;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.resource.ResourceIndexerDao;
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
  private ResourceIndexerDao resourceIndexerDao;
  private DefaultRubyComponentService componentService;

  @Before
  public void before() {
    resourceDao = mock(ResourceDao.class);
    finder = mock(DefaultComponentFinder.class);
    resourceIndexerDao = mock(ResourceIndexerDao.class);
    componentService = new DefaultRubyComponentService(resourceDao, finder, resourceIndexerDao);
  }

  @Test
  public void should_find_by_key() {
    Component<?> component = mock(Component.class);
    when(resourceDao.findByKey("struts")).thenReturn(component);

    assertThat(componentService.findByKey("struts")).isEqualTo(component);
  }

  @Test
  public void should_create_component_and_index_it() {
    String componentKey = "new-project";
    String componentName = "New Project";
    String scope = Scopes.PROJECT;
    String qualifier = Qualifiers.PROJECT;
    long componentId = Long.MAX_VALUE;
    ComponentDto component = mock(ComponentDto.class);
    when(component.getId()).thenReturn(componentId);
    when(resourceDao.findByKey(componentKey)).thenReturn(null).thenReturn(component);

    componentService.createComponent(componentKey, componentName, scope, qualifier);

    ArgumentCaptor<ResourceDto> resourceCaptor = ArgumentCaptor.forClass(ResourceDto.class);
    verify(resourceDao).insertOrUpdate(resourceCaptor.capture());
    ResourceDto created = resourceCaptor.getValue();
    assertThat(created.getKey()).isEqualTo(componentKey);
    assertThat(created.getName()).isEqualTo(componentName);
    assertThat(created.getLongName()).isEqualTo(componentName);
    assertThat(created.getScope()).isEqualTo(scope);
    assertThat(created.getQualifier()).isEqualTo(qualifier);
    verify(resourceDao, times(2)).findByKey(componentKey);
    verify(resourceIndexerDao).indexResource(componentId);
  }

  @Test(expected = BadRequestException.class)
  public void should_thow_if_create_fails() {
    String componentKey = "new-project";
    String componentName = "New Project";
    String scope = Scopes.PROJECT;
    String qualifier = Qualifiers.PROJECT;
    when(resourceDao.findByKey(componentKey)).thenReturn(null);

    componentService.createComponent(componentKey, componentName, scope, qualifier);
  }

  @Test(expected = BadRequestException.class)
  public void should_throw_if_component_already_exists() {
    String componentKey = "new-project";
    String componentName = "New Project";
    String scope = Scopes.PROJECT;
    String qualifier = Qualifiers.PROJECT;
    when(resourceDao.findByKey(componentKey)).thenReturn(mock(ComponentDto.class));

    componentService.createComponent(componentKey, componentName, scope, qualifier);
  }

  @Test(expected = NotFoundException.class)
  public void should_throw_if_updating_unknown_component() {
    final long componentId = 1234l;
    when(resourceDao.getResource(componentId)).thenReturn(null);
    componentService.updateComponent(componentId, "key", "name");
  }

  @Test
  public void should_update_component() {
    final long componentId = 1234l;
    final String newKey = "newKey";
    final String newName = "newName";
    ResourceDto resource = mock(ResourceDto.class);
    when(resourceDao.getResource(componentId)).thenReturn(resource);
    when(resource.setKey(newKey)).thenReturn(resource);
    when(resource.setName(newName)).thenReturn(resource);
    componentService.updateComponent(componentId, newKey, newName);
    verify(resource).setKey(newKey);
    verify(resource).setName(newName);
    verify(resourceDao).insertOrUpdate(resource);
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
  public void should_find_provisioned_projects() {
    List<String> qualifiers = newArrayList("TRK");

    Map<String, Object> map = newHashMap();
    map.put("qualifiers", qualifiers);

    componentService.findProvisionedProjects(map);
    verify(resourceDao).selectProvisionedProjects(anyListOf(String.class));
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
