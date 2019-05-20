/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.core.platform;

import java.io.Closeable;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.Startable;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class StartableCloseableSafeLifecyleStrategyTest {
  @Rule
  public LogTester logTester = new LogTester();

  private StartableCloseableSafeLifecyleStrategy underTest = new StartableCloseableSafeLifecyleStrategy();

  @Test
  public void start_calls_start_on_Startable_subclass() {
    Startable startable = mock(Startable.class);

    underTest.start(startable);

    verify(startable).start();
    verifyNoMoreInteractions(startable);
  }

  @Test
  public void start_calls_start_on_api_Startable_subclass() {
    org.picocontainer.Startable startable = mock(org.picocontainer.Startable.class);

    underTest.start(startable);

    verify(startable).start();
    verifyNoMoreInteractions(startable);
  }

  @Test
  public void start_does_not_call_stop_on_class_with_method_start_not_implementing_startable() {
    Object startable = spy(new Object() {
      public void start() {
        // nothing to do
      }
    });

    underTest.start(startable);

    verifyNoMoreInteractions(startable);
  }

  @Test
  public void stop_calls_stop_on_Startable_subclass() {
    Startable startable = mock(Startable.class);

    underTest.stop(startable);

    verify(startable).stop();
    verifyNoMoreInteractions(startable);
  }

  @Test
  public void stop_calls_stop_on_api_Startable_subclass() {
    org.picocontainer.Startable startable = mock(org.picocontainer.Startable.class);

    underTest.stop(startable);

    verify(startable).stop();
    verifyNoMoreInteractions(startable);
  }

  @Test
  public void stop_does_not_call_stop_on_class_with_method_stop_not_implementing_startable() {
    Object startable = spy(new Object() {
      public void stop() {
        // nothing to do
      }
    });

    underTest.stop(startable);

    verifyNoMoreInteractions(startable);
  }

  @Test
  public void dispose_calls_close_on_Closeable_subclass() throws IOException {
    Closeable closeable = mock(Closeable.class);

    underTest.dispose(closeable);

    verify(closeable).close();
    verifyNoMoreInteractions(closeable);
  }

  @Test
  public void dispose_calls_close_on_AutoCloseable_subclass() throws Exception {
    AutoCloseable autoCloseable = mock(AutoCloseable.class);

    underTest.dispose(autoCloseable);

    verify(autoCloseable).close();
    verifyNoMoreInteractions(autoCloseable);
  }

  @Test
  public void dispose_does_not_call_close_on_class_with_method_close_not_implementing_Closeable_nor_AutoCloseable() {
    Object closeable = spy(new Object() {
      public void close() {
        // nothing to do
      }
    });

    underTest.dispose(closeable);

    verifyNoMoreInteractions(closeable);
  }

  @Test
  public void hasLifecycle_returns_true_on_Startable_and_subclass() {
    Startable startable = mock(Startable.class);

    assertThat(underTest.hasLifecycle(Startable.class)).isTrue();
    assertThat(underTest.hasLifecycle(startable.getClass())).isTrue();
  }

  @Test
  public void hasLifecycle_returns_true_on_api_Startable_and_subclass() {
    org.picocontainer.Startable startable = mock(org.picocontainer.Startable.class);

    assertThat(underTest.hasLifecycle(org.picocontainer.Startable.class)).isTrue();
    assertThat(underTest.hasLifecycle(startable.getClass())).isTrue();
  }

  @Test
  public void hasLifecycle_returns_true_on_api_Closeable_and_subclass() {
    Closeable closeable = mock(Closeable.class);

    assertThat(underTest.hasLifecycle(Closeable.class)).isTrue();
    assertThat(underTest.hasLifecycle(closeable.getClass())).isTrue();
  }

  @Test
  public void hasLifecycle_returns_true_on_api_AutoCloseable_and_subclass() {
    AutoCloseable autoCloseable = mock(AutoCloseable.class);

    assertThat(underTest.hasLifecycle(AutoCloseable.class)).isTrue();
    assertThat(underTest.hasLifecycle(autoCloseable.getClass())).isTrue();
  }

  @Test
  public void hasLifeCycle_returns_false_and_log_a_warning_for_type_defining_start_without_implementating_Startable() {
    Object startable = new Object() {
      public void start() {
        // nothing to do
      }
    };

    assertThat(underTest.hasLifecycle(startable.getClass())).isFalse();
    verifyWarnLog(startable.getClass());
  }

  @Test
  public void hasLifeCycle_returns_false_and_log_a_warning_for_type_defining_stop_without_implementating_Startable() {
    Object startable = new Object() {
      public void stop() {
        // nothing to do
      }
    };

    assertThat(underTest.hasLifecycle(startable.getClass())).isFalse();
    verifyWarnLog(startable.getClass());
  }

  private void verifyWarnLog(Class<?> type) {
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("Component of type class " + type.getName() + " defines methods start() and/or stop(). Neither will be invoked to start/stop the component. " +
        "Please implement either org.picocontainer.Startable or org.sonar.api.Startable");
  }

  @Test
  public void hasLifeCycle_returns_false_and_log_a_warning_for_type_defining_close_without_implementating_Closeable_nor_AutoCloseable() {
    Object startable = new Object() {
      public void close() {
        // nothing to do
      }
    };

    assertThat(underTest.hasLifecycle(startable.getClass())).isFalse();
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN))
      .contains("Component of type class " + startable.getClass().getName() + " defines method close(). It won't be invoked to dispose the component. " +
        "Please implement either java.io.Closeable or java.lang.AutoCloseable");
  }
}
