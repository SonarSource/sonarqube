/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.hamcrest.core.Is;
import org.junit.Test;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.utils.AnnotationUtils;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class PropertyDefinitionTest {

  @Test
  public void createFromAnnotation() {
    Properties props = AnnotationUtils.getClassAnnotation(Init.class, Properties.class);
    Property prop = props.value()[0];

    PropertyDefinition def = PropertyDefinition.create(prop);

    assertThat(def.getKey(), Is.is("hello"));
    assertThat(def.getName(), Is.is("Hello"));
    assertThat(def.getDefaultValue(), Is.is("world"));
    assertThat(def.getCategory(), Is.is("categ"));
    assertThat(def.getOptions().length, Is.is(2));
    assertThat(Arrays.asList(def.getOptions()), hasItems("de", "en"));
    assertThat(def.getDescription(), Is.is("desc"));
    assertThat(def.getType(), Is.is(PropertyType.FLOAT));
    assertThat(def.isGlobal(), Is.is(false));
    assertThat(def.isOnProject(), Is.is(true));
    assertThat(def.isOnModule(), Is.is(true));
  }

  @Properties({
    @Property(key = "hello", name = "Hello", defaultValue = "world", description = "desc",
      options = {"de", "en"}, category = "categ", type = PropertyType.FLOAT, global = false, project = true, module = true)
  })
  static class Init {
  }

  @Test
  public void createFromAnnotation_default_values() {
    Properties props = AnnotationUtils.getClassAnnotation(DefaultValues.class, Properties.class);
    Property prop = props.value()[0];

    PropertyDefinition def = PropertyDefinition.create(prop);

    assertThat(def.getKey(), Is.is("hello"));
    assertThat(def.getName(), Is.is("Hello"));
    assertThat(def.getDefaultValue(), Is.is(""));
    assertThat(def.getCategory(), Is.is(""));
    assertThat(def.getOptions().length, Is.is(0));
    assertThat(def.getDescription(), Is.is(""));
    assertThat(def.getType(), Is.is(PropertyType.STRING));
    assertThat(def.isGlobal(), Is.is(true));
    assertThat(def.isOnProject(), Is.is(false));
    assertThat(def.isOnModule(), Is.is(false));
  }

  @Properties({
    @Property(key = "hello", name = "Hello")
  })
  static class DefaultValues {
  }

  @Test
  public void validate_string() {
    PropertyDefinition def = new PropertyDefinition(PropertyType.STRING, new String[0]);

    assertThat(def.validate(null).isValid(), is(true));
    assertThat(def.validate("").isValid(), is(true));
    assertThat(def.validate("   ").isValid(), is(true));
    assertThat(def.validate("foo").isValid(), is(true));
  }

  @Test
  public void validate_boolean() {
    PropertyDefinition def = new PropertyDefinition(PropertyType.BOOLEAN, new String[0]);

    assertThat(def.validate(null).isValid(), is(true));
    assertThat(def.validate("").isValid(), is(true));
    assertThat(def.validate("   ").isValid(), is(true));
    assertThat(def.validate("true").isValid(), is(true));
    assertThat(def.validate("false").isValid(), is(true));

    assertThat(def.validate("foo").isValid(), is(false));
    assertThat(def.validate("foo").getErrorKey(), is("notBoolean"));
  }

  @Test
  public void validate_integer() {
    PropertyDefinition def = new PropertyDefinition(PropertyType.INTEGER, new String[0]);

    assertThat(def.validate(null).isValid(), is(true));
    assertThat(def.validate("").isValid(), is(true));
    assertThat(def.validate("   ").isValid(), is(true));
    assertThat(def.validate("123456").isValid(), is(true));

    assertThat(def.validate("foo").isValid(), is(false));
    assertThat(def.validate("foo").getErrorKey(), is("notInteger"));
  }

  @Test
  public void validate_float() {
    PropertyDefinition def = new PropertyDefinition(PropertyType.FLOAT, new String[0]);

    assertThat(def.validate(null).isValid(), is(true));
    assertThat(def.validate("").isValid(), is(true));
    assertThat(def.validate("   ").isValid(), is(true));
    assertThat(def.validate("123456").isValid(), is(true));
    assertThat(def.validate("3.14").isValid(), is(true));

    assertThat(def.validate("foo").isValid(), is(false));
    assertThat(def.validate("foo").getErrorKey(), is("notFloat"));
  }

  @Test
  public void validate_single_select_list() {
    PropertyDefinition def = new PropertyDefinition(PropertyType.SINGLE_SELECT_LIST, new String[]{"de", "en"});

    assertThat(def.validate(null).isValid(), is(true));
    assertThat(def.validate("").isValid(), is(true));
    assertThat(def.validate("   ").isValid(), is(true));
    assertThat(def.validate("de").isValid(), is(true));
    assertThat(def.validate("en").isValid(), is(true));

    assertThat(def.validate("fr").isValid(), is(false));
    assertThat(def.validate("fr").getErrorKey(), is("notInOptions"));
  }

  @Properties({
    @Property(key = "scm.password.secured", name = "SCM password")
  })
  static class OldScmPlugin {
  }

  @Test
  public void autodetectPasswordType() {
    Properties props = AnnotationUtils.getClassAnnotation(OldScmPlugin.class, Properties.class);
    Property prop = props.value()[0];

    PropertyDefinition def = PropertyDefinition.create(prop);

    assertThat(def.getKey(), Is.is("scm.password.secured"));
    assertThat(def.getType(), Is.is(PropertyType.PASSWORD));
  }
}
