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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.view.index.ViewIndexDefinition;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_COMPONENTS;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURE;

public class ComponentUpdaterTest {

  private static final String DEFAULT_PROJECT_KEY = "project-key";
  private static final String DEFAULT_PROJECT_NAME = "project-name";

  private System2 system2 = System2.INSTANCE;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = new EsTester(
    new ComponentIndexDefinition(new MapSettings()),
    new ProjectMeasuresIndexDefinition(new MapSettings()),
    new IssueIndexDefinition(new MapSettings()),
    new ViewIndexDefinition(new MapSettings()));

  @Rule
  public I18nRule i18n = new I18nRule().put("qualifier.TRK", "Project");

  private Settings settings = new MapSettings();

  private PermissionTemplateDto permissionTemplateDto;

  ComponentUpdater underTest = new ComponentUpdater(db.getDbClient(), i18n, system2,
    new PermissionTemplateService(db.getDbClient(), settings, new PermissionIndexer(db.getDbClient(), es.client()), null),
    new FavoriteUpdater(db.getDbClient()),
    new ProjectMeasuresIndexer(system2, db.getDbClient(), es.client()),
    new ComponentIndexer(db.getDbClient(), es.client()));

  @Before
  public void setUp() throws Exception {
    permissionTemplateDto = db.permissionTemplates().insertTemplate(db.getDefaultOrganization());
    setTemplateAsDefault(permissionTemplateDto);
  }

  @Test
  public void create_project() throws Exception {
    ComponentDto project = underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey(DEFAULT_PROJECT_KEY)
        .setName(DEFAULT_PROJECT_NAME)
        .setOrganizationUuid(db.getDefaultOrganization().getUuid())
        .build(),
      null);

    assertThat(project.getKey()).isEqualTo(DEFAULT_PROJECT_KEY);
    assertThat(project.deprecatedKey()).isEqualTo(DEFAULT_PROJECT_KEY);
    assertThat(project.name()).isEqualTo(DEFAULT_PROJECT_NAME);
    assertThat(project.longName()).isEqualTo(DEFAULT_PROJECT_NAME);
    assertThat(project.qualifier()).isEqualTo("TRK");
    assertThat(project.scope()).isEqualTo("PRJ");
    assertThat(project.getOrganizationUuid()).isEqualTo(db.getDefaultOrganization().getUuid());
    assertThat(project.uuid()).isNotNull();
    assertThat(project.projectUuid()).isEqualTo(project.uuid());
    assertThat(project.moduleUuid()).isNull();
    assertThat(project.moduleUuidPath()).isEqualTo("." + project.uuid() + ".");
    assertThat(project.getCreatedAt()).isNotNull();
    assertThat(db.getDbClient().componentDao().selectOrFailByKey(db.getSession(), DEFAULT_PROJECT_KEY)).isNotNull();
  }

  @Test
  public void create_project_with_branch() throws Exception {
    ComponentDto project = underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey(DEFAULT_PROJECT_KEY)
        .setName(DEFAULT_PROJECT_NAME)
        .setBranch("origin/master")
        .setOrganizationUuid(db.getDefaultOrganization().getUuid())
        .build(),
      null);

    assertThat(project.getKey()).isEqualTo("project-key:origin/master");
  }

  @Test
  public void remove_duplicated_components_when_creating_project() throws Exception {
    String projectKey = "PROJECT_KEY";

    DbSession session = mock(DbSession.class);

    ComponentDao componentDao = mock(ComponentDao.class);
    when(componentDao.selectByKey(session, projectKey)).thenReturn(Optional.absent());

    DbClient dbClient = mock(DbClient.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.componentDao()).thenReturn(componentDao);

    doAnswer(invocation -> {
      ((ComponentDto) invocation.getArguments()[1]).setId(1L);
      return null;
    }).when(componentDao).insert(eq(session), any(ComponentDto.class));

    OrganizationDto organization = db.getDefaultOrganization();
    when(componentDao.selectComponentsHavingSameKeyOrderedById(session, projectKey)).thenReturn(newArrayList(
      ComponentTesting.newProjectDto(organization).setId(1L).setKey(projectKey),
      ComponentTesting.newProjectDto(organization).setId(2L).setKey(projectKey),
      ComponentTesting.newProjectDto(organization).setId(3L).setKey(projectKey)));

    underTest = new ComponentUpdater(dbClient, i18n, System2.INSTANCE, mock(PermissionTemplateService.class), null, mock(ProjectMeasuresIndexer.class),
      mock(ComponentIndexer.class));
    underTest.create(
      session,
      newComponentBuilder()
        .setOrganizationUuid(organization.getUuid())
        .setKey(projectKey)
        .setName(projectKey)
        .build(),
      null);

    verify(componentDao).delete(session, 2L);
    verify(componentDao).delete(session, 3L);
  }

  @Test
  public void verify_permission_template_is_applied() throws Exception {
    UserDto userDto = db.users().insertUser();
    db.permissionTemplates().addUserToTemplate(permissionTemplateDto.getId(), userDto.getId(), USER);

    ComponentDto project = underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey(DEFAULT_PROJECT_KEY)
        .setName(DEFAULT_PROJECT_NAME)
        .setOrganizationUuid(db.getDefaultOrganization().getUuid())
        .build(),
      null);

    assertThat(db.users().selectProjectPermissionsOfUser(userDto, project)).containsOnly(USER);
  }

  @Test
  public void add_project_to_favorite_when_user() throws Exception {
    UserDto userDto = db.users().insertUser();
    db.permissionTemplates().addProjectCreatorToTemplate(permissionTemplateDto.getId(), USER);

    ComponentDto project = underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey(DEFAULT_PROJECT_KEY)
        .setName(DEFAULT_PROJECT_NAME)
        .setOrganizationUuid(db.getDefaultOrganization().getUuid())
        .build(),
      userDto.getId());

    assertThat(db.favorites().hasFavorite(project, userDto.getId())).isTrue();
  }

  @Test
  public void does_not_add_project_to_favorite_when_no_user() throws Exception {
    db.permissionTemplates().addProjectCreatorToTemplate(permissionTemplateDto.getId(), USER);

    ComponentDto project = underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey(DEFAULT_PROJECT_KEY)
        .setName(DEFAULT_PROJECT_NAME)
        .setOrganizationUuid(db.getDefaultOrganization().getUuid())
        .build(),
      null);

    assertThat(db.favorites().hasNoFavorite(project)).isTrue();
  }

  @Test
  public void does_not_add_project_to_favorite_when_project_has_no_permission_on_template() throws Exception {
    UserDto userDto = db.users().insertUser();

    ComponentDto project = underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey(DEFAULT_PROJECT_KEY)
        .setName(DEFAULT_PROJECT_NAME)
        .setOrganizationUuid(db.getDefaultOrganization().getUuid())
        .build(),
      null);

    assertThat(db.favorites().hasNoFavorite(project)).isTrue();
  }

  @Test
  public void verify_project_exists_in_es_indexes() throws Exception {
    ComponentDto project = underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey(DEFAULT_PROJECT_KEY)
        .setName(DEFAULT_PROJECT_NAME)
        .setOrganizationUuid(db.getDefaultOrganization().getUuid())
        .build(),
      null);

    assertThat(es.getIds(INDEX_COMPONENTS, TYPE_COMPONENT)).containsOnly(project.uuid());
    assertThat(es.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).containsOnly(project.uuid());
  }

  @Test
  public void fail_when_project_already_exists() throws Exception {
    db.components().insertComponent(ComponentTesting.newProjectDto(db.getDefaultOrganization()).setKey(DEFAULT_PROJECT_KEY));
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Could not create Project, key already exists: project-key");

    underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey(DEFAULT_PROJECT_KEY)
        .setName(DEFAULT_PROJECT_NAME)
        .setOrganizationUuid(db.getDefaultOrganization().getUuid())
        .build(),
      null);
  }

  @Test
  public void fail_when_key_has_bad_format() throws Exception {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Malformed key for Project: 1234");

    underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey("1234")
        .setName(DEFAULT_PROJECT_NAME)
        .setOrganizationUuid(db.getDefaultOrganization().getUuid())
        .build(),
      null);
  }

  @Test
  public void fail_to_create_new_component_on_invalid_branch() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Malformed branch for Project: origin?branch. Allowed characters are alphanumeric, '-', '_', '.' and '/', with at least one non-digit.");

    underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey(DEFAULT_PROJECT_KEY)
        .setName(DEFAULT_PROJECT_NAME)
        .setBranch("origin?branch")
        .setOrganizationUuid(db.getDefaultOrganization().getUuid())
        .build(),
      null);
  }

  @Test
  public void create_view() {
    ComponentDto view = underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey("view-key")
        .setName("view-name")
        .setQualifier(VIEW)
        .setOrganizationUuid(db.getDefaultOrganization().getUuid())
        .build(),
      null);

    assertThat(view.getKey()).isEqualTo("view-key");
    assertThat(view.name()).isEqualTo("view-name");
    assertThat(view.qualifier()).isEqualTo("VW");
    assertThat(es.getIds(INDEX_COMPONENTS, TYPE_COMPONENT)).containsOnly(view.uuid());
    // Indexes related to project measures, issues and views are not indexed
    assertThat(es.getIds(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE)).isEmpty();
    assertThat(es.getIds(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION)).isEmpty();
    assertThat(es.getIds(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW)).isEmpty();
  }

  private void setTemplateAsDefault(PermissionTemplateDto permissionTemplateDto) {
    settings.appendProperty("sonar.permission.template.default", permissionTemplateDto.getUuid());
  }

}
