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
package org.sonar.db.user;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.user.UserQuery;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.dashboard.ActiveDashboardDto;
import org.sonar.db.dashboard.DashboardDto;
import org.sonar.db.issue.IssueFilterDto;
import org.sonar.db.issue.IssueFilterFavouriteDto;
import org.sonar.db.measure.MeasureFilterDto;
import org.sonar.db.measure.MeasureFilterFavouriteDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.db.user.GroupMembershipQuery.builder;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UserDaoTest {
  private static final long NOW = 1_500_000_000_000L;
  private static final long DATE_1 = 1_222_001L;
  private static final long DATE_2 = 4_333_555L;

  private System2 system2 = mock(System2.class);

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system2);

  private DbClient dbClient = db.getDbClient();
  private DbSession session = db.getSession();
  private UserDao underTest = db.getDbClient().userDao();

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(NOW);
  }

  @Test
  public void selectUsersIds() {
    db.prepareDbUnit(getClass(), "selectUsersByIds.xml");

    Collection<UserDto> users = underTest.selectByIds(session, asList(100L, 101L, 987L));
    assertThat(users).hasSize(2);
    assertThat(users).extracting("login").containsOnly("marius", "inactive_user");

    assertThat(underTest.selectByIds(session, Collections.<Long>emptyList())).isEmpty();
  }

  @Test
  public void selectUserByLogin_ignore_inactive() {
    db.prepareDbUnit(getClass(), "selectActiveUserByLogin.xml");

    UserDto user = underTest.selectUserById(50);
    assertThat(user.getLogin()).isEqualTo("inactive_user");

    user = underTest.selectActiveUserByLogin("inactive_user");
    assertThat(user).isNull();
  }

  @Test
  public void selectUserByLogin_not_found() {
    db.prepareDbUnit(getClass(), "selectActiveUserByLogin.xml");

    UserDto user = underTest.selectActiveUserByLogin("not_found");
    assertThat(user).isNull();
  }

  @Test
  public void selectUsersByLogins() {
    db.prepareDbUnit(getClass(), "selectUsersByLogins.xml");

    Collection<UserDto> users = underTest.selectByLogins(asList("marius", "inactive_user", "other"));
    assertThat(users).hasSize(2);
    assertThat(users).extracting("login").containsOnly("marius", "inactive_user");
  }

  @Test
  public void selectUsersByLogins_empty_logins() {
    // no need to access db
    Collection<UserDto> users = underTest.selectByLogins(Collections.<String>emptyList());
    assertThat(users).isEmpty();
  }

  @Test
  public void selectByOrderedLogins() {
    underTest.insert(session, newUserDto().setLogin("U1").setActive(true));
    underTest.insert(session, newUserDto().setLogin("U2").setActive(true));
    session.commit();

    Iterable<UserDto> users = underTest.selectByOrderedLogins(session, asList("U1", "U2", "U3"));
    assertThat(users).extracting("login").containsExactly("U1", "U2");

    users = underTest.selectByOrderedLogins(session, asList("U2", "U3", "U1"));
    assertThat(users).extracting("login").containsExactly("U2", "U1");

    assertThat(underTest.selectByOrderedLogins(session, Collections.<String>emptyList())).isEmpty();
  }

  @Test
  public void selectUsersByQuery_all() {
    db.prepareDbUnit(getClass(), "selectUsersByQuery.xml");

    UserQuery query = UserQuery.builder().includeDeactivated().build();
    List<UserDto> users = underTest.selectUsers(query);
    assertThat(users).hasSize(2);
  }

  @Test
  public void selectUsersByQuery_only_actives() {
    db.prepareDbUnit(getClass(), "selectUsersByQuery.xml");

    UserQuery query = UserQuery.ALL_ACTIVES;
    List<UserDto> users = underTest.selectUsers(query);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getName()).isEqualTo("Marius");
  }

  @Test
  public void selectUsersByQuery_filter_by_login() {
    db.prepareDbUnit(getClass(), "selectUsersByQuery.xml");

    UserQuery query = UserQuery.builder().logins("marius", "john").build();
    List<UserDto> users = underTest.selectUsers(query);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getName()).isEqualTo("Marius");
  }

  @Test
  public void selectUsersByQuery_search_by_login_text() {
    db.prepareDbUnit(getClass(), "selectUsersByText.xml");

    UserQuery query = UserQuery.builder().searchText("sbr").build();
    List<UserDto> users = underTest.selectUsers(query);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getLogin()).isEqualTo("sbrandhof");
  }

  @Test
  public void selectUsersByQuery_search_by_name_text() {
    db.prepareDbUnit(getClass(), "selectUsersByText.xml");

    UserQuery query = UserQuery.builder().searchText("Simon").build();
    List<UserDto> users = underTest.selectUsers(query);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getLogin()).isEqualTo("sbrandhof");
  }

  @Test
  public void selectUsersByQuery_escape_special_characters_in_like() {
    db.prepareDbUnit(getClass(), "selectUsersByText.xml");

    UserQuery query = UserQuery.builder().searchText("%s%").build();
    // we expect really a login or name containing the 3 characters "%s%"

    List<UserDto> users = underTest.selectUsers(query);
    assertThat(users).isEmpty();
  }

  @Test
  public void selectUsers_returns_both_only_root_or_only_non_root_depending_on_mustBeRoot_and_mustNotBeRoot_calls_on_query() {
    UserDto user1 = insertUser(true);
    UserDto root1 = insertRootUser(newUserDto());
    UserDto user2 = insertUser(true);
    UserDto root2 = insertRootUser(newUserDto());

    assertThat(underTest.selectUsers(UserQuery.builder().build()))
      .extracting(UserDto::getLogin)
      .containsOnly(user1.getLogin(), user2.getLogin(), root1.getLogin(), root2.getLogin());
    assertThat(underTest.selectUsers(UserQuery.builder().mustBeRoot().build()))
      .extracting(UserDto::getLogin)
      .containsOnly(root1.getLogin(), root2.getLogin());
    assertThat(underTest.selectUsers(UserQuery.builder().mustNotBeRoot().build()))
      .extracting(UserDto::getLogin)
      .containsOnly(user1.getLogin(), user2.getLogin());
  }

  @Test
  public void countRootUsersButLogin_returns_0_when_there_is_no_user_at_all() {
    assertThat(underTest.countRootUsersButLogin(session, "bla")).isEqualTo(0);
  }

  @Test
  public void countRootUsersButLogin_returns_0_when_there_is_no_root() {
    underTest.insert(session, newUserDto());
    session.commit();

    assertThat(underTest.countRootUsersButLogin(session, "bla")).isEqualTo(0);
  }

  @Test
  public void countRootUsersButLogin_returns_0_when_there_is_no_active_root() {
    insertNonRootUser(newUserDto());
    insertInactiveRootUser(newUserDto());
    session.commit();

    assertThat(underTest.countRootUsersButLogin(session, "bla")).isEqualTo(0);
  }

  @Test
  public void countRootUsersButLogin_returns_count_of_all_active_roots_when_there_specified_login_does_not_exist() {
    insertRootUser(newUserDto());
    insertNonRootUser(newUserDto());
    insertRootUser(newUserDto());
    insertRootUser(newUserDto());
    insertInactiveRootUser(newUserDto());
    insertInactiveRootUser(newUserDto());
    session.commit();

    assertThat(underTest.countRootUsersButLogin(session, "bla")).isEqualTo(3);
  }

  @Test
  public void countRootUsersButLogin_returns_count_of_all_active_roots_when_specified_login_is_not_root() {
    insertRootUser(newUserDto());
    String login = insertNonRootUser(newUserDto()).getLogin();
    insertRootUser(newUserDto());
    insertRootUser(newUserDto());
    insertInactiveRootUser(newUserDto());
    insertInactiveRootUser(newUserDto());
    session.commit();

    assertThat(underTest.countRootUsersButLogin(session, login)).isEqualTo(3);
  }

  @Test
  public void countRootUsersButLogin_returns_count_of_all_active_roots_when_specified_login_is_inactive_root() {
    insertRootUser(newUserDto());
    insertNonRootUser(newUserDto());
    insertRootUser(newUserDto());
    insertRootUser(newUserDto());
    String inactiveRootLogin = insertInactiveRootUser(newUserDto()).getLogin();
    insertInactiveRootUser(newUserDto());
    session.commit();

    assertThat(underTest.countRootUsersButLogin(session, inactiveRootLogin)).isEqualTo(3);
  }

  @Test
  public void countRootUsersButLogin_returns_count_of_all_active_roots_minus_one_when_specified_login_is_active_root() {
    insertRootUser(newUserDto());
    insertNonRootUser(newUserDto());
    insertRootUser(newUserDto());
    String rootLogin = insertRootUser(newUserDto()).getLogin();
    insertInactiveRootUser(newUserDto());
    insertInactiveRootUser(newUserDto());
    session.commit();

    assertThat(underTest.countRootUsersButLogin(session, rootLogin)).isEqualTo(2);
  }

  private UserDto insertInactiveRootUser(UserDto dto) {
    insertRootUser(dto);
    dto.setActive(false);
    underTest.update(session, dto);
    session.commit();
    return dto;
  }

  private UserDto insertRootUser(UserDto dto) {
    underTest.insert(session, dto);
    underTest.setRoot(session, dto.getLogin(), true);
    session.commit();
    return dto;
  }

  private UserDto insertNonRootUser(UserDto dto) {
    underTest.insert(session, dto);
    session.commit();
    return dto;
  }

  @Test
  public void insert_user() {
    Long date = DateUtils.parseDate("2014-06-20").getTime();

    UserDto userDto = new UserDto()
      .setId(1L)
      .setLogin("john")
      .setName("John")
      .setEmail("jo@hn.com")
      .setScmAccounts(",jo.hn,john2,")
      .setActive(true)
      .setSalt("1234")
      .setCryptedPassword("abcd")
      .setExternalIdentity("johngithub")
      .setExternalIdentityProvider("github")
      .setLocal(true)
      .setCreatedAt(date)
      .setUpdatedAt(date);
    underTest.insert(db.getSession(), userDto);
    db.getSession().commit();

    UserDto user = underTest.selectActiveUserByLogin("john");
    assertThat(user).isNotNull();
    assertThat(user.getId()).isNotNull();
    assertThat(user.getLogin()).isEqualTo("john");
    assertThat(user.getName()).isEqualTo("John");
    assertThat(user.getEmail()).isEqualTo("jo@hn.com");
    assertThat(user.isActive()).isTrue();
    assertThat(user.getScmAccounts()).isEqualTo(",jo.hn,john2,");
    assertThat(user.getSalt()).isEqualTo("1234");
    assertThat(user.getCryptedPassword()).isEqualTo("abcd");
    assertThat(user.getExternalIdentity()).isEqualTo("johngithub");
    assertThat(user.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(user.isLocal()).isTrue();
    assertThat(user.isRoot()).isFalse();
    assertThat(user.getCreatedAt()).isEqualTo(date);
    assertThat(user.getUpdatedAt()).isEqualTo(date);
  }

  @Test
  public void update_user() {
    UserDto existingUser = new UserDto()
      .setLogin("john")
      .setName("John")
      .setEmail("jo@hn.com")
      .setCreatedAt(1418215735482L)
      .setUpdatedAt(1418215735482L)
      .setActive(true)
      .setLocal(true);
    db.getDbClient().userDao().insert(db.getSession(), existingUser);
    db.getSession().commit();

    UserDto userDto = new UserDto()
      .setId(1L)
      .setLogin("john")
      .setName("John Doo")
      .setEmail("jodoo@hn.com")
      .setScmAccounts(",jo.hn,john2,johndoo,")
      .setActive(false)
      .setSalt("12345")
      .setCryptedPassword("abcde")
      .setExternalIdentity("johngithub")
      .setExternalIdentityProvider("github")
      .setLocal(false)
      .setUpdatedAt(1500000000000L);
    underTest.update(db.getSession(), userDto);
    db.getSession().commit();

    UserDto user = underTest.selectUserById(db.getSession(), existingUser.getId());
    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo(existingUser.getId());
    assertThat(user.getLogin()).isEqualTo("john");
    assertThat(user.getName()).isEqualTo("John Doo");
    assertThat(user.getEmail()).isEqualTo("jodoo@hn.com");
    assertThat(user.isActive()).isFalse();
    assertThat(user.getScmAccounts()).isEqualTo(",jo.hn,john2,johndoo,");
    assertThat(user.getSalt()).isEqualTo("12345");
    assertThat(user.getCryptedPassword()).isEqualTo("abcde");
    assertThat(user.getExternalIdentity()).isEqualTo("johngithub");
    assertThat(user.getExternalIdentityProvider()).isEqualTo("github");
    assertThat(user.isLocal()).isFalse();
    assertThat(user.isRoot()).isFalse();
    assertThat(user.getCreatedAt()).isEqualTo(1418215735482L);
    assertThat(user.getUpdatedAt()).isEqualTo(1500000000000L);
  }

  @Test
  public void deactivate_user() throws Exception {
    UserDto user = newActiveUser();
    DashboardDto dashboard = insertDashboard(user, false);
    ActiveDashboardDto activeDashboard = insertActiveDashboard(dashboard, user);
    IssueFilterDto issueFilter = insertIssueFilter(user, false);
    IssueFilterFavouriteDto issueFilterFavourite = insertIssueFilterFavourite(issueFilter, user);
    MeasureFilterDto measureFilter = insertMeasureFilter(user, false);
    MeasureFilterFavouriteDto measureFilterFavourite = insertMeasureFilterFavourite(measureFilter, user);
    PropertyDto property = insertProperty(user);
    insertUserPermission(user);
    insertUserGroup(user);

    UserDto otherUser = newActiveUser();

    session.commit();

    boolean deactivated = underTest.deactivateUserByLogin(session, user.getLogin());
    assertThat(deactivated).isTrue();

    UserDto userReloaded = underTest.selectUserById(session, user.getId());
    assertThat(userReloaded.isActive()).isFalse();
    assertThat(userReloaded.getEmail()).isNull();
    assertThat(userReloaded.getScmAccounts()).isNull();
    assertThat(userReloaded.getSalt()).isNull();
    assertThat(userReloaded.getCryptedPassword()).isNull();
    assertThat(userReloaded.getExternalIdentity()).isNull();
    assertThat(userReloaded.getExternalIdentityProvider()).isNull();
    assertThat(userReloaded.isRoot()).isFalse();
    assertThat(userReloaded.getUpdatedAt()).isEqualTo(NOW);

    assertThat(underTest.selectUserById(session, otherUser.getId())).isNotNull();

    assertThat(dbClient.dashboardDao().selectById(session, dashboard.getId())).isNull();
    assertThat(dbClient.activeDashboardDao().selectById(session, activeDashboard.getId())).isNull();
    assertThat(dbClient.issueFilterDao().selectById(session, issueFilter.getId())).isNull();
    assertThat(dbClient.issueFilterFavouriteDao().selectById(session, issueFilterFavourite.getId())).isNull();
    assertThat(dbClient.measureFilterDao().selectById(session, measureFilter.getId())).isNull();
    assertThat(dbClient.measureFilterFavouriteDao().selectById(session, measureFilterFavourite.getId())).isNull();
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setKey(property.getKey()).build(), session)).isEmpty();
    assertThat(dbClient.userPermissionDao().selectPermissionsByLogin(session, user.getLogin(), null)).isEmpty();
    assertThat(dbClient.groupMembershipDao().countGroups(session, builder().login(user.getLogin()).membership(IN).build(), user.getId())).isZero();
  }

  @Test
  public void deactivate_user_does_not_remove_shared_dashboard() throws Exception {
    UserDto user = newActiveUser();
    DashboardDto notSharedDashboard = insertDashboard(user, false);
    DashboardDto sharedDashboard = insertDashboard(user, true);
    session.commit();

    boolean deactivated = underTest.deactivateUserByLogin(session, user.getLogin());
    assertThat(deactivated).isTrue();

    assertThat(dbClient.dashboardDao().selectById(session, notSharedDashboard.getId())).isNull();
    DashboardDto sharedDashboardReloaded = dbClient.dashboardDao().selectById(session, sharedDashboard.getId());
    assertThat(sharedDashboardReloaded).isNotNull();
    assertThat(sharedDashboardReloaded.getUserId()).isEqualTo(user.getId());
  }

  @Test
  public void deactivate_user_does_not_remove_shared_issue_filter() throws Exception {
    UserDto user = newActiveUser();
    IssueFilterDto notSharedFilter = insertIssueFilter(user, false);
    IssueFilterDto sharedFilter = insertIssueFilter(user, true);
    session.commit();

    boolean deactivated = underTest.deactivateUserByLogin(session, user.getLogin());
    assertThat(deactivated).isTrue();

    assertThat(dbClient.issueFilterDao().selectById(session, notSharedFilter.getId())).isNull();
    IssueFilterDto sharedFilterReloaded = dbClient.issueFilterDao().selectById(session, sharedFilter.getId());
    assertThat(sharedFilterReloaded).isNotNull();
    assertThat(sharedFilterReloaded.getUserLogin()).isEqualTo(user.getLogin());
  }

  @Test
  public void deactivate_user_does_not_remove_shared_measure_filter() throws Exception {
    UserDto user = newActiveUser();
    MeasureFilterDto notSharedFilter = insertMeasureFilter(user, false);
    MeasureFilterDto sharedFilter = insertMeasureFilter(user, true);
    session.commit();

    boolean deactivated = underTest.deactivateUserByLogin(session, user.getLogin());
    assertThat(deactivated).isTrue();

    assertThat(dbClient.measureFilterDao().selectById(session, notSharedFilter.getId())).isNull();
    MeasureFilterDto sharedFilterReloaded = dbClient.measureFilterDao().selectById(session, sharedFilter.getId());
    assertThat(sharedFilterReloaded).isNotNull();
    assertThat(sharedFilterReloaded.getUserId()).isEqualTo(user.getId());
  }

  @Test
  public void deactivate_user_also_remove_default_assignee_login_properties() throws Exception {
    UserDto user = newActiveUser();
    insertProperty("sonar.issues.defaultAssigneeLogin", user.getLogin(), 10L);
    insertProperty("sonar.issues.defaultAssigneeLogin", user.getLogin(), 11L);
    insertProperty("sonar.issues.defaultAssigneeLogin", user.getLogin(), 12L);

    UserDto otherUser = newActiveUser();
    insertProperty("sonar.issues.defaultAssigneeLogin", otherUser.getLogin(), 13L);

    session.commit();

    boolean deactivated = underTest.deactivateUserByLogin(session, user.getLogin());
    assertThat(deactivated).isTrue();

    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setKey("sonar.issues.defaultAssigneeLogin").build(), session)).hasSize(1);
  }

  @Test
  public void deactivate_missing_user() {
    String login = "does_not_exist";
    boolean deactivated = underTest.deactivateUserByLogin(session, login);
    assertThat(deactivated).isFalse();
    assertThat(underTest.selectActiveUserByLogin(login)).isNull();
  }

  @Test
  public void select_by_login() {
    db.prepareDbUnit(getClass(), "select_by_login.xml");

    UserDto dto = underTest.selectOrFailByLogin(session, "marius");
    assertThat(dto.getId()).isEqualTo(101);
    assertThat(dto.getLogin()).isEqualTo("marius");
    assertThat(dto.getName()).isEqualTo("Marius");
    assertThat(dto.getEmail()).isEqualTo("marius@lesbronzes.fr");
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getScmAccountsAsList()).containsOnly("ma", "marius33");
    assertThat(dto.getSalt()).isEqualTo("79bd6a8e79fb8c76ac8b121cc7e8e11ad1af8365");
    assertThat(dto.getCryptedPassword()).isEqualTo("650d2261c98361e2f67f90ce5c65a95e7d8ea2fg");
    assertThat(dto.isRoot()).isFalse();
    assertThat(dto.getCreatedAt()).isEqualTo(1418215735482L);
    assertThat(dto.getUpdatedAt()).isEqualTo(1418215735485L);

    dto = underTest.selectOrFailByLogin(session, "sbrandhof");
    assertThat(dto.isRoot()).isTrue();
  }

  @Test
  public void select_nullable_by_scm_account() {
    db.prepareDbUnit(getClass(), "select_nullable_by_scm_account.xml");

    List<UserDto> results = underTest.selectByScmAccountOrLoginOrEmail(session, "ma");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getLogin()).isEqualTo("marius");

    results = underTest.selectByScmAccountOrLoginOrEmail(session, "marius");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getLogin()).isEqualTo("marius");

    results = underTest.selectByScmAccountOrLoginOrEmail(session, "marius@lesbronzes.fr");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getLogin()).isEqualTo("marius");

    results = underTest.selectByScmAccountOrLoginOrEmail(session, "marius@lesbronzes.fr");
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getLogin()).isEqualTo("marius");

    results = underTest.selectByScmAccountOrLoginOrEmail(session, "m");
    assertThat(results).isEmpty();

    results = underTest.selectByScmAccountOrLoginOrEmail(session, "unknown");
    assertThat(results).isEmpty();
  }

  @Test
  public void select_nullable_by_scm_account_return_many_results_when_same_email_is_used_by_many_users() {
    db.prepareDbUnit(getClass(), "select_nullable_by_scm_account_return_many_results_when_same_email_is_used_by_many_users.xml");

    List<UserDto> results = underTest.selectByScmAccountOrLoginOrEmail(session, "marius@lesbronzes.fr");
    assertThat(results).hasSize(2);
  }

  @Test
  public void select_by_login_with_unknown_login() {
    try {
      underTest.selectOrFailByLogin(session, "unknown");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(RowNotFoundException.class).hasMessage("User with login 'unknown' has not been found");
    }
  }

  @Test
  public void select_nullable_by_login() {
    db.prepareDbUnit(getClass(), "select_by_login.xml");

    assertThat(underTest.selectByLogin(session, "marius")).isNotNull();

    assertThat(underTest.selectByLogin(session, "unknown")).isNull();
  }

  @Test
  public void exists_by_email() throws Exception {
    UserDto activeUser = newActiveUser();
    UserDto disableUser = insertUser(false);

    assertThat(underTest.doesEmailExist(session, activeUser.getEmail())).isTrue();
    assertThat(underTest.doesEmailExist(session, disableUser.getEmail())).isFalse();
    assertThat(underTest.doesEmailExist(session, "unknown")).isFalse();
  }

  @Test
  public void setRoot_does_not_fail_on_non_existing_login() {
    underTest.setRoot(session, "unkown", true);
    underTest.setRoot(session, "unkown", false);
  }

  @Test
  public void setRoot_set_root_flag_of_specified_user_to_specified_value_and_updates_udpateAt() {
    String login = newActiveUser().getLogin();
    UserDto otherUser = newActiveUser();
    assertThat(underTest.selectByLogin(session, login).isRoot()).isEqualTo(false);
    assertThat(underTest.selectByLogin(session, otherUser.getLogin()).isRoot()).isEqualTo(false);

    // does not fail when changing to same value
    when(system2.now()).thenReturn(15_000L);
    commit(() -> underTest.setRoot(session, login, false));
    verifyRootAndUpdatedAt(login, false, 15_000L);
    verifyRootAndUpdatedAt(otherUser.getLogin(), false, otherUser.getUpdatedAt());

    // change value
    when(system2.now()).thenReturn(26_000L);
    commit(() -> underTest.setRoot(session, login, true));
    verifyRootAndUpdatedAt(login, true, 26_000L);
    verifyRootAndUpdatedAt(otherUser.getLogin(), false, otherUser.getUpdatedAt());

    // does not fail when changing to same value
    when(system2.now()).thenReturn(37_000L);
    commit(() -> underTest.setRoot(session, login, true));
    verifyRootAndUpdatedAt(login, true, 37_000L);
    verifyRootAndUpdatedAt(otherUser.getLogin(), false, otherUser.getUpdatedAt());

    // change value back
    when(system2.now()).thenReturn(48_000L);
    commit(() -> underTest.setRoot(session, login, false));
    verifyRootAndUpdatedAt(login, false, 48_000L);
    verifyRootAndUpdatedAt(otherUser.getLogin(), false, otherUser.getUpdatedAt());
  }

  private void verifyRootAndUpdatedAt(String login1, boolean root, long updatedAt) {
    UserDto userDto = underTest.selectByLogin(session, login1);
    assertThat(userDto.isRoot()).isEqualTo(root);
    assertThat(userDto.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  public void setRoot_has_no_effect_on_root_flag_of_inactive_user() {
    String nonRootInactiveUser = insertUser(false).getLogin();
    commit(() -> underTest.setRoot(session, nonRootInactiveUser, true));
    assertThat(underTest.selectByLogin(session, nonRootInactiveUser).isRoot()).isFalse();

    // create inactive root user
    UserDto rootUser = newActiveUser();
    commit(() -> underTest.setRoot(session, rootUser.getLogin(), true));
    rootUser.setActive(false);
    commit(() -> underTest.update(session, rootUser));
    UserDto inactiveRootUser = underTest.selectByLogin(session, rootUser.getLogin());
    assertThat(inactiveRootUser.isRoot()).isTrue();
    assertThat(inactiveRootUser.isActive()).isFalse();

    commit(() -> underTest.setRoot(session, inactiveRootUser.getLogin(), false));
    assertThat(underTest.selectByLogin(session, inactiveRootUser.getLogin()).isRoot()).isTrue();
  }

  @Test
  public void updateRootFlagFromPermissions_sets_root_flag_to_false_if_user_has_no_permission_at_all() {
    UserDto rootUser = db.users().makeRoot(db.users().insertUser());
    UserDto notRootUser = db.users().insertUser();

    call_updateRootFlagFromPermissions(rootUser, DATE_1);
    db.rootFlag().verify(rootUser, false, DATE_1);
    db.rootFlag().verify(notRootUser, false, notRootUser.getUpdatedAt());

    call_updateRootFlagFromPermissions(notRootUser, DATE_2);
    db.rootFlag().verify(rootUser, false, DATE_1);
    db.rootFlag().verify(notRootUser, false, DATE_2);
  }

  @Test
  public void updateRootFlagFromPermissions_sets_root_flag_to_true_if_user_has_admin_user_permission_on_default_organization() {
    UserDto rootUser = db.users().insertRootByUserPermission();
    UserDto incorrectlyNotRootUser = db.users().insertUser();
    db.users().insertPermissionOnUser(db.getDefaultOrganization(), incorrectlyNotRootUser, SYSTEM_ADMIN);

    call_updateRootFlagFromPermissions(rootUser, DATE_1);
    db.rootFlag().verify(rootUser, true, DATE_1);
    db.rootFlag().verify(incorrectlyNotRootUser, false, incorrectlyNotRootUser.getUpdatedAt());

    call_updateRootFlagFromPermissions(incorrectlyNotRootUser, DATE_2);
    db.rootFlag().verify(rootUser, true, DATE_1);
    db.rootFlag().verify(incorrectlyNotRootUser, true, DATE_2);
  }

  @Test
  public void updateRootFlagFromPermissions_ignores_permissions_on_anyone_on_default_organization() {
    UserDto rootUser = db.users().makeRoot(db.users().insertUser());
    UserDto incorrectlyNotRootUser = db.users().insertUser();
    db.users().insertPermissionOnAnyone(db.getDefaultOrganization(), SYSTEM_ADMIN);

    call_updateRootFlagFromPermissions(rootUser, DATE_1);
    db.rootFlag().verify(rootUser, false, DATE_1);
    db.rootFlag().verify(incorrectlyNotRootUser, false, incorrectlyNotRootUser.getUpdatedAt());

    call_updateRootFlagFromPermissions(incorrectlyNotRootUser, DATE_2);
    db.rootFlag().verify(rootUser, false, DATE_1);
    db.rootFlag().verify(incorrectlyNotRootUser, false, DATE_2);
  }

  @Test
  public void updateRootFlagFromPermissions_ignores_permissions_on_anyone_on_other_organization() {
    UserDto falselyRootUser = db.users().makeRoot(db.users().insertUser());
    UserDto notRootUser = db.users().insertUser();
    OrganizationDto otherOrganization = db.organizations().insert();
    db.users().insertPermissionOnAnyone(otherOrganization, SYSTEM_ADMIN);

    call_updateRootFlagFromPermissions(falselyRootUser, DATE_2);
    db.rootFlag().verify(falselyRootUser, false, DATE_2);
    db.rootFlag().verify(notRootUser, false, notRootUser.getUpdatedAt());

    call_updateRootFlagFromPermissions(notRootUser, DATE_1);
    db.rootFlag().verify(falselyRootUser, false, DATE_2);
    db.rootFlag().verify(notRootUser, false, DATE_1);
  }

  @Test
  public void updateRootFlagFromPermissions_sets_root_flag_to_false_if_user_has_admin_user_permission_on_other_organization() {
    UserDto falselyRootUser = db.users().makeRoot(db.users().insertUser());
    UserDto notRootUser = db.users().insertUser();
    OrganizationDto otherOrganization = db.organizations().insert();
    db.users().insertPermissionOnUser(otherOrganization, falselyRootUser, SYSTEM_ADMIN);
    db.users().insertPermissionOnUser(otherOrganization, notRootUser, SYSTEM_ADMIN);

    call_updateRootFlagFromPermissions(falselyRootUser, DATE_1);
    db.rootFlag().verify(falselyRootUser, false, DATE_1);
    db.rootFlag().verify(notRootUser, false, notRootUser.getUpdatedAt());

    call_updateRootFlagFromPermissions(notRootUser, DATE_2);
    db.rootFlag().verify(falselyRootUser, false, DATE_1);
    db.rootFlag().verify(notRootUser, false, DATE_2);
  }

  @Test
  public void updateRootFlagFromPermissions_sets_root_flag_to_true_if_user_has_admin_group_permission_on_default_organization() {
    UserDto rootUser = db.users().makeRoot(db.users().insertUser());
    UserDto incorrectlyNotRootUser = db.users().insertUser();
    GroupDto groupDto = db.users().insertAdminGroup(db.getDefaultOrganization());
    db.users().insertMembers(groupDto, rootUser, incorrectlyNotRootUser);

    call_updateRootFlagFromPermissions(rootUser, DATE_1);
    db.rootFlag().verify(rootUser, true, DATE_1);
    db.rootFlag().verify(incorrectlyNotRootUser, false, incorrectlyNotRootUser.getUpdatedAt());

    call_updateRootFlagFromPermissions(incorrectlyNotRootUser, DATE_2);
    db.rootFlag().verify(rootUser, true, DATE_1);
    db.rootFlag().verify(incorrectlyNotRootUser, true, DATE_2);
  }

  @Test
  public void updateRootFlagFromPermissions_sets_root_flag_to_false_if_user_has_admin_group_permission_on_other_organization() {
    UserDto falselyRootUser = db.users().makeRoot(db.users().insertUser());
    UserDto notRootUser = db.users().insertUser();
    GroupDto otherOrganizationGroupDto = db.users().insertGroup(db.organizations().insert());
    db.users().insertPermissionOnGroup(otherOrganizationGroupDto, SYSTEM_ADMIN);
    db.users().insertMembers(otherOrganizationGroupDto, falselyRootUser, notRootUser);

    call_updateRootFlagFromPermissions(falselyRootUser, DATE_2);
    db.rootFlag().verify(falselyRootUser, false, DATE_2);
    db.rootFlag().verify(notRootUser, false, notRootUser.getUpdatedAt());

    call_updateRootFlagFromPermissions(notRootUser, DATE_1);
    db.rootFlag().verify(falselyRootUser, false, DATE_2);
    db.rootFlag().verify(notRootUser, false, DATE_1);
  }

  private void commit(Runnable runnable) {
    runnable.run();
    session.commit();
  }

  private UserDto newActiveUser() {
    return insertUser(true);
  }

  private UserDto insertUser(boolean active) {
    UserDto dto = newUserDto().setActive(active);
    underTest.insert(session, dto);
    return dto;
  }

  private DashboardDto insertDashboard(UserDto user, boolean shared) {
    DashboardDto dto = new DashboardDto().setUserId(user.getId()).setShared(shared);
    dbClient.dashboardDao().insert(session, dto);
    return dto;
  }

  private ActiveDashboardDto insertActiveDashboard(DashboardDto dashboard, UserDto user) {
    ActiveDashboardDto dto = new ActiveDashboardDto().setDashboardId(dashboard.getId()).setUserId(user.getId());
    dbClient.activeDashboardDao().insert(session, dto);
    return dto;
  }

  private IssueFilterDto insertIssueFilter(UserDto user, boolean shared) {
    IssueFilterDto dto = new IssueFilterDto().setUserLogin(user.getLogin()).setName(randomAlphanumeric(100)).setShared(shared);
    dbClient.issueFilterDao().insert(session, dto);
    return dto;
  }

  private IssueFilterFavouriteDto insertIssueFilterFavourite(IssueFilterDto filter, UserDto user) {
    IssueFilterFavouriteDto dto = new IssueFilterFavouriteDto().setUserLogin(user.getLogin()).setIssueFilterId(filter.getId());
    dbClient.issueFilterFavouriteDao().insert(session, dto);
    return dto;
  }

  private MeasureFilterDto insertMeasureFilter(UserDto user, boolean shared) {
    MeasureFilterDto dto = new MeasureFilterDto().setUserId(user.getId()).setName(randomAlphanumeric(100)).setShared(shared);
    dbClient.measureFilterDao().insert(session, dto);
    return dto;
  }

  private MeasureFilterFavouriteDto insertMeasureFilterFavourite(MeasureFilterDto measureFilter, UserDto user) {
    MeasureFilterFavouriteDto dto = new MeasureFilterFavouriteDto().setUserId(user.getId()).setMeasureFilterId(measureFilter.getId());
    dbClient.measureFilterFavouriteDao().insert(session, dto);
    return dto;
  }

  private PropertyDto insertProperty(UserDto user) {
    PropertyDto dto = new PropertyDto().setKey(randomAlphanumeric(100)).setUserId(user.getId());
    dbClient.propertiesDao().saveProperty(session, dto);
    return dto;
  }

  private PropertyDto insertProperty(String key, String value, long componentId) {
    PropertyDto dto = new PropertyDto().setKey(key).setValue(value).setResourceId(componentId);
    dbClient.propertiesDao().saveProperty(session, dto);
    return dto;
  }

  private org.sonar.db.permission.UserPermissionDto insertUserPermission(UserDto user) {
    String permission = randomAlphanumeric(64);
    UserPermissionDto dto = new UserPermissionDto(db.getDefaultOrganization().getUuid(), permission, user.getId(), null);
    dbClient.userPermissionDao().insert(session, dto);
    return dto;
  }

  private UserGroupDto insertUserGroup(UserDto user) {
    GroupDto group = newGroupDto().setName(randomAlphanumeric(30));
    dbClient.groupDao().insert(session, group);

    UserGroupDto dto = new UserGroupDto().setUserId(user.getId()).setGroupId(group.getId());
    dbClient.userGroupDao().insert(session, dto);
    return dto;
  }

  private void call_updateRootFlagFromPermissions(UserDto userDto, long now) {
    when(system2.now()).thenReturn(now);
    underTest.updateRootFlagFromPermissions(db.getSession(), userDto.getId(), db.getDefaultOrganization().getUuid());
    db.commit();
  }

}
