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

import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.db.user.GroupMembershipQuery.builder;
import static org.sonar.db.user.UserTesting.newUserDto;

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
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;

public class UserDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  static final long NOW = 1500000000000L;

  DbClient dbClient = db.getDbClient();

  UserDao underTest = db.getDbClient().userDao();
  final DbSession session = db.getSession();

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
    insertUserRole(user);
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
    assertThat(userReloaded.getUpdatedAt()).isEqualTo(NOW);

    assertThat(underTest.selectUserById(session, otherUser.getId())).isNotNull();

    assertThat(dbClient.dashboardDao().selectById(session, dashboard.getId())).isNull();
    assertThat(dbClient.activeDashboardDao().selectById(session, activeDashboard.getId())).isNull();
    assertThat(dbClient.issueFilterDao().selectById(session, issueFilter.getId())).isNull();
    assertThat(dbClient.issueFilterFavouriteDao().selectById(session, issueFilterFavourite.getId())).isNull();
    assertThat(dbClient.measureFilterDao().selectById(session, measureFilter.getId())).isNull();
    assertThat(dbClient.measureFilterFavouriteDao().selectById(session, measureFilterFavourite.getId())).isNull();
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setKey(property.getKey()).build(), session)).isEmpty();
    assertThat(dbClient.roleDao().selectUserPermissions(session, user.getLogin(), null)).isEmpty();
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
    assertThat(dto.getCreatedAt()).isEqualTo(1418215735482L);
    assertThat(dto.getUpdatedAt()).isEqualTo(1418215735485L);
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
    dbClient.propertiesDao().insertProperty(session, dto);
    return dto;
  }

  private PropertyDto insertProperty(String key, String value, long componentId) {
    PropertyDto dto = new PropertyDto().setKey(key).setValue(value).setResourceId(componentId);
    dbClient.propertiesDao().insertProperty(session, dto);
    return dto;
  }

  private UserPermissionDto insertUserRole(UserDto user) {
    UserPermissionDto dto = new UserPermissionDto().setUserId(user.getId()).setPermission(randomAlphanumeric(64));
    dbClient.roleDao().insertUserRole(session, dto);
    return dto;
  }

  private UserGroupDto insertUserGroup(UserDto user) {
    GroupDto group = new GroupDto().setName(randomAlphanumeric(30));
    dbClient.groupDao().insert(session, group);

    UserGroupDto dto = new UserGroupDto().setUserId(user.getId()).setGroupId(group.getId());
    dbClient.userGroupDao().insert(session, dto);
    return dto;
  }
}
