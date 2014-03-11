/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Test;
import org.sonar.runner.impl.RunnerException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ExceptionHandlingTest {

  private static final String MESSAGE = "the error message";

  @Test
  public void should_log_message_and_throw_exception() throws Exception {
    Log log = mock(Log.class);
    try {
      ExceptionHandling.handle(MESSAGE, log);
      fail();
    } catch (MojoExecutionException e) {
      assertThat(e.getMessage()).isEqualTo(MESSAGE);
      verify(log).error(MESSAGE);
    }
  }

  @Test
  public void should_log_message_and_rethrow_exception() throws Exception {
    Log log = mock(Log.class);
    IllegalStateException cause = new IllegalStateException(MESSAGE);
    try {
      ExceptionHandling.handle(cause, log);
      fail();
    } catch (MojoExecutionException e) {
      assertThat(e.getMessage()).isEqualTo(MESSAGE);
      assertThat(e.getCause()).isSameAs(cause);
      verify(log).error(MESSAGE);
    }
  }

  @Test
  public void should_hide_sonar_runner_stacktrace() throws Exception {
    Log log = mock(Log.class);
    IllegalStateException cause = new IllegalStateException(MESSAGE);
    try {
      ExceptionHandling.handle(new RunnerException(cause), log);
      fail();
    } catch (MojoExecutionException e) {
      assertThat(e.getMessage()).isEqualTo(MESSAGE);
      assertThat(e.getCause()).isSameAs(cause);
      verify(log).error(MESSAGE);
    }
  }
}
