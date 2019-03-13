/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonar.db.property.PropertyTesting.newPropertyDto;
import static org.sonar.db.property.PropertyTesting.newUserPropertyDto;

@RunWith(DataProviderRunner.class)
public class PropertiesDaoTest {
  private static final String VALUE_SIZE_4000 = String.format("%1$4000.4000s", "*");
  private static final String VALUE_SIZE_4001 = VALUE_SIZE_4000 + "P";
  private static final long DATE_1 = 1_555_000L;
  private static final long DATE_2 = 1_666_000L;
  private static final long DATE_3 = 1_777_000L;
  private static final long DATE_4 = 1_888_000L;
  private static final long DATE_5 = 1_999_000L;

  private System2 system2 = mock(System2.class);

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system2);

  private DbClient dbClient = db.getDbClient();
  private DbSession session = db.getSession();

  private PropertiesDao underTest = db.getDbClient().propertiesDao();

  @Test
  public void shouldFindUsersForNotification() {
    ComponentDto project1 = insertPrivateProject("uuid_45");
    ComponentDto project2 = insertPrivateProject("uuid_56");
    UserDto user1 = db.users().insertUser(u -> u.setLogin("user1"));
    UserDto user2 = db.users().insertUser(u -> u.setLogin("user2"));
    UserDto user3 = db.users().insertUser(u -> u.setLogin("user3"));
    insertProperty("notification.NewViolations.Email", "true", project1.getId(), user2.getId());
    insertProperty("notification.NewViolations.Twitter", "true", null, user3.getId());
    insertProperty("notification.NewViolations.Twitter", "true", project2.getId(), user1.getId());
    insertProperty("notification.NewViolations.Twitter", "true", project1.getId(), user2.getId());
    insertProperty("notification.NewViolations.Twitter", "true", project2.getId(), user3.getId());
    db.users().insertProjectPermissionOnUser(user2, UserRole.USER, project1);
    db.users().insertProjectPermissionOnUser(user3, UserRole.USER, project2);
    db.users().insertProjectPermissionOnUser(user1, UserRole.USER, project2);

    assertThat(underTest.findUsersForNotification("NewViolations", "Email", null))
      .isEmpty();

    assertThat(underTest.findUsersForNotification("NewViolations", "Email", "uuid_78"))
      .isEmpty();

    assertThat(underTest.findUsersForNotification("NewViolations", "Email", project1.getKey()))
      .containsOnly(new Subscriber("user2", false));

    assertThat(underTest.findUsersForNotification("NewViolations", "Email", project2.getKey()))
      .isEmpty();

    assertThat(underTest.findUsersForNotification("NewViolations", "Twitter", null))
      .containsOnly(new Subscriber("user3", true));

    assertThat(underTest.findUsersForNotification("NewViolations", "Twitter", "uuid_78"))
      .containsOnly(new Subscriber("user3", true));

    assertThat(underTest.findUsersForNotification("NewViolations", "Twitter", project1.getKey()))
      .containsOnly(new Subscriber("user2", false), new Subscriber("user3", true));

    assertThat(underTest.findUsersForNotification("NewViolations", "Twitter", project2.getKey()))
      .containsOnly(new Subscriber("user1", false), new Subscriber("user3", true), new Subscriber("user3", false));
  }

  @Test
  public void hasNotificationSubscribers() {
    int userId1 = db.users().insertUser(u -> u.setLogin("user1")).getId();
    int userId2 = db.users().insertUser(u -> u.setLogin("user2")).getId();
    Long projectId = insertPrivateProject("PROJECT_A").getId();
    // global subscription
    insertProperty("notification.DispatcherWithGlobalSubscribers.Email", "true", null, userId2);
    // project subscription
    insertProperty("notification.DispatcherWithProjectSubscribers.Email", "true", projectId, userId1);
    insertProperty("notification.DispatcherWithGlobalAndProjectSubscribers.Email", "true", 56L, userId1);
    insertProperty("notification.DispatcherWithGlobalAndProjectSubscribers.Email", "true", projectId, userId1);
    // global subscription
    insertProperty("notification.DispatcherWithGlobalAndProjectSubscribers.Email", "true", null, userId2);

    // Nobody is subscribed
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", singletonList("NotSexyDispatcher")))
      .isFalse();

    // Global subscribers
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", singletonList("DispatcherWithGlobalSubscribers")))
      .isTrue();

    // Project subscribers
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", singletonList("DispatcherWithProjectSubscribers")))
      .isTrue();
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_B", singletonList("DispatcherWithProjectSubscribers")))
      .isFalse();

    // Global + Project subscribers
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", singletonList("DispatcherWithGlobalAndProjectSubscribers")))
      .isTrue();
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_B", singletonList("DispatcherWithGlobalAndProjectSubscribers")))
      .isTrue();
  }

  @Test
  public void selectGlobalProperties() {
    // global
    long id1 = insertProperty("global.one", "one", null, null);
    long id2 = insertProperty("global.two", "two", null, null);

    List<PropertyDto> properties = underTest.selectGlobalProperties();
    assertThat(properties.size())
      .isEqualTo(2);

    assertThatDto(findByKey(properties, "global.one"))
      .hasKey("global.one")
      .hasNoUserId()
      .hasNoResourceId()
      .hasValue("one");

    assertThatDto(findByKey(properties, "global.two"))
      .hasKey("global.two")
      .hasNoResourceId()
      .hasNoUserId()
      .hasValue("two");
  }

  @Test
  @UseDataProvider("allValuesForSelect")
  public void selectGlobalProperties_supports_all_values(String dbValue, String expected) {
    insertProperty("global.one", dbValue, null, null);

    List<PropertyDto> dtos = underTest.selectGlobalProperties();
    assertThat(dtos)
      .hasSize(1);
    assertThatDto(dtos.iterator().next())
      .hasKey("global.one")
      .hasNoResourceId()
      .hasNoUserId()
      .hasValue(expected);
  }

  @Test
  public void selectGlobalProperty() {
    // global
    insertProperty("global.one", "one", null, null);
    insertProperty("global.two", "two", null, null);
    // project
    insertProperty("project.one", "one", 10L, null);
    // user
    insertProperty("user.one", "one", null, 100);

    assertThatDto(underTest.selectGlobalProperty("global.one"))
      .hasNoResourceId()
      .hasNoUserId()
      .hasValue("one");

    assertThat(underTest.selectGlobalProperty("project.one")).isNull();
    assertThat(underTest.selectGlobalProperty("user.one")).isNull();
    assertThat(underTest.selectGlobalProperty("unexisting")).isNull();
  }

  @Test
  @UseDataProvider("allValuesForSelect")
  public void selectGlobalProperty_supports_all_values(String dbValue, String expected) {
    insertProperty("global.one", dbValue, null, null);

    assertThatDto(underTest.selectGlobalProperty("global.one"))
      .hasNoResourceId()
      .hasNoUserId()
      .hasValue(expected);
  }

  @Test
  public void selectProjectProperties() {
    ComponentDto projectDto = insertPrivateProject("A");
    long projectId = projectDto.getId();
    // global
    insertProperty("global.one", "one", null, null);
    insertProperty("global.two", "two", null, null);
    // project
    insertProperty("project.one", "Pone", projectId, null);
    insertProperty("project.two", "Ptwo", projectId, null);

    List<PropertyDto> dtos = underTest.selectProjectProperties(projectDto.getDbKey());
    assertThat(dtos)
      .hasSize(2);
    assertThatDto(findByKey(dtos, "project.one"))
      .hasKey("project.one")
      .hasResourceId(projectId)
      .hasValue("Pone");
    assertThatDto(findByKey(dtos, "project.two"))
      .hasKey("project.two")
      .hasResourceId(projectId)
      .hasValue("Ptwo");
  }

  @Test
  @UseDataProvider("allValuesForSelect")
  public void selectProjectProperties_supports_all_values(String dbValue, String expected) {
    ComponentDto projectDto = insertPrivateProject("A");
    insertProperty("project.one", dbValue, projectDto.getId(), null);

    List<PropertyDto> dtos = underTest.selectProjectProperties(projectDto.getDbKey());
    assertThat(dtos).hasSize(1);
    assertThatDto(dtos.iterator().next())
      .hasKey("project.one")
      .hasResourceId(projectDto.getId())
      .hasValue(expected);
  }

  @DataProvider
  public static Object[][] allValuesForSelect() {
    return new Object[][] {
      {null, ""},
      {"", ""},
      {"some value", "some value"},
      {VALUE_SIZE_4000, VALUE_SIZE_4000},
      {VALUE_SIZE_4001, VALUE_SIZE_4001}
    };
  }

  @Test
  public void selectProjectProperty() {
    insertProperty("project.one", "one", 10L, null);

    PropertyDto property = underTest.selectProjectProperty(10L, "project.one");

    assertThatDto(property)
      .hasKey("project.one")
      .hasResourceId(10L)
      .hasNoUserId()
      .hasValue("one");
  }

  @Test
  public void select_by_query() {
    // global
    insertProperty("global.one", "one", null, null);
    insertProperty("global.two", "two", null, null);
    // struts
    insertProperty("struts.one", "one", 10L, null);
    // commons
    insertProperty("commonslang.one", "one", 11L, null);
    // user
    insertProperty("user.one", "one", null, 100);
    insertProperty("user.two", "two", 10L, 100);
    // other
    insertProperty("other.one", "one", 12L, null);

    List<PropertyDto> results = underTest.selectByQuery(PropertyQuery.builder().setKey("user.two").setComponentId(10L).setUserId(100).build(), db.getSession());
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getValue()).isEqualTo("two");

    results = underTest.selectByQuery(PropertyQuery.builder().setKey("user.one").setUserId(100).build(), db.getSession());
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getValue()).isEqualTo("one");
  }

  @Test
  public void select_global_properties_by_keys() {
    insertPrivateProject("A");
    int userId = db.users().insertUser(u -> u.setLogin("B")).getId();

    String key = "key";
    String anotherKey = "anotherKey";
    insertProperty(key, "value", null, null);
    insertProperty(key, "value", 10L, null);
    insertProperty(key, "value", null, userId);
    insertProperty(anotherKey, "value", null, null);

    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet(key)))
      .extracting("key")
      .containsOnly(key);
    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet(key, anotherKey)))
      .extracting("key")
      .containsOnly(key, anotherKey);
    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet(key, anotherKey, "unknown")))
      .extracting("key")
      .containsOnly(key, anotherKey);
    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet("unknown")))
      .isEmpty();
  }

  @Test
  public void select_component_properties_by_ids() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();

    String key = "key";
    String anotherKey = "anotherKey";
    insertProperties(
      newGlobalPropertyDto().setKey(key),
      newComponentPropertyDto(project).setKey(key),
      newComponentPropertyDto(project2).setKey(key),
      newComponentPropertyDto(project2).setKey(anotherKey),
      newUserPropertyDto(user).setKey(key));

    assertThat(underTest.selectPropertiesByComponentIds(session, newHashSet(project.getId())))
      .extracting("key", "resourceId").containsOnly(tuple(key, project.getId()));
    assertThat(underTest.selectPropertiesByComponentIds(session, newHashSet(project.getId(), project2.getId())))
      .extracting("key", "resourceId").containsOnly(
        tuple(key, project.getId()),
        tuple(key, project2.getId()),
        tuple(anotherKey, project2.getId()));

    assertThat(underTest.selectPropertiesByComponentIds(session, newHashSet(123456789L))).isEmpty();
  }

  @Test
  public void select_properties_by_keys_and_component_ids() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();

    String key = "key";
    String anotherKey = "anotherKey";
    insertProperties(
      newGlobalPropertyDto().setKey(key),
      newComponentPropertyDto(project).setKey(key),
      newComponentPropertyDto(project2).setKey(key),
      newComponentPropertyDto(project2).setKey(anotherKey),
      newUserPropertyDto(user).setKey(key));

    assertThat(underTest.selectPropertiesByKeysAndComponentIds(session, newHashSet(key), newHashSet(project.getId())))
      .extracting("key", "resourceId").containsOnly(tuple(key, project.getId()));
    assertThat(underTest.selectPropertiesByKeysAndComponentIds(session, newHashSet(key), newHashSet(project.getId(), project2.getId())))
      .extracting("key", "resourceId").containsOnly(
        tuple(key, project.getId()),
        tuple(key, project2.getId()));
    assertThat(underTest.selectPropertiesByKeysAndComponentIds(session, newHashSet(key, anotherKey), newHashSet(project.getId(), project2.getId())))
      .extracting("key", "resourceId").containsOnly(
        tuple(key, project.getId()),
        tuple(key, project2.getId()),
        tuple(anotherKey, project2.getId()));

    assertThat(underTest.selectPropertiesByKeysAndComponentIds(session, newHashSet("unknown"), newHashSet(project.getId()))).isEmpty();
    assertThat(underTest.selectPropertiesByKeysAndComponentIds(session, newHashSet("key"), newHashSet(123456789L))).isEmpty();
    assertThat(underTest.selectPropertiesByKeysAndComponentIds(session, newHashSet("unknown"), newHashSet(123456789L))).isEmpty();
  }

  @Test
  public void select_by_key_and_matching_value() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    db.properties().insertProperties(
      newComponentPropertyDto("key", "value", project1),
      newComponentPropertyDto("key", "value", project2),
      newGlobalPropertyDto("key", "value"),
      newComponentPropertyDto("another key", "value", project1));

    assertThat(underTest.selectByKeyAndMatchingValue(db.getSession(), "key", "value"))
      .extracting(PropertyDto::getValue, PropertyDto::getResourceId)
      .containsExactlyInAnyOrder(
        tuple("value", project1.getId()),
        tuple("value", project2.getId()),
        tuple("value", null));
  }

  @Test
  public void selectByKeyAndUserIdAndComponentQualifier() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto file1 = db.components().insertComponent(ComponentTesting.newFileDto(project1));
    ComponentDto project2 = db.components().insertPrivateProject();
    db.properties().insertProperties(
      newPropertyDto("key", "1", project1, user1),
      newPropertyDto("key", "2", project2, user1),
      newPropertyDto("key", "3", file1, user1),
      newPropertyDto("another key", "4", project1, user1),
      newPropertyDto("key", "5", project1, user2),
      newGlobalPropertyDto("key", "global"));

    assertThat(underTest.selectByKeyAndUserIdAndComponentQualifier(db.getSession(), "key", user1.getId(), "TRK"))
      .extracting(PropertyDto::getValue).containsExactlyInAnyOrder("1", "2");
    assertThat(underTest.selectByKeyAndUserIdAndComponentQualifier(db.getSession(), "key", user1.getId(), "FIL"))
      .extracting(PropertyDto::getValue).containsExactlyInAnyOrder("3");
    assertThat(underTest.selectByKeyAndUserIdAndComponentQualifier(db.getSession(), "key", user2.getId(), "FIL")).isEmpty();
  }

  @Test
  public void saveProperty_inserts_global_properties_when_they_do_not_exist_in_db() {
    when(system2.now()).thenReturn(DATE_1, DATE_2, DATE_3, DATE_4, DATE_5);

    underTest.saveProperty(new PropertyDto().setKey("global.null").setValue(null));
    underTest.saveProperty(new PropertyDto().setKey("global.empty").setValue(""));
    underTest.saveProperty(new PropertyDto().setKey("global.text").setValue("some text"));
    underTest.saveProperty(new PropertyDto().setKey("global.4000").setValue(VALUE_SIZE_4000));
    underTest.saveProperty(new PropertyDto().setKey("global.clob").setValue(VALUE_SIZE_4001));

    assertThatPropertiesRow("global.null")
      .hasNoResourceId()
      .hasNoUserId()
      .isEmpty()
      .hasCreatedAt(DATE_1);
    assertThatPropertiesRow("global.empty")
      .hasNoResourceId()
      .hasNoUserId()
      .isEmpty()
      .hasCreatedAt(DATE_2);
    assertThatPropertiesRow("global.text")
      .hasNoResourceId()
      .hasNoUserId()
      .hasTextValue("some text")
      .hasCreatedAt(DATE_3);
    assertThatPropertiesRow("global.4000")
      .hasNoResourceId()
      .hasNoUserId()
      .hasTextValue(VALUE_SIZE_4000)
      .hasCreatedAt(DATE_4);
    assertThatPropertiesRow("global.clob")
      .hasNoResourceId()
      .hasNoUserId()
      .hasClobValue(VALUE_SIZE_4001)
      .hasCreatedAt(DATE_5);
  }

  @Test
  public void saveProperty_inserts_component_properties_when_they_do_not_exist_in_db() {
    when(system2.now()).thenReturn(DATE_1, DATE_2, DATE_3, DATE_4, DATE_5);

    long resourceId = 12;
    underTest.saveProperty(new PropertyDto().setKey("component.null").setResourceId(resourceId).setValue(null));
    underTest.saveProperty(new PropertyDto().setKey("component.empty").setResourceId(resourceId).setValue(""));
    underTest.saveProperty(new PropertyDto().setKey("component.text").setResourceId(resourceId).setValue("some text"));
    underTest.saveProperty(new PropertyDto().setKey("component.4000").setResourceId(resourceId).setValue(VALUE_SIZE_4000));
    underTest.saveProperty(new PropertyDto().setKey("component.clob").setResourceId(resourceId).setValue(VALUE_SIZE_4001));

    assertThatPropertiesRow("component.null")
      .hasResourceId(resourceId)
      .hasNoUserId()
      .isEmpty()
      .hasCreatedAt(DATE_1);
    assertThatPropertiesRow("component.empty")
      .hasResourceId(resourceId)
      .hasNoUserId()
      .isEmpty()
      .hasCreatedAt(DATE_2);
    assertThatPropertiesRow("component.text")
      .hasResourceId(resourceId)
      .hasNoUserId()
      .hasTextValue("some text")
      .hasCreatedAt(DATE_3);
    assertThatPropertiesRow("component.4000")
      .hasResourceId(resourceId)
      .hasNoUserId()
      .hasTextValue(VALUE_SIZE_4000)
      .hasCreatedAt(DATE_4);
    assertThatPropertiesRow("component.clob")
      .hasResourceId(resourceId)
      .hasNoUserId()
      .hasClobValue(VALUE_SIZE_4001)
      .hasCreatedAt(DATE_5);
  }

  @Test
  public void saveProperty_inserts_user_properties_when_they_do_not_exist_in_db() {
    when(system2.now()).thenReturn(DATE_1, DATE_2, DATE_3, DATE_4, DATE_5);

    int userId = 100;
    underTest.saveProperty(new PropertyDto().setKey("user.null").setUserId(userId).setValue(null));
    underTest.saveProperty(new PropertyDto().setKey("user.empty").setUserId(userId).setValue(""));
    underTest.saveProperty(new PropertyDto().setKey("user.text").setUserId(userId).setValue("some text"));
    underTest.saveProperty(new PropertyDto().setKey("user.4000").setUserId(userId).setValue(VALUE_SIZE_4000));
    underTest.saveProperty(new PropertyDto().setKey("user.clob").setUserId(userId).setValue(VALUE_SIZE_4001));

    assertThatPropertiesRow("user.null")
      .hasNoResourceId()
      .hasUserId(userId)
      .isEmpty()
      .hasCreatedAt(DATE_1);
    assertThatPropertiesRow("user.empty")
      .hasNoResourceId()
      .hasUserId(userId)
      .isEmpty()
      .hasCreatedAt(DATE_2);
    assertThatPropertiesRow("user.text")
      .hasNoResourceId()
      .hasUserId(userId)
      .hasTextValue("some text")
      .hasCreatedAt(DATE_3);
    assertThatPropertiesRow("user.4000")
      .hasNoResourceId()
      .hasUserId(userId)
      .hasTextValue(VALUE_SIZE_4000)
      .hasCreatedAt(DATE_4);
    assertThatPropertiesRow("user.clob")
      .hasNoResourceId()
      .hasUserId(userId)
      .hasClobValue(VALUE_SIZE_4001)
      .hasCreatedAt(DATE_5);
  }

  @Test
  @UseDataProvider("valueUpdatesDataProvider")
  public void saveProperty_deletes_then_inserts_global_properties_when_they_exist_in_db(@Nullable String oldValue, @Nullable String newValue) throws SQLException {
    long id = insertProperty("global", oldValue, null, null, DATE_1);
    when(system2.now()).thenReturn(DATE_4);

    underTest.saveProperty(new PropertyDto().setKey("global").setValue(newValue));

    assertThatPropertiesRow(id)
      .doesNotExist();

    PropertiesRowAssert propertiesRowAssert = assertThatPropertiesRow("global")
      .hasNoResourceId()
      .hasNoUserId()
      .hasCreatedAt(DATE_4);
    if (newValue == null || newValue.isEmpty()) {
      propertiesRowAssert.isEmpty();
    } else if (newValue.length() > 4000) {
      propertiesRowAssert.hasClobValue(newValue);
    } else {
      propertiesRowAssert.hasTextValue(newValue);
    }
  }

  @Test
  @UseDataProvider("valueUpdatesDataProvider")
  public void saveProperty_deletes_then_inserts_component_properties_when_they_exist_in_db(@Nullable String oldValue, @Nullable String newValue) throws SQLException {
    long resourceId = 999L;
    long id = insertProperty("global", oldValue, resourceId, null, DATE_1);
    when(system2.now()).thenReturn(DATE_4);

    underTest.saveProperty(new PropertyDto().setKey("global").setResourceId(resourceId).setValue(newValue));

    assertThatPropertiesRow(id)
      .doesNotExist();

    PropertiesRowAssert propertiesRowAssert = assertThatPropertiesRow("global")
      .hasResourceId(resourceId)
      .hasNoUserId()
      .hasCreatedAt(DATE_4);
    if (newValue == null || newValue.isEmpty()) {
      propertiesRowAssert.isEmpty();
    } else if (newValue.length() > 4000) {
      propertiesRowAssert.hasClobValue(newValue);
    } else {
      propertiesRowAssert.hasTextValue(newValue);
    }
  }

  @Test
  @UseDataProvider("valueUpdatesDataProvider")
  public void saveProperty_deletes_then_inserts_user_properties_when_they_exist_in_db(@Nullable String oldValue, @Nullable String newValue) throws SQLException {
    int userId = 90;
    long id = insertProperty("global", oldValue, null, userId, DATE_1);
    when(system2.now()).thenReturn(DATE_4);

    underTest.saveProperty(new PropertyDto().setKey("global").setUserId(userId).setValue(newValue));

    assertThatPropertiesRow(id)
      .doesNotExist();

    PropertiesRowAssert propertiesRowAssert = assertThatPropertiesRow("global")
      .hasNoResourceId()
      .hasUserId(userId)
      .hasCreatedAt(DATE_4);
    if (newValue == null || newValue.isEmpty()) {
      propertiesRowAssert.isEmpty();
    } else if (newValue.length() > 4000) {
      propertiesRowAssert.hasClobValue(newValue);
    } else {
      propertiesRowAssert.hasTextValue(newValue);
    }
  }

  @DataProvider
  public static Object[][] valueUpdatesDataProvider() {
    return new Object[][] {
      {null, null},
      {null, ""},
      {null, "some value"},
      {null, VALUE_SIZE_4000},
      {null, VALUE_SIZE_4001},
      {"", null},
      {"", ""},
      {"", "some value"},
      {"", VALUE_SIZE_4000},
      {"", VALUE_SIZE_4001},
      {"a value", null},
      {"a value", ""},
      {"a value", "a value"},
      {"a value", "some value"},
      {"a value", VALUE_SIZE_4000},
      {"a value", VALUE_SIZE_4001},
      {VALUE_SIZE_4000, null},
      {VALUE_SIZE_4000, ""},
      {VALUE_SIZE_4000, "a value"},
      {VALUE_SIZE_4000, VALUE_SIZE_4000},
      {VALUE_SIZE_4000, VALUE_SIZE_4000.substring(1) + "a"},
      {VALUE_SIZE_4000, VALUE_SIZE_4001},
      {VALUE_SIZE_4001, null},
      {VALUE_SIZE_4001, ""},
      {VALUE_SIZE_4001, "a value"},
      {VALUE_SIZE_4001, VALUE_SIZE_4000},
      {VALUE_SIZE_4001, VALUE_SIZE_4001},
      {VALUE_SIZE_4001, VALUE_SIZE_4001 + "dfsdfs"},
    };
  }

  @Test
  public void delete_project_property() {
    long projectId1 = insertPrivateProject("A").getId();
    long projectId2 = insertPrivateProject("B").getId();
    long projectId3 = insertPrivateProject("C").getId();
    long id1 = insertProperty("global.one", "one", null, null);
    long id2 = insertProperty("global.two", "two", null, null);
    long id3 = insertProperty("struts.one", "one", projectId1, null);
    long id4 = insertProperty("commonslang.one", "one", projectId2, null);
    long id5 = insertProperty("user.one", "one", null, 100);
    long id6 = insertProperty("user.two", "two", null, 100);
    long id7 = insertProperty("other.one", "one", projectId3, null);

    underTest.deleteProjectProperty("struts.one", projectId1);

    assertThatPropertiesRow(id1)
      .hasKey("global.one")
      .hasNoResourceId()
      .hasNoUserId()
      .hasTextValue("one");
    assertThatPropertiesRow(id2)
      .hasKey("global.two")
      .hasNoResourceId()
      .hasNoUserId()
      .hasTextValue("two");
    assertThatPropertiesRow(id3)
      .doesNotExist();
    assertThatPropertiesRow(id4)
      .hasKey("commonslang.one")
      .hasResourceId(projectId2)
      .hasNoUserId()
      .hasTextValue("one");
    assertThatPropertiesRow(id5)
      .hasKey("user.one")
      .hasNoResourceId()
      .hasUserId(100)
      .hasTextValue("one");
    assertThatPropertiesRow(id6)
      .hasKey("user.two")
      .hasNoResourceId()
      .hasUserId(100)
      .hasTextValue("two");
    assertThatPropertiesRow(id7)
      .hasKey("other.one")
      .hasResourceId(projectId3)
      .hasNoUserId()
      .hasTextValue("one");
  }

  @Test
  public void delete_project_properties() {
    long id1 = insertProperty("sonar.profile.java", "Sonar Way", 1L, null);
    long id2 = insertProperty("sonar.profile.java", "Sonar Way", 2L, null);

    long id3 = insertProperty("sonar.profile.java", "Sonar Way", null, null);

    long id4 = insertProperty("sonar.profile.js", "Sonar Way", 1L, null);
    long id5 = insertProperty("sonar.profile.js", "Sonar Way", 2L, null);
    long id6 = insertProperty("sonar.profile.js", "Sonar Way", null, null);

    underTest.deleteProjectProperties("sonar.profile.java", "Sonar Way");

    assertThatPropertiesRow(id1)
      .doesNotExist();
    assertThatPropertiesRow(id2)
      .doesNotExist();
    assertThatPropertiesRow(id3)
      .hasKey("sonar.profile.java")
      .hasNoResourceId()
      .hasNoUserId()
      .hasTextValue("Sonar Way");
    assertThatPropertiesRow(id4)
      .hasKey("sonar.profile.js")
      .hasResourceId(1)
      .hasNoUserId()
      .hasTextValue("Sonar Way");
    assertThatPropertiesRow(id5)
      .hasKey("sonar.profile.js")
      .hasResourceId(2)
      .hasNoUserId()
      .hasTextValue("Sonar Way");
    assertThatPropertiesRow(id6)
      .hasKey("sonar.profile.js")
      .hasNoResourceId()
      .hasNoUserId()
      .hasTextValue("Sonar Way");
  }

  @Test
  public void deleteGlobalProperty() {
    // global
    long id1 = insertProperty("global.key", "new_global", null, null);
    long id2 = insertProperty("to_be_deleted", "xxx", null, null);
    // project - do not delete this project property that has the same key
    long id3 = insertProperty("to_be_deleted", "new_project", 10L, null);
    // user
    long id4 = insertProperty("user.key", "new_user", null, 100);

    underTest.deleteGlobalProperty("to_be_deleted");

    assertThatPropertiesRow(id1)
      .hasKey("global.key")
      .hasNoUserId()
      .hasNoResourceId()
      .hasTextValue("new_global");
    assertThatPropertiesRow(id2)
      .doesNotExist();
    assertThatPropertiesRow("to_be_deleted", null, null)
      .doesNotExist();
    assertThatPropertiesRow(id3)
      .hasKey("to_be_deleted")
      .hasResourceId(10)
      .hasNoUserId()
      .hasTextValue("new_project");
    assertThatPropertiesRow(id4)
      .hasKey("user.key")
      .hasNoResourceId()
      .hasUserId(100)
      .hasTextValue("new_user");
  }

  @Test
  public void delete_by_organization_and_user() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    ComponentDto anotherProject = db.components().insertPrivateProject(anotherOrganization);
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    insertProperty("KEY_11", "VALUE", project.getId(), user.getId());
    insertProperty("KEY_12", "VALUE", project.getId(), user.getId());
    insertProperty("KEY_11", "VALUE", project.getId(), anotherUser.getId());
    insertProperty("KEY_11", "VALUE", anotherProject.getId(), user.getId());

    underTest.deleteByOrganizationAndUser(session, organization.getUuid(), user.getId());

    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(project.getId()).build(), session))
      .hasSize(1)
      .extracting(PropertyDto::getUserId).containsOnly(anotherUser.getId());
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(anotherProject.getId()).build(), session)).extracting(PropertyDto::getUserId)
      .hasSize(1).containsOnly(user.getId());
  }

  @Test
  public void delete_by_organization_and_matching_login() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    ComponentDto anotherProject = db.components().insertPrivateProject(anotherOrganization);
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    insertProperty("KEY_11", user.getLogin(), project.getId(), null);
    insertProperty("KEY_12", user.getLogin(), project.getId(), null);
    insertProperty("KEY_11", anotherUser.getLogin(), project.getId(), null);
    insertProperty("KEY_11", user.getLogin(), anotherProject.getId(), null);

    underTest.deleteByOrganizationAndMatchingLogin(session, organization.getUuid(), user.getLogin(), newArrayList("KEY_11", "KEY_12"));

    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(project.getId()).build(), session))
      .hasSize(1)
      .extracting(PropertyDto::getValue).containsOnly(anotherUser.getLogin());
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setComponentId(anotherProject.getId()).build(), session)).extracting(PropertyDto::getValue)
      .hasSize(1).containsOnly(user.getLogin());
  }

  @Test
  public void delete_by_key_and_value() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto anotherProject = db.components().insertPrivateProject();
    insertProperty("KEY", "VALUE", null, null);
    insertProperty("KEY", "VALUE", project.getId(), null);
    insertProperty("KEY", "VALUE", null, 100);
    insertProperty("KEY", "VALUE", project.getId(), 100);
    insertProperty("KEY", "VALUE", anotherProject.getId(), null);
    // Should not be removed
    insertProperty("KEY", "ANOTHER_VALUE", null, null);
    insertProperty("ANOTHER_KEY", "VALUE", project.getId(), 100);

    underTest.deleteByKeyAndValue(session, "KEY", "VALUE");
    db.commit();

    assertThat(db.select("select prop_key as \"key\", text_value as \"value\", resource_id as \"projectId\", user_id as \"userId\" from properties"))
      .extracting((row) -> row.get("key"), (row) -> row.get("value"), (row) -> row.get("projectId"), (row) -> row.get("userId"))
      .containsOnly(tuple("KEY", "ANOTHER_VALUE", null, null), tuple("ANOTHER_KEY", "VALUE", project.getId(), 100L));
  }

  @Test
  public void saveGlobalProperties_insert_property_if_does_not_exist_in_db() {
    when(system2.now()).thenReturn(DATE_1, DATE_2, DATE_3, DATE_4, DATE_5);

    underTest.saveGlobalProperties(mapOf(
      "null_value_property", null,
      "empty_value_property", "",
      "text_value_property", "dfdsfsd",
      "4000_char_value_property", VALUE_SIZE_4000,
      "clob_value_property", VALUE_SIZE_4001));

    assertThatPropertiesRow("null_value_property")
      .hasNoResourceId()
      .hasNoUserId()
      .isEmpty()
      .hasCreatedAt(DATE_1);
    assertThatPropertiesRow("empty_value_property")
      .hasNoResourceId()
      .hasNoUserId()
      .isEmpty()
      .hasCreatedAt(DATE_2);
    assertThatPropertiesRow("text_value_property")
      .hasNoResourceId()
      .hasNoUserId()
      .hasTextValue("dfdsfsd")
      .hasCreatedAt(DATE_3);
    assertThatPropertiesRow("4000_char_value_property")
      .hasNoResourceId()
      .hasNoUserId()
      .hasTextValue(VALUE_SIZE_4000)
      .hasCreatedAt(DATE_4);
    assertThatPropertiesRow("clob_value_property")
      .hasNoResourceId()
      .hasNoUserId()
      .hasClobValue(VALUE_SIZE_4001)
      .hasCreatedAt(DATE_5);
  }

  @Test
  public void saveGlobalProperties_delete_and_insert_new_value_when_property_exists_in_db() throws SQLException {
    long id = insertProperty("to_be_updated", "old_value", null, null, DATE_1);
    when(system2.now()).thenReturn(DATE_3);

    underTest.saveGlobalProperties(ImmutableMap.of("to_be_updated", "new value"));

    assertThatPropertiesRow(id)
      .doesNotExist();

    assertThatPropertiesRow("to_be_updated")
      .hasNoResourceId()
      .hasNoUserId()
      .hasTextValue("new value")
      .hasCreatedAt(DATE_3);
  }

  private static Map<String, String> mapOf(String... values) {
    // use LinkedHashMap to keep order of array
    Map<String, String> res = new LinkedHashMap<>(values.length / 2);
    Iterator<String> iterator = Arrays.asList(values).iterator();
    while (iterator.hasNext()) {
      res.put(iterator.next(), iterator.next());
    }
    return res;
  }

  @Test
  public void renamePropertyKey_updates_global_component_and_user_properties() throws SQLException {
    long id1 = insertProperty("foo", "bar", null, null, DATE_1);
    long id2 = insertProperty("old_name", "doc1", null, null, DATE_1);
    long id3 = insertProperty("old_name", "doc2", 15L, null, DATE_1);
    long id4 = insertProperty("old_name", "doc3", 16L, null, DATE_1);
    long id5 = insertProperty("old_name", "doc4", null, 100, DATE_1);
    long id6 = insertProperty("old_name", "doc5", null, 101, DATE_1);

    underTest.renamePropertyKey("old_name", "new_name");

    assertThatPropertiesRow(id1)
      .hasKey("foo")
      .hasNoUserId()
      .hasNoResourceId()
      .hasTextValue("bar")
      .hasCreatedAt(DATE_1);
    assertThatPropertiesRow(id2)
      .hasKey("new_name")
      .hasNoResourceId()
      .hasNoUserId()
      .hasTextValue("doc1")
      .hasCreatedAt(DATE_1);
    assertThatPropertiesRow(id3)
      .hasKey("new_name")
      .hasResourceId(15)
      .hasNoUserId()
      .hasTextValue("doc2")
      .hasCreatedAt(DATE_1);
    assertThatPropertiesRow(id4)
      .hasKey("new_name")
      .hasResourceId(16)
      .hasNoUserId()
      .hasTextValue("doc3")
      .hasCreatedAt(DATE_1);
    assertThatPropertiesRow(id5)
      .hasKey("new_name")
      .hasNoResourceId()
      .hasUserId(100)
      .hasTextValue("doc4")
      .hasCreatedAt(DATE_1);
    assertThatPropertiesRow(id6)
      .hasKey("new_name")
      .hasNoResourceId()
      .hasUserId(101)
      .hasTextValue("doc5")
      .hasCreatedAt(DATE_1);
  }

  @Test
  public void rename_to_same_key_has_no_effect() throws SQLException {
    long now = 1_890_999L;
    long id = insertProperty("foo", "bar", null, null, now);

    assertThatPropertiesRow(id)
      .hasCreatedAt(now);

    underTest.renamePropertyKey("foo", "foo");

    assertThatPropertiesRow(id)
      .hasKey("foo")
      .hasNoUserId()
      .hasNoResourceId()
      .hasTextValue("bar")
      .hasCreatedAt(now);
  }

  @Test
  public void should_not_rename_with_empty_key() {
    thrown.expect(IllegalArgumentException.class);
    underTest.renamePropertyKey("foo", "");
  }

  @Test
  public void should_not_rename_an_empty_key() {
    thrown.expect(IllegalArgumentException.class);
    underTest.renamePropertyKey(null, "foo");
  }

  private PropertyDto findByKey(List<PropertyDto> properties, String key) {
    for (PropertyDto property : properties) {
      if (key.equals(property.getKey())) {
        return property;
      }
    }
    return null;
  }

  private void insertProperties(PropertyDto... properties) {
    for (PropertyDto propertyDto : properties) {
      underTest.saveProperty(session, propertyDto);
    }
    session.commit();
  }

  private long insertProperty(String key, @Nullable String value, @Nullable Long resourceId, @Nullable Integer userId, long createdAt) {
    when(system2.now()).thenReturn(createdAt);
    return insertProperty(key, value, resourceId, userId);
  }

  private long insertProperty(String key, @Nullable String value, @Nullable Long resourceId, @Nullable Integer userId) {
    PropertyDto dto = new PropertyDto().setKey(key)
      .setResourceId(resourceId)
      .setUserId(userId)
      .setValue(value);
    db.properties().insertProperty(dto);

    return (long) db.selectFirst(session, "select id as \"id\" from properties" +
      " where prop_key='" + key + "'" +
      " and user_id" + (userId == null ? " is null" : "='" + userId + "'") +
      " and resource_id" + (resourceId == null ? " is null" : "='" + resourceId + "'")).get("id");
  }

  private ComponentDto insertPrivateProject(String uuid) {
    return db.components().insertPrivateProject(db.getDefaultOrganization(), uuid);
  }

  private static PropertyDtoAssert assertThatDto(@Nullable PropertyDto dto) {
    return new PropertyDtoAssert(dto);
  }

  private PropertiesRowAssert assertThatPropertiesRow(String key, @Nullable Integer userId, @Nullable Integer componentId) {
    return new PropertiesRowAssert(db, key, userId, componentId);
  }

  private PropertiesRowAssert assertThatPropertiesRow(String key) {
    return new PropertiesRowAssert(db, key);
  }

  private PropertiesRowAssert assertThatPropertiesRow(long id) {
    return new PropertiesRowAssert(db, id);
  }

}
