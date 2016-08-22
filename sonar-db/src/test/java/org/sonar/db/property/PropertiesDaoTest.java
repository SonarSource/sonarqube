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
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonar.db.property.PropertyTesting.newUserPropertyDto;

public class PropertiesDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();
  DbSession session = dbTester.getSession();

  PropertiesDao dao = dbTester.getDbClient().propertiesDao();

  @Test
  public void shouldFindUsersForNotification() {
    dbTester.prepareDbUnit(getClass(), "shouldFindUsersForNotification.xml");

    List<String> users = dao.selectUsersForNotification("NewViolations", "Email", null);
    assertThat(users).isEmpty();

    users = dao.selectUsersForNotification("NewViolations", "Email", "uuid_78");
    assertThat(users).isEmpty();

    users = dao.selectUsersForNotification("NewViolations", "Email", "uuid_45");
    assertThat(users).hasSize(1);
    assertThat(users).containsOnly("user2");

    users = dao.selectUsersForNotification("NewViolations", "Twitter", null);
    assertThat(users).hasSize(1);
    assertThat(users).containsOnly("user3");

    users = dao.selectUsersForNotification("NewViolations", "Twitter", "uuid_78");
    assertThat(users).isEmpty();

    users = dao.selectUsersForNotification("NewViolations", "Twitter", "uuid_56");
    assertThat(users).hasSize(2);
    assertThat(users).containsOnly("user1", "user3");
  }

  @Test
  public void findNotificationSubscribers() {
    dbTester.prepareDbUnit(getClass(), "findNotificationSubscribers.xml");

    // Nobody is subscribed
    List<String> users = dao.selectNotificationSubscribers("NotSexyDispatcher", "Email", "org.apache:struts");
    assertThat(users).isEmpty();

    // Global subscribers
    users = dao.selectNotificationSubscribers("DispatcherWithGlobalSubscribers", "Email", "org.apache:struts");
    assertThat(users).containsOnly("simon");

    users = dao.selectNotificationSubscribers("DispatcherWithGlobalSubscribers", "Email", null);
    assertThat(users).containsOnly("simon");

    // Project subscribers
    users = dao.selectNotificationSubscribers("DispatcherWithProjectSubscribers", "Email", "org.apache:struts");
    assertThat(users).containsOnly("eric");

    // Global + Project subscribers
    users = dao.selectNotificationSubscribers("DispatcherWithGlobalAndProjectSubscribers", "Email", "org.apache:struts");
    assertThat(users).containsOnly("eric", "simon");
  }

  @Test
  public void hasNotificationSubscribers() {
    dbTester.prepareDbUnit(getClass(), "findNotificationSubscribers.xml");

    // Nobody is subscribed
    assertThat(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", asList("NotSexyDispatcher"))).isFalse();

    // Global subscribers
    assertThat(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", asList("DispatcherWithGlobalSubscribers"))).isTrue();

    // Project subscribers
    assertThat(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", asList("DispatcherWithProjectSubscribers"))).isTrue();
    assertThat(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_B", asList("DispatcherWithProjectSubscribers"))).isFalse();

    // Global + Project subscribers
    assertThat(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", asList("DispatcherWithGlobalAndProjectSubscribers"))).isTrue();
    assertThat(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_B", asList("DispatcherWithGlobalAndProjectSubscribers"))).isTrue();
  }

  @Test
  public void selectGlobalProperties() {
    dbTester.prepareDbUnit(getClass(), "selectGlobalProperties.xml");
    List<PropertyDto> properties = dao.selectGlobalProperties();
    assertThat(properties.size(), is(2));

    PropertyDto first = findById(properties, 1);
    assertThat(first.getKey(), is("global.one"));
    assertThat(first.getValue(), is("one"));

    PropertyDto second = findById(properties, 2);
    assertThat(second.getKey(), is("global.two"));
    assertThat(second.getValue(), is("two"));
  }

  @Test
  public void selectGlobalProperty() {
    dbTester.prepareDbUnit(getClass(), "selectGlobalProperties.xml");

    PropertyDto prop = dao.selectGlobalProperty("global.one");
    assertThat(prop).isNotNull();
    assertThat(prop.getValue(), is("one"));

    assertThat(dao.selectGlobalProperty("project.one")).isNull();
    assertThat(dao.selectGlobalProperty("user.one")).isNull();
    assertThat(dao.selectGlobalProperty("unexisting")).isNull();
  }

  @Test
  public void selectProjectProperties() {
    dbTester.prepareDbUnit(getClass(), "selectProjectProperties.xml");
    List<PropertyDto> properties = dao.selectProjectProperties("org.struts:struts");
    assertThat(properties.size(), is(1));

    PropertyDto first = properties.get(0);
    assertThat(first.getKey(), is("struts.one"));
    assertThat(first.getValue(), is("one"));
  }

  @Test
  public void select_module_properties_tree() {
    dbTester.prepareDbUnit(getClass(), "select_module_properties_tree.xml");

    List<PropertyDto> properties = dao.selectEnabledDescendantModuleProperties("ABCD", dbTester.getSession());
    assertThat(properties.size(), is(4));
    assertThat(properties).extracting("key").containsOnly("struts.one", "core.one", "core.two", "data.one");
    assertThat(properties).extracting("value").containsOnly("one", "two");

    properties = dao.selectEnabledDescendantModuleProperties("EFGH", dbTester.getSession());
    assertThat(properties.size(), is(3));
    assertThat(properties).extracting("key").containsOnly("core.one", "core.two", "data.one");

    properties = dao.selectEnabledDescendantModuleProperties("FGHI", dbTester.getSession());
    assertThat(properties.size(), is(1));
    assertThat(properties).extracting("key").containsOnly("data.one");

    assertThat(dao.selectEnabledDescendantModuleProperties("unknown-result.xml", dbTester.getSession()).size(), is(0));
  }

  @Test
  public void selectProjectProperty() {
    dbTester.prepareDbUnit(getClass(), "selectProjectProperties.xml");
    PropertyDto property = dao.selectProjectProperty(11L, "commonslang.one");

    assertThat(property.getKey(), is("commonslang.one"));
    assertThat(property.getValue(), is("two"));
  }

  @Test
  public void select_by_query() {
    dbTester.prepareDbUnit(getClass(), "select_by_query.xml");

    List<PropertyDto> results = dao.selectByQuery(PropertyQuery.builder().setKey("user.two").setComponentId(10L).setUserId(100).build(), dbTester.getSession());
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getValue()).isEqualTo("two");

    results = dao.selectByQuery(PropertyQuery.builder().setKey("user.one").setUserId(100).build(), dbTester.getSession());
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

    assertThat(dao.selectGlobalPropertiesByKeys(session, newHashSet(key))).extracting("key").containsOnly(key);
    assertThat(dao.selectGlobalPropertiesByKeys(session, newHashSet(key, anotherKey))).extracting("key").containsOnly(key, anotherKey);
    assertThat(dao.selectGlobalPropertiesByKeys(session, newHashSet(key, anotherKey, "unknown"))).extracting("key").containsOnly(key, anotherKey);

    assertThat(dao.selectGlobalPropertiesByKeys(session, newHashSet("unknown"))).isEmpty();
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

    assertThat(dao.selectPropertiesByKeysAndComponentId(session, newHashSet(key), project.getId())).extracting("key").containsOnly(key);
    assertThat(dao.selectPropertiesByKeysAndComponentId(session, newHashSet(key, anotherKey), project.getId())).extracting("key").containsOnly(key, anotherKey);
    assertThat(dao.selectPropertiesByKeysAndComponentId(session, newHashSet(key, anotherKey, "unknown"), project.getId())).extracting("key").containsOnly(key, anotherKey);

    assertThat(dao.selectPropertiesByKeysAndComponentId(session, newHashSet("unknown"), project.getId())).isEmpty();
    assertThat(dao.selectPropertiesByKeysAndComponentId(session, newHashSet(key), 123456789L)).isEmpty();
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

    assertThat(dao.selectPropertiesByKeysAndComponentIds(session, newHashSet(key), newHashSet(project.getId())))
      .extracting("key", "resourceId").containsOnly(Tuple.tuple(key, project.getId()));
    assertThat(dao.selectPropertiesByKeysAndComponentIds(session, newHashSet(key), newHashSet(project.getId(), project2.getId())))
      .extracting("key", "resourceId").containsOnly(
        Tuple.tuple(key, project.getId()),
        Tuple.tuple(key, project2.getId()));
    assertThat(dao.selectPropertiesByKeysAndComponentIds(session, newHashSet(key, anotherKey), newHashSet(project.getId(), project2.getId())))
      .extracting("key", "resourceId").containsOnly(
        Tuple.tuple(key, project.getId()),
        Tuple.tuple(key, project2.getId()),
        Tuple.tuple(anotherKey, project2.getId()));

    assertThat(dao.selectPropertiesByKeysAndComponentIds(session, newHashSet("unknown"), newHashSet(project.getId()))).isEmpty();
    assertThat(dao.selectPropertiesByKeysAndComponentIds(session, newHashSet("key"), newHashSet(123456789L))).isEmpty();
    assertThat(dao.selectPropertiesByKeysAndComponentIds(session, newHashSet("unknown"), newHashSet(123456789L))).isEmpty();
  }

  @Test
  public void setProperty_update() {
    dbTester.prepareDbUnit(getClass(), "update.xml");

    dao.insertProperty(new PropertyDto().setKey("global.key").setValue("new_global"));
    dao.insertProperty(new PropertyDto().setKey("project.key").setResourceId(10L).setValue("new_project"));
    dao.insertProperty(new PropertyDto().setKey("user.key").setUserId(100L).setValue("new_user"));
    dao.insertProperty(new PropertyDto().setKey("null.value").setValue(null));

    dbTester.assertDbUnit(getClass(), "update-result.xml", "properties");
  }

  @Test
  public void setProperty_insert() {
    dbTester.prepareDbUnit(getClass(), "insert.xml");

    dao.insertProperty(new PropertyDto().setKey("global.key").setValue("new_global"));
    dao.insertProperty(new PropertyDto().setKey("project.key").setResourceId(10L).setValue("new_project"));
    dao.insertProperty(new PropertyDto().setKey("user.key").setUserId(100L).setValue("new_user"));

    dbTester.assertDbUnit(getClass(), "insert-result.xml", "properties");
  }

  @Test
  public void delete_property_by_id() {
    dbTester.prepareDbUnit(getClass(), "delete.xml");

    dao.deleteById(dbTester.getSession(), 1L);
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "delete-result.xml", "properties");
  }

  @Test
  public void delete_project_property() {
    dbTester.prepareDbUnit(getClass(), "delete_project_property.xml");

    dao.deleteProjectProperty("struts.one", 10L);

    dbTester.assertDbUnit(getClass(), "delete_project_property-result.xml", "properties");
  }

  @Test
  public void delete_project_properties() {
    dbTester.prepareDbUnit(getClass(), "delete_project_properties.xml");

    dao.deleteProjectProperties("sonar.profile.java", "Sonar Way");

    dbTester.assertDbUnit(getClass(), "delete_project_properties-result.xml", "properties");
  }

  @Test
  public void deleteGlobalProperties() {
    dbTester.prepareDbUnit(getClass(), "deleteGlobalProperties.xml");

    dao.deleteGlobalProperties();

    dbTester.assertDbUnit(getClass(), "deleteGlobalProperties-result.xml", "properties");
  }

  @Test
  public void deleteGlobalProperty() {
    dbTester.prepareDbUnit(getClass(), "deleteGlobalProperty.xml");

    dao.deleteGlobalProperty("to_be_deleted");

    dbTester.assertDbUnit(getClass(), "deleteGlobalProperty-result.xml", "properties");
  }

  @Test
  public void deleteAllProperties() {
    dbTester.prepareDbUnit(getClass(), "deleteAllProperties.xml");

    dao.deleteAllProperties("to_be_deleted");

    dbTester.assertDbUnit(getClass(), "deleteAllProperties-result.xml", "properties");
  }

  @Test
  public void insertGlobalProperties() {
    dbTester.prepareDbUnit(getClass(), "insertGlobalProperties.xml");

    dao.insertGlobalProperties(ImmutableMap.of("to_be_inserted", "inserted"));

    dbTester.assertDbUnitTable(getClass(), "insertGlobalProperties-result.xml", "properties", "prop_key", "text_value", "resource_id", "user_id");
  }

  @Test
  public void updateGlobalProperties() {
    dbTester.prepareDbUnit(getClass(), "updateGlobalProperties.xml");

    dao.insertGlobalProperties(ImmutableMap.of("to_be_updated", "updated"));

    dbTester.assertDbUnitTable(getClass(), "updateGlobalProperties-result.xml", "properties", "prop_key", "text_value", "resource_id", "user_id");
  }

  @Test
  public void renamePropertyKey() {
    dbTester.prepareDbUnit(getClass(), "renamePropertyKey.xml");

    dao.renamePropertyKey("sonar.license.secured", "sonar.license");

    dbTester.assertDbUnitTable(getClass(), "renamePropertyKey-result.xml", "properties", "prop_key", "text_value", "resource_id", "user_id");
  }

  @Test
  public void should_not_rename_if_same_key() {
    dbTester.prepareDbUnit(getClass(), "should_not_rename_if_same_key.xml");

    dao.renamePropertyKey("foo", "foo");

    dbTester.assertDbUnitTable(getClass(), "should_not_rename_if_same_key-result.xml", "properties", "prop_key", "text_value", "resource_id", "user_id");
  }

  @Test
  public void should_not_rename_with_empty_key() {
    thrown.expect(IllegalArgumentException.class);
    dao.renamePropertyKey("foo", "");
  }

  @Test
  public void should_not_rename_an_empty_key() {
    thrown.expect(IllegalArgumentException.class);
    dao.renamePropertyKey(null, "foo");
  }

  @Test
  public void updatePropertiesFromKeyAndValueToNewValue() {
    dbTester.prepareDbUnit(getClass(), "updatePropertiesFromKeyAndValueToNewValue.xml");

    dao.updateProperties("sonar.profile.java", "Sonar Way", "Default");

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
      dao.insertProperty(session, propertyDto);
    }
    session.commit();
  }
}
