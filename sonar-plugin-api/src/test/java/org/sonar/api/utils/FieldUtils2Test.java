/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.utils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FieldUtils2Test {

  @Test
  public void shouldGetFieldsOfSingleClass() {
    List<String> fields = fieldsName(FieldUtils2.getFields(FieldsWithDifferentModifiers.class, true));
    assertThat(fields).contains("publicField");
    assertThat(fields).contains("protectedField");
    assertThat(fields).contains("packageField");
    assertThat(fields).contains("privateField");
    assertThat(fields).contains("publicStaticField");
    assertThat(fields).contains("protectedStaticField");
    assertThat(fields).contains("packageStaticField");
    assertThat(fields).contains("privateStaticField");
  }

  @Test
  public void shouldGetFieldsOfClassHierarchy() {
    List<String> fields = fieldsName(FieldUtils2.getFields(Child.class, true));
    assertThat(fields).contains("publicField");
    assertThat(fields).contains("protectedField");
    assertThat(fields).contains("packageField");
    assertThat(fields).contains("privateField");
    assertThat(fields).contains("publicStaticField");
    assertThat(fields).contains("protectedStaticField");
    assertThat(fields).contains("packageStaticField");
    assertThat(fields).contains("privateStaticField");
    assertThat(fields).contains("childPrivateField");
  }

  @Test
  public void shouldGetOnlyAccessibleFields() {
    List<String> fields = fieldsName(FieldUtils2.getFields(Child.class, false));

    assertThat(fields).contains("publicField");
    assertThat(fields).contains("publicStaticField");
  }

  @Test
  public void shouldGetFieldsOfInterface() {
    List<String> fields = fieldsName(FieldUtils2.getFields(InterfaceWithFields.class, true));

    assertThat(fields).contains("INTERFACE_FIELD");
  }

  @Test
  public void shouldGetFieldsOfInterfaceImplementation() {
    List<String> fields = fieldsName(FieldUtils2.getFields(InterfaceImplementation.class, true));

    assertThat(fields).contains("INTERFACE_FIELD");
  }

  private static List<String> fieldsName(List<Field> fields) {
    return fields.stream().map(f -> f != null ? f.getName() : null).collect(Collectors.toList());
  }

  interface InterfaceWithFields {
    String INTERFACE_FIELD = "foo";
  }

  static class InterfaceImplementation implements InterfaceWithFields {
  }

  static class FieldsWithDifferentModifiers {
    public String publicField;
    protected String protectedField;
    String packageField;
    private String privateField;

    public static String publicStaticField;
    protected static String protectedStaticField;
    static String packageStaticField;
    private static String privateStaticField;
  }

  static class Child extends FieldsWithDifferentModifiers {
    private String childPrivateField;
  }

}
