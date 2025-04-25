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
package org.sonar.issue.workflow.statemachine;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StateTest {

  private static class WfEntity {
  }

  private static class WfEntityActions {
  }

  private final Transition<WfEntity, WfEntityActions> t1 = Transition.<WfEntity, WfEntityActions>builder("close").from("OPEN").to("CLOSED").build();

  @Test
  void key_should_be_set() {
    List<Transition<WfEntity, WfEntityActions>> transitions = List.of();
    assertThatThrownBy(() -> new State<>("", transitions))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("State key must be set");
  }

  @Test
  void no_duplicated_out_transitions() {
    var transitions = List.of(t1, t1);
    assertThatThrownBy(() -> new State<>("CLOSE", transitions))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Transition 'close' is declared several times from the originating state 'CLOSE'");
  }

  @Test
  void fail_when_transition_is_unknown() {
    State<WfEntity, WfEntityActions> state = new State<>("VALIDATED", List.of());

    assertThatThrownBy(() -> state.transition("Unknown Transition"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Transition from state VALIDATED does not exist: Unknown Transition");
  }
}
