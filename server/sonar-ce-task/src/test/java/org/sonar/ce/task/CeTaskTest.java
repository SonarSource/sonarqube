/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class CeTaskTest {

  private CeTask.Builder underTest = new CeTask.Builder();

  @Test
  @UseDataProvider("oneAndOnlyOneOfComponentAndEntity")
  public void build_fails_with_IAE_if_only_one_of_component_and_main_component_is_non_null(CeTask.Component component, CeTask.Component entity) {
    underTest.setType("TYPE_1");
    underTest.setUuid("UUID_1");
    underTest.setComponent(component);
    underTest.setEntity(entity);

    assertThatThrownBy(() -> underTest.build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("None or both component and entity must be non null");
  }

  @DataProvider
  public static Object[][] oneAndOnlyOneOfComponentAndEntity() {
    CeTask.Component component = new CeTask.Component("COMPONENT_UUID_1", "COMPONENT_KEY_1", "The component");
    return new Object[][] {
      {component, null},
      {null, component}
    };
  }

  @Test
  public void verify_getters() {
    CeTask.Component component = new CeTask.Component("COMPONENT_UUID_1", "COMPONENT_KEY_1", "The component");
    CeTask.Component entity = new CeTask.Component("ENTITY_UUID_1", "ENTITY_KEY_1", "The entity");
    CeTask.User submitter = new CeTask.User("UUID_USER_1", "LOGIN_1");
    underTest.setType("TYPE_1");
    underTest.setUuid("UUID_1");
    underTest.setSubmitter(submitter);
    underTest.setComponent(component);
    underTest.setEntity(entity);
    underTest.setCharacteristics(ImmutableMap.of("k1", "v1", "k2", "v2"));

    CeTask task = underTest.build();

    assertThat(task.getUuid()).isEqualTo("UUID_1");
    assertThat(task.getType()).isEqualTo("TYPE_1");
    assertThat(task.getSubmitter()).isEqualTo(submitter);
    assertThat(task.getComponent()).contains(component);
    assertThat(task.getEntity()).contains(entity);
    assertThat(task.getCharacteristics())
      .hasSize(2)
      .containsEntry("k1", "v1")
      .containsEntry("k2", "v2");
  }

  @Test
  public void verify_toString() {
    CeTask.Component component = new CeTask.Component("COMPONENT_UUID_1", "COMPONENT_KEY_1", "The component");
    CeTask.Component entity = new CeTask.Component("ENTITY_UUID_1", "ENTITY_KEY_1", "The entity");
    underTest.setType("TYPE_1");
    underTest.setUuid("UUID_1");
    underTest.setComponent(component);
    underTest.setEntity(entity);
    underTest.setSubmitter(new CeTask.User("UUID_USER_1", "LOGIN_1"));
    underTest.setCharacteristics(ImmutableMap.of("k1", "v1", "k2", "v2"));

    CeTask task = underTest.build();
    System.out.println(task.toString());

    assertThat(task).hasToString("CeTask{" +
      "type=TYPE_1, " +
      "uuid=UUID_1, " +
      "component=Component{uuid='COMPONENT_UUID_1', key='COMPONENT_KEY_1', name='The component'}, " +
      "entity=Component{uuid='ENTITY_UUID_1', key='ENTITY_KEY_1', name='The entity'}, " +
      "submitter=User{uuid='UUID_USER_1', login='LOGIN_1'}" +
      "}");
  }

  @Test
  public void empty_in_submitterLogin_is_considered_as_null() {
    CeTask ceTask = underTest.setUuid("uuid").setType("type")
      .setSubmitter(new CeTask.User("USER_ID", ""))
      .build();

    assertThat(ceTask.getSubmitter().login()).isNull();
  }

  @Test
  public void equals_and_hashCode_on_uuid() {
    underTest.setType("TYPE_1").setUuid("UUID_1");
    CeTask task1 = underTest.build();
    CeTask task1bis = underTest.build();
    CeTask task2 = new CeTask.Builder().setType("TYPE_1").setUuid("UUID_2").build();

    assertThat(task1.equals(task1)).isTrue();
    assertThat(task1.equals(task1bis)).isTrue();
    assertThat(task1.equals(task2)).isFalse();
    assertThat(task1)
      .hasSameHashCodeAs(task1)
      .hasSameHashCodeAs(task1bis);
  }

  @Test
  public void setCharacteristics_null_is_considered_as_empty() {
    CeTask task = underTest.setType("TYPE_1").setUuid("UUID_1")
      .setCharacteristics(null)
      .build();
    assertThat(task.getCharacteristics()).isEmpty();
  }

  @Test
  public void verify_submitter_getters() {
    CeTask.User user = new CeTask.User("UUID", "LOGIN");

    assertThat(user.uuid()).isEqualTo("UUID");
    assertThat(user.login()).isEqualTo("LOGIN");
  }

  @Test
  public void submitter_equals_and_hashCode_on_uuid() {
    CeTask.User user1 = new CeTask.User("UUID_1", null);
    CeTask.User user1bis = new CeTask.User("UUID_1", null);
    CeTask.User user2 = new CeTask.User("UUID_2", null);
    CeTask.User user1_diff_login = new CeTask.User("UUID_1", "LOGIN");

    assertThat(user1.equals(null)).isFalse();
    assertThat(user1)
      .isEqualTo(user1)
      .isEqualTo(user1bis)
      .isNotEqualTo(user2)
      .hasSameHashCodeAs(user1)
      .hasSameHashCodeAs(user1bis)
      .hasSameHashCodeAs(user1_diff_login);
  }
}
