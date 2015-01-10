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
package org.sonar.server.db.migrations.v36;

import com.google.common.collect.Lists;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;
import org.sonar.api.config.Settings;

import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViolationConvertersTest {

  @Test
  public void default_number_of_threads() throws Exception {
    assertThat(new ViolationConverters(new Settings()).numberOfThreads()).isEqualTo(ViolationConverters.DEFAULT_THREADS);
  }

  @Test
  public void configure_number_of_threads() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(ViolationConverters.THREADS_PROPERTY, 2);
    assertThat(new ViolationConverters(settings).numberOfThreads()).isEqualTo(2);
  }

  @Test
  public void number_of_threads_should_not_be_negative() throws Exception {
    try {
      Settings settings = new Settings();
      settings.setProperty(ViolationConverters.THREADS_PROPERTY, -2);
      new ViolationConverters(settings).numberOfThreads();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Bad value of " + ViolationConverters.THREADS_PROPERTY + ": -2");
    }
  }

  @Test
  public void propagate_converter_failure() throws Exception {
    Callable<Object> callable = mock(Callable.class);
    when(callable.call()).thenThrow(new IllegalStateException("Need to cry"));

    List<Callable<Object>> callables = Lists.newArrayList(callable);
    try {
      new ViolationConverters(new Settings()).doExecute(new FakeTimerTask(), callables);
      fail();
    } catch (Exception e) {
      assertThat(ExceptionUtils.getRootCause(e).getMessage()).isEqualTo("Need to cry");
    }

  }

  static class FakeTimerTask extends TimerTask {
    @Override
    public void run() {
    }
  }
}
