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
package org.sonar.application.command;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.process.ProcessId;
import org.sonar.process.System2;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class AbstractCommandTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void constructor_throws_NPE_of_ProcessId_is_null() throws IOException {
    assertThatThrownBy(() -> {
      new AbstractCommand<AbstractCommand>(null, temp.newFolder(), System2.INSTANCE) {

      };
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("ProcessId can't be null");
  }

  @Test
  public void constructor_throws_NPE_of_workDir_is_null() {
    assertThatThrownBy(() -> {
      new AbstractCommand<AbstractCommand>(ProcessId.WEB_SERVER, null, System2.INSTANCE) {

      };
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("workDir can't be null");
  }

  @Test
  public void setEnvVariable_fails_with_NPE_if_key_is_null() throws IOException {
    File workDir = temp.newFolder();
    AbstractCommand underTest = new AbstractCommand(ProcessId.ELASTICSEARCH, workDir, System2.INSTANCE) {

    };

    assertThatThrownBy(() -> underTest.setEnvVariable(null, randomAlphanumeric(30)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("key can't be null");
  }

  @Test
  public void setEnvVariable_fails_with_NPE_if_value_is_null() throws IOException {
    File workDir = temp.newFolder();
    AbstractCommand underTest = new AbstractCommand(ProcessId.ELASTICSEARCH, workDir, System2.INSTANCE) {

    };

    assertThatThrownBy(() -> underTest.setEnvVariable(randomAlphanumeric(30), null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("value can't be null");
  }

  @Test
  public void constructor_puts_System_getEnv_into_map_of_env_variables() throws IOException {
    File workDir = temp.newFolder();
    System2 system2 = Mockito.mock(System2.class);
    Map<String, String> env = IntStream.range(0, 1 + new Random().nextInt(99)).mapToObj(String::valueOf).collect(Collectors.toMap(i -> "key" + i, j -> "value" + j));
    when(system2.getenv()).thenReturn(env);
    AbstractCommand underTest = new AbstractCommand(ProcessId.ELASTICSEARCH, workDir, system2) {

    };

    assertThat(underTest.getEnvVariables()).isEqualTo(env);
  }

  @Test
  public void suppressEnvVariable_remove_existing_env_variable_and_add_variable_to_set_of_suppressed_variables() throws IOException {
    File workDir = temp.newFolder();
    System2 system2 = Mockito.mock(System2.class);
    Map<String, String> env = new HashMap<>();
    String key1 = randomAlphanumeric(3);
    env.put(key1, randomAlphanumeric(9));
    when(system2.getenv()).thenReturn(env);
    AbstractCommand underTest = new AbstractCommand(ProcessId.ELASTICSEARCH, workDir, system2) {

    };

    underTest.suppressEnvVariable(key1);

    assertThat(underTest.getEnvVariables()).doesNotContainKey(key1);
    assertThat(underTest.getSuppressedEnvVariables()).containsOnly(key1);
  }

}
