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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.tester.UserSessionRule;

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
  public EsTester es = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings()),
    new ComponentIndexDefinition(new MapSettings()));

  private I18nRule i18n = new I18nRule();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private ComponentService componentService = new ComponentService(dbClient, i18n, userSession, system2,
    new ProjectMeasuresIndexer(system2, dbClient, es.client()), new ComponentIndexer(dbClient, es.client()));
  private PermissionTemplateService permissionTemplateService = mock(PermissionTemplateService.class);
  private FavoriteUpdater favoriteUpdater = mock(FavoriteUpdater.class);
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);

  private DefaultRubyComponentService underTest = new DefaultRubyComponentService(dbClient, componentService,
    permissionTemplateService, favoriteUpdater, defaultOrganizationProvider);

  private String defaultOrganizationUuid;

  @Before
  public void setUp() throws Exception {
    defaultOrganizationUuid = db.getDefaultOrganization().getUuid();
  }

  @Test
  public void create_component() {
    userSession.login("john").setGlobalPermissions(PROVISIONING);
    String componentKey = "new-project";
    String componentName = "New Project";
    String qualifier = Qualifiers.PROJECT;
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), eq(defaultOrganizationUuid), any(ComponentDto.class))).thenReturn(true);

    Long result = underTest.createComponent(componentKey, componentName, qualifier);

    ComponentDto project = dbClient.componentDao().selectOrFailByKey(dbSession, componentKey);
    assertThat(project.key()).isEqualTo(componentKey);
    assertThat(project.name()).isEqualTo(componentName);
    assertThat(project.qualifier()).isEqualTo(qualifier);
    assertThat(project.getId()).isEqualTo(result);
    verify(permissionTemplateService).applyDefaultPermissionTemplate(any(DbSession.class), eq(defaultOrganizationUuid), eq(componentKey));
    verify(favoriteUpdater).add(any(DbSession.class), eq(project), eq(null));
  }

  @Test(expected = BadRequestException.class)
  public void should_throw_if_malformed_key1() {
    userSession.login("john").setGlobalPermissions(PROVISIONING);
    underTest.createComponent("1234", "New Project", Qualifiers.PROJECT);
  }

}
