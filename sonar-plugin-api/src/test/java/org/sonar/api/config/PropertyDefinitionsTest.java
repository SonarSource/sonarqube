/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.config;

import org.junit.Test;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.resources.Qualifiers;

import static org.fest.assertions.Assertions.assertThat;

public class PropertyDefinitionsTest {

  @Test
  public void should_inspect_plugin_objects() {
    PropertyDefinitions def = new PropertyDefinitions(
        PropertyDefinition.build("foo").name("Foo").build(),
        PropertyDefinition.build("one").name("One").build(),
        PropertyDefinition.build("two").name("Two").defaultValue("2").build()
        );

    assertProperties(def);
  }

  @Test
  public void should_inspect_annotation_plugin_objects() {
    PropertyDefinitions def = new PropertyDefinitions(new PluginWithProperty(), new PluginWithProperties());

    assertProperties(def);
  }

  @Test
  public void should_inspect_plugin_classes() {
    PropertyDefinitions def = new PropertyDefinitions(PluginWithProperty.class, PluginWithProperties.class);

    assertProperties(def);
  }

  @Test
  public void test_categories() {
    PropertyDefinitions def = new PropertyDefinitions(
        PropertyDefinition.build("inCateg").name("In Categ").category("categ").build(),
        PropertyDefinition.build("noCateg").name("No categ").build()
        );

    assertThat(def.getCategory("inCateg")).isEqualTo("categ");
    assertThat(def.getCategory("noCateg")).isEmpty();
  }

  @Test
  public void test_categories_on_annotation_plugin() {
    PropertyDefinitions def = new PropertyDefinitions(Categories.class);

    assertThat(def.getCategory("inCateg")).isEqualTo("categ");
    assertThat(def.getCategory("noCateg")).isEqualTo("");
  }

  @Test
  public void test_default_category() {
    PropertyDefinitions def = new PropertyDefinitions();
    def.addComponent(PropertyDefinition.build("inCateg").name("In Categ").category("categ").build(), "default");
    def.addComponent(PropertyDefinition.build("noCateg").name("No categ").build(), "default");

    assertThat(def.getCategory("inCateg")).isEqualTo("categ");
    assertThat(def.getCategory("noCateg")).isEqualTo("default");
  }

  @Test
  public void test_default_category_on_annotation_plugin() {
    PropertyDefinitions def = new PropertyDefinitions();
    def.addComponent(Categories.class, "default");
    assertThat(def.getCategory("inCateg")).isEqualTo("categ");
    assertThat(def.getCategory("noCateg")).isEqualTo("default");
  }

  @Test
  public void should_group_by_category() {
    PropertyDefinitions def = new PropertyDefinitions(
        PropertyDefinition.build("global1").name("Global1").category("catGlobal1").global(true).build(),
        PropertyDefinition.build("global2").name("Global2").category("catGlobal1").global(true).build(),
        PropertyDefinition.build("global3").name("Global3").category("catGlobal2").global(true).build(),
        PropertyDefinition.build("project").name("Project").category("catProject").global(false).qualifiers(Qualifiers.PROJECT).build(),
        PropertyDefinition.build("module").name("Module").category("catModule").global(false).qualifiers(Qualifiers.MODULE).build(),
        PropertyDefinition.build("view").name("View").category("catView").global(false).qualifiers(Qualifiers.VIEW).build()
        );

    assertThat(def.getPropertiesByCategory(null).keySet()).containsOnly("catGlobal1", "catGlobal2");
    assertThat(def.getPropertiesByCategory(Qualifiers.PROJECT).keySet()).containsOnly("catProject");
    assertThat(def.getPropertiesByCategory(Qualifiers.MODULE).keySet()).containsOnly("catModule");
    assertThat(def.getPropertiesByCategory(Qualifiers.VIEW).keySet()).containsOnly("catView");
    assertThat(def.getPropertiesByCategory("Unkown").keySet()).isEmpty();
  }

  @Test
  public void should_group_by_subcategory() {
    PropertyDefinitions def = new PropertyDefinitions(
        PropertyDefinition.build("global1").name("Global1").category("catGlobal1").subcategory("sub1").global(true).build(),
        PropertyDefinition.build("global2").name("Global2").category("catGlobal1").subcategory("sub2").global(true).build(),
        PropertyDefinition.build("global3").name("Global3").category("catGlobal1").global(true).build(),
        PropertyDefinition.build("global4").name("Global4").category("catGlobal2").global(true).build()
        );

    assertThat(def.getPropertiesByCategory(null).get("catGlobal1").keySet()).containsOnly("default", "sub1", "sub2");
    assertThat(def.getPropertiesByCategory(null).get("catGlobal2").keySet()).containsOnly("default");
  }

  @Test
  public void should_group_by_category_on_annotation_plugin() {
    PropertyDefinitions def = new PropertyDefinitions(ByCategory.class);

    assertThat(def.getPropertiesByCategory().keySet()).containsOnly("catGlobal1", "catGlobal2");
    assertThat(def.getPropertiesByCategory(Qualifiers.PROJECT).keySet()).containsOnly("catProject");
    assertThat(def.getPropertiesByCategory(Qualifiers.MODULE).keySet()).containsOnly("catModule");
  }

  private void assertProperties(PropertyDefinitions definitions) {
    assertThat(definitions.get("foo").name()).isEqualTo("Foo");
    assertThat(definitions.get("one").name()).isEqualTo("One");
    assertThat(definitions.get("two").name()).isEqualTo("Two");
    assertThat(definitions.get("unknown")).isNull();

    assertThat(definitions.getDefaultValue("foo")).isNull();
    assertThat(definitions.getDefaultValue("two")).isEqualTo("2");

    assertThat(definitions.getAll().size()).isEqualTo(3);
  }

  @Property(key = "foo", name = "Foo")
  static final class PluginWithProperty {
  }

  @Properties({
    @Property(key = "one", name = "One"),
    @Property(key = "two", name = "Two", defaultValue = "2")
  })
  static final class PluginWithProperties {
  }

  @Properties({
    @Property(key = "inCateg", name = "In Categ", category = "categ"),
    @Property(key = "noCateg", name = "No categ")
  })
  static final class Categories {
  }

  @Properties({
    @Property(key = "global1", name = "Global1", category = "catGlobal1", global = true, project = false, module = false),
    @Property(key = "global2", name = "Global2", category = "catGlobal1", global = true, project = false, module = false),
    @Property(key = "global3", name = "Global3", category = "catGlobal2", global = true, project = false, module = false),
    @Property(key = "project", name = "Project", category = "catProject", global = false, project = true, module = false),
    @Property(key = "module", name = "Module", category = "catModule", global = false, project = false, module = true)
  })
  static final class ByCategory {
  }
}
