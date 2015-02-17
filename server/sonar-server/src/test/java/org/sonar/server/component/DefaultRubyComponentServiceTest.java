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

package org.sonar.server.component;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.server.exceptions.BadRequestException;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.*;

public class DefaultRubyComponentServiceTest {

  ResourceDao resourceDao;
  DefaultComponentFinder finder;
  ComponentService componentService;

  DefaultRubyComponentService service;

  @Before
  public void before() {
    resourceDao = mock(ResourceDao.class);
    finder = mock(DefaultComponentFinder.class);
    componentService = mock(ComponentService.class);
    service = new DefaultRubyComponentService(resourceDao, finder, componentService);
  }

  @Test
  public void find_by_key() {
    Component component = mock(Component.class);
    when(resourceDao.findByKey("struts")).thenReturn(component);

    assertThat(service.findByKey("struts")).isEqualTo(component);
  }

  @Test
  public void find_by_uuid() {
    ComponentDto component = new ComponentDto();
    when(componentService.getNullableByUuid("ABCD")).thenReturn(component);

    assertThat(service.findByUuid("ABCD")).isEqualTo(component);
  }

  @Test
  public void create_component() {
    String componentKey = "new-project";
    String componentName = "New Project";
    String qualifier = Qualifiers.PROJECT;
    when(resourceDao.findByKey(componentKey)).thenReturn(ComponentTesting.newProjectDto());
    when(componentService.create(any(NewComponent.class))).thenReturn(componentKey);

    service.createComponent(componentKey, componentName, qualifier);

    ArgumentCaptor<NewComponent> newComponentArgumentCaptor = ArgumentCaptor.forClass(NewComponent.class);
    verify(componentService).create(newComponentArgumentCaptor.capture());
    NewComponent newComponent = newComponentArgumentCaptor.getValue();
    assertThat(newComponent.key()).isEqualTo(componentKey);
    assertThat(newComponent.name()).isEqualTo(componentName);
    assertThat(newComponent.branch()).isNull();
    assertThat(newComponent.qualifier()).isEqualTo(Qualifiers.PROJECT);
  }

  @Test
  public void not_create_component_on_sub_views() {
    when(resourceDao.findByKey(anyString())).thenReturn(ComponentTesting.newProjectDto());

    service.createComponent("new-project", "New Project", Qualifiers.SUBVIEW);

    verify(componentService, never()).create(any(NewComponent.class));
  }

  @Test(expected = BadRequestException.class)
  public void should_throw_exception_if_create_fails() {
    String componentKey = "new-project";
    String componentName = "New Project";
    String qualifier = Qualifiers.PROJECT;
    when(resourceDao.findByKey(componentKey)).thenReturn(null);

    service.createComponent(componentKey, componentName, qualifier);
  }

  @Test(expected = BadRequestException.class)
  public void should_throw_if_malformed_key1() {
    service.createComponent("1234", "New Project", Qualifiers.PROJECT);
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

    service.find(map);
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

    service.findWithUncompleteProjects(map);
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

    service.findGhostsProjects(map);
    verify(resourceDao).selectGhostsProjects(anyListOf(String.class));
    verify(finder).find(any(ComponentQuery.class), anyListOf(Component.class));
  }

  @Test
  public void should_find_provisioned_projects() {
    List<String> qualifiers = newArrayList("TRK");

    Map<String, Object> map = newHashMap();
    map.put("qualifiers", qualifiers);

    service.findProvisionedProjects(map);
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

  @Test
  public void update_key() {
    service.updateKey("oldKey", "newKey");
    verify(componentService).updateKey("oldKey", "newKey");
  }

  @Test
  public void check_module_keys_before_renaming() {
    service.checkModuleKeysBeforeRenaming("oldKey", "old", "new");
    verify(componentService).checkModuleKeysBeforeRenaming("oldKey", "old", "new");
  }

  @Test
  public void bulk_update_key() {
    service.bulkUpdateKey("oldKey", "old", "new");
    verify(componentService).bulkUpdateKey("oldKey", "old", "new");
  }
}
