/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component.Status;

import static com.google.common.base.Strings.repeat;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ComponentImpl.builder;

public class ComponentImplTest {

  static final String KEY = "KEY";
  static final String UUID = "UUID";


  @Test
  public void verify_key_uuid_and_name() {
    ComponentImpl component = buildSimpleComponent(FILE, KEY).setUuid(UUID).setName("name").build();

    assertThat(component.getKey()).isEqualTo(KEY);
    assertThat(component.getUuid()).isEqualTo(UUID);
    assertThat(component.getName()).isEqualTo("name");
  }

  @Test
  public void builder_throws_NPE_if_component_arg_is_Null() {
    assertThatThrownBy(() -> builder(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void builder_throws_NPE_if_status_arg_is_Null() {
    assertThatThrownBy(() -> builder(FILE).setStatus(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void builder_throws_NPE_if_status_is_Null() {
    assertThatThrownBy(() -> {
      builder(Component.Type.DIRECTORY)
        .setName("DIR")
        .setKey(KEY)
        .setUuid(UUID)
        .setReportAttributes(ReportAttributes.newBuilder(1).build())
        .build();
    })
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void set_key_throws_NPE_if_component_arg_is_Null() {
    assertThatThrownBy(() -> builder(FILE).setUuid(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void set_uuid_throws_NPE_if_component_arg_is_Null() {
    assertThatThrownBy(() -> builder(FILE).setKey(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void build_without_key_throws_NPE_if_component_arg_is_Null() {
    assertThatThrownBy(() -> builder(FILE).setUuid("ABCD").build())
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void build_without_uuid_throws_NPE_if_component_arg_is_Null() {
    assertThatThrownBy(() -> builder(FILE).setKey(KEY).build())
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void get_name_from_batch_component() {
    String name = "project";
    ComponentImpl component = buildSimpleComponent(FILE, "file").setName(name).build();
    assertThat(component.getName()).isEqualTo(name);
  }

  @Test
  public void getFileAttributes_throws_ISE_if_BatchComponent_does_not_have_type_FILE() {
    Arrays.stream(Component.Type.values())
      .filter(type -> type != FILE)
      .forEach((componentType) -> {
        ComponentImpl component = buildSimpleComponent(componentType, componentType.name()).build();
        try {
          component.getFileAttributes();
          fail("A IllegalStateException should have been raised");
        } catch (IllegalStateException e) {
          assertThat(e).hasMessage("Only component of type FILE have a FileAttributes object");
        }
      });
  }

  @Test
  public void getSubViewAttributes_throws_ISE_if_component_is_not_have_type_SUBVIEW() {
    Arrays.stream(Component.Type.values())
      .filter(type -> type != FILE)
      .forEach((componentType) -> {
        ComponentImpl component = buildSimpleComponent(componentType, componentType.name()).build();
        try {
          component.getSubViewAttributes();
          fail("A IllegalStateException should have been raised");
        } catch (IllegalStateException e) {
          assertThat(e).hasMessage("Only component of type SUBVIEW have a SubViewAttributes object");
        }
      });
  }

  @Test
  public void getViewAttributes_throws_ISE_if_component_is_not_have_type_VIEW() {
    Arrays.stream(Component.Type.values())
      .filter(type -> type != FILE)
      .forEach((componentType) -> {
        ComponentImpl component = buildSimpleComponent(componentType, componentType.name()).build();
        try {
          component.getViewAttributes();
          fail("A IllegalStateException should have been raised");
        } catch (IllegalStateException e) {
          assertThat(e).hasMessage("Only component of type VIEW have a ViewAttributes object");
        }
      });
  }

  @Test
  public void isUnitTest_returns_true_if_IsTest_is_set_in_BatchComponent() {
    ComponentImpl component = buildSimpleComponent(FILE, "file").setFileAttributes(new FileAttributes(true, null, 1)).build();

    assertThat(component.getFileAttributes().isUnitTest()).isTrue();
  }

  @Test
  public void isUnitTest_returns_value_of_language_of_BatchComponent() {
    String languageKey = "some language key";
    ComponentImpl component = buildSimpleComponent(FILE, "file").setFileAttributes(new FileAttributes(false, languageKey, 1)).build();

    assertThat(component.getFileAttributes().getLanguageKey()).isEqualTo(languageKey);
  }

  @Test
  public void keep_500_first_characters_of_name() {
    String veryLongString = repeat("a", 3_000);

    ComponentImpl underTest = buildSimpleComponent(FILE, "file")
      .setName(veryLongString)
      .build();

    String expectedName = repeat("a", 500 - 3) + "...";
    assertThat(underTest.getName()).isEqualTo(expectedName);
  }

  @Test
  public void keep_2000_first_characters_of_description() {
    String veryLongString = repeat("a", 3_000);

    ComponentImpl underTest = buildSimpleComponent(FILE, "file")
      .setDescription(veryLongString)
      .build();

    String expectedDescription = repeat("a", 2_000 - 3) + "...";
    assertThat(underTest.getDescription()).isEqualTo(expectedDescription);
  }

  @Test
  public void build_with_child() {
    ComponentImpl child = builder(FILE)
      .setName("CHILD_NAME")
      .setKey("CHILD_KEY")
      .setUuid("CHILD_UUID")
      .setStatus(Status.UNAVAILABLE)
      .setReportAttributes(ReportAttributes.newBuilder(2).build())
      .build();
    ComponentImpl componentImpl = builder(Component.Type.DIRECTORY)
      .setName("DIR")
      .setKey(KEY)
      .setUuid(UUID)
      .setStatus(Status.UNAVAILABLE)
      .setReportAttributes(ReportAttributes.newBuilder(1).build())
      .addChildren(Collections.singletonList(child))
      .build();

    assertThat(componentImpl.getChildren()).hasSize(1);
    Component childReloaded = componentImpl.getChildren().iterator().next();
    assertThat(childReloaded.getKey()).isEqualTo("CHILD_KEY");
    assertThat(childReloaded.getUuid()).isEqualTo("CHILD_UUID");
    assertThat(childReloaded.getType()).isEqualTo(FILE);
  }

  @Test
  public void equals_compares_on_uuid_only() {
    ComponentImpl.Builder builder = buildSimpleComponent(FILE, "1").setUuid(UUID);

    assertThat(builder.build()).isEqualTo(builder.build());
    assertThat(builder.build()).isEqualTo(buildSimpleComponent(FILE, "2").setUuid(UUID).build());
    assertThat(builder.build()).isNotEqualTo(buildSimpleComponent(FILE, "1").setUuid("otherUUid").build());
  }

  @Test
  public void hashCode_is_hashcode_of_uuid() {
    ComponentImpl.Builder builder = buildSimpleComponent(FILE, "1").setUuid(UUID);

    assertThat(builder.build()).hasSameHashCodeAs(builder.build().hashCode());
    assertThat(builder.build()).hasSameHashCodeAs(buildSimpleComponent(FILE, "2").setUuid(UUID).build().hashCode());
    assertThat(builder.build()).hasSameHashCodeAs(UUID.hashCode());
  }

  private static ComponentImpl.Builder buildSimpleComponent(Component.Type type, String dbKey) {
    ComponentImpl.Builder builder = builder(type)
      .setName("name_" + dbKey)
      .setKey(dbKey)
      .setStatus(Status.UNAVAILABLE)
      .setUuid("uuid_" + dbKey)
      .setReportAttributes(ReportAttributes.newBuilder(dbKey.hashCode()).build());
    if (type == PROJECT) {
      String buildString = secure().nextAlphabetic(15);
      builder.setProjectAttributes(new ProjectAttributes("version_1", buildString, "453def"));
    }
    return builder;
  }
}
