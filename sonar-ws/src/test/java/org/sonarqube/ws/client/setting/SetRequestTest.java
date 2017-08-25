/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.setting;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class SetRequestTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  SetRequest.Builder underTest = SetRequest.builder();

  @Test
  public void create_set_request() {
    SetRequest result = underTest.setKey("my.key").setValue("my value").build();

    assertThat(result.getKey()).isEqualTo("my.key");
    assertThat(result.getValue()).isEqualTo("my value");
    assertThat(result.getValues()).isNotNull().isEmpty();
    assertThat(result.getComponent()).isNull();
    assertThat(result.getBranch()).isNull();
  }

  @Test
  public void create_request_with_component_key() {
    SetRequest result = underTest.setKey("my.key").setValue("my value").setComponent("projectKey").build();

    assertThat(result.getKey()).isEqualTo("my.key");
    assertThat(result.getValue()).isEqualTo("my value");
    assertThat(result.getComponent()).isEqualTo("projectKey");
    assertThat(result.getBranch()).isNull();
  }

  @Test
  public void create_request_with_component_and_branch() {
    SetRequest result = underTest.setKey("my.key").setValue("my value").setComponent("projectKey").setBranch("my_branch").build();

    assertThat(result.getKey()).isEqualTo("my.key");
    assertThat(result.getValue()).isEqualTo("my value");
    assertThat(result.getComponent()).isEqualTo("projectKey");
    assertThat(result.getBranch()).isEqualTo("my_branch");
  }

  @Test
  public void fail_when_empty_key() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Setting key is mandatory and must not be empty");

    underTest
      .setKey("")
      .setValue("value")
      .build();
  }

  @Test
  public void fail_when_values_is_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Setting values must not be null");

    underTest.setKey("my.key").setValues(null).build();
  }
}
