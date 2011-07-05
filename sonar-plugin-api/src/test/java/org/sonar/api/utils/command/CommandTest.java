/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class CommandTest {

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailWhenBlankExecutable() throws Exception {
    Command.create("  ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailWhenNullExecutable() throws Exception {
    Command.create(null);
  }

  @Test
  public void shouldCreateCommand() throws Exception {
    Command command = Command.create("java");
    command.addArgument("-Xmx512m");
    command.addArgument("-Dfoo=bar");
    assertThat(command.getExecutable(), is("java"));
    assertThat(command.getArguments().size(), is(2));
    assertThat(command.toCommandLine(), is("java -Xmx512m -Dfoo=bar"));
  }

  @Test
  public void shouldSetWorkingDirectory() throws Exception {
    Command command = Command.create("java");
    assertThat(command.getDirectory(), nullValue());

    File working = new File("working");
    command = Command.create("java").setDirectory(working);
    assertThat(command.getDirectory(), is(working));
  }
}
