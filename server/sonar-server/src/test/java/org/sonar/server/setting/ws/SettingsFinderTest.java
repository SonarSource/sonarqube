/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.property.PropertyDto;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.PropertyType.PROPERTY_SET;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;

public class SettingsFinderTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private PropertyDefinitions propertyDefinitions = new PropertyDefinitions();

  private SettingsFinder underTest = new SettingsFinder(dbClient, propertyDefinitions);

  @Test
  public void return_global_settings() {
    PropertyDefinition definition = PropertyDefinition.builder("foo").build();
    addDefinitions(definition);
    insertProperties(newGlobalPropertyDto().setKey("foo").setValue("one"));

    List<Setting> settings = underTest.loadGlobalSettings(dbSession, newHashSet("foo"));
    assertThat(settings).hasSize(1);
    assertSetting(settings.get(0), "foo", "one", null, true);

    assertThat(underTest.loadGlobalSettings(dbSession, newHashSet("unknown"))).isEmpty();
  }

  @Test
  public void return_global_setting_even_if_no_definition() {
    insertProperties(newGlobalPropertyDto().setKey("foo").setValue("one"));

    List<Setting> settings = underTest.loadGlobalSettings(dbSession, newHashSet("foo"));
    assertThat(settings).hasSize(1);
    assertSetting(settings.get(0), "foo", "one", null, false);
  }

  @Test
  public void return_global_settings_with_property_set() {
    addDefinitions(PropertyDefinition.builder("set1")
      .type(PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("size.value").name("Size").build()))
      .build(),
      PropertyDefinition.builder("another")
        .type(PROPERTY_SET)
        .fields(singletonList(PropertyFieldDefinition.build("key").name("Key").build()))
        .build());
    insertProperties(
      newGlobalPropertyDto().setKey("set1").setValue("1,2"),
      newGlobalPropertyDto().setKey("set1.1.key").setValue("key1"),
      newGlobalPropertyDto().setKey("set1.1.size.value").setValue("size1"),
      newGlobalPropertyDto().setKey("set1.2.key").setValue("key2"),
      newGlobalPropertyDto().setKey("set2").setValue("1"),
      newGlobalPropertyDto().setKey("another.1.key").setValue("key1"));

    List<Setting> settings = underTest.loadGlobalSettings(dbSession, newHashSet("set1"));
    assertThat(settings).hasSize(1);
    assertSetting(settings.get(0), "set1", "1,2", null, true, ImmutableMap.of("key", "key1", "size.value", "size1"), ImmutableMap.of("key", "key2"));
  }

  @Test
  public void return_component_settings() {
    ComponentDto project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()));
    addDefinitions(PropertyDefinition.builder("property").defaultValue("default").build());
    insertProperties(newComponentPropertyDto(project).setKey("property").setValue("one"));

    Multimap<String, Setting> result = underTest.loadComponentSettings(dbSession, newHashSet("property"), project);
    assertThat(result.values()).hasSize(1);
    List<Setting> settings = new ArrayList<>(result.get(project.uuid()));
    assertThat(settings).hasSize(1);
    assertSetting(settings.get(0), "property", "one", project.getId(), true);

    assertThat(underTest.loadComponentSettings(dbSession, newHashSet("unknown"), project)).isEmpty();
  }

  @Test
  public void return_component_setting_even_if_no_definition() {
    ComponentDto project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()));
    insertProperties(newComponentPropertyDto(project).setKey("property").setValue("one"));

    Multimap<String, Setting> settings = underTest.loadComponentSettings(dbSession, newHashSet("property"), project);
    assertThat(settings.values()).hasSize(1);
    assertSetting(settings.get(project.uuid()).iterator().next(), "property", "one", project.getId(), false);
  }

  @Test
  public void return_component_settings_with_property_set() {
    ComponentDto project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()));
    addDefinitions(PropertyDefinition.builder("set1")
      .type(PROPERTY_SET)
      .fields(asList(
        PropertyFieldDefinition.build("key").name("Key").build(),
        PropertyFieldDefinition.build("size").name("Size").build()))
      .build(),
      PropertyDefinition.builder("another")
        .type(PROPERTY_SET)
        .fields(singletonList(PropertyFieldDefinition.build("key").name("Key").build()))
        .build());
    insertProperties(
      newComponentPropertyDto(project).setKey("set1").setValue("1,2"),
      newComponentPropertyDto(project).setKey("set1.1.key").setValue("key1"),
      newComponentPropertyDto(project).setKey("set1.1.size").setValue("size1"),
      newComponentPropertyDto(project).setKey("set1.2.key").setValue("key2"),
      newComponentPropertyDto(project).setKey("set2").setValue("1"),
      newComponentPropertyDto(project).setKey("another.1.key").setValue("key1"));

    Multimap<String, Setting> settings = underTest.loadComponentSettings(dbSession, newHashSet("set1"), project);
    assertThat(settings).hasSize(1);
    assertSetting(settings.get(project.uuid()).iterator().next(), "set1", "1,2", project.getId(), true, ImmutableMap.of("key", "key1", "size", "size1"), ImmutableMap.of("key", "key2"));
  }

  @Test
  public void return_module_settings() {
    ComponentDto project = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()));
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    ComponentDto subModule = componentDb.insertComponent(newModuleDto(module));
    ComponentDto anotherProject = componentDb.insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()));

    insertProperties(
      newComponentPropertyDto(project).setKey("property").setValue("one"),
      newComponentPropertyDto(module).setKey("property").setValue("two"),
      newComponentPropertyDto(subModule).setKey("property2").setValue("three"),
      newComponentPropertyDto(anotherProject).setKey("property").setValue("another one"));

    Multimap<String, Setting> result = underTest.loadComponentSettings(dbSession, newHashSet("property", "property2"), subModule);
    assertThat(result).hasSize(3);
    assertThat(result.keySet()).containsExactly(project.uuid(), module.uuid(), subModule.uuid());

    assertSetting(result.get(subModule.uuid()).iterator().next(), "property2", "three", subModule.getId(), false);
    assertSetting(result.get(module.uuid()).iterator().next(), "property", "two", module.getId(), false);
    assertSetting(result.get(project.uuid()).iterator().next(), "property", "one", project.getId(), false);
  }

  private void assertSetting(Setting setting, String expectedKey, String expectedValue, @Nullable Long expectedComponentId, boolean hasPropertyDefinition,
    Map<String, String>... propertySets) {
    assertThat(setting.getKey()).isEqualTo(expectedKey);
    assertThat(setting.getValue()).isEqualTo(expectedValue);
    assertThat(setting.getComponentId()).isEqualTo(expectedComponentId);
    if (hasPropertyDefinition) {
      assertThat(setting.getDefinition()).isEqualTo(propertyDefinitions.get(expectedKey));
    } else {
      assertThat(setting.getDefinition()).isNull();
    }
    assertThat(setting.getPropertySets()).containsOnly(propertySets);
  }

  private void insertProperties(PropertyDto... properties) {
    for (PropertyDto propertyDto : properties) {
      dbClient.propertiesDao().saveProperty(dbSession, propertyDto);
    }
    dbSession.commit();
  }

  private void addDefinitions(PropertyDefinition... definitions) {
    propertyDefinitions.addComponents(asList(definitions));
  }
}
