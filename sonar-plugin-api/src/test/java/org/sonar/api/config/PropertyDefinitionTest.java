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

import org.sonar.api.PropertyField;

import org.junit.Test;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.utils.AnnotationUtils;

import static org.fest.assertions.Assertions.assertThat;

public class PropertyDefinitionTest {
  @Test
  public void createFromAnnotation() {
    Properties props = AnnotationUtils.getAnnotation(Init.class, Properties.class);
    Property prop = props.value()[0];

    PropertyDefinition def = PropertyDefinition.create(prop);

    assertThat(def.getKey()).isEqualTo("hello");
    assertThat(def.getName()).isEqualTo("Hello");
    assertThat(def.getDefaultValue()).isEqualTo("world");
    assertThat(def.getCategory()).isEqualTo("categ");
    assertThat(def.getOptions()).hasSize(2);
    assertThat(def.getOptions()).contains("de", "en");
    assertThat(def.getDescription()).isEqualTo("desc");
    assertThat(def.getType()).isEqualTo(PropertyType.FLOAT);
    assertThat(def.isGlobal()).isFalse();
    assertThat(def.isOnProject()).isTrue();
    assertThat(def.isOnModule()).isTrue();
    assertThat(def.isMultiValues()).isTrue();
    assertThat(def.getPropertySetKey()).isEqualTo("set");
    assertThat(def.getFields()).isEmpty();
  }

  @Properties(@Property(key = "hello", name = "Hello", defaultValue = "world", description = "desc",
    options = {"de", "en"}, category = "categ", type = PropertyType.FLOAT, global = false, project = true, module = true, multiValues = true, propertySetKey = "set"))
  static class Init {
  }

  @Test
  public void createFromAnnotation_default_values() {
    Properties props = AnnotationUtils.getAnnotation(DefaultValues.class, Properties.class);
    Property prop = props.value()[0];

    PropertyDefinition def = PropertyDefinition.create(prop);

    assertThat(def.getKey()).isEqualTo("hello");
    assertThat(def.getName()).isEqualTo("Hello");
    assertThat(def.getDefaultValue()).isEmpty();
    assertThat(def.getCategory()).isEmpty();
    assertThat(def.getOptions()).isEmpty();
    assertThat(def.getDescription()).isEmpty();
    assertThat(def.getType()).isEqualTo(PropertyType.STRING);
    assertThat(def.isGlobal()).isTrue();
    assertThat(def.isOnProject()).isFalse();
    assertThat(def.isOnModule()).isFalse();
    assertThat(def.isMultiValues()).isFalse();
    assertThat(def.getPropertySetKey()).isEmpty();
    assertThat(def.getFields()).isEmpty();
  }

  @Properties(@Property(key = "hello", name = "Hello", fields = {
    @PropertyField(key = "first", name = "First", description = "Description", options = {"A", "B"}),
    @PropertyField(key = "second", name = "Second", type = PropertyType.INTEGER, indicativeSize = 5)}))
  static class WithPropertySet {
  }

  @Test
  public void should_support_property_sets() {
    Properties props = AnnotationUtils.getAnnotation(WithPropertySet.class, Properties.class);
    Property prop = props.value()[0];

    PropertyDefinition def = PropertyDefinition.create(prop);

    assertThat(def.getFields()).hasSize(2);
    assertThat(def.getFields().get(0).getKey()).isEqualTo("first");
    assertThat(def.getFields().get(0).getName()).isEqualTo("First");
    assertThat(def.getFields().get(0).getDescription()).isEqualTo("Description");
    assertThat(def.getFields().get(0).getType()).isEqualTo(PropertyType.STRING);
    assertThat(def.getFields().get(0).getOptions()).containsOnly("A", "B");
    assertThat(def.getFields().get(0).getIndicativeSize()).isEqualTo(20);
    assertThat(def.getFields().get(1).getKey()).isEqualTo("second");
    assertThat(def.getFields().get(1).getName()).isEqualTo("Second");
    assertThat(def.getFields().get(1).getType()).isEqualTo(PropertyType.INTEGER);
    assertThat(def.getFields().get(1).getOptions()).isEmpty();
    assertThat(def.getFields().get(1).getIndicativeSize()).isEqualTo(5);
  }

  @Properties(@Property(key = "hello", name = "Hello"))
  static class DefaultValues {
  }

  @Test
  public void validate_string() {
    PropertyDefinition def = PropertyDefinition.create("foo", PropertyType.STRING, new String[0]);

    assertThat(def.validate(null).isValid()).isTrue();
    assertThat(def.validate("").isValid()).isTrue();
    assertThat(def.validate("   ").isValid()).isTrue();
    assertThat(def.validate("foo").isValid()).isTrue();
  }

  @Test
  public void validate_boolean() {
    PropertyDefinition def = PropertyDefinition.create("foo", PropertyType.BOOLEAN, new String[0]);

    assertThat(def.validate(null).isValid()).isTrue();
    assertThat(def.validate("").isValid()).isTrue();
    assertThat(def.validate("   ").isValid()).isTrue();
    assertThat(def.validate("true").isValid()).isTrue();
    assertThat(def.validate("false").isValid()).isTrue();

    assertThat(def.validate("foo").isValid()).isFalse();
    assertThat(def.validate("foo").getErrorKey()).isEqualTo("notBoolean");
  }

  @Test
  public void validate_integer() {
    PropertyDefinition def = PropertyDefinition.create("foo", PropertyType.INTEGER, new String[0]);

    assertThat(def.validate(null).isValid()).isTrue();
    assertThat(def.validate("").isValid()).isTrue();
    assertThat(def.validate("   ").isValid()).isTrue();
    assertThat(def.validate("123456").isValid()).isTrue();

    assertThat(def.validate("foo").isValid()).isFalse();
    assertThat(def.validate("foo").getErrorKey()).isEqualTo("notInteger");
  }

  @Test
  public void validate_float() {
    PropertyDefinition def = PropertyDefinition.create("foo", PropertyType.FLOAT, new String[0]);

    assertThat(def.validate(null).isValid()).isTrue();
    assertThat(def.validate("").isValid()).isTrue();
    assertThat(def.validate("   ").isValid()).isTrue();
    assertThat(def.validate("123456").isValid()).isTrue();
    assertThat(def.validate("3.14").isValid()).isTrue();

    assertThat(def.validate("foo").isValid()).isFalse();
    assertThat(def.validate("foo").getErrorKey()).isEqualTo("notFloat");
  }

  @Test
  public void validate_single_select_list() {
    PropertyDefinition def = PropertyDefinition.create("foo", PropertyType.SINGLE_SELECT_LIST, new String[]{"de", "en"});

    assertThat(def.validate(null).isValid()).isTrue();
    assertThat(def.validate("").isValid()).isTrue();
    assertThat(def.validate("   ").isValid()).isTrue();
    assertThat(def.validate("de").isValid()).isTrue();
    assertThat(def.validate("en").isValid()).isTrue();

    assertThat(def.validate("fr").isValid()).isFalse();
    assertThat(def.validate("fr").getErrorKey()).isEqualTo("notInOptions");
  }

  @Properties(@Property(key = "scm.password.secured", name = "SCM password"))
  static class OldScmPlugin {
  }

  @Test
  public void autoDetectPasswordType() {
    Properties props = AnnotationUtils.getAnnotation(OldScmPlugin.class, Properties.class);
    Property prop = props.value()[0];

    PropertyDefinition def = PropertyDefinition.create(prop);

    assertThat(def.getKey()).isEqualTo("scm.password.secured");
    assertThat(def.getType()).isEqualTo(PropertyType.PASSWORD);
  }

  @Properties(@Property(key = "views.license.secured", name = "Views license"))
  static class ViewsPlugin {
  }

  @Test
  public void autoDetectLicenseType() {
    Properties props = AnnotationUtils.getAnnotation(ViewsPlugin.class, Properties.class);
    Property prop = props.value()[0];

    PropertyDefinition def = PropertyDefinition.create(prop);

    assertThat(def.getKey()).isEqualTo("views.license.secured");
    assertThat(def.getType()).isEqualTo(PropertyType.LICENSE);
  }
}
