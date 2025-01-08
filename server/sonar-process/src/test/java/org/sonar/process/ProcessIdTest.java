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
package org.sonar.process;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProcessIdTest {


  @Test
  public void test_constants() {
    assertThat(ProcessId.COMPUTE_ENGINE.getKey()).isEqualTo("ce");
    assertThat(ProcessId.COMPUTE_ENGINE.getIpcIndex()).isEqualTo(3);
  }

  @Test
  public void all_values_are_unique() {
    Set<Integer> ipcIndices = new HashSet<>();
    Set<String> keys = new HashSet<>();
    Set<String> humanReadableNames = new HashSet<>();
    for (ProcessId processId : ProcessId.values()) {
      ipcIndices.add(processId.getIpcIndex());
      keys.add(processId.getKey());
      humanReadableNames.add(processId.getHumanReadableName());
    }
    assertThat(ipcIndices).hasSize(ProcessId.values().length);
    assertThat(keys).hasSize(ProcessId.values().length);
    assertThat(humanReadableNames).hasSize(ProcessId.values().length);
  }

  @Test
  public void fromKey_searches_process_by_its_key() {
    assertThat(ProcessId.fromKey("app")).isEqualTo(ProcessId.APP);
    assertThat(ProcessId.fromKey("ce")).isEqualTo(ProcessId.COMPUTE_ENGINE);
    assertThat(ProcessId.fromKey("es")).isEqualTo(ProcessId.ELASTICSEARCH);
    assertThat(ProcessId.fromKey("web")).isEqualTo(ProcessId.WEB_SERVER);
  }

  @Test
  public void fromKey_throws_IAE_if_key_is_null() {
    assertThatThrownBy(() -> ProcessId.fromKey(null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Process [null] does not exist");
  }

  @Test
  public void fromKey_throws_IAE_if_key_does_not_exist() {
    assertThatThrownBy(() -> ProcessId.fromKey("foo"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Process [foo] does not exist");
  }

}
