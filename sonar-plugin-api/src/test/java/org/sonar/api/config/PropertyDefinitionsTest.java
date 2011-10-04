/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.config;

import org.junit.Test;
import org.sonar.api.Properties;
import org.sonar.api.Property;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class PropertyDefinitionsTest {

  @Test
  public void shouldInspectPluginObjects() {
    PropertyDefinitions def = new PropertyDefinitions(new PluginWithProperty(), new PluginWithProperties());

    assertProperties(def);
  }

  @Test
  public void shouldInspectPluginClasses() {
    PropertyDefinitions def = new PropertyDefinitions(PluginWithProperty.class, PluginWithProperties.class);

    assertProperties(def);
  }

  private void assertProperties(PropertyDefinitions def) {
    assertThat(def.getProperty("foo").name(), is("Foo"));
    assertThat(def.getProperty("one").name(), is("One"));
    assertThat(def.getProperty("two").name(), is("Two"));
    assertThat(def.getProperty("unknown"), nullValue());

    assertThat(def.getDefaultValue("foo"), nullValue());
    assertThat(def.getDefaultValue("two"), is("2"));

    assertThat(def.getProperties().size(), is(3));
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


  @Test
  public void testCategories() {
    PropertyDefinitions def = new PropertyDefinitions(Categories.class);
    assertThat(def.getCategory("inCateg"), is("categ"));
    assertThat(def.getCategory("noCateg"), is(""));
  }

  @Test
  public void testDefaultCategory() {
    PropertyDefinitions def = new PropertyDefinitions();
    def.addComponent(Categories.class, "default");
    assertThat(def.getCategory("inCateg"), is("categ"));
    assertThat(def.getCategory("noCateg"), is("default"));
  }

  @Properties({
      @Property(key = "inCateg", name="In Categ", category = "categ"),
      @Property(key = "noCateg", name="No categ")
  })
  static final class Categories {
  }
}
