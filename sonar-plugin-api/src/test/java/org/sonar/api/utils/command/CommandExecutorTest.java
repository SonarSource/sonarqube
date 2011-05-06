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

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.OrderingComparisons.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparisons.lessThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class CommandExecutorTest {

  @Test
  public void shouldEchoArguments() {
    String executable = getScript("echo");
    int exitCode = CommandExecutor.create().execute(Command.create(executable), 1000L);
    assertThat(exitCode, is(0));
  }

  @Test
  public void shouldStopWithTimeout() {
    String executable = getScript("forever");
    long start = System.currentTimeMillis();
    try {
      CommandExecutor.create().execute(Command.create(executable), 300L);
      fail();
    } catch (CommandException e) {
      long duration = System.currentTimeMillis()-start;
      assertThat(e.getMessage(), duration, greaterThanOrEqualTo(300L));
    }
  }

  @Test(expected = CommandException.class)
  public void shouldFailIfScriptNotFound() {
    CommandExecutor.create().execute(Command.create("notfound"), 1000L);
  }

  private String getScript(String name) {
    String filename;
    if (SystemUtils.IS_OS_WINDOWS) {
      filename = name + ".bat";
    } else {
      filename = name + ".sh";
    }
    return new File("src/test/scripts/" + filename).getPath();
  }
}
