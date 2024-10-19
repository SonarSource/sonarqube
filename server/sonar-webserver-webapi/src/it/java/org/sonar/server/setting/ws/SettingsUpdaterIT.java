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
package org.sonar.server.setting.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonar.db.property.PropertyTesting.newUserPropertyDto;

public class SettingsUpdaterIT {


  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  PropertyDbTester propertyDb = new PropertyDbTester(db);

  PropertyDefinitions definitions = new PropertyDefinitions(System2.INSTANCE);
  ProjectDto project;

  SettingsUpdater underTest = new SettingsUpdater(dbClient, definitions);

  @Before
  public void setUp() {
    project = db.components().insertPrivateProject().getProjectDto();
  }

  @Test
  public void delete_global_settings() {
    definitions.addComponent(PropertyDefinition.builder("foo").build());
    propertyDb.insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("foo").setValue("value"));
    propertyDb.insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("foo").setValue("one"));
    propertyDb.insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("bar").setValue("two"));

    underTest.deleteGlobalSettings(dbSession, "foo", "bar");

    assertGlobalPropertyDoesNotExist("foo");
    assertGlobalPropertyDoesNotExist("bar");
    assertProjectPropertyExists("foo");
  }

  @Test
  public void delete_component_settings() {
    definitions.addComponent(PropertyDefinition.builder("foo").build());
    propertyDb.insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("foo").setValue("value"));
    propertyDb.insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("foo").setValue("one"));
    propertyDb.insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("bar").setValue("two"));

    underTest.deleteComponentSettings(dbSession, project, "foo", "bar");

    assertProjectPropertyDoesNotExist("foo");
    assertProjectPropertyDoesNotExist("bar");
    assertGlobalPropertyExists("foo");
  }

  @Test
  public void does_not_fail_when_deleting_unknown_setting() {
    propertyDb.insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("foo").setValue("one"));

    underTest.deleteGlobalSettings(dbSession, "unknown");

    assertGlobalPropertyExists("foo");
  }

  @Test
  public void does_not_delete_user_settings() {
    UserDto user = dbClient.userDao().insert(dbSession, UserTesting.newUserDto());
    propertyDb.insertProperties(user.getLogin(), null, null, null, newUserPropertyDto("foo", "one", user));
    propertyDb.insertProperties(null, null, null, null, newGlobalPropertyDto().setKey("foo").setValue("one"));

    underTest.deleteGlobalSettings(dbSession, "foo");

    assertUserPropertyExists("foo", user);
  }

  @Test
  public void delete_global_property_set() {
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("size").name("Size").build()))
      .build());
    propertyDb.insertProperties(null, null, null, null,
      newGlobalPropertyDto().setKey("foo").setValue("1,2"),
      newGlobalPropertyDto().setKey("foo.1.key").setValue("key1"),
      newGlobalPropertyDto().setKey("foo.1.size").setValue("size1"),
      newGlobalPropertyDto().setKey("foo.2.key").setValue("key2"));

    underTest.deleteGlobalSettings(dbSession, "foo");

    assertGlobalPropertyDoesNotExist("foo");
    assertGlobalPropertyDoesNotExist("foo.1.key");
    assertGlobalPropertyDoesNotExist("foo.1.size");
    assertGlobalPropertyDoesNotExist("foo.2.key");
  }

  @Test
  public void delete_component_property_set() {
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("size").name("Size").build()))
      .build());
    propertyDb.insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("foo").setValue("1,2"),
      newComponentPropertyDto(project).setKey("foo.1.key").setValue("key1"),
      newComponentPropertyDto(project).setKey("foo.1.size").setValue("size1"),
      newComponentPropertyDto(project).setKey("foo.2.key").setValue("key2"));

    underTest.deleteComponentSettings(dbSession, project, "foo");

    assertProjectPropertyDoesNotExist("foo");
    assertProjectPropertyDoesNotExist("foo.1.key");
    assertProjectPropertyDoesNotExist("foo.1.size");
    assertProjectPropertyDoesNotExist("foo.2.key");
  }

  @Test
  public void does_not_fail_when_deleting_unknown_property_set() {
    definitions.addComponent(PropertyDefinition
      .builder("foo")
      .type(PropertyType.PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("size").name("Size").build()))
      .build());
    propertyDb.insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("other").setValue("1,2"),
      newComponentPropertyDto(project).setKey("other.1.key").setValue("key1"));

    underTest.deleteComponentSettings(dbSession, project, "foo");

    assertProjectPropertyExists("other");
  }

  @Test
  public void fail_to_delete_global_setting_when_no_setting_key() {
    assertThatThrownBy(() -> {
      underTest.deleteGlobalSettings(dbSession);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("At least one setting key is required");
  }

  @Test
  public void fail_to_delete_component_setting_when_no_setting_key() {
    assertThatThrownBy(() -> {
      underTest.deleteComponentSettings(dbSession, project);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("At least one setting key is required");
  }

  private void assertGlobalPropertyDoesNotExist(String key) {
    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, key)).isNull();
  }

  private void assertGlobalPropertyExists(String key) {
    assertThat(dbClient.propertiesDao().selectGlobalProperty(dbSession, key)).isNotNull();
  }

  private void assertProjectPropertyDoesNotExist(String key) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setEntityUuid(project.getUuid()).setKey(key).build(), dbSession)).isEmpty();
  }

  private void assertProjectPropertyExists(String key) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setEntityUuid(project.getUuid()).setKey(key).build(), dbSession)).isNotEmpty();
  }

  private void assertUserPropertyExists(String key, UserDto user) {
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
        .setKey(key)
        .setUserUuid(user.getUuid())
        .build(),
      dbSession)).isNotEmpty();
  }
}
