/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.project.DefaultBranchNameResolver;

import static java.util.stream.IntStream.rangeClosed;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;

public class ComponentUpdaterTest {

  private static final String DEFAULT_PROJECT_KEY = "project-key";
  private static final String DEFAULT_PROJECT_NAME = "project-name";

  private final System2 system2 = System2.INSTANCE;

  @Rule
  public final DbTester db = DbTester.create(system2);
  @Rule
  public final I18nRule i18n = new I18nRule().put("qualifier.TRK", "Project");

  private final TestProjectIndexers projectIndexers = new TestProjectIndexers();
  private final PermissionTemplateService permissionTemplateService = mock(PermissionTemplateService.class);
  private final DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);

  private final ComponentUpdater underTest = new ComponentUpdater(db.getDbClient(), i18n, system2,
    permissionTemplateService,
    new FavoriteUpdater(db.getDbClient()),
    projectIndexers, new SequenceUuidFactory(), defaultBranchNameResolver);

  @Before
  public void before() {
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void persist_and_index_when_creating_project() {
    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setPrivate(true)
      .build();
    ComponentDto returned = underTest.create(db.getSession(), project, null, null);

    ComponentDto loaded = db.getDbClient().componentDao().selectOrFailByUuid(db.getSession(), returned.uuid());
    assertThat(loaded.getKey()).isEqualTo(DEFAULT_PROJECT_KEY);
    assertThat(loaded.name()).isEqualTo(DEFAULT_PROJECT_NAME);
    assertThat(loaded.longName()).isEqualTo(DEFAULT_PROJECT_NAME);
    assertThat(loaded.qualifier()).isEqualTo(Qualifiers.PROJECT);
    assertThat(loaded.scope()).isEqualTo(Scopes.PROJECT);
    assertThat(loaded.uuid()).isNotNull();
    assertThat(loaded.branchUuid()).isEqualTo(loaded.uuid());
    assertThat(loaded.moduleUuid()).isNull();
    assertThat(loaded.moduleUuidPath()).isEqualTo("." + loaded.uuid() + ".");
    assertThat(loaded.isPrivate()).isEqualTo(project.isPrivate());
    assertThat(loaded.getCreatedAt()).isNotNull();
    assertThat(db.getDbClient().componentDao().selectByKey(db.getSession(), DEFAULT_PROJECT_KEY)).isPresent();

    assertThat(projectIndexers.hasBeenCalled(loaded.uuid(), ProjectIndexer.Cause.PROJECT_CREATION)).isTrue();

    Optional<BranchDto> branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), returned.uuid());
    assertThat(branch).isPresent();
    assertThat(branch.get().getKey()).isEqualTo(DEFAULT_MAIN_BRANCH_NAME);
    assertThat(branch.get().getMergeBranchUuid()).isNull();
    assertThat(branch.get().getBranchType()).isEqualTo(BranchType.BRANCH);
    assertThat(branch.get().getUuid()).isEqualTo(returned.uuid());
    assertThat(branch.get().getProjectUuid()).isEqualTo(returned.uuid());
  }

  @Test
  public void create_project_with_main_branch_global_property() {
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn("main-branch-global");
    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setPrivate(true)
      .build();

    ComponentDto returned = underTest.create(db.getSession(), project, null, null);

    Optional<BranchDto> branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), returned.branchUuid());
    assertThat(branch).get().extracting(BranchDto::getBranchKey).isEqualTo("main-branch-global");
  }

  @Test
  public void create_project_with_main_branch_param() {
    String customBranchName = "main-branch-custom";
    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setPrivate(true)
      .build();

    ComponentDto returned = underTest.create(db.getSession(), project, null, null, customBranchName);

    Optional<BranchDto> branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), returned.branchUuid());
    assertThat(branch).get().extracting(BranchDto::getBranchKey).isEqualTo(customBranchName);
  }

  @Test
  public void persist_private_flag_true_when_creating_project() {
    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setPrivate(true)
      .build();
    ComponentDto returned = underTest.create(db.getSession(), project, null, null);
    ComponentDto loaded = db.getDbClient().componentDao().selectOrFailByUuid(db.getSession(), returned.uuid());
    assertThat(loaded.isPrivate()).isEqualTo(project.isPrivate());
  }

  @Test
  public void persist_private_flag_false_when_creating_project() {
    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setPrivate(false)
      .build();
    ComponentDto returned = underTest.create(db.getSession(), project, null, null);
    ComponentDto loaded = db.getDbClient().componentDao().selectOrFailByUuid(db.getSession(), returned.uuid());
    assertThat(loaded.isPrivate()).isEqualTo(project.isPrivate());
  }

  @Test
  public void create_view() {
    NewComponent view = NewComponent.newComponentBuilder()
      .setKey("view-key")
      .setName("view-name")
      .setQualifier(VIEW)
      .build();

    ComponentDto returned = underTest.create(db.getSession(), view, null, null);

    ComponentDto loaded = db.getDbClient().componentDao().selectOrFailByUuid(db.getSession(), returned.uuid());
    assertThat(loaded.getKey()).isEqualTo("view-key");
    assertThat(loaded.name()).isEqualTo("view-name");
    assertThat(loaded.qualifier()).isEqualTo("VW");
    assertThat(projectIndexers.hasBeenCalled(loaded.uuid(), ProjectIndexer.Cause.PROJECT_CREATION)).isTrue();
    Optional<BranchDto> branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), returned.uuid());
    assertThat(branch).isNotPresent();
  }

  @Test
  public void create_application() {
    NewComponent application = NewComponent.newComponentBuilder()
      .setKey("app-key")
      .setName("app-name")
      .setQualifier(APP)
      .build();

    ComponentDto returned = underTest.create(db.getSession(), application, null, null);

    ComponentDto loaded = db.getDbClient().componentDao().selectOrFailByUuid(db.getSession(), returned.uuid());
    assertThat(loaded.getKey()).isEqualTo("app-key");
    assertThat(loaded.name()).isEqualTo("app-name");
    assertThat(loaded.qualifier()).isEqualTo("APP");
    assertThat(projectIndexers.hasBeenCalled(loaded.uuid(), ProjectIndexer.Cause.PROJECT_CREATION)).isTrue();
    Optional<BranchDto> branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), returned.uuid());
    assertThat(branch).isPresent();
    assertThat(branch.get().getKey()).isEqualTo(DEFAULT_MAIN_BRANCH_NAME);
    assertThat(branch.get().getMergeBranchUuid()).isNull();
    assertThat(branch.get().getBranchType()).isEqualTo(BranchType.BRANCH);
    assertThat(branch.get().getUuid()).isEqualTo(returned.uuid());
    assertThat(branch.get().getProjectUuid()).isEqualTo(returned.uuid());
  }

  @Test
  public void apply_default_permission_template() {
    String userUuid = "42";
    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    ComponentDto dto = underTest.create(db.getSession(), project, userUuid, "user-login");

    verify(permissionTemplateService).applyDefaultToNewComponent(db.getSession(), dto, userUuid);
  }

  @Test
  public void add_project_to_user_favorites_if_project_creator_is_defined_in_permission_template() {
    UserDto userDto = db.users().insertUser();
    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ComponentDto.class)))
      .thenReturn(true);

    ComponentDto dto = underTest.create(db.getSession(), project, userDto.getUuid(), userDto.getLogin());

    assertThat(db.favorites().hasFavorite(dto, userDto.getUuid())).isTrue();
  }

  @Test
  public void do_not_add_project_to_user_favorites_if_project_creator_is_defined_in_permission_template_and_already_100_favorites() {
    UserDto user = db.users().insertUser();
    rangeClosed(1, 100).forEach(i -> db.favorites().add(db.components().insertPrivateProject(), user.getUuid(), user.getLogin()));
    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(eq(db.getSession()), any(ComponentDto.class)))
      .thenReturn(true);

    ComponentDto dto = underTest.create(db.getSession(),
      project,
      user.getUuid(),
      user.getLogin());

    assertThat(db.favorites().hasFavorite(dto, user.getUuid())).isFalse();
  }

  @Test
  public void does_not_add_project_to_favorite_when_anonymously_created() {
    ComponentDto project = underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey(DEFAULT_PROJECT_KEY)
        .setName(DEFAULT_PROJECT_NAME)
        .build(),
      null, null);

    assertThat(db.favorites().hasNoFavorite(project)).isTrue();
  }

  @Test
  public void does_not_add_project_to_favorite_when_project_has_no_permission_on_template() {
    ComponentDto project = underTest.create(db.getSession(),
      NewComponent.newComponentBuilder()
        .setKey(DEFAULT_PROJECT_KEY)
        .setName(DEFAULT_PROJECT_NAME)
        .build(),
      null, null);

    assertThat(db.favorites().hasNoFavorite(project)).isTrue();
  }

  @Test
  public void fail_when_project_key_already_exists() {
    ComponentDto existing = db.components().insertPrivateProject();

    DbSession session = db.getSession();
    NewComponent newComponent = NewComponent.newComponentBuilder()
      .setKey(existing.getKey())
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    assertThatThrownBy(() -> underTest.create(session, newComponent, null, null))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"%s\". A similar key already exists: \"%s\"", existing.getKey(), existing.getKey());
  }

  @Test
  public void fail_when_key_has_bad_format() {
    DbSession session = db.getSession();
    NewComponent newComponent = NewComponent.newComponentBuilder()
      .setKey("1234")
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    assertThatThrownBy(() -> underTest.create(session, newComponent, null, null))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Malformed key for Project: '1234'");
  }

  @Test
  public void fail_when_key_contains_percent_character() {
    DbSession session = db.getSession();
    NewComponent newComponent = NewComponent.newComponentBuilder()
      .setKey("roject%Key")
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    assertThatThrownBy(() -> underTest.create(session, newComponent, null, null))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Malformed key for Project: 'roject%Key'");
  }

  @Test
  public void create_shouldFail_whenCreatingProjectWithExistingKeyButDifferentCase() {
    String existingKey = randomAlphabetic(5).toUpperCase();
    db.components().insertPrivateProject(component -> component.setKey(existingKey));
    String newKey = existingKey.toLowerCase();

    NewComponent newComponent = NewComponent.newComponentBuilder()
      .setKey(newKey)
      .setName(DEFAULT_PROJECT_NAME)
      .build();

    DbSession dbSession = db.getSession();
    assertThatThrownBy(() -> underTest.create(dbSession, newComponent, null, null))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"%s\". A similar key already exists: \"%s\"", newKey, existingKey);
  }

  @Test
  public void create_createsComponentWithMasterBranchName() {
    String componentNameAndKey = "createApplicationOrPortfolio";
    ComponentDto app = underTest.create(db.getSession(), NewComponent.newComponentBuilder().setName(componentNameAndKey)
      .setKey(componentNameAndKey).setQualifier("APP").build(), null, null, null);

    Optional<BranchDto> branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), app.branchUuid());
    assertThat(branch).isPresent();
    assertThat(branch.get().getBranchKey()).isEqualTo(DEFAULT_MAIN_BRANCH_NAME);
  }
}
