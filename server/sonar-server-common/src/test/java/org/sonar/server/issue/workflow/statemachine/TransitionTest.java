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
package org.sonar.server.issue.workflow.statemachine;

import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.sonar.db.permission.ProjectPermission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransitionTest {

  private static class WfEntity {
  }

  private static class WfEntityActions {
    void action1() {
    }

    void action2() {
    }
  }

  Predicate<WfEntity> condition1 = mock();
  Predicate<WfEntity> condition2 = mock();

  @Test
  void test_builder() {
    Transition<WfEntity, WfEntityActions> transition = Transition.<WfEntity, WfEntityActions>builder("close")
      .from("OPEN").to("CLOSED")
      .conditions(condition1, condition2)
      .actions(WfEntityActions::action1, WfEntityActions::action2)
      .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
      .build();
    assertThat(transition.key()).isEqualTo("close");
    assertThat(transition.from()).isEqualTo("OPEN");
    assertThat(transition.to()).isEqualTo("CLOSED");
    assertThat(transition.conditions()).containsOnly(condition1, condition2);
    assertThat(transition.actions()).hasSize(2);
    assertThat(transition.automatic()).isFalse();
    assertThat(transition.requiredProjectPermission()).isEqualTo(ProjectPermission.ISSUE_ADMIN);
  }

  @Test
  void test_simplest_transition() {
    Transition<WfEntity, WfEntityActions> transition = Transition.<WfEntity, WfEntityActions>builder("close")
      .from("OPEN").to("CLOSED")
      .build();
    assertThat(transition.key()).isEqualTo("close");
    assertThat(transition.from()).isEqualTo("OPEN");
    assertThat(transition.to()).isEqualTo("CLOSED");
    assertThat(transition.conditions()).isEmpty();
    assertThat(transition.actions()).isEmpty();
    assertThat(transition.requiredProjectPermission()).isNull();
  }

  @Test
  void key_should_be_set() {
    assertThatThrownBy(() -> Transition.<WfEntity, WfEntityActions>builder("").from("OPEN").to("CLOSED").build())
      .hasMessage("Transition key must be set");
  }

  @Test
  void key_should_be_lower_case() {
    assertThatThrownBy(() -> Transition.<WfEntity, WfEntityActions>builder("CLOSE").from("OPEN").to("CLOSED").build())
      .hasMessage("Transition key must be lower-case");
  }

  @Test
  void originating_status_should_be_set() {
    assertThatThrownBy(() -> Transition.<WfEntity, WfEntityActions>builder("close").from("").to("CLOSED").build())
      .hasMessage("Originating status must be set");
  }

  @Test
  void destination_status_should_be_set() {
    assertThatThrownBy(() -> Transition.<WfEntity, WfEntityActions>builder("close").from("OPEN").to("").build())
      .hasMessage("Destination status must be set");
  }

  @Test
  void should_verify_conditions() {
    WfEntity entity = mock();
    Transition<WfEntity, WfEntityActions> transition = Transition.<WfEntity, WfEntityActions>builder("close")
      .from("OPEN").to("CLOSED")
      .conditions(condition1, condition2)
      .build();

    when(condition1.test(entity)).thenReturn(true);
    when(condition2.test(entity)).thenReturn(false);
    assertThat(transition.supports(entity)).isFalse();

    when(condition1.test(entity)).thenReturn(true);
    when(condition2.test(entity)).thenReturn(true);
    assertThat(transition.supports(entity)).isTrue();
  }

  @Test
  void test_toString() {
    Transition<WfEntity, WfEntityActions> t1 = Transition.create("resolve", "OPEN", "RESOLVED");
    assertThat(t1).hasToString("OPEN->resolve->RESOLVED");
  }

  @Test
  void test_automatic_transition() {
    Transition<WfEntity, WfEntityActions> transition = Transition.<WfEntity, WfEntityActions>builder("close")
      .from("OPEN").to("CLOSED")
      .automatic()
      .build();
    assertThat(transition.automatic()).isTrue();
  }
}
