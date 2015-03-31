/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.properties;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PropertiesDaoTest extends AbstractDaoTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  DbSession session;
  PropertiesDao dao;

  @Before
  public void createDao() {
    dao = new PropertiesDao(getMyBatis());
    session = getMyBatis().openSession(false);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void shouldFindUsersForNotification() {
    setupData("shouldFindUsersForNotification");

    List<String> users = dao.findUsersForNotification("NewViolations", "Email", null);
    assertThat(users).isEmpty();

    users = dao.findUsersForNotification("NewViolations", "Email", 78L);
    assertThat(users).isEmpty();

    users = dao.findUsersForNotification("NewViolations", "Email", 45L);
    assertThat(users).hasSize(1);
    assertThat(users).containsOnly("user2");

    users = dao.findUsersForNotification("NewViolations", "Twitter", null);
    assertThat(users).hasSize(1);
    assertThat(users).containsOnly("user3");

    users = dao.findUsersForNotification("NewViolations", "Twitter", 78L);
    assertThat(users).isEmpty();

    users = dao.findUsersForNotification("NewViolations", "Twitter", 56L);
    assertThat(users).hasSize(2);
    assertThat(users).containsOnly("user1", "user3");
  }

  @Test
  public void findNotificationSubscribers() {
    setupData("findNotificationSubscribers");

    // Nobody is subscribed
    List<String> users = dao.findNotificationSubscribers("NotSexyDispatcher", "Email", "org.apache:struts");
    assertThat(users).isEmpty();

    // Global subscribers
    users = dao.findNotificationSubscribers("DispatcherWithGlobalSubscribers", "Email", "org.apache:struts");
    assertThat(users).containsOnly("simon");

    users = dao.findNotificationSubscribers("DispatcherWithGlobalSubscribers", "Email", null);
    assertThat(users).containsOnly("simon");

    // Project subscribers
    users = dao.findNotificationSubscribers("DispatcherWithProjectSubscribers", "Email", "org.apache:struts");
    assertThat(users).containsOnly("eric");

    // Global + Project subscribers
    users = dao.findNotificationSubscribers("DispatcherWithGlobalAndProjectSubscribers", "Email", "org.apache:struts");
    assertThat(users).containsOnly("eric", "simon");
  }

  @Test
  public void hasNotificationSubscribers() throws Exception {
    setupData("findNotificationSubscribers");

    // Nobody is subscribed
    assertThat(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", Arrays.asList("NotSexyDispatcher"))).isFalse();

    // Global subscribers
    assertThat(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", Arrays.asList("DispatcherWithGlobalSubscribers"))).isTrue();

    // Project subscribers
    assertThat(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", Arrays.asList("DispatcherWithProjectSubscribers"))).isTrue();
    assertThat(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_B", Arrays.asList("DispatcherWithProjectSubscribers"))).isFalse();

    // Global + Project subscribers
    assertThat(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_A", Arrays.asList("DispatcherWithGlobalAndProjectSubscribers"))).isTrue();
    assertThat(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_B", Arrays.asList("DispatcherWithGlobalAndProjectSubscribers"))).isTrue();
  }

  @Test
  public void selectGlobalProperties() {
    setupData("selectGlobalProperties");
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
    setupData("selectGlobalProperties");

    PropertyDto prop = dao.selectGlobalProperty("global.one");
    assertThat(prop).isNotNull();
    assertThat(prop.getValue(), is("one"));

    assertThat(dao.selectGlobalProperty("project.one")).isNull();
    assertThat(dao.selectGlobalProperty("user.one")).isNull();
    assertThat(dao.selectGlobalProperty("unexisting")).isNull();
  }

  @Test
  public void selectProjectProperties() {
    setupData("selectProjectProperties");
    List<PropertyDto> properties = dao.selectProjectProperties("org.struts:struts");
    assertThat(properties.size(), is(1));

    PropertyDto first = properties.get(0);
    assertThat(first.getKey(), is("struts.one"));
    assertThat(first.getValue(), is("one"));
  }

  @Test
  public void selectProjectPropertiesByResourceId() {
    setupData("selectProjectPropertiesByResourceId");

    List<PropertyDto> properties = dao.selectProjectProperties(10L);

    assertThat(properties.size(), is(2));
    assertThat(properties).extracting("key").containsOnly("struts.one", "user.two");
    assertThat(properties).extracting("value").containsOnly("one", "two");
  }

  @Test
  public void select_module_properties_tree() throws Exception {
    setupData("select_module_properties_tree");

    List<PropertyDto> properties = dao.selectEnabledDescendantModuleProperties("ABCD", session);
    assertThat(properties.size(), is(4));
    assertThat(properties).extracting("key").containsOnly("struts.one", "core.one", "core.two", "data.one");
    assertThat(properties).extracting("value").containsOnly("one", "two");

    properties = dao.selectEnabledDescendantModuleProperties("EFGH", session);
    assertThat(properties.size(), is(3));
    assertThat(properties).extracting("key").containsOnly("core.one", "core.two", "data.one");

    properties = dao.selectEnabledDescendantModuleProperties("FGHI", session);
    assertThat(properties.size(), is(1));
    assertThat(properties).extracting("key").containsOnly("data.one");

    assertThat(dao.selectEnabledDescendantModuleProperties("unknown", session).size(), is(0));
  }

  @Test
  public void selectProjectProperty() {
    setupData("selectProjectProperties");
    PropertyDto property = dao.selectProjectProperty(11L, "commonslang.one");

    assertThat(property.getKey(), is("commonslang.one"));
    assertThat(property.getValue(), is("two"));
  }

  @Test
  public void select_by_query() {
    setupData("select_by_query");

    List<PropertyDto> results = dao.selectByQuery(PropertyQuery.builder().setKey("user.two").setComponentId(10L).setUserId(100).build(), session);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getValue()).isEqualTo("two");

    results = dao.selectByQuery(PropertyQuery.builder().setKey("user.one").setUserId(100).build(), session);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getValue()).isEqualTo("one");
  }

  @Test
  public void setProperty_update() {
    setupData("update");

    dao.setProperty(new PropertyDto().setKey("global.key").setValue("new_global"));
    dao.setProperty(new PropertyDto().setKey("project.key").setResourceId(10L).setValue("new_project"));
    dao.setProperty(new PropertyDto().setKey("user.key").setUserId(100L).setValue("new_user"));
    dao.setProperty(new PropertyDto().setKey("null.value").setValue(null));

    checkTables("update", "properties");
  }

  @Test
  public void setProperty_insert() {
    setupData("insert");

    dao.setProperty(new PropertyDto().setKey("global.key").setValue("new_global"));
    dao.setProperty(new PropertyDto().setKey("project.key").setResourceId(10L).setValue("new_project"));
    dao.setProperty(new PropertyDto().setKey("user.key").setUserId(100L).setValue("new_user"));

    checkTables("insert", "properties");
  }

  @Test
  public void delete_project_property() {
    setupData("delete_project_property");

    dao.deleteProjectProperty("struts.one", 10L);

    checkTables("delete_project_property", "properties");
  }

  @Test
  public void delete_project_properties() {
    setupData("delete_project_properties");

    dao.deleteProjectProperties("sonar.profile.java", "Sonar Way");

    checkTables("delete_project_properties", "properties");
  }

  @Test
  public void deleteGlobalProperties() {
    setupData("deleteGlobalProperties");

    dao.deleteGlobalProperties();

    checkTables("deleteGlobalProperties", "properties");
  }

  @Test
  public void deleteGlobalProperty() {
    setupData("deleteGlobalProperty");

    dao.deleteGlobalProperty("to_be_deleted");

    checkTables("deleteGlobalProperty", "properties");
  }

  @Test
  public void deleteAllProperties() {
    setupData("deleteAllProperties");

    dao.deleteAllProperties("to_be_deleted");

    checkTables("deleteAllProperties", "properties");
  }

  @Test
  public void insertGlobalProperties() {
    setupData("insertGlobalProperties");

    dao.saveGlobalProperties(ImmutableMap.of("to_be_inserted", "inserted"));

    checkTable("insertGlobalProperties", "properties", "prop_key", "text_value", "resource_id", "user_id");
  }

  @Test
  public void updateGlobalProperties() {
    setupData("updateGlobalProperties");

    dao.saveGlobalProperties(ImmutableMap.of("to_be_updated", "updated"));

    checkTable("updateGlobalProperties", "properties", "prop_key", "text_value", "resource_id", "user_id");
  }

  @Test
  public void renamePropertyKey() {
    setupData("renamePropertyKey");

    dao.renamePropertyKey("sonar.license.secured", "sonar.license");

    checkTable("renamePropertyKey", "properties", "prop_key", "text_value", "resource_id", "user_id");
  }

  @Test
  public void should_not_rename_if_same_key() {
    setupData("should_not_rename_if_same_key");

    dao.renamePropertyKey("foo", "foo");

    checkTable("should_not_rename_if_same_key", "properties", "prop_key", "text_value", "resource_id", "user_id");
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
    setupData("updatePropertiesFromKeyAndValueToNewValue");

    dao.updateProperties("sonar.profile.java", "Sonar Way", "Default");

    checkTable("updatePropertiesFromKeyAndValueToNewValue", "properties", "prop_key", "text_value", "resource_id", "user_id");
  }

  private PropertyDto findById(List<PropertyDto> properties, int id) {
    for (PropertyDto property : properties) {
      if (property.getId() == id) {
        return property;
      }
    }
    return null;
  }
}
