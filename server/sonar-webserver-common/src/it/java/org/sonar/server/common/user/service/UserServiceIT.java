/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.common.user.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.scim.ScimUserDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.SessionTokenDto;
import org.sonar.db.user.UserDismissedMessageDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.user.UserAnonymizer;
import org.sonar.server.common.user.UserDeactivator;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.property.PropertyTesting.newUserPropertyDto;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UserServiceIT {

  private static final UsersSearchRequest SEARCH_REQUEST = getBuilderWithDefaultsPageSize().build();
  private GroupDto defaultGroup;

  @Rule
  public DbTester db = DbTester.create();
  private final DbSession dbSession = db.getSession();

  private final ManagedInstanceService managedInstanceService = mock(ManagedInstanceService.class);
  private final ManagedInstanceChecker managedInstanceChecker = mock(ManagedInstanceChecker.class);

  private final UserAnonymizer userAnonymizer = new UserAnonymizer(db.getDbClient(), () -> "anonymized");

  private final UserDeactivator userDeactivator = new UserDeactivator(db.getDbClient(), userAnonymizer);
  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());
  private final UserUpdater userUpdater = new UserUpdater(mock(NewUserNotifier.class), db.getDbClient(), new DefaultGroupFinder(db.getDbClient()),
    settings.asConfig(), new NoOpAuditPersister(), localAuthentication);

  private final UserService userService = new UserService(db.getDbClient(), new AvatarResolverImpl(), managedInstanceService, managedInstanceChecker, userDeactivator, userUpdater);

  @Before
  public void setUp() {
    defaultGroup = db.users().insertDefaultGroup();
  }

  @Test
  public void search_for_all_active_users() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser(u -> u.setActive(false));

    SearchResults<UserInformation> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), r -> r.userDto().getName())
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName()),
        tuple(user2.getLogin(), user2.getName()));
  }

  @Test
  public void search_deactivated_users() {
    UserDto user1 = db.users().insertUser(u -> u.setActive(false));
    UserDto user2 = db.users().insertUser(u -> u.setActive(true));

    SearchResults<UserInformation> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setDeactivated(true).build());

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), r -> r.userDto().getName())
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName()));
  }

  @Test
  public void search_with_query() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("user-%_%-login")
      .setName("user-name")
      .setEmail("user@mail.com")
      .setLocal(true)
      .setScmAccounts(singletonList("user1")));

    SearchResults<UserInformation> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setQuery("user-%_%-").build());
    assertThat(users.searchResults()).extracting(UserInformation::userDto).extracting(UserDto::getLogin)
      .containsExactly(user.getLogin());

    users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setQuery("user@MAIL.com").build());
    assertThat(users.searchResults()).extracting(UserInformation::userDto).extracting(UserDto::getLogin)
      .containsExactly(user.getLogin());

    users = userService.findUsers(getBuilderWithDefaultsPageSize().setQuery("user-name").build());
    assertThat(users.searchResults()).extracting(UserInformation::userDto).extracting(UserDto::getLogin)
      .containsExactly(user.getLogin());
  }

  @Test
  public void return_avatar() {
    UserDto user = db.users().insertUser(u -> u.setEmail("john@doe.com"));

    SearchResults<UserInformation> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), UserInformation::avatar)
      .containsExactlyInAnyOrder(
        tuple(user.getLogin(), Optional.of("6a6c19fea4a3676970167ce51f39e6ee")));

  }

  @Test
  public void return_isManagedFlag() {
    UserDto nonManagedUser = db.users().insertUser(u -> u.setEmail("john@doe.com"));
    UserDto managedUser = db.users().insertUser(u -> u.setEmail("externalUser@doe.com"));
    mockUsersAsManaged(managedUser.getUuid());

    SearchResults<UserInformation> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), UserInformation::managed)
      .containsExactlyInAnyOrder(
        tuple(managedUser.getLogin(), true),
        tuple(nonManagedUser.getLogin(), false));

  }

  @Test
  public void search_whenFilteringByManagedAndInstanceManaged_returnsCorrectResults() {
    UserDto nonManagedUser = db.users().insertUser(u -> u.setEmail("john@doe.com"));
    UserDto managedUser = db.users().insertUser(u -> u.setEmail("externalUser@doe.com"));
    db.users().enableScimForUser(managedUser);
    mockUsersAsManaged(managedUser.getUuid());
    mockInstanceExternallyManagedAndFilterForManagedUsers();

    SearchResults<UserInformation> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setManaged(true).build());

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), UserInformation::managed)
      .containsExactlyInAnyOrder(
        tuple(managedUser.getLogin(), true));

  }

  @Test
  public void search_whenFilteringByNonManagedAndInstanceManaged_returnsCorrectResults() {
    UserDto nonManagedUser = db.users().insertUser(u -> u.setEmail("john@doe.com"));
    UserDto managedUser = db.users().insertUser(u -> u.setEmail("externalUser@doe.com"));
    db.users().enableScimForUser(managedUser);
    mockUsersAsManaged(managedUser.getUuid());
    mockInstanceExternallyManagedAndFilterForManagedUsers();

    SearchResults<UserInformation> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setManaged(false).build());

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), UserInformation::managed)
      .containsExactlyInAnyOrder(
        tuple(nonManagedUser.getLogin(), false));
  }

  private void mockInstanceExternallyManagedAndFilterForManagedUsers() {
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);
    when(managedInstanceService.getManagedUsersSqlFilter(anyBoolean()))
      .thenAnswer(invocation -> {
        Boolean managed = invocation.getArgument(0, Boolean.class);
        return new ScimUserDao(mock(UuidFactory.class)).getManagedUserSqlFilter(managed);
      });
  }

  @Test
  public void search_whenFilteringByExternalLoginAndMatchFound_returnsTheCorrectResult() {
    prepareUsersWithExternalLogin();

    SearchResults<UserInformation> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setExternalLogin("user1").build());

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getExternalLogin())
      .containsExactly("user1");
  }

  @Test
  public void search_whenFilteringByExternalLoginAndNoMatchFound_returnsNoResult() {
    prepareUsersWithExternalLogin();

    SearchResults<UserInformation> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setExternalLogin("nomatch").build());

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getExternalLogin())
      .isEmpty();
  }

  private void prepareUsersWithExternalLogin() {
    db.users().insertUser(user -> user.setExternalLogin("user1"));
    db.users().insertUser(user -> user.setExternalLogin("USER1"));
    db.users().insertUser(user -> user.setExternalLogin("user1-oldaccount"));
  }

  @Test
  public void return_scm_accounts() {
    UserDto user = db.users().insertUser(u -> u.setScmAccounts(asList("john1", "john2")));

    SearchResults<UserInformation> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), userSearchResult -> userSearchResult.userDto().getSortedScmAccounts())
      .containsExactlyInAnyOrder(tuple(user.getLogin(), asList("john1", "john2")));
  }

  @Test
  public void return_tokens_count_when_system_administer() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user);
    db.users().insertToken(user);

    SearchResults<UserInformation> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), UserInformation::tokensCount)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), 2));
  }

  @Test
  public void return_user_not_having_email() {
    UserDto user = db.users().insertUser(u -> u.setEmail(null));

    SearchResults<UserInformation> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), userSearchResult -> userSearchResult.userDto().getEmail())
      .containsExactlyInAnyOrder(tuple(user.getLogin(), null));
  }

  @Test
  public void return_groups() {
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");
    GroupDto group3 = db.users().insertGroup("group3");
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    SearchResults<UserInformation> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), UserInformation::groups)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), asList(group1.getName(), group2.getName())));
  }

  @Test
  public void return_external_information() {
    UserDto user = db.users().insertUser();

    SearchResults<UserInformation> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(
        r -> r.userDto().getLogin(),
        userSearchResult -> userSearchResult.userDto().getExternalLogin(),
        userSearchResult -> userSearchResult.userDto().getExternalIdentityProvider())
      .containsExactlyInAnyOrder(tuple(user.getLogin(), user.getExternalLogin(), user.getExternalIdentityProvider()));
  }

  @Test
  public void return_last_connection_date() {
    UserDto userWithLastConnectionDate = db.users().insertUser();
    db.users().updateLastConnectionDate(userWithLastConnectionDate, 10_000_000_000L);
    UserDto userWithoutLastConnectionDate = db.users().insertUser();

    SearchResults<UserInformation> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin(), userSearchResult -> userSearchResult.userDto().getLastConnectionDate())
      .containsExactlyInAnyOrder(
        tuple(userWithLastConnectionDate.getLogin(), 10_000_000_000L),
        tuple(userWithoutLastConnectionDate.getLogin(), null));
  }

  @Test
  public void return_all_fields_for_logged_user() {
    UserDto user = db.users().insertUser(u -> u.setEmail("aa@bb.com"));
    db.users().updateLastConnectionDate(user, 10_000_000_000L);
    db.users().insertToken(user);
    db.users().insertToken(user);
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);

    SearchResults<UserInformation> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults())
      .extracting(UserInformation::userDto)
      .extracting(UserDto::getLogin, UserDto::getName, UserDto::getEmail, UserDto::getExternalLogin, UserDto::getExternalIdentityProvider,
        userDto -> !userDto.getSortedScmAccounts().isEmpty(), UserDto::getLastConnectionDate)
      .containsExactlyInAnyOrder(
        tuple(user.getLogin(), user.getName(), user.getEmail(), user.getExternalLogin(), user.getExternalIdentityProvider(), true, 10_000_000_000L));

    assertThat(users.searchResults())
      .extracting(UserInformation::avatar, UserInformation::tokensCount, userSearchResult -> userSearchResult.groups().size())
      .containsExactly(tuple(Optional.of("5dcdf28d944831f2fb87d48b81500c66"), 2, 1));

  }

  @Test
  public void search_whenNoPagingInformationProvided_setsDefaultValues() {
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertUser(u -> u.setLogin("user-" + i).setName("User " + i)));

    SearchResults<UserInformation> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.total()).isEqualTo(10);
  }

  @Test
  public void search_with_paging() {
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertUser(u -> u.setLogin("user-" + i).setName("User " + i)));

    SearchResults<UserInformation> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(5).build());

    assertThat(users.searchResults())
      .extracting(u -> u.userDto().getLogin())
      .containsExactly("user-0", "user-1", "user-2", "user-3", "user-4");
    assertThat(users.total()).isEqualTo(10);

    users = userService.findUsers(UsersSearchRequest.builder().setPage(2).setPageSize(5).build());

    assertThat(users.searchResults())
      .extracting(u -> u.userDto().getLogin())
      .containsExactly("user-5", "user-6", "user-7", "user-8", "user-9");
    assertThat(users.total()).isEqualTo(10);

  }

  @Test
  public void search_whenPageSizeIsZero_shouldOnlyReturnPaginationInfo() {
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertUser(u -> u.setLogin("user-" + i).setName("User " + i)));

    SearchResults<UserInformation> users = userService.findUsers(UsersSearchRequest.builder().setPageSize(0).build());
    assertThat(users.searchResults()).isEmpty();
    assertThat(users.total()).isEqualTo(10);
  }

  @Test
  public void return_empty_result_when_no_user() {
    SearchResults<UserInformation> users = userService.findUsers(SEARCH_REQUEST);

    assertThat(users.searchResults()).isEmpty();
    assertThat(users.total()).isZero();
  }

  @Test
  public void search_whenFilteringConnectionDate_shouldApplyFilter() {
    final Instant lastConnection = Instant.now();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("user-%_%-login")
      .setName("user-name")
      .setEmail("user@mail.com")
      .setLocal(true)
      .setScmAccounts(singletonList("user1")));
    user = db.users().updateLastConnectionDate(user, lastConnection.toEpochMilli());
    user = db.users().updateSonarLintLastConnectionDate(user, lastConnection.toEpochMilli());

    SearchResults<UserInformation> users = userService.findUsers(UsersSearchRequest.builder().setPage(1).setPageSize(50).setQuery("user-%_%-").build());

    assertThat(users.searchResults())
      .extracting(r -> r.userDto().getLogin())
      .containsExactlyInAnyOrder(user.getLogin());

    assertUserWithFilter(b -> b.setLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.minus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), true);
    assertUserWithFilter(b -> b.setLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.plus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), false);
    assertUserWithFilter(b -> b.setLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.minus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), false);
    assertUserWithFilter(b -> b.setLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.plus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), true);

    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.minus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), true);
    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.plus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), false);
    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.minus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), false);
    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.plus(1, ChronoUnit.DAYS).toEpochMilli())), user.getLogin(), true);

    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.toEpochMilli())), user.getLogin(), true);
    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.toEpochMilli())), user.getLogin(), true);
  }

  @Test
  public void search_whenNoLastConnection_shouldReturnForBeforeOnly() {
    final Instant lastConnection = Instant.now();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("user-%_%-login")
      .setName("user-name")
      .setEmail("user@mail.com")
      .setLocal(true)
      .setScmAccounts(singletonList("user1")));

    assertUserWithFilter(b -> b.setLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.toEpochMilli())), user.getLogin(), false);
    assertUserWithFilter(b -> b.setLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.toEpochMilli())), user.getLogin(), true);

    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateFrom(DateUtils.formatDateTime(lastConnection.toEpochMilli())), user.getLogin(), false);
    assertUserWithFilter(b -> b.setSonarLintLastConnectionDateTo(DateUtils.formatDateTime(lastConnection.toEpochMilli())), user.getLogin(), true);
  }

  @Test
  public void deactivate_user_and_delete_their_related_data() {
    createAdminUser();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("ada.lovelace")
      .setEmail("ada.lovelace@noteg.com")
      .setName("Ada Lovelace")
      .setScmAccounts(singletonList("al")));

    userService.deactivate(user.getLogin(), false);

    verifyThatUserIsDeactivated(user.getLogin());
  }

  @Test
  public void anonymize_user_if_anonymize_is_true() {
    createAdminUser();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("ada.lovelace")
      .setEmail("ada.lovelace@noteg.com")
      .setName("Ada Lovelace")
      .setScmAccounts(singletonList("al")));

    userService.deactivate(user.getLogin(), true);

    verifyThatUserIsDeactivated("anonymized");
    verifyThatUserIsAnomymized("anonymized");
  }

  @Test
  public void deactivate_user_deletes_their_group_membership() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    db.users().insertGroup();
    db.users().insertMember(group1, user);

    userService.deactivate(user.getLogin(), false);

    assertThat(db.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(dbSession, user.getUuid())).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_their_tokens() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    db.users().insertToken(user);
    db.users().insertToken(user);
    db.commit();

    userService.deactivate(user.getLogin(), false);

    assertThat(db.getDbClient().userTokenDao().selectByUser(dbSession, user)).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_their_properties() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.properties().insertProperty(newUserPropertyDto(user), null, null, null, user.getLogin());
    db.properties().insertProperty(newUserPropertyDto(user), null, null, null, user.getLogin());
    db.properties().insertProperty(newUserPropertyDto(user).setEntityUuid(project.uuid()), project.getKey(),
      project.name(), project.qualifier(), user.getLogin());

    userService.deactivate(user.getLogin(), false);

    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setUserUuid(user.getUuid()).build(), dbSession)).isEmpty();
    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setUserUuid(user.getUuid()).setEntityUuid(project.uuid()).build(), dbSession)).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_their_permissions() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.SCAN);
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.ADMINISTER_QUALITY_PROFILES);
    db.users().insertProjectPermissionOnUser(user, UserRole.USER, project);
    db.users().insertProjectPermissionOnUser(user, UserRole.CODEVIEWER, project);

    userService.deactivate(user.getLogin(), false);

    assertThat(db.getDbClient().userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getUuid())).isEmpty();
    assertThat(db.getDbClient().userPermissionDao().selectEntityPermissionsOfUser(dbSession, user.getUuid(), project.uuid())).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_their_permission_templates() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate();
    PermissionTemplateDto anotherTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(template.getUuid(), user.getUuid(), UserRole.USER, template.getName(), user.getLogin());
    db.permissionTemplates().addUserToTemplate(anotherTemplate.getUuid(), user.getUuid(), UserRole.CODEVIEWER, anotherTemplate.getName(), user.getLogin());

    userService.deactivate(user.getLogin(), false);

    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, template.getUuid())).extracting(PermissionTemplateUserDto::getUserUuid)
      .isEmpty();
    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, anotherTemplate.getUuid())).extracting(PermissionTemplateUserDto::getUserUuid)
      .isEmpty();
  }

  @Test
  public void deactivate_user_deletes_their_qprofiles_permissions() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    QProfileDto profile = db.qualityProfiles().insert();
    db.qualityProfiles().addUserPermission(profile, user);

    userService.deactivate(user.getLogin(), false);

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(dbSession, profile, user)).isFalse();
  }

  @Test
  public void deactivate_user_deletes_their_default_assignee_settings() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto anotherProject = db.components().insertPrivateProject().getMainBranchComponent();
    db.properties().insertProperty(new PropertyDto().setKey("sonar.issues.defaultAssigneeLogin").setValue(user.getLogin())
      .setEntityUuid(project.uuid()), project.getKey(), project.name(), project.qualifier(), user.getLogin());
    db.properties().insertProperty(new PropertyDto().setKey("sonar.issues.defaultAssigneeLogin").setValue(user.getLogin())
      .setEntityUuid(anotherProject.uuid()), anotherProject.getKey(), anotherProject.name(), anotherProject.qualifier(), user.getLogin());
    db.properties().insertProperty(new PropertyDto().setKey("other").setValue(user.getLogin())
      .setEntityUuid(anotherProject.uuid()), anotherProject.getKey(), anotherProject.name(), anotherProject.qualifier(), user.getLogin());

    userService.deactivate(user.getLogin(), false);

    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setKey("sonar.issues.defaultAssigneeLogin").build(), db.getSession())).isEmpty();
    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().build(), db.getSession())).extracting(PropertyDto::getKey).containsOnly("other");
  }

  @Test
  public void deactivate_user_deletes_their_qgate_permissions() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().addUserPermission(qualityGate, user);
    assertThat(db.countRowsOfTable("qgate_user_permissions")).isOne();

    userService.deactivate(user.getLogin(), false);

    assertThat(db.countRowsOfTable("qgate_user_permissions")).isZero();
  }

  @Test
  public void deactivate_user_deletes_their_alm_pat() {
    createAdminUser();
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();
    UserDto user = db.users().insertUser();
    db.almPats().insert(p -> p.setUserUuid(user.getUuid()), p -> p.setAlmSettingUuid(almSettingDto.getUuid()));
    UserDto anotherUser = db.users().insertUser();
    db.almPats().insert(p -> p.setUserUuid(anotherUser.getUuid()), p -> p.setAlmSettingUuid(almSettingDto.getUuid()));

    userService.deactivate(user.getLogin(), false);

    assertThat(db.getDbClient().almPatDao().selectByUserAndAlmSetting(dbSession, user.getUuid(), almSettingDto)).isEmpty();
    assertThat(db.getDbClient().almPatDao().selectByUserAndAlmSetting(dbSession, anotherUser.getUuid(), almSettingDto)).isNotNull();
  }

  @Test
  public void deactivate_user_deletes_their_session_tokens() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    SessionTokenDto sessionToken1 = db.users().insertSessionToken(user);
    SessionTokenDto sessionToken2 = db.users().insertSessionToken(user);
    UserDto anotherUser = db.users().insertUser();
    SessionTokenDto sessionToken3 = db.users().insertSessionToken(anotherUser);

    userService.deactivate(user.getLogin(), false);

    assertThat(db.getDbClient().sessionTokensDao().selectByUuid(dbSession, sessionToken1.getUuid())).isNotPresent();
    assertThat(db.getDbClient().sessionTokensDao().selectByUuid(dbSession, sessionToken2.getUuid())).isNotPresent();
    assertThat(db.getDbClient().sessionTokensDao().selectByUuid(dbSession, sessionToken3.getUuid())).isPresent();
  }

  @Test
  public void deactivate_user_deletes_their_dismissed_messages() {
    createAdminUser();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser();

    db.users().insertUserDismissedMessageOnProject(user, project1, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    db.users().insertUserDismissedMessageOnProject(user, project2, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    UserDto anotherUser = db.users().insertUser();
    UserDismissedMessageDto msg3 = db.users().insertUserDismissedMessageOnProject(anotherUser, project1, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    UserDismissedMessageDto msg4 = db.users().insertUserDismissedMessageOnProject(anotherUser, project2, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);

    userService.deactivate(user.getLogin(), false);

    assertThat(db.getDbClient().userDismissedMessagesDao().selectByUser(dbSession, user)).isEmpty();
    assertThat(db.getDbClient().userDismissedMessagesDao().selectByUser(dbSession, anotherUser))
      .extracting(UserDismissedMessageDto::getUuid)
      .containsExactlyInAnyOrder(msg3.getUuid(), msg4.getUuid());
  }

  @Test
  public void fail_if_user_does_not_exist() {

    assertThatThrownBy(() -> {
      userService.deactivate("someone", false);
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User 'someone' not found");
  }

  @Test
  public void fail_to_deactivate_last_administrator() {
    UserDto admin = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(admin, GlobalPermission.ADMINISTER);

    assertThatThrownBy(() -> {
      userService.deactivate(admin.getLogin(), false);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("User is last administrator, and cannot be deactivated");
  }

  @Test
  public void administrators_can_be_deactivated_if_there_are_still_other_administrators() {
    UserDto admin = createAdminUser();

    UserDto anotherAdmin = createAdminUser();

    userService.deactivate(admin.getLogin(), false);

    verifyThatUserIsDeactivated(admin.getLogin());
    verifyThatUserExists(anotherAdmin.getLogin());
  }

  @Test
  public void anonymizeUser_whenSamlAndScimUser_shouldDeleteScimMapping() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    db.getDbClient().scimUserDao().enableScimForUser(dbSession, user.getUuid());
    db.commit();

    userService.deactivate(user.getLogin(), true);

    assertThat(db.getDbClient().scimUserDao().findByUserUuid(dbSession, user.getUuid())).isEmpty();
  }

  @Test
  public void handle_whenUserManagedAndInstanceManaged_shouldThrow() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    doThrow(new IllegalStateException("User managed")).when(managedInstanceChecker).throwIfUserIsManaged(any(), eq(user.getUuid()));

    String login = user.getLogin();
    assertThatThrownBy(() -> userService.deactivate(login, false))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("User managed");
  }

  @Test
  public void fetchUser_whenUserDoesntExist_shouldThrowNotFoundException() {
    assertThatThrownBy(() -> userService.fetchUser("login"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User 'login' not found");
  }

  @Test
  public void fetchUser_whenUserExists_shouldReturnUser() {
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup("group1");
    GroupDto group2 = db.users().insertGroup("group2");
    db.users().insertMember(group1, user);
    db.users().insertMember(group2, user);

    db.users().insertToken(user);
    db.users().insertToken(user);

    when(managedInstanceService.isUserManaged(any(), eq(user.getUuid()))).thenReturn(false);

    UserInformation result = userService.fetchUser(user.getLogin());
    UserDto resultUser = result.userDto();
    Collection<String> resultGroups = result.groups();

    assertThat(resultUser).usingRecursiveComparison().isEqualTo(user);
    assertThat(resultGroups).containsExactlyInAnyOrder(group1.getName(), group2.getName());
    assertThat(result.managed()).isFalse();
  }

  @Test
  public void updateUser_whenUserDoesntExist_shouldThrowNotFoundException() {
    UpdateUser update = new UpdateUser();
    assertThatThrownBy(() -> userService.updateUser("login", update))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User 'login' not found");
  }

  @Test
  public void updateUser_whenUserExists_shouldApplyUpdateAndReturnUpdatedUser() {
    UserDto user = db.users().insertUser();

    UpdateUser updateUser = new UpdateUser();
    updateUser.setName("new name");
    updateUser.setEmail("newemail@example.com");
    updateUser.setScmAccounts(List.of("account1", "account2"));

    userService.updateUser(user.getLogin(), updateUser);

    UserDto updatedUser = db.users().selectUserByLogin(user.getLogin()).orElseThrow();

    assertThat(updatedUser.getName()).isEqualTo("new name");
    assertThat(updatedUser.getEmail()).isEqualTo("newemail@example.com");
    assertThat(updatedUser.getSortedScmAccounts()).containsExactly("account1", "account2");
  }

  private void assertUserWithFilter(Function<UsersSearchRequest.Builder, UsersSearchRequest.Builder> query, String userLogin, boolean isExpectedToBeThere) {

    UsersSearchRequest.Builder builder = getBuilderWithDefaultsPageSize();
    builder = query.apply(builder);

    SearchResults<UserInformation> users = userService.findUsers(builder.setQuery("user-%_%-").build());

    var assertion = assertThat(users.searchResults());
    if (isExpectedToBeThere) {
      assertion
        .extracting(r -> r.userDto().getLogin())
        .containsExactlyInAnyOrder(userLogin);
    } else {
      assertion.isEmpty();
    }
  }

  private void mockUsersAsManaged(String... userUuids) {
    when(managedInstanceService.getUserUuidToManaged(any(), any())).thenAnswer(invocation ->
      {
        Set<?> allUsersUuids = invocation.getArgument(1, Set.class);
        return allUsersUuids.stream()
          .map(userUuid -> (String) userUuid)
          .collect(toMap(identity(), userUuid -> Set.of(userUuids).contains(userUuid)));
      }
    );
  }

  private static UsersSearchRequest.Builder getBuilderWithDefaultsPageSize() {
    return UsersSearchRequest.builder().setPage(1).setPageSize(50);
  }

  @Test
  public void createUser_shouldCreateLocalUser() {
    UserCreateRequest userCreateRequest = UserCreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setScmAccounts(singletonList("jn"))
      .setPassword("1234")
      .setLocal(true)
      .build();

    UserInformation user = userService.createUser(userCreateRequest);

    assertThat(user.userDto())
      .extracting(UserDto::getLogin, UserDto::getName, UserDto::getEmail, UserDto::getSortedScmAccounts, UserDto::isLocal)
      .containsOnly("john", "John", "john@email.com", singletonList("jn"), true);

    Optional<UserDto> dbUser = db.users().selectUserByLogin("john");
    assertThat(dbUser).isPresent();

    assertThat(db.users().selectGroupUuidsOfUser(dbUser.get())).containsOnly(defaultGroup.getUuid());
  }

  @Test
  public void createUser_shouldCreateNonLocalUser() {
    UserCreateRequest userCreateRequest = UserCreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setLocal(false)
      .build();

    userService.createUser(userCreateRequest);

    assertThat(db.users().selectUserByLogin("john").get())
      .extracting(UserDto::isLocal, UserDto::getExternalIdentityProvider, UserDto::getExternalLogin)
      .containsOnly(false, "sonarqube", "john");
  }

  @Test
  public void createUser_shouldHandleCommasInScmAccounts() {
    UserCreateRequest userCreateRequest = UserCreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setScmAccounts(singletonList("j,n"))
      .setPassword("1234")
      .setLocal(true)
      .build();

    UserInformation user = userService.createUser(userCreateRequest);

    assertThat(user.userDto().getSortedScmAccounts()).containsOnly("j,n");
  }

  @Test
  public void createUser_whenWhitespaceInScmAccounts_shouldFail() {
    UserCreateRequest userCreateRequest = UserCreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setScmAccounts(List.of("admin", "  admin  "))
      .setPassword("1234")
      .setLocal(true)
      .build();

    assertThatThrownBy(() -> userService.createUser(userCreateRequest))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("SCM account cannot start or end with whitespace: '  admin  '");
  }

  @Test
  public void createUser_whenDuplicatesInScmAccounts_shouldFail() {
    UserCreateRequest userCreateRequest = UserCreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setScmAccounts(List.of("admin", "admin"))
      .setPassword("1234")
      .setLocal(true)
      .build();

    assertThatThrownBy(() -> userService.createUser(userCreateRequest))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Duplicate SCM account: 'admin'");
  }

  @Test
  public void createUser_whenEmptyEmail_shouldCreateUser() {
    UserCreateRequest userCreateRequest = UserCreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setPassword("1234")
      .setEmail("")
      .setLocal(true)
      .build();

    userService.createUser(userCreateRequest);

    assertThat(db.users().selectUserByLogin("john").get())
      .extracting(UserDto::getExternalLogin)
      .isEqualTo("john");
  }

  @Test
  public void createUser_whenDeactivatedUserExists_shouldReactivate() {
    db.users().insertUser(newUserDto("john", "John", "john@email.com").setActive(false));

    UserCreateRequest userCreateRequest = UserCreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setEmail("john@email.com")
      .setScmAccounts(singletonList("jn"))
      .setPassword("1234")
      .setLocal(true)
      .build();

    userService.createUser(userCreateRequest);

    assertThat(db.users().selectUserByLogin("john").get().isActive()).isTrue();
  }

  @Test
  public void createUser_whenActiveUserExists_shouldThrow() {
    UserDto user = db.users().insertUser();

    UserCreateRequest userCreateRequest = UserCreateRequest.builder()
      .setLogin(user.getLogin())
      .setName("John")
      .setPassword("1234")
      .setLocal(true)
      .build();

    assertThatThrownBy(() -> userService.createUser(userCreateRequest))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("An active user with login '%s' already exists", user.getLogin()));
  }

  @Test
  public void createUser_whenInstanceManaged_shouldThrow() {
    BadRequestException badRequestException = BadRequestException.create("message");
    doThrow(badRequestException).when(managedInstanceChecker).throwIfInstanceIsManaged();

    UserCreateRequest userCreateRequest = UserCreateRequest.builder()
      .setLogin("john")
      .setName("John")
      .setLocal(false)
      .build();

    assertThatThrownBy(() -> userService.createUser(userCreateRequest))
      .isEqualTo(badRequestException);
  }

  private void verifyThatUserIsDeactivated(String login) {
    Optional<UserDto> user = db.users().selectUserByLogin(login);
    assertThat(user).isPresent();
    assertThat(user.get().isActive()).isFalse();
    assertThat(user.get().getEmail()).isNull();
    assertThat(user.get().getSortedScmAccounts()).isEmpty();
  }

  private void verifyThatUserIsAnomymized(String login) {
    Optional<UserDto> user = db.users().selectUserByLogin(login);
    assertThat(user).isPresent();
    assertThat(user.get().getName()).isEqualTo(login);
    assertThat(user.get().getExternalLogin()).isEqualTo(login);
    assertThat(user.get().getExternalId()).isEqualTo(login);
    assertThat(user.get().getExternalIdentityProvider()).isEqualTo(ExternalIdentity.SQ_AUTHORITY);
  }

  private UserDto createAdminUser() {
    UserDto admin = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(admin, GlobalPermission.ADMINISTER);
    db.commit();
    return admin;
  }

  private void verifyThatUserExists(String login) {
    assertThat(db.users().selectUserByLogin(login)).isPresent();
  }

}
