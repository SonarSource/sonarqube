/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PropertyNewValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UserPropertiesDaoWithPersisterTest {
  private static final long NOW = 1_500_000_000_000L;
  private static final String SECURED_PROPERTY_KEY = "a_key.secured";
  private static final String PROPERTY_KEY = "a_key";

  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private ArgumentCaptor<PropertyNewValue> newValueCaptor = ArgumentCaptor.forClass(PropertyNewValue.class);

  private TestSystem2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2, auditPersister);

  private UserPropertiesDao underTest = db.getDbClient().userPropertiesDao();

  @Test
  public void insertTrackedUserPropertyIsPersisted() {
    UserDto user = db.users().insertUser();

    verify(auditPersister).addUser(eq(db.getSession()), any());

    UserPropertyDto userSetting = underTest.insertOrUpdate(db.getSession(), new UserPropertyDto()
        .setUserUuid(user.getUuid())
        .setKey(PROPERTY_KEY)
        .setValue("a_value"),
      user.getLogin());

    verify(auditPersister).addProperty(eq(db.getSession()), newValueCaptor.capture(), eq(true));
    assertThat(newValueCaptor.getValue())
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue, PropertyNewValue::getUserUuid,
        PropertyNewValue::getUserLogin)
      .containsExactly(userSetting.getKey(), userSetting.getValue(), user.getUuid(), user.getLogin());

  }

  @Test
  public void insertTrackedAndSecuredUserPropertyIsPersisted() {
    UserDto user = db.users().insertUser();

    verify(auditPersister).addUser(eq(db.getSession()), any());

    UserPropertyDto userSetting = underTest.insertOrUpdate(db.getSession(), new UserPropertyDto()
        .setUserUuid(user.getUuid())
        .setKey(SECURED_PROPERTY_KEY)
        .setValue("a_value"),
      user.getLogin());

    verify(auditPersister).addProperty(eq(db.getSession()), newValueCaptor.capture(), eq(true));
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue, PropertyNewValue::getUserUuid,
        PropertyNewValue::getUserLogin)
      .containsExactly(userSetting.getKey(), null, user.getUuid(), user.getLogin());
    assertThat(newValue.toString()).doesNotContain("propertyValue");
  }

  @Test
  public void updateTrackedUserPropertyIsPersisted() {
    UserDto user = db.users().insertUser();
    underTest.insertOrUpdate(db.getSession(), new UserPropertyDto()
        .setUserUuid(user.getUuid())
        .setKey(PROPERTY_KEY)
        .setValue("old_value"),
      user.getLogin());
    system2.setNow(2_000_000_000_000L);
    UserPropertyDto userSetting = underTest.insertOrUpdate(db.getSession(), new UserPropertyDto()
        .setUserUuid(user.getUuid())
        .setKey(PROPERTY_KEY)
        .setValue("new_value"),
      user.getLogin());

    verify(auditPersister).addUser(eq(db.getSession()), any());
    verify(auditPersister).addProperty(eq(db.getSession()), any(), eq(true));
    verify(auditPersister).updateProperty(eq(db.getSession()), newValueCaptor.capture(), eq(true));
    assertThat(newValueCaptor.getValue())
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue, PropertyNewValue::getUserUuid,
        PropertyNewValue::getUserLogin)
      .containsExactly(userSetting.getKey(), userSetting.getValue(), user.getUuid(), user.getLogin());
  }

  @Test
  public void deleteTrackedUserPropertyIsPersisted() {
    when(auditPersister.isTrackedProperty(PROPERTY_KEY)).thenReturn(true);
    when(auditPersister.isTrackedProperty(SECURED_PROPERTY_KEY)).thenReturn(false);

    UserDto user = db.users().insertUser();
    UserPropertyDto userSetting = underTest.insertOrUpdate(db.getSession(), new UserPropertyDto()
        .setUserUuid(user.getUuid())
        .setKey(PROPERTY_KEY)
        .setValue("a_value"),
      user.getLogin());
    underTest.insertOrUpdate(db.getSession(), new UserPropertyDto()
        .setUserUuid(user.getUuid())
        .setKey(SECURED_PROPERTY_KEY)
        .setValue("another_value"),
      user.getLogin());
    underTest.deleteByUser(db.getSession(), user);

    verify(auditPersister).addUser(eq(db.getSession()), any());
    verify(auditPersister, times(2)).addProperty(eq(db.getSession()), any(), eq(true));
    verify(auditPersister).isTrackedProperty(PROPERTY_KEY);
    verify(auditPersister).isTrackedProperty(SECURED_PROPERTY_KEY);
    verify(auditPersister).deleteProperty(eq(db.getSession()), newValueCaptor.capture(), eq(true));
    verifyNoMoreInteractions(auditPersister);
    assertThat(newValueCaptor.getValue())
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue, PropertyNewValue::getUserUuid,
        PropertyNewValue::getUserLogin)
      .containsExactly(userSetting.getKey(), userSetting.getValue(), user.getUuid(), user.getLogin());
  }

  @Test
  public void deleteTrackedUserPropertyWithoutAffectedRowsIsNotPersisted() {
    UserDto user = db.users().insertUser();

    underTest.deleteByUser(db.getSession(), user);

    verify(auditPersister).addUser(eq(db.getSession()), any());
    verifyNoMoreInteractions(auditPersister);
  }
}
