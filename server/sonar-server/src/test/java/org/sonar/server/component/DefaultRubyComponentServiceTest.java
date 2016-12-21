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

import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.server.component.es.ProjectMeasuresIndexDefinition;
import org.sonar.server.component.es.ProjectMeasuresIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;

public class DefaultRubyComponentServiceTest {

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public EsTester es = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings()));

  private I18nRule i18n = new I18nRule();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private ResourceDao resourceDao = dbClient.resourceDao();
  private ComponentService componentService = new ComponentService(dbClient, i18n, userSession, system2, new ComponentFinder(dbClient),
    new ProjectMeasuresIndexer(system2, dbClient, es.client()));
  private PermissionTemplateService permissionTemplateService = mock(PermissionTemplateService.class);
  private FavoriteUpdater favoriteUpdater = mock(FavoriteUpdater.class);

  ComponentDbTester componentDb = new ComponentDbTester(db);

  DefaultRubyComponentService underTest = new DefaultRubyComponentService(dbClient, resourceDao, componentService, permissionTemplateService, favoriteUpdater);

  @Test
  public void find_by_key() {
    ComponentDto componentDto = componentDb.insertProject();

    assertThat(underTest.findByKey(componentDto.getKey())).isNotNull();
  }

  @Test
  public void find_by_uuid() {
    ComponentDto componentDto = componentDb.insertProject();

    assertThat(underTest.findByUuid(componentDto.uuid())).isNotNull();
  }

  @Test
  public void not_find_by_uuid() {
    componentDb.insertProject();

    assertThat(underTest.findByUuid("UNKNOWN")).isNull();
  }

  @Test
  public void create_component() {
    userSession.login("john").setGlobalPermissions(PROVISIONING);
    String componentKey = "new-project";
    String componentName = "New Project";
    String qualifier = Qualifiers.PROJECT;
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ComponentDto.class))).thenReturn(true);

    Long result = underTest.createComponent(componentKey, componentName, qualifier);

    ComponentDto project = dbClient.componentDao().selectOrFailByKey(dbSession, componentKey);
    assertThat(project.key()).isEqualTo(componentKey);
    assertThat(project.name()).isEqualTo(componentName);
    assertThat(project.qualifier()).isEqualTo(qualifier);
    assertThat(project.getId()).isEqualTo(result);
    verify(permissionTemplateService).applyDefaultPermissionTemplate(any(DbSession.class), eq(componentKey));
    verify(favoriteUpdater).add(any(DbSession.class), eq(project));
  }

  @Test(expected = BadRequestException.class)
  public void should_throw_if_malformed_key1() {
    userSession.login("john").setGlobalPermissions(PROVISIONING);
    underTest.createComponent("1234", "New Project", Qualifiers.PROJECT);
  }

  @Test
  public void should_find_provisioned_projects() {
    componentDb.insertProject();
    List<String> qualifiers = newArrayList("TRK");
    Map<String, Object> map = newHashMap();
    map.put("qualifiers", qualifiers);

    List<ResourceDto> resourceDtos = underTest.findProvisionedProjects(map);
    assertThat(resourceDtos).hasSize(1);
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
