/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.container.ComputeEngineStatus;
import org.sonar.ce.container.ComputeEngineContainer;
import org.sonar.process.Props;

public class ComputeEngineImplTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ComputeEngineContainer computeEngineContainer = new NoOpComputeEngineContainer();
  private ComputeEngine underTest = new ComputeEngineImpl(new Props(new Properties()), computeEngineContainer);

  @Test
  public void startup_throws_ISE_when_called_twice() {
    underTest.startup();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("startup() can not be called multiple times");

    underTest.startup();
  }

  @Test
  public void shutdown_throws_ISE_if_startup_was_not_called_before() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("shutdown() must not be called before startup()");

    underTest.shutdown();
  }

  @Test
  public void shutdown_throws_ISE_if_called_twice() {
    underTest.startup();
    underTest.shutdown();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("shutdown() can not be called multiple times");

    underTest.shutdown();
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
    public ComputeEngineContainer stop() {
      return this;
    }
  }
}
