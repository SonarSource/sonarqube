/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.utils.command;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;


public class CommandTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldFailWhenBlankExecutable() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    Command.create("  ");
  }

  @Test
  public void shouldFailWhenNullExecutable() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    Command.create(null);
  }

  @Test
  public void shouldCreateCommand() throws Exception {
    Command command = Command.create("java");
    command.addArgument("-Xmx512m");
    command.addArguments(Arrays.asList("-a", "-b"));
    command.addArguments(new String[]{"-x", "-y"});
    assertThat(command.getExecutable()).isEqualTo("java");
    assertThat(command.getArguments()).hasSize(5);
    assertThat(command.toCommandLine()).isEqualTo("java -Xmx512m -a -b -x -y");
  }

  @Test
  public void toString_is_the_command_line() {
    Command command = Command.create("java");
    command.addArgument("-Xmx512m");
    assertThat(command.toString()).isEqualTo(command.toCommandLine());
  }

  @Test
  public void shouldSetWorkingDirectory() throws Exception {
    Command command = Command.create("java");
    assertThat(command.getDirectory()).isNull();

    File working = new File("working");
    command = Command.create("java").setDirectory(working);
    assertThat(command.getDirectory()).isEqualTo(working);
  }

  @Test
  public void initialize_with_current_env() throws Exception {
    Command command = Command.create("java");
    assertThat(command.getEnvironmentVariables()).isNotEmpty();
  }

  @Test
  public void override_env_variables() throws Exception {
    Command command = Command.create("java");
    command.setEnvironmentVariable("JAVA_HOME", "/path/to/java");
    assertThat(command.getEnvironmentVariables().get("JAVA_HOME")).isEqualTo("/path/to/java");
  }
}
