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
package org.sonar.db.property;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonar.db.property.PropertyTesting.newUserPropertyDto;

public class PropertiesDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession session = dbTester.getSession();

  private PropertiesDao underTest = dbTester.getDbClient().propertiesDao();

  @Test
  public void shouldFindUsersForNotification() {
    insertProject("uuid_45", 45);
    insertProject("uuid_56", 56);
    insertUser(1);
    insertUser(2);
    insertUser(3);
    insertProperty(1, "notification.NewViolations.Email", "true", 45, 2);
    insertProperty(2, "notification.NewViolations.Twitter", "true", null, 3);
    insertProperty(3, "notification.NewViolations.Twitter", "true", 56, 1);
    insertProperty(4, "notification.NewViolations.Twitter", "true", 56, 3);

    List<String> users = underTest.selectUsersForNotification("NewViolations", "Email", null);
    assertThat(users).isEmpty();

    users = underTest.selectUsersForNotification("NewViolations", "Email", "uuid_78");
    assertThat(users).isEmpty();

    users = underTest.selectUsersForNotification("NewViolations", "Email", "uuid_45");
    assertThat(users).hasSize(1);
    assertThat(users).containsOnly("user2");

    users = underTest.selectUsersForNotification("NewViolations", "Twitter", null);
    assertThat(users).hasSize(1);
    assertThat(users).containsOnly("user3");

    users = underTest.selectUsersForNotification("NewViolations", "Twitter", "uuid_78");
    assertThat(users).isEmpty();

    users = underTest.selectUsersForNotification("NewViolations", "Twitter", "uuid_56");
    assertThat(users).hasSize(2);
    assertThat(users).containsOnly("user1", "user3");
  }

  @Test
  public void findNotificationSubscribers() {
    insertUser(1);
    insertUser(2);
    insertProject("PROJECT_A", 42);
    // global subscription
    insertProperty(1, "notification.DispatcherWithGlobalSubscribers.Email", "true", null, 2);
    // project subscription
    insertProperty(2, "notification.DispatcherWithProjectSubscribers.Email", "true", 42, 1);
    insertProperty(3, "notification.DispatcherWithGlobalAndProjectSubscribers.Email", "true", 56, 1);
    insertProperty(4, "notification.DispatcherWithGlobalAndProjectSubscribers.Email", "true", 42, 1);
    // global subscription
    insertProperty(5, "notification.DispatcherWithGlobalAndProjectSubscribers.Email", "true", null, 2);

    // Nobody is subscribed
    List<String> users = underTest.selectNotificationSubscribers("NotSexyDispatcher", "Email", "project42");
    assertThat(users).isEmpty();

    // Global subscribers
    users = underTest.selectNotificationSubscribers("DispatcherWithGlobalSubscribers", "Email", "project42");
    assertThat(users).containsOnly("user2");

    users = underTest.selectNotificationSubscribers("DispatcherWithGlobalSubscribers", "Email", null);
    assertThat(users).containsOnly("user2");

    // Project subscribers
    users = underTest.selectNotificationSubscribers("DispatcherWithProjectSubscribers", "Email", "project42");
    assertThat(users).containsOnly("user1");

    // Global + Project subscribers
    users = underTest.selectNotificationSubscribers("DispatcherWithGlobalAndProjectSubscribers", "Email", "project42");
    assertThat(users).containsOnly("user1", "user2");
  }

  @Test
  public void hasNotificationSubscribers() {
    insertUser(1);
    insertUser(2);
    insertProject("PROJECT_A", 42);
    // global subscription
    insertProperty(1, "notification.DispatcherWithGlobalSubscribers.Email", "true", null, 2);
    // project subscription
    insertProperty(2, "notification.DispatcherWithProjectSubscribers.Email", "true", 42, 1);
    insertProperty(3, "notification.DispatcherWithGlobalAndProjectSubscribers.Email", "true", 56, 1);
    insertProperty(4, "notification.DispatcherWithGlobalAndProjectSubscribers.Email", "true", 42, 1);
    // global subscription
    insertProperty(5, "notification.DispatcherWithGlobalAndProjectSubscribers.Email", "true", null, 2);

    // Nobody is subscribed
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", singletonList("NotSexyDispatcher"))).isFalse();

    // Global subscribers
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", singletonList("DispatcherWithGlobalSubscribers"))).isTrue();

    // Project subscribers
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", singletonList("DispatcherWithProjectSubscribers"))).isTrue();
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_B", singletonList("DispatcherWithProjectSubscribers"))).isFalse();

    // Global + Project subscribers
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", singletonList("DispatcherWithGlobalAndProjectSubscribers"))).isTrue();
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_B", singletonList("DispatcherWithGlobalAndProjectSubscribers"))).isTrue();
  }

  @Test
  public void selectGlobalProperties() {
    // global
    insertProperty(1, "global.one", "one", null, null);
    insertProperty(2, "global.two", "two", null, null);

    List<PropertyDto> properties = underTest.selectGlobalProperties();
    assertThat(properties.size()).isEqualTo(2);

    PropertyDto first = findById(properties, 1);
    assertThat(first.getKey()).isEqualTo("global.one");
    assertThat(first.getValue()).isEqualTo("one");

    PropertyDto second = findById(properties, 2);
    assertThat(second.getKey()).isEqualTo("global.two");
    assertThat(second.getValue()).isEqualTo("two");
  }

  @Test
  public void selectGlobalProperty() {
    // global
    insertProperty(1, "global.one", "one", null, null);
    insertProperty(2, "global.two", "two", null, null);
    // project
    insertProperty(3, "project.one", "one", 10, null);
    // user
    insertProperty(4, "user.one", "one", null, 100);

    PropertyDto prop = underTest.selectGlobalProperty("global.one");
    assertThat(prop).isNotNull();
    assertThat(prop.getValue()).isEqualTo("one");

    assertThat(underTest.selectGlobalProperty("project.one")).isNull();
    assertThat(underTest.selectGlobalProperty("user.one")).isNull();
    assertThat(underTest.selectGlobalProperty("unexisting")).isNull();
  }

  @Test
  public void selectProjectProperties() {
    insertProject("A", 10);
    // global
    insertProperty(1, "global.one", "one", null, null);
    insertProperty(2, "global.two", "two", null, null);
    // project
    insertProperty(3, "project.one", "one", 10, null);
    insertProperty(4, "project.two", "two", 10, null);

    List<PropertyDto> properties = underTest.selectProjectProperties("project10");
    assertThat(properties)
      .hasSize(2)
      .extracting("key", "value")
      .containsOnly(tuple("project.one", "one"), tuple("project.two", "two"));
  }

  @Test
  public void selectProjectProperty() {
    insertProject("A", 10);
    // global
    insertProperty(1, "global.one", "one", null, null);
    insertProperty(2, "global.two", "two", null, null);
    // project
    insertProperty(3, "project.one", "one", 10, null);

    PropertyDto property = underTest.selectProjectProperty(10L, "project.one");

    assertThat(property.getKey()).isEqualTo("project.one");
    assertThat(property.getValue()).isEqualTo("one");
  }

  @Test
  public void select_module_properties_tree() {
    dbTester.prepareDbUnit(getClass(), "select_module_properties_tree.xml");

    List<PropertyDto> properties = underTest.selectEnabledDescendantModuleProperties("ABCD", dbTester.getSession());
    assertThat(properties.size()).isEqualTo(4);
    assertThat(properties).extracting("key").containsOnly("struts.one", "core.one", "core.two", "data.one");
    assertThat(properties).extracting("value").containsOnly("one", "two");

    properties = underTest.selectEnabledDescendantModuleProperties("EFGH", dbTester.getSession());
    assertThat(properties.size()).isEqualTo(3);
    assertThat(properties).extracting("key").containsOnly("core.one", "core.two", "data.one");

    properties = underTest.selectEnabledDescendantModuleProperties("FGHI", dbTester.getSession());
    assertThat(properties.size()).isEqualTo(1);
    assertThat(properties).extracting("key").containsOnly("data.one");

    assertThat(underTest.selectEnabledDescendantModuleProperties("unknown-result.xml", dbTester.getSession()).size()).isEqualTo(0);
  }

  @Test
  public void select_by_query() {
    dbTester.prepareDbUnit(getClass(), "select_by_query.xml");

    List<PropertyDto> results = underTest.selectByQuery(PropertyQuery.builder().setKey("user.two").setComponentId(10L).setUserId(100).build(), dbTester.getSession());
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getValue()).isEqualTo("two");

    results = underTest.selectByQuery(PropertyQuery.builder().setKey("user.one").setUserId(100).build(), dbTester.getSession());
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getValue()).isEqualTo("one");
  }

  @Test
  public void select_global_properties_by_keys() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    dbClient.componentDao().insert(session, project);
    UserDto user = UserTesting.newUserDto();
    dbClient.userDao().insert(session, user);

    String key = "key";
    String anotherKey = "anotherKey";
    insertProperties(
      newGlobalPropertyDto().setKey(key),
      newComponentPropertyDto(project).setKey(key),
      newUserPropertyDto(user).setKey(key),
      newGlobalPropertyDto().setKey(anotherKey));

    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet(key))).extracting("key").containsOnly(key);
    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet(key, anotherKey))).extracting("key").containsOnly(key, anotherKey);
    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet(key, anotherKey, "unknown"))).extracting("key").containsOnly(key, anotherKey);

    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet("unknown"))).isEmpty();
  }

  @Test
  public void select_component_properties_by_keys() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    dbClient.componentDao().insert(session, project);
    UserDto user = UserTesting.newUserDto();
    dbClient.userDao().insert(session, user);

    String key = "key";
    String anotherKey = "anotherKey";
    insertProperties(
      newGlobalPropertyDto().setKey(key),
      newComponentPropertyDto(project).setKey(key),
      newUserPropertyDto(user).setKey(key),
      newComponentPropertyDto(project).setKey(anotherKey));

    assertThat(underTest.selectPropertiesByKeysAndComponentId(session, newHashSet(key), project.getId())).extracting("key").containsOnly(key);
    assertThat(underTest.selectPropertiesByKeysAndComponentId(session, newHashSet(key, anotherKey), project.getId())).extracting("key").containsOnly(key, anotherKey);
    assertThat(underTest.selectPropertiesByKeysAndComponentId(session, newHashSet(key, anotherKey, "unknown"), project.getId())).extracting("key").containsOnly(key, anotherKey);

    assertThat(underTest.selectPropertiesByKeysAndComponentId(session, newHashSet("unknown"), project.getId())).isEmpty();
    assertThat(underTest.selectPropertiesByKeysAndComponentId(session, newHashSet(key), 123456789L)).isEmpty();
  }

  @Test
  public void select_properties_by_keys_and_component_ids() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    dbClient.componentDao().insert(session, project);
    ComponentDto project2 = ComponentTesting.newProjectDto();
    dbClient.componentDao().insert(session, project2);

    UserDto user = UserTesting.newUserDto();
    dbClient.userDao().insert(session, user);

    String key = "key";
    String anotherKey = "anotherKey";
    insertProperties(
      newGlobalPropertyDto().setKey(key),
      newComponentPropertyDto(project).setKey(key),
      newComponentPropertyDto(project2).setKey(key),
      newComponentPropertyDto(project2).setKey(anotherKey),
      newUserPropertyDto(user).setKey(key));

    assertThat(underTest.selectPropertiesByKeysAndComponentIds(session, newHashSet(key), newHashSet(project.getId())))
      .extracting("key", "resourceId").containsOnly(Tuple.tuple(key, project.getId()));
    assertThat(underTest.selectPropertiesByKeysAndComponentIds(session, newHashSet(key), newHashSet(project.getId(), project2.getId())))
      .extracting("key", "resourceId").containsOnly(
        Tuple.tuple(key, project.getId()),
        Tuple.tuple(key, project2.getId()));
    assertThat(underTest.selectPropertiesByKeysAndComponentIds(session, newHashSet(key, anotherKey), newHashSet(project.getId(), project2.getId())))
      .extracting("key", "resourceId").containsOnly(
        Tuple.tuple(key, project.getId()),
        Tuple.tuple(key, project2.getId()),
        Tuple.tuple(anotherKey, project2.getId()));

    assertThat(underTest.selectPropertiesByKeysAndComponentIds(session, newHashSet("unknown"), newHashSet(project.getId()))).isEmpty();
    assertThat(underTest.selectPropertiesByKeysAndComponentIds(session, newHashSet("key"), newHashSet(123456789L))).isEmpty();
    assertThat(underTest.selectPropertiesByKeysAndComponentIds(session, newHashSet("unknown"), newHashSet(123456789L))).isEmpty();
  }

  @Test
  public void setProperty_update() {
    dbTester.prepareDbUnit(getClass(), "update.xml");

    underTest.insertProperty(new PropertyDto().setKey("global.key").setValue("new_global"));
    underTest.insertProperty(new PropertyDto().setKey("project.key").setResourceId(10L).setValue("new_project"));
    underTest.insertProperty(new PropertyDto().setKey("user.key").setUserId(100L).setValue("new_user"));
    underTest.insertProperty(new PropertyDto().setKey("null.value").setValue(null));

    dbTester.assertDbUnit(getClass(), "update-result.xml", "properties");
  }

  @Test
  public void setProperty_insert() {
    dbTester.prepareDbUnit(getClass(), "insert.xml");

    underTest.insertProperty(new PropertyDto().setKey("global.key").setValue("new_global"));
    underTest.insertProperty(new PropertyDto().setKey("project.key").setResourceId(10L).setValue("new_project"));
    underTest.insertProperty(new PropertyDto().setKey("user.key").setUserId(100L).setValue("new_user"));

    dbTester.assertDbUnit(getClass(), "insert-result.xml", "properties");
  }

  @Test
  public void delete_property_by_id() {
    dbTester.prepareDbUnit(getClass(), "delete.xml");

    underTest.deleteById(dbTester.getSession(), 1L);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "delete-result.xml", "properties");
  }

  @Test
  public void delete_project_property() {
    dbTester.prepareDbUnit(getClass(), "delete_project_property.xml");

    underTest.deleteProjectProperty("struts.one", 10L);

    dbTester.assertDbUnit(getClass(), "delete_project_property-result.xml", "properties");
  }

  @Test
  public void delete_project_properties() {
    dbTester.prepareDbUnit(getClass(), "delete_project_properties.xml");

    underTest.deleteProjectProperties("sonar.profile.java", "Sonar Way");

    dbTester.assertDbUnit(getClass(), "delete_project_properties-result.xml", "properties");
  }

  @Test
  public void deleteGlobalProperties() {
    dbTester.prepareDbUnit(getClass(), "deleteGlobalProperties.xml");

    underTest.deleteGlobalProperties();

    dbTester.assertDbUnit(getClass(), "deleteGlobalProperties-result.xml", "properties");
  }

  @Test
  public void deleteGlobalProperty() {
    dbTester.prepareDbUnit(getClass(), "deleteGlobalProperty.xml");

    underTest.deleteGlobalProperty("to_be_deleted");

    dbTester.assertDbUnit(getClass(), "deleteGlobalProperty-result.xml", "properties");
  }

  @Test
  public void deleteAllProperties() {
    dbTester.prepareDbUnit(getClass(), "deleteAllProperties.xml");

    underTest.deleteAllProperties("to_be_deleted");

    dbTester.assertDbUnit(getClass(), "deleteAllProperties-result.xml", "properties");
  }

  @Test
  public void insertGlobalProperties() {
    dbTester.prepareDbUnit(getClass(), "insertGlobalProperties.xml");

    underTest.insertGlobalProperties(ImmutableMap.of("to_be_inserted", "inserted"));

    dbTester.assertDbUnitTable(getClass(), "insertGlobalProperties-result.xml", "properties", "prop_key", "text_value", "resource_id", "user_id");
  }

  @Test
  public void updateGlobalProperties() {
    dbTester.prepareDbUnit(getClass(), "updateGlobalProperties.xml");

    underTest.insertGlobalProperties(ImmutableMap.of("to_be_updated", "updated"));

    dbTester.assertDbUnitTable(getClass(), "updateGlobalProperties-result.xml", "properties", "prop_key", "text_value", "resource_id", "user_id");
  }

  @Test
  public void renamePropertyKey() {
    dbTester.prepareDbUnit(getClass(), "renamePropertyKey.xml");

    underTest.renamePropertyKey("sonar.license.secured", "sonar.license");

    dbTester.assertDbUnitTable(getClass(), "renamePropertyKey-result.xml", "properties", "prop_key", "text_value", "resource_id", "user_id");
  }

  @Test
  public void should_not_rename_if_same_key() {
    dbTester.prepareDbUnit(getClass(), "should_not_rename_if_same_key.xml");

    underTest.renamePropertyKey("foo", "foo");

    dbTester.assertDbUnitTable(getClass(), "should_not_rename_if_same_key-result.xml", "properties", "prop_key", "text_value", "resource_id", "user_id");
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

  @Test
  public void updatePropertiesFromKeyAndValueToNewValue() {
    dbTester.prepareDbUnit(getClass(), "updatePropertiesFromKeyAndValueToNewValue.xml");

    underTest.updateProperties("sonar.profile.java", "Sonar Way", "Default");

    dbTester.assertDbUnitTable(getClass(), "updatePropertiesFromKeyAndValueToNewValue-result.xml", "properties", "prop_key", "text_value", "resource_id", "user_id");
  }

  private PropertyDto findById(List<PropertyDto> properties, int id) {
    for (PropertyDto property : properties) {
      if (property.getId() == id) {
        return property;
      }
    }
    return null;
  }

  private void insertProperties(PropertyDto... properties) {
    for (PropertyDto propertyDto : properties) {
      underTest.insertProperty(session, propertyDto);
    }
    session.commit();
  }

  private void insertProperty(int id, String key, String value, @Nullable Integer resourceId, @Nullable Integer userId) {
    dbTester.executeInsert("PROPERTIES",
      "ID", valueOf(id),
      "prop_key", key,
      "text_value", value,
      "resource_id", resourceId == null ? null : valueOf(resourceId),
      "user_id", userId == null ? null : valueOf(userId));
    dbTester.commit();
  }

  private void insertProject(String uuid, int id) {
    dbTester.executeInsert("PROJECTS",
      "uuid", uuid,
      "uuid_path", "NOT_USED",
      "root_uuid", uuid,
      "kee", "project" + id,
      "id", valueOf(id));
    dbTester.commit();
  }

  private void insertUser(int id) {
    dbTester.executeInsert("USERS",
      "id", valueOf(id),
      "login", "user" + id);
    dbTester.commit();
  }
}
