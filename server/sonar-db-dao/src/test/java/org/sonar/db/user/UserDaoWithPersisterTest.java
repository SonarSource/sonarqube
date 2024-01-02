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
package org.sonar.db.user;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserNewValue;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;

@RunWith(DataProviderRunner.class)
public class UserDaoWithPersisterTest {
  private static final long NOW = 1_500_000_000_000L;
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  private final ArgumentCaptor<UserNewValue> newValueCaptor = ArgumentCaptor.forClass(UserNewValue.class);

  @Rule
  public final DbTester db = DbTester.create(system2, auditPersister);

  private final DbClient dbClient = db.getDbClient();
  private final UserDao underTest = db.getDbClient().userDao();

  @Test
  public void insertUserIsPersisted() {
    UserDto userDto = new UserDto()
      .setLogin("john")
      .setName("John")
      .setEmail("jo@hn.com")
      .setExternalLogin("john-1")
      .setExternalIdentityProvider("sonarqube")
      .setExternalId("EXT_ID");
    underTest.insert(db.getSession(), userDto);
    db.getSession().commit();
    UserDto user = underTest.selectActiveUserByLogin(db.getSession(), "john");

    verify(auditPersister).addUser(eq(db.getSession()), newValueCaptor.capture());
    UserNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(UserNewValue::getUserUuid, UserNewValue::getUserLogin)
      .containsExactly(user.getUuid(), user.getLogin());
    assertThat(newValue.toString()).doesNotContain("name");
  }

  @Test
  public void updateUserIsPersisted() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("john")
      .setName("John")
      .setEmail("jo@hn.com")
      .setActive(true)
      .setLocal(true)
      .setResetPassword(false));
    UserDto updatedUser = newUserDto()
      .setUuid(user.getUuid())
      .setLogin("johnDoo")
      .setName("John Doo")
      .setEmail("jodoo@hn.com")
      .setScmAccounts(",jo.hn,john2,johndoo,")
      .setActive(false)
      .setResetPassword(true)
      .setSalt("12345")
      .setCryptedPassword("abcde")
      .setHashMethod("BCRYPT")
      .setExternalLogin("johngithub")
      .setExternalIdentityProvider("github")
      .setExternalId("EXT_ID")
      .setLocal(false)
      .setHomepageType("project")
      .setHomepageParameter("OB1")
      .setLastConnectionDate(10_000_000_000L);
    underTest.update(db.getSession(), updatedUser);

    verify(auditPersister).updateUser(eq(db.getSession()), newValueCaptor.capture());
    UserNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(UserNewValue::getUserUuid, UserNewValue::getUserLogin, UserNewValue::getName, UserNewValue::getEmail, UserNewValue::isActive,
        UserNewValue::getScmAccounts, UserNewValue::getExternalId, UserNewValue::getExternalLogin, UserNewValue::getExternalIdentityProvider,
        UserNewValue::isLocal, UserNewValue::getLastConnectionDate)
      .containsExactly(updatedUser.getUuid(), updatedUser.getLogin(), updatedUser.getName(), updatedUser.getEmail(), updatedUser.isActive(),
        updatedUser.getScmAccounts(), updatedUser.getExternalId(), updatedUser.getExternalLogin(), updatedUser.getExternalIdentityProvider(),
        updatedUser.isLocal(), updatedUser.getLastConnectionDate());
    assertThat(newValue.toString())
      .contains("name")
      .contains(DateUtils.formatDateTime(updatedUser.getLastConnectionDate()));
  }

  @Test
  public void updateUserWithoutTrackIsNotPersisted() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("john")
      .setName("John")
      .setEmail("jo@hn.com")
      .setActive(true)
      .setLocal(true)
      .setResetPassword(false));

    verify(auditPersister).addUser(eq(db.getSession()), newValueCaptor.capture());

    UserDto updatedUser = newUserDto()
      .setUuid(user.getUuid())
      .setLogin("johnDoo");
    underTest.update(db.getSession(), updatedUser, false);

    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void deactivateUserIsPersisted() {
    UserDto user = insertActiveUser();
    insertUserGroup(user);
    underTest.update(db.getSession(), user.setLastConnectionDate(10_000_000_000L));
    db.getSession().commit();
    underTest.deactivateUser(db.getSession(), user);

    verify(auditPersister).deactivateUser(eq(db.getSession()), newValueCaptor.capture());
    assertThat(newValueCaptor.getValue())
      .extracting(UserNewValue::getUserUuid, UserNewValue::getUserLogin)
      .containsExactly(user.getUuid(), user.getLogin());
  }

  private UserDto insertActiveUser() {
    UserDto dto = newUserDto().setActive(true);
    underTest.insert(db.getSession(), dto);
    return dto;
  }

  private UserGroupDto insertUserGroup(UserDto user) {
    GroupDto group = newGroupDto().setName(randomAlphanumeric(30));
    dbClient.groupDao().insert(db.getSession(), group);

    UserGroupDto dto = new UserGroupDto().setUserUuid(user.getUuid()).setGroupUuid(group.getUuid());
    dbClient.userGroupDao().insert(db.getSession(), dto, group.getName(), user.getLogin());
    return dto;
  }
}
