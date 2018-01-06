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
package org.sonar.ce.queue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class CeTaskTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CeTask.Builder underTest = new CeTask.Builder();

  @Test
  public void build_fails_with_NPE_if_organizationUuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("organizationUuid can't be null nor empty");

    underTest.build();
  }

  @Test
  public void build_fails_with_NPE_if_organizationUuid_is_empty() {
    underTest.setOrganizationUuid("");

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("organizationUuid can't be null nor empty");

    underTest.build();
  }

  @Test
  public void build_fails_with_NPE_if_uid_is_null() {
    underTest.setOrganizationUuid("org1");

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null nor empty");

    underTest.build();
  }

  @Test
  public void build_fails_with_NPE_if_uuid_is_empty() {
    underTest.setOrganizationUuid("org1").setUuid("");

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null nor empty");

    underTest.build();
  }

  @Test
  public void build_fails_with_NPE_if_type_is_null() {
    underTest.setOrganizationUuid("org1").setUuid("uuid");

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("type can't be null nor empty");

    underTest.build();
  }

  @Test
  public void build_fails_with_NPE_if_type_is_empty() {
    underTest.setOrganizationUuid("org1").setUuid("uuid").setType("");

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("type can't be null nor empty");

    underTest.build();
  }

  @Test
  public void verify_getters() {
    underTest.setOrganizationUuid("org1");
    underTest.setType("TYPE_1");
    underTest.setUuid("UUID_1");
    underTest.setSubmitterLogin("LOGIN_1");
    underTest.setComponentKey("COMPONENT_KEY_1");
    underTest.setComponentUuid("COMPONENT_UUID_1");
    underTest.setComponentName("The component");

    CeTask task = underTest.build();

    assertThat(task.getOrganizationUuid()).isEqualTo("org1");
    assertThat(task.getUuid()).isEqualTo("UUID_1");
    assertThat(task.getType()).isEqualTo("TYPE_1");
    assertThat(task.getSubmitterLogin()).isEqualTo("LOGIN_1");
    assertThat(task.getComponentKey()).isEqualTo("COMPONENT_KEY_1");
    assertThat(task.getComponentUuid()).isEqualTo("COMPONENT_UUID_1");
    assertThat(task.getComponentName()).isEqualTo("The component");
  }

  @Test
  public void empty_in_component_properties_is_considered_as_null() {
    CeTask ceTask = underTest.setOrganizationUuid("org1").setUuid("uuid").setType("type")
      .setComponentKey("")
      .setComponentName("")
      .setComponentUuid("")
      .build();

    assertThat(ceTask.getComponentKey()).isNull();
    assertThat(ceTask.getComponentName()).isNull();
    assertThat(ceTask.getComponentUuid()).isNull();
  }

  @Test
  public void empty_in_submitterLogin_is_considered_as_null() {
    CeTask ceTask = underTest.setOrganizationUuid("org1").setUuid("uuid").setType("type")
      .setSubmitterLogin("")
      .build();

    assertThat(ceTask.getSubmitterLogin()).isNull();
  }

  @Test
  public void equals_and_hashCode_on_uuid() {
    underTest.setOrganizationUuid("org1").setType("TYPE_1").setUuid("UUID_1");
    CeTask task1 = underTest.build();
    CeTask task1bis = underTest.build();
    CeTask task2 = new CeTask.Builder().setOrganizationUuid("org1").setType("TYPE_1").setUuid("UUID_2").build();

    assertThat(task1.equals(task1)).isTrue();
    assertThat(task1.equals(task1bis)).isTrue();
    assertThat(task1.equals(task2)).isFalse();
    assertThat(task1.hashCode()).isEqualTo(task1.hashCode());
    assertThat(task1.hashCode()).isEqualTo(task1bis.hashCode());
  }
}
