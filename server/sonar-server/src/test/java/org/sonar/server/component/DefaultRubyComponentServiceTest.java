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
package org.sonar.server.component;

import com.google.common.base.Optional;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceDao;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.permission.PermissionService;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultRubyComponentServiceTest {

  ResourceDao resourceDao = mock(ResourceDao.class);
  DefaultComponentFinder finder = mock(DefaultComponentFinder.class);
  ComponentService componentService = mock(ComponentService.class);
  PermissionService permissionService = mock(PermissionService.class);

  DefaultRubyComponentService service;

  @Before
  public void before() {
    service = new DefaultRubyComponentService(resourceDao, finder, componentService, permissionService);
  }

  @Test
  public void find_by_key() {
    Component component = mock(Component.class);
    when(resourceDao.selectByKey("struts")).thenReturn(component);

    assertThat(service.findByKey("struts")).isEqualTo(component);
  }

  @Test
  public void find_by_uuid() {
    ComponentDto component = new ComponentDto();
    when(componentService.getByUuid("ABCD")).thenReturn(Optional.of(component));

    assertThat(service.findByUuid("ABCD")).isEqualTo(component);
  }

  @Test
  public void not_find_by_uuid() {
    when(componentService.getByUuid("ABCD")).thenReturn(Optional.<ComponentDto>absent());

    assertThat(service.findByUuid("ABCD")).isNull();
  }

  @Test
  public void create_component() {
    String componentKey = "new-project";
    String componentName = "New Project";
    String qualifier = Qualifiers.PROJECT;
    ComponentDto projectDto = ComponentTesting.newProjectDto().setKey(componentKey);
    when(resourceDao.selectByKey(componentKey)).thenReturn(projectDto);
    when(componentService.create(any(NewComponent.class))).thenReturn(projectDto);

    service.createComponent(componentKey, componentName, qualifier);

    ArgumentCaptor<NewComponent> newComponentArgumentCaptor = ArgumentCaptor.forClass(NewComponent.class);

    verify(componentService).create(newComponentArgumentCaptor.capture());
    NewComponent newComponent = newComponentArgumentCaptor.getValue();
    assertThat(newComponent.key()).isEqualTo(componentKey);
    assertThat(newComponent.name()).isEqualTo(componentName);
    assertThat(newComponent.branch()).isNull();
    assertThat(newComponent.qualifier()).isEqualTo(Qualifiers.PROJECT);

    verify(permissionService).applyDefaultPermissionTemplate(componentKey);
  }

  @Test
  public void not_create_component_on_sub_views() {
    when(resourceDao.selectByKey(anyString())).thenReturn(ComponentTesting.newProjectDto());

    service.createComponent("new-project", "New Project", Qualifiers.SUBVIEW);

    verify(componentService, never()).create(any(NewComponent.class));
    verify(permissionService, never()).applyDefaultPermissionTemplate(anyString());
  }

  @Test(expected = BadRequestException.class)
  public void should_throw_exception_if_create_fails() {
    String componentKey = "new-project";
    String componentName = "New Project";
    String qualifier = Qualifiers.PROJECT;
    when(resourceDao.selectByKey(componentKey)).thenReturn(null);

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
