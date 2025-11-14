/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce;

import java.util.Properties;
import org.junit.Test;
import org.sonar.ce.container.ComputeEngineContainer;
import org.sonar.ce.container.ComputeEngineStatus;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ComputeEngineImplTest {

  private final ComputeEngineContainer computeEngineContainer = new NoOpComputeEngineContainer();
  private final ComputeEngine underTest = new ComputeEngineImpl(new Props(new Properties()), computeEngineContainer);

  @Test
  public void startup_throws_ISE_when_called_twice() {
    underTest.startup();

    assertThatThrownBy(underTest::startup)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("startup() can not be called multiple times");
  }

  @Test
  public void stopProcessing_throws_ISE_if_startup_was_not_called_before() {
    assertThatThrownBy(underTest::stopProcessing)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("stopProcessing() must not be called before startup()");
  }

  @Test
  public void stopProcessing_throws_ISE_if_called_after_shutdown() {
    underTest.startup();
    underTest.shutdown();

    assertThatThrownBy(underTest::stopProcessing)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("stopProcessing() can not be called after shutdown()");
  }

  @Test
  public void stopProcessing_throws_ISE_if_called_twice() {
    underTest.startup();
    underTest.stopProcessing();

    assertThatThrownBy(underTest::stopProcessing)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("stopProcessing() can not be called multiple times");
  }

  @Test
  public void shutdown_throws_ISE_if_startup_was_not_called_before() {
    assertThatThrownBy(underTest::shutdown)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("shutdown() must not be called before startup()");
  }

  @Test
  public void shutdown_throws_ISE_if_called_twice() {
    underTest.startup();
    underTest.shutdown();

    assertThatThrownBy(underTest::shutdown)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("shutdown() can not be called multiple times");
  }

  private static class NoOpComputeEngineContainer implements ComputeEngineContainer {
    @Override
    public void setComputeEngineStatus(ComputeEngineStatus computeEngineStatus) {
    }

    @Override
    public ComputeEngineContainer start(Props props) {
      return this;
    }

    @Override
    public ComputeEngineContainer stopWorkers() {
      return this;
    }

    @Override
    public ComputeEngineContainer stop() {
      return this;
    }
  }
}
