/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.db.property;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PropertyNewValue;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class PropertiesDaoWithPersisterTest {
  private static final String KEY = "key";
  private static final String ANOTHER_KEY = "another_key";
  private static final String PROJECT_NAME = "project_name";
  private static final String PROJECT_UUID = "project_uuid";
  private static final String SECURED_KEY = "key.secured";
  private static final String USER_LOGIN = "user_login";
  private static final String USER_UUID = "user_uuid";
  private static final String VALUE = "value";

  private static final long INITIAL_DATE = 1_444_000L;

  private final AlwaysIncreasingSystem2 system2 = new AlwaysIncreasingSystem2(INITIAL_DATE, 1);

  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final ArgumentCaptor<PropertyNewValue> newValueCaptor = ArgumentCaptor.forClass(PropertyNewValue.class);

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system2, auditPersister);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession session = db.getSession();
  private final PropertiesDao underTest = dbClient.propertiesDao();

  @Test
  public void saveGlobalTrackedPropertyIsPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(true);
    underTest.saveProperty(new PropertyDto().setKey(KEY).setValue(VALUE));

    verify(auditPersister).isTrackedProperty(KEY);
    verify(auditPersister).addProperty(any(), newValueCaptor.capture(), eq(false));

    PropertyNewValue newValue = newValueCaptor.getValue();

    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin)
      .containsExactly(KEY, VALUE, null, null);
    assertThat(newValue.toString()).doesNotContain("projectUuid");
  }

  @Test
  public void saveGlobalNotTrackedPropertyIsNotPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(false);

    underTest.saveProperty(new PropertyDto().setKey(KEY).setValue(VALUE));

    verify(auditPersister).isTrackedProperty(KEY);
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void saveGlobalTrackedAndSecuredPropertyIsPersisted() {
    when(auditPersister.isTrackedProperty(SECURED_KEY)).thenReturn(true);

    underTest.saveProperty(new PropertyDto().setKey(SECURED_KEY).setValue(VALUE));

    verify(auditPersister).isTrackedProperty(SECURED_KEY);
    verify(auditPersister).addProperty(any(), newValueCaptor.capture(), eq(false));
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin)
      .containsExactly(SECURED_KEY, null, null, null);
    assertThat(newValue.toString()).doesNotContain("projectUuid");
  }

  @Test
  public void saveProjectTrackedPropertyIsPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(true);

    PropertyDto propertyDto = getPropertyDto(KEY);
    underTest.saveProperty(session, propertyDto, USER_LOGIN, PROJECT_NAME, Qualifiers.PROJECT);

    verify(auditPersister).isTrackedProperty(KEY);
    verify(auditPersister).addProperty(any(), newValueCaptor.capture(), eq(false));
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName, PropertyNewValue::getQualifier)
      .containsExactly(propertyDto.getKey(), propertyDto.getValue(), propertyDto.getUserUuid(),
        USER_LOGIN, propertyDto.getComponentUuid(), PROJECT_NAME, "project");
    assertThat(newValue.toString()).contains("componentUuid");
  }

  @Test
  public void saveApplicationTrackedPropertyIsPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(true);

    PropertyDto propertyDto = getPropertyDto(KEY);
    underTest.saveProperty(session, propertyDto, USER_LOGIN, "app-name", Qualifiers.APP);

    verify(auditPersister).isTrackedProperty(KEY);
    verify(auditPersister).addProperty(any(), newValueCaptor.capture(), eq(false));
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName, PropertyNewValue::getQualifier)
      .containsExactly(propertyDto.getKey(), propertyDto.getValue(), propertyDto.getUserUuid(),
        USER_LOGIN, propertyDto.getComponentUuid(), "app-name", "application");
    assertThat(newValue.toString())
      .contains("componentUuid");
  }

  @Test
  public void savePortfolioTrackedPropertyIsPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(true);

    PropertyDto propertyDto = getPropertyDto(KEY);
    underTest.saveProperty(session, propertyDto, USER_LOGIN, "portfolio-name", Qualifiers.VIEW);

    verify(auditPersister).isTrackedProperty(KEY);
    verify(auditPersister).addProperty(any(), newValueCaptor.capture(), eq(false));
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName, PropertyNewValue::getQualifier)
      .containsExactly(propertyDto.getKey(), propertyDto.getValue(), propertyDto.getUserUuid(),
        USER_LOGIN, propertyDto.getComponentUuid(), "portfolio-name", "portfolio");
    assertThat(newValue.toString())
      .contains("componentUuid");
  }

  @Test
  public void saveProjectTrackedAndSecuredPropertyIsPersisted() {
    when(auditPersister.isTrackedProperty(SECURED_KEY)).thenReturn(true);

    PropertyDto propertyDto = getPropertyDto(SECURED_KEY);
    underTest.saveProperty(session, propertyDto, USER_LOGIN, PROJECT_NAME, Qualifiers.PROJECT);

    verify(auditPersister).isTrackedProperty(SECURED_KEY);
    verify(auditPersister).addProperty(any(), newValueCaptor.capture(), eq(false));
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName, PropertyNewValue::getQualifier)
      .containsExactly(propertyDto.getKey(), null, propertyDto.getUserUuid(),
        USER_LOGIN, propertyDto.getComponentUuid(), PROJECT_NAME, "project");
    assertThat(newValue.toString()).contains("componentUuid");
  }

  @Test
  public void deleteTrackedPropertyByQueryIsPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(true);

    PropertyQuery query = getPropertyQuery(KEY);
    underTest.deleteByQuery(session, query);

    verify(auditPersister).isTrackedProperty(KEY);
    verify(auditPersister).deleteProperty(any(), newValueCaptor.capture(), eq(false));
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName)
      .containsExactly(query.key(), null, query.userUuid(),
        null, query.componentUuid(), null);
    assertThat(newValue.toString()).doesNotContain("userLogin");
  }

  @Test
  public void deleteNotTrackedPropertyByQueryIsNotPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(false);

    PropertyQuery query = getPropertyQuery(KEY);
    underTest.deleteByQuery(session, query);

    verify(auditPersister).isTrackedProperty(KEY);
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void deleteTrackedPropertyIsPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(true);

    PropertyDto propertyDto = getPropertyDto(KEY);
    underTest.delete(session, propertyDto, USER_LOGIN, PROJECT_NAME, Qualifiers.PROJECT);

    verify(auditPersister).isTrackedProperty(KEY);
    verify(auditPersister).deleteProperty(any(), newValueCaptor.capture(), eq(false));
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName)
      .containsExactly(propertyDto.getKey(), propertyDto.getValue(), propertyDto.getUserUuid(),
        USER_LOGIN, propertyDto.getComponentUuid(), PROJECT_NAME);
    assertThat(newValue.toString()).contains("userLogin");
  }

  @Test
  public void deleteNotTrackedPropertyIsNotPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(false);

    PropertyDto propertyDto = getPropertyDto(KEY);
    underTest.delete(session, propertyDto, USER_LOGIN, PROJECT_NAME, Qualifiers.PROJECT);

    verify(auditPersister).isTrackedProperty(KEY);
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void deleteTrackedProjectPropertyIsPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(true);
    underTest.deleteProjectProperty(KEY, PROJECT_UUID, PROJECT_NAME, Qualifiers.PROJECT);

    verify(auditPersister).isTrackedProperty(KEY);
    verify(auditPersister).deleteProperty(any(), newValueCaptor.capture(), eq(false));
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName)
      .containsExactly(KEY, null, null,
        null, PROJECT_UUID, PROJECT_NAME);
    assertThat(newValue.toString()).doesNotContain("userLogin");
  }

  @Test
  public void deleteNotTrackedProjectPropertyIsNotPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(false);

    PropertyDto propertyDto = getPropertyDto(KEY);
    underTest.delete(session, propertyDto, USER_LOGIN, PROJECT_NAME, Qualifiers.PROJECT);

    verify(auditPersister).isTrackedProperty(KEY);
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void deleteTrackedProjectPropertiesIsPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(true);

    underTest.deleteProjectProperties(KEY, VALUE);

    verify(auditPersister).isTrackedProperty(KEY);
    verify(auditPersister).deleteProperty(any(), newValueCaptor.capture(), eq(false));
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName)
      .containsExactly(KEY, VALUE, null,
        null, null, null);
    assertThat(newValue.toString()).doesNotContain("projectUuid");
  }

  @Test
  public void deleteNotTrackedProjectPropertiesIsNotPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(false);

    underTest.deleteProjectProperties(KEY, VALUE);

    verify(auditPersister).isTrackedProperty(KEY);
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void deleteTrackedGlobalPropertyIsPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(true);

    underTest.deleteGlobalProperty(KEY, session);

    verify(auditPersister).isTrackedProperty(KEY);
    verify(auditPersister).deleteProperty(any(), newValueCaptor.capture(), eq(false));
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName)
      .containsExactly(KEY, null, null,
        null, null, null);
    assertThat(newValue.toString()).doesNotContain("projectUuid");
  }

  @Test
  public void deleteNotTrackedGlobalPropertyIsNotPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(false);

    underTest.deleteGlobalProperty(KEY, session);

    verify(auditPersister).isTrackedProperty(KEY);
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void deletePropertyByUserIsPersisted() {
    UserDto user = setUserProperties(VALUE);
    underTest.deleteByUser(session, user.getUuid(), user.getLogin());

    verify(auditPersister, times(2)).isTrackedProperty(KEY);
    verify(auditPersister, times(2)).isTrackedProperty(ANOTHER_KEY);
    verify(auditPersister, times(2)).isTrackedProperty(SECURED_KEY);
    verify(auditPersister, times(2)).deleteProperty(any(), newValueCaptor.capture(), eq(false));
    List<PropertyNewValue> newValues = newValueCaptor.getAllValues();
    assertThat(newValues.get(0))
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName)
      .containsExactly(KEY, null, user.getUuid(),
        user.getLogin(), null, null);
    assertThat(newValues.get(0).toString()).contains("userUuid");
    assertThat(newValues.get(1))
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName)
      .containsExactly(SECURED_KEY, null, user.getUuid(),
        user.getLogin(), null, null);
    assertThat(newValues.get(1).toString()).doesNotContain("value");
  }

  @Test
  public void deletePropertyByUserLoginIsPersisted() {
    UserDto user = setUserProperties(null);
    underTest.deleteByMatchingLogin(session, user.getLogin(), newArrayList(KEY, ANOTHER_KEY, SECURED_KEY));

    verify(auditPersister, times(2)).isTrackedProperty(KEY);
    verify(auditPersister, times(2)).isTrackedProperty(ANOTHER_KEY);
    verify(auditPersister, times(2)).isTrackedProperty(SECURED_KEY);
    verify(auditPersister, times(2)).deleteProperty(any(), newValueCaptor.capture(), eq(false));
    List<PropertyNewValue> newValues = newValueCaptor.getAllValues();
    assertThat(newValues.get(0))
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName)
      .containsExactly(KEY, null, null,
        user.getLogin(), null, null);
    assertThat(newValues.get(0).toString()).contains("userLogin");
    assertThat(newValues.get(1))
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName)
      .containsExactly(SECURED_KEY, null, null,
        user.getLogin(), null, null);
    assertThat(newValues.get(1).toString()).doesNotContain("value");
  }

  @Test
  public void deleteTrackedPropertyByKeyAndValueIsPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(true);

    underTest.deleteByKeyAndValue(session, KEY, VALUE);

    verify(auditPersister).isTrackedProperty(KEY);
    verify(auditPersister).deleteProperty(any(), newValueCaptor.capture(), eq(false));
    PropertyNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PropertyNewValue::getPropertyKey, PropertyNewValue::getPropertyValue,
        PropertyNewValue::getUserUuid, PropertyNewValue::getUserLogin,
        PropertyNewValue::getComponentUuid, PropertyNewValue::getComponentName)
      .containsExactly(KEY, VALUE, null,
        null, null, null);
    assertThat(newValue.toString()).doesNotContain("projectUuid");
  }

  @Test
  public void deleteNotTrackedPropertyByKeyAndValueIsNotPersisted() {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(false);

    underTest.deleteByKeyAndValue(session, KEY, VALUE);

    verify(auditPersister).isTrackedProperty(KEY);
    verifyNoMoreInteractions(auditPersister);
  }

  private PropertyDto getPropertyDto(String key) {
    return new PropertyDto()
      .setKey(key)
      .setValue(VALUE)
      .setUserUuid(USER_UUID)
      .setComponentUuid(PROJECT_UUID);
  }

  private PropertyQuery getPropertyQuery(String key) {
    return PropertyQuery.builder()
      .setKey(key)
      .setComponentUuid(PROJECT_UUID)
      .setUserUuid(USER_UUID)
      .build();
  }

  private UserDto setUserProperties(@Nullable String value) {
    when(auditPersister.isTrackedProperty(KEY)).thenReturn(true);
    when(auditPersister.isTrackedProperty(ANOTHER_KEY)).thenReturn(false);
    when(auditPersister.isTrackedProperty(SECURED_KEY)).thenReturn(true);

    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();

    if (value == null) {
      value = user.getLogin();
    }

    PropertyDto dto1 = new PropertyDto().setKey(KEY)
      .setComponentUuid(project.uuid())
      .setUserUuid(user.getUuid())
      .setValue(value);
    PropertyDto dto2 = new PropertyDto().setKey(ANOTHER_KEY)
      .setComponentUuid(project.uuid())
      .setUserUuid(user.getUuid())
      .setValue(value);
    PropertyDto dto3 = new PropertyDto().setKey(SECURED_KEY)
      .setComponentUuid(project.uuid())
      .setUserUuid(user.getUuid())
      .setValue(value);
    db.properties().insertProperty(dto1, project.name(), project.qualifier(), user.getLogin());
    db.properties().insertProperty(dto2, project.name(), project.qualifier(), user.getLogin());
    db.properties().insertProperty(dto3, project.name(), project.qualifier(), user.getLogin());
    return user;
  }
}
