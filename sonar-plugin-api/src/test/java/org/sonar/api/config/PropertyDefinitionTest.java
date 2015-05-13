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
package org.sonar.api.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyField;
import org.sonar.api.PropertyType;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.AnnotationUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyDefinitionTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_override_toString() {
    PropertyDefinition def = PropertyDefinition.builder("hello").build();
    assertThat(def.toString()).isEqualTo("hello");
  }

  @Test
  public void should_create_property() {
    PropertyDefinition def = PropertyDefinition.builder("hello")
      .name("Hello")
      .defaultValue("world")
      .category("categ")
      .options("de", "en")
      .description("desc")
      .type(PropertyType.FLOAT)
      .onlyOnQualifiers(Qualifiers.FILE, Qualifiers.CLASS)
      .multiValues(true)
      .propertySetKey("set")
      .build();

    assertThat(def.key()).isEqualTo("hello");
    assertThat(def.name()).isEqualTo("Hello");
    assertThat(def.defaultValue()).isEqualTo("world");
    assertThat(def.category()).isEqualTo("categ");
    assertThat(def.options()).containsOnly("de", "en");
    assertThat(def.description()).isEqualTo("desc");
    assertThat(def.type()).isEqualTo(PropertyType.FLOAT);
    assertThat(def.global()).isFalse();
    assertThat(def.qualifiers()).containsOnly(Qualifiers.FILE, Qualifiers.CLASS);
    assertThat(def.multiValues()).isTrue();
    assertThat(def.propertySetKey()).isEqualTo("set");
    assertThat(def.fields()).isEmpty();
  }

  @Test
  public void should_create_from_annotation() {
    Properties props = AnnotationUtils.getAnnotation(Init.class, Properties.class);
    Property prop = props.value()[0];

    PropertyDefinition def = PropertyDefinition.create(prop);

    assertThat(def.key()).isEqualTo("hello");
    assertThat(def.name()).isEqualTo("Hello");
    assertThat(def.defaultValue()).isEqualTo("world");
    assertThat(def.category()).isEqualTo("categ");
    assertThat(def.options()).containsOnly("de", "en");
    assertThat(def.description()).isEqualTo("desc");
    assertThat(def.type()).isEqualTo(PropertyType.FLOAT);
    assertThat(def.global()).isFalse();
    assertThat(def.qualifiers()).containsOnly(Qualifiers.PROJECT, Qualifiers.MODULE);
    assertThat(def.multiValues()).isTrue();
    assertThat(def.propertySetKey()).isEqualTo("set");
    assertThat(def.fields()).isEmpty();
  }

  @Test
  public void should_create_hidden_property() {
    PropertyDefinition def = PropertyDefinition.builder("hello")
      .name("Hello")
      .hidden()
      .build();

    assertThat(def.key()).isEqualTo("hello");
    assertThat(def.qualifiers()).isEmpty();
    assertThat(def.global()).isFalse();
  }

  @Test
  public void should_create_property_with_default_values() {
    PropertyDefinition def = PropertyDefinition.builder("hello")
      .name("Hello")
      .build();

    assertThat(def.key()).isEqualTo("hello");
    assertThat(def.name()).isEqualTo("Hello");
    assertThat(def.defaultValue()).isEmpty();
    assertThat(def.category()).isEmpty();
    assertThat(def.options()).isEmpty();
    assertThat(def.description()).isEmpty();
    assertThat(def.type()).isEqualTo(PropertyType.STRING);
    assertThat(def.global()).isTrue();
    assertThat(def.qualifiers()).isEmpty();
    assertThat(def.multiValues()).isFalse();
    assertThat(def.propertySetKey()).isEmpty();
    assertThat(def.fields()).isEmpty();
  }

  @Test
  public void should_create_from_annotation_default_values() {
    Properties props = AnnotationUtils.getAnnotation(DefaultValues.class, Properties.class);
    Property prop = props.value()[0];

    PropertyDefinition def = PropertyDefinition.create(prop);

    assertThat(def.key()).isEqualTo("hello");
    assertThat(def.name()).isEqualTo("Hello");
    assertThat(def.defaultValue()).isEmpty();
    assertThat(def.category()).isEmpty();
    assertThat(def.options()).isEmpty();
    assertThat(def.description()).isEmpty();
    assertThat(def.type()).isEqualTo(PropertyType.STRING);
    assertThat(def.global()).isTrue();
    assertThat(def.qualifiers()).isEmpty();
    assertThat(def.multiValues()).isFalse();
    assertThat(def.propertySetKey()).isEmpty();
    assertThat(def.fields()).isEmpty();
  }

  @Test
  public void should_support_property_sets() {
    PropertyDefinition def = PropertyDefinition.builder("hello")
      .name("Hello")
      .fields(
        PropertyFieldDefinition.build("first").name("First").description("Description").options("A", "B").build(),
        PropertyFieldDefinition.build("second").name("Second").type(PropertyType.INTEGER).indicativeSize(5).build()
      )
      .build();

    assertThat(def.fields()).hasSize(2);
    assertThat(def.fields().get(0).key()).isEqualTo("first");
    assertThat(def.fields().get(0).name()).isEqualTo("First");
    assertThat(def.fields().get(0).description()).isEqualTo("Description");
    assertThat(def.fields().get(0).type()).isEqualTo(PropertyType.STRING);
    assertThat(def.fields().get(0).options()).containsOnly("A", "B");
    assertThat(def.fields().get(0).indicativeSize()).isEqualTo(20);
    assertThat(def.fields().get(1).key()).isEqualTo("second");
    assertThat(def.fields().get(1).name()).isEqualTo("Second");
    assertThat(def.fields().get(1).type()).isEqualTo(PropertyType.INTEGER);
    assertThat(def.fields().get(1).options()).isEmpty();
    assertThat(def.fields().get(1).indicativeSize()).isEqualTo(5);
  }

  @Test
  public void should_support_property_sets_from_annotation() {
    Properties props = AnnotationUtils.getAnnotation(WithPropertySet.class, Properties.class);
    Property prop = props.value()[0];

    PropertyDefinition def = PropertyDefinition.create(prop);

    assertThat(def.fields()).hasSize(2);
    assertThat(def.fields().get(0).key()).isEqualTo("first");
    assertThat(def.fields().get(0).name()).isEqualTo("First");
    assertThat(def.fields().get(0).description()).isEqualTo("Description");
    assertThat(def.fields().get(0).type()).isEqualTo(PropertyType.STRING);
    assertThat(def.fields().get(0).options()).containsOnly("A", "B");
    assertThat(def.fields().get(0).indicativeSize()).isEqualTo(20);
    assertThat(def.fields().get(1).key()).isEqualTo("second");
    assertThat(def.fields().get(1).name()).isEqualTo("Second");
    assertThat(def.fields().get(1).type()).isEqualTo(PropertyType.INTEGER);
    assertThat(def.fields().get(1).options()).isEmpty();
    assertThat(def.fields().get(1).indicativeSize()).isEqualTo(5);
  }

  @Test
  public void should_validate_string() {
    PropertyDefinition def = PropertyDefinition.builder("foo").name("foo").type(PropertyType.STRING).build();

    assertThat(def.validate(null).isValid()).isTrue();
    assertThat(def.validate("").isValid()).isTrue();
    assertThat(def.validate("   ").isValid()).isTrue();
    assertThat(def.validate("foo").isValid()).isTrue();
  }

  @Test
  public void should_validate_boolean() {
    PropertyDefinition def = PropertyDefinition.builder("foo").name("foo").type(PropertyType.BOOLEAN).build();

    assertThat(def.validate(null).isValid()).isTrue();
    assertThat(def.validate("").isValid()).isTrue();
    assertThat(def.validate("   ").isValid()).isTrue();
    assertThat(def.validate("true").isValid()).isTrue();
    assertThat(def.validate("false").isValid()).isTrue();

    assertThat(def.validate("foo").isValid()).isFalse();
    assertThat(def.validate("foo").getErrorKey()).isEqualTo("notBoolean");
  }

  @Test
  public void should_validate_integer() {
    PropertyDefinition def = PropertyDefinition.builder("foo").name("foo").type(PropertyType.INTEGER).build();

    assertThat(def.validate(null).isValid()).isTrue();
    assertThat(def.validate("").isValid()).isTrue();
    assertThat(def.validate("   ").isValid()).isTrue();
    assertThat(def.validate("123456").isValid()).isTrue();

    assertThat(def.validate("foo").isValid()).isFalse();
    assertThat(def.validate("foo").getErrorKey()).isEqualTo("notInteger");
  }

  @Test
  public void should_validate_float() {
    PropertyDefinition def = PropertyDefinition.builder("foo").name("foo").type(PropertyType.FLOAT).build();

    assertThat(def.validate(null).isValid()).isTrue();
    assertThat(def.validate("").isValid()).isTrue();
    assertThat(def.validate("   ").isValid()).isTrue();
    assertThat(def.validate("123456").isValid()).isTrue();
    assertThat(def.validate("3.14").isValid()).isTrue();

    assertThat(def.validate("foo").isValid()).isFalse();
    assertThat(def.validate("foo").getErrorKey()).isEqualTo("notFloat");
  }

  @Test
  public void should_validate_single_select_list() {
    PropertyDefinition def = PropertyDefinition.builder("foo").name("foo").type(PropertyType.SINGLE_SELECT_LIST).options("de", "en").build();

    assertThat(def.validate(null).isValid()).isTrue();
    assertThat(def.validate("").isValid()).isTrue();
    assertThat(def.validate("   ").isValid()).isTrue();
    assertThat(def.validate("de").isValid()).isTrue();
    assertThat(def.validate("en").isValid()).isTrue();

    assertThat(def.validate("fr").isValid()).isFalse();
    assertThat(def.validate("fr").getErrorKey()).isEqualTo("notInOptions");
  }

  @Test
  public void should_auto_detect_password_type() {
    PropertyDefinition def = PropertyDefinition.builder("scm.password.secured").name("SCM password").build();

    assertThat(def.key()).isEqualTo("scm.password.secured");
    assertThat(def.type()).isEqualTo(PropertyType.PASSWORD);
  }

  @Test
  public void PropertyDef() {
    PropertyDefinition def = PropertyDefinition.builder("views.license.secured").name("Views license").build();

    assertThat(def.key()).isEqualTo("views.license.secured");
    assertThat(def.type()).isEqualTo(PropertyType.LICENSE);
  }

  @Test
  public void should_not_authorise_empty_key() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Key must be set");

    PropertyDefinition.builder(null).build();
  }

  @Test
  public void should_not_authorize_defining_on_qualifiers_and_hidden() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Cannot be hidden and defining qualifiers on which to display");

    PropertyDefinition.builder("foo").name("foo").onQualifiers(Qualifiers.FILE).hidden().build();
  }

  @Test
  public void should_not_authorize_defining_ony_on_qualifiers_and_hidden() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Cannot be hidden and defining qualifiers on which to display");

    PropertyDefinition.builder("foo").name("foo").onlyOnQualifiers(Qualifiers.FILE).hidden().build();
  }

  @Test
  public void should_not_authorize_defining_on_qualifiers_and_only_on_qualifiers() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Cannot define both onQualifiers and onlyOnQualifiers");

    PropertyDefinition.builder("foo").name("foo").onQualifiers(Qualifiers.FILE).onlyOnQualifiers(Qualifiers.PROJECT).build();
  }

  @Properties(@Property(key = "hello", name = "Hello", defaultValue = "world", description = "desc",
    options = {"de", "en"}, category = "categ", type = PropertyType.FLOAT, global = false, project = true, module = true, multiValues = true, propertySetKey = "set"))
  static class Init {
  }

  @Properties(@Property(key = "hello", name = "Hello", fields = {
    @PropertyField(key = "first", name = "First", description = "Description", options = {"A", "B"}),
    @PropertyField(key = "second", name = "Second", type = PropertyType.INTEGER, indicativeSize = 5)}))
  static class WithPropertySet {
  }

  @Properties(@Property(key = "hello", name = "Hello"))
  static class DefaultValues {
  }

}
