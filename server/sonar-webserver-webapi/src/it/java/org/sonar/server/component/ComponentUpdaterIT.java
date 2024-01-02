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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.IndexersImpl;
import org.sonar.server.es.TestIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.permission.GroupPermissionChanger;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.UserPermissionChange;
import org.sonar.server.permission.UserPermissionChanger;
import org.sonar.server.permission.index.FooIndexDefinition;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.project.DefaultBranchNameResolver;

import static java.util.stream.IntStream.rangeClosed;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;

public class ComponentUpdaterIT {

  private static final String DEFAULT_PROJECT_KEY = "project-key";
  private static final String DEFAULT_PROJECT_NAME = "project-name";
  private static final NewComponent DEFAULT_COMPONENT = NewComponent.newComponentBuilder()
    .setKey(DEFAULT_PROJECT_KEY)
    .setName(DEFAULT_PROJECT_NAME)
    .build();
  private static final NewComponent PRIVATE_COMPONENT = NewComponent.newComponentBuilder()
    .setKey(DEFAULT_PROJECT_KEY)
    .setName(DEFAULT_PROJECT_NAME)
    .setPrivate(true)
    .build();
  private static final String DEFAULT_USER_UUID = "user-uuid";
  public static final String DEFAULT_USER_LOGIN = "user-login";

  private final System2 system2 = System2.INSTANCE;

  private final AuditPersister auditPersister = mock();

  @Rule
  public final DbTester db = DbTester.create(system2, auditPersister);
  @Rule
  public final I18nRule i18n = new I18nRule().put("qualifier.TRK", "Project");

  private final TestIndexers projectIndexers = new TestIndexers();
  private final PermissionTemplateService permissionTemplateService = mock(PermissionTemplateService.class);
  private final DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);
  public EsTester es = EsTester.createCustom(new FooIndexDefinition());
  private final PermissionUpdater<UserPermissionChange> userPermissionUpdater = new PermissionUpdater(
    new IndexersImpl(new PermissionIndexer(db.getDbClient(), es.client())),
    Set.of(new UserPermissionChanger(db.getDbClient(), new SequenceUuidFactory()),
      new GroupPermissionChanger(db.getDbClient(), new SequenceUuidFactory())));
  private final PermissionService permissionService = new PermissionServiceImpl(new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT));

  private final ComponentUpdater underTest = new ComponentUpdater(db.getDbClient(), i18n, system2,
    permissionTemplateService,
    new FavoriteUpdater(db.getDbClient()),
    projectIndexers, new SequenceUuidFactory(), defaultBranchNameResolver, userPermissionUpdater, permissionService);

  @Before
  public void before() {
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void persist_and_index_when_creating_project() {
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(PRIVATE_COMPONENT)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();
    ComponentCreationData returned = underTest.create(db.getSession(), creationParameters);

    ComponentDto loaded = db.getDbClient().componentDao().selectOrFailByUuid(db.getSession(), returned.mainBranchComponent().uuid());
    assertThat(loaded.getKey()).isEqualTo(DEFAULT_PROJECT_KEY);
    assertThat(loaded.name()).isEqualTo(DEFAULT_PROJECT_NAME);
    assertThat(loaded.longName()).isEqualTo(DEFAULT_PROJECT_NAME);
    assertThat(loaded.qualifier()).isEqualTo(Qualifiers.PROJECT);
    assertThat(loaded.scope()).isEqualTo(Scopes.PROJECT);
    assertThat(loaded.uuid()).isNotNull();
    assertThat(loaded.branchUuid()).isEqualTo(loaded.uuid());
    assertThat(loaded.isPrivate()).isEqualTo(PRIVATE_COMPONENT.isPrivate());
    assertThat(loaded.getCreatedAt()).isNotNull();
    assertThat(db.getDbClient().componentDao().selectByKey(db.getSession(), DEFAULT_PROJECT_KEY)).isPresent();

    assertThat(projectIndexers.hasBeenCalledForEntity(returned.projectDto().getUuid(), Indexers.EntityEvent.CREATION)).isTrue();

    Optional<BranchDto> branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), returned.mainBranchComponent().uuid());
    assertThat(branch).isPresent();
    assertThat(branch.get().getKey()).isEqualTo(DEFAULT_MAIN_BRANCH_NAME);
    assertThat(branch.get().getMergeBranchUuid()).isNull();
    assertThat(branch.get().getBranchType()).isEqualTo(BranchType.BRANCH);
    assertThat(branch.get().getUuid()).isEqualTo(returned.mainBranchComponent().uuid());
    assertThat(branch.get().getProjectUuid()).isEqualTo(returned.projectDto().getUuid());
  }

  @Test
  public void create_project_with_main_branch_global_property() {
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn("main-branch-global");
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(PRIVATE_COMPONENT)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();

    ComponentDto returned = underTest.create(db.getSession(), creationParameters).mainBranchComponent();

    Optional<BranchDto> branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), returned.branchUuid());
    assertThat(branch).get().extracting(BranchDto::getBranchKey).isEqualTo("main-branch-global");
  }

  @Test
  public void persist_private_flag_true_when_creating_project() {
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(PRIVATE_COMPONENT)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();
    ComponentDto returned = underTest.create(db.getSession(), creationParameters).mainBranchComponent();
    ComponentDto loaded = db.getDbClient().componentDao().selectOrFailByUuid(db.getSession(), returned.uuid());
    assertThat(loaded.isPrivate()).isEqualTo(PRIVATE_COMPONENT.isPrivate());
  }

  @Test
  public void persist_private_flag_false_when_creating_project() {
    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setPrivate(false)
      .build();
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(project)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();
    ComponentDto returned = underTest.create(db.getSession(), creationParameters).mainBranchComponent();
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

    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(view)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();
    ComponentDto returned = underTest.create(db.getSession(), creationParameters).mainBranchComponent();

    ComponentDto loaded = db.getDbClient().componentDao().selectOrFailByUuid(db.getSession(), returned.uuid());
    assertThat(loaded.getKey()).isEqualTo("view-key");
    assertThat(loaded.name()).isEqualTo("view-name");
    assertThat(loaded.qualifier()).isEqualTo("VW");
    assertThat(projectIndexers.hasBeenCalledForEntity(loaded.uuid(), Indexers.EntityEvent.CREATION)).isTrue();
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
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(application)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();
    ComponentCreationData returned = underTest.create(db.getSession(), creationParameters);

    ProjectDto loaded = db.getDbClient().projectDao().selectByUuid(db.getSession(), returned.projectDto().getUuid()).get();
    assertThat(loaded.getKey()).isEqualTo("app-key");
    assertThat(loaded.getName()).isEqualTo("app-name");
    assertThat(loaded.getQualifier()).isEqualTo("APP");
    assertThat(projectIndexers.hasBeenCalledForEntity(loaded.getUuid(), Indexers.EntityEvent.CREATION)).isTrue();
    Optional<BranchDto> branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), returned.mainBranchComponent().uuid());
    assertThat(branch).isPresent();
    assertThat(branch.get().getKey()).isEqualTo(DEFAULT_MAIN_BRANCH_NAME);
    assertThat(branch.get().getMergeBranchUuid()).isNull();
    assertThat(branch.get().getBranchType()).isEqualTo(BranchType.BRANCH);
    assertThat(branch.get().getUuid()).isEqualTo(returned.mainBranchComponent().uuid());
    assertThat(branch.get().getProjectUuid()).isEqualTo(returned.projectDto().getUuid());
  }

  @Test
  public void apply_default_permission_template() {
    ComponentCreationParameters componentCreationParameters = ComponentCreationParameters.builder()
      .newComponent(DEFAULT_COMPONENT)
      .userLogin(DEFAULT_USER_LOGIN)
      .userUuid(DEFAULT_USER_UUID)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();

    ProjectDto dto = underTest.create(db.getSession(), componentCreationParameters).projectDto();

    verify(permissionTemplateService).applyDefaultToNewComponent(db.getSession(), dto, DEFAULT_USER_UUID);
  }

  @Test
  public void add_project_to_user_favorites_if_project_creator_is_defined_in_permission_template() {
    UserDto userDto = db.users().insertUser();
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(DEFAULT_COMPONENT)
      .userLogin(userDto.getLogin())
      .userUuid(userDto.getUuid())
      .creationMethod(CreationMethod.LOCAL_API)
      .build();

    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ProjectDto.class)))
      .thenReturn(true);

    ProjectDto dto = underTest.create(db.getSession(), creationParameters).projectDto();

    assertThat(db.favorites().hasFavorite(dto, userDto.getUuid())).isTrue();
  }

  @Test
  public void do_not_add_project_to_user_favorites_if_project_creator_is_defined_in_permission_template_and_already_100_favorites() {
    UserDto user = db.users().insertUser();
    rangeClosed(1, 100).forEach(i -> db.favorites().add(db.components().insertPrivateProject().getProjectDto(), user.getUuid(), user.getLogin()));
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(DEFAULT_COMPONENT)
      .userLogin(user.getLogin())
      .userUuid(user.getUuid())
      .creationMethod(CreationMethod.LOCAL_API)
      .build();

    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(eq(db.getSession()), any(ProjectDto.class)))
      .thenReturn(true);

    ProjectDto dto = underTest.create(db.getSession(), creationParameters).projectDto();

    assertThat(db.favorites().hasFavorite(dto, user.getUuid())).isFalse();
  }

  @Test
  public void does_not_add_project_to_favorite_when_anonymously_created() {
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(DEFAULT_COMPONENT)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();
    ProjectDto projectDto = underTest.create(db.getSession(), creationParameters).projectDto();

    assertThat(db.favorites().hasNoFavorite(projectDto)).isTrue();
  }

  @Test
  public void fail_when_project_key_already_exists() {
    ComponentDto existing = db.components().insertPrivateProject().getMainBranchComponent();
    DbSession session = db.getSession();

    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(existing.getKey())
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(project)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();

    assertThatThrownBy(() -> underTest.create(session, creationParameters))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"%s\". A similar key already exists: \"%s\"", existing.getKey(), existing.getKey());
  }

  @Test
  public void fail_when_key_has_bad_format() {
    DbSession session = db.getSession();
    NewComponent project = NewComponent.newComponentBuilder()
      .setKey("1234")
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(project)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();

    assertThatThrownBy(() -> underTest.create(session, creationParameters))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Malformed key for Project: '1234'");
  }

  @Test
  public void fail_when_key_contains_percent_character() {
    DbSession session = db.getSession();
    NewComponent project = NewComponent.newComponentBuilder()
      .setKey("roject%Key")
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(project)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();

    assertThatThrownBy(() -> underTest.create(session, creationParameters))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Malformed key for Project: 'roject%Key'");
  }

  @Test
  public void create_shouldFail_whenCreatingProjectWithExistingKeyButDifferentCase() {
    createComponent_shouldFail_whenCreatingComponentWithExistingKeyButDifferentCase(PROJECT);
  }

  @Test
  public void create_shouldFail_whenCreatingPortfolioWithExistingKeyButDifferentCase() {
    createComponent_shouldFail_whenCreatingComponentWithExistingKeyButDifferentCase(VIEW);
  }

  @Test
  public void create_shouldFail_whenCreatingApplicationWithExistingKeyButDifferentCase() {
    createComponent_shouldFail_whenCreatingComponentWithExistingKeyButDifferentCase(APP);
  }

  private void createComponent_shouldFail_whenCreatingComponentWithExistingKeyButDifferentCase(String qualifier) {
    String existingKey = randomAlphabetic(5).toUpperCase();
    db.components().insertPrivateProject(component -> component.setKey(existingKey));
    String newKey = existingKey.toLowerCase();

    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(newKey)
      .setName(DEFAULT_PROJECT_NAME)
      .setQualifier(qualifier)
      .build();
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(project)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();

    DbSession dbSession = db.getSession();
    assertThatThrownBy(() -> underTest.create(dbSession, creationParameters))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"%s\". A similar key already exists: \"%s\"", newKey, existingKey);
  }

  @Test
  public void createComponent_shouldFail_whenCreatingComponentWithMultipleExistingKeyButDifferentCase() {
    String existingKey = randomAlphabetic(5).toUpperCase();
    String existingKeyLowerCase = existingKey.toLowerCase();
    db.components().insertPrivateProject(component -> component.setKey(existingKey));
    db.components().insertPrivateProject(component -> component.setKey(existingKeyLowerCase));
    String newKey = StringUtils.capitalize(existingKeyLowerCase);

    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(newKey)
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(project)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();

    DbSession dbSession = db.getSession();
    assertThatThrownBy(() -> underTest.create(dbSession, creationParameters))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"%s\". A similar key already exists: \"%s, %s\"", newKey, existingKey, existingKeyLowerCase);
  }

  @Test
  public void createComponent_shouldFail_whenCreatingComponentWithMultipleExistingPortfolioKeysButDifferentCase() {
    String existingKey = randomAlphabetic(5).toUpperCase();
    String existingKeyLowerCase = existingKey.toLowerCase();
    db.components().insertPrivatePortfolio(portfolio -> portfolio.setKey(existingKey));
    db.components().insertPrivatePortfolio(portfolio -> portfolio.setKey(existingKeyLowerCase));
    String newKey = StringUtils.capitalize(existingKeyLowerCase);

    NewComponent project = NewComponent.newComponentBuilder()
      .setKey(newKey)
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(project)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();

    DbSession dbSession = db.getSession();
    assertThatThrownBy(() -> underTest.create(dbSession, creationParameters))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"%s\". A similar key already exists: \"%s, %s\"", newKey, existingKey, existingKeyLowerCase);
  }

  @Test
  public void create_createsComponentWithMasterBranchName() {
    String componentNameAndKey = "createApplicationOrPortfolio";
    NewComponent app = NewComponent.newComponentBuilder()
      .setKey(componentNameAndKey)
      .setName(componentNameAndKey)
      .setQualifier("APP")
      .build();
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(app)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();

    ComponentDto appDto = underTest.create(db.getSession(), creationParameters).mainBranchComponent();

    Optional<BranchDto> branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), appDto.branchUuid());
    assertThat(branch).isPresent();
    assertThat(branch.get().getBranchKey()).isEqualTo(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void createWithoutCommit_whenProjectIsManaged_doesntApplyPermissionTemplate() {
    UserDto userDto = db.users().insertUser();
    ComponentCreationParameters componentCreationParameters = ComponentCreationParameters.builder()
      .newComponent(DEFAULT_COMPONENT)
      .userLogin(userDto.getLogin())
      .userUuid(userDto.getUuid())
      .mainBranchName(null)
      .isManaged(true)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();
    underTest.createWithoutCommit(db.getSession(), componentCreationParameters);

    verify(permissionTemplateService, never()).applyDefaultToNewComponent(any(), any(), any());
  }

  @Test
  public void createWithoutCommit_whenInsertingPortfolio_shouldOnlyAddOneEntryToAuditLogs() {
    String portfolioKey = "portfolio";
    NewComponent portfolio = NewComponent.newComponentBuilder()
      .setKey(portfolioKey)
      .setName(portfolioKey)
      .setQualifier(VIEW)
      .build();
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(portfolio)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();

    underTest.createWithoutCommit(db.getSession(), creationParameters);
    db.commit();

    verify(auditPersister, times(1)).addComponent(argThat(d -> d.equals(db.getSession())),
      argThat(newValue -> newValue.getComponentKey().equals(portfolioKey)));
  }

  @Test
  public void createWithoutCommit_whenProjectIsManagedAndPrivate_applyPublicPermissionsToCreator() {
    UserDto userDto = db.users().insertUser();
    NewComponent newComponent = NewComponent.newComponentBuilder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setPrivate(true)
      .build();

    DbSession session = db.getSession();

    ComponentCreationParameters componentCreationParameters = ComponentCreationParameters.builder()
      .newComponent(PRIVATE_COMPONENT)
      .userLogin(userDto.getLogin())
      .userUuid(userDto.getUuid())
      .mainBranchName(null)
      .isManaged(true)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();
    ComponentCreationData componentCreationData = underTest.createWithoutCommit(session, componentCreationParameters);

    List<String> permissions = db.getDbClient().userPermissionDao().selectEntityPermissionsOfUser(session, userDto.getUuid(), componentCreationData.projectDto().getUuid());
    assertThat(permissions)
      .containsExactlyInAnyOrder(UserRole.USER, UserRole.CODEVIEWER);
  }

  @Test
  public void create_whenCreationMethodIsLocalApi_persistsIt() {
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(DEFAULT_COMPONENT)
      .creationMethod(CreationMethod.LOCAL_API)
      .build();
    ProjectDto projectDto = underTest.create(db.getSession(), creationParameters).projectDto();
    assertThat(projectDto.getCreationMethod()).isEqualTo(CreationMethod.LOCAL_API);
  }

  @Test
  public void create_whenCreationMethodIsAlmImportBrowser_persistsIt() {
    ComponentCreationParameters creationParameters = ComponentCreationParameters.builder()
      .newComponent(DEFAULT_COMPONENT)
      .creationMethod(CreationMethod.ALM_IMPORT_BROWSER)
      .build();
    ProjectDto projectDto = underTest.create(db.getSession(), creationParameters).projectDto();
    assertThat(projectDto.getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_BROWSER);
  }
}
