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
package org.sonar.server.platform;

import org.junit.After;
import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.views.ViewsBridge;

import static com.google.common.base.Preconditions.checkArgument;
import static org.assertj.core.api.Assertions.assertThat;

public class ViewsIntegrationTest {
  private final ViewsBridgeSimulator viewsBridgeSimulator = new ViewsBridgeSimulator();

  private ServerTester serverTester = new ServerTester();

  @After
  public void tearDown() throws Exception {
    serverTester.stop();
  }

  @Test
  public void verify_no_interaction_when_not_added_to_the_server() {
    serverTester.start();

    assertThat(viewsBridgeSimulator.isBootstrapCalled()).isFalse();
    assertThat(viewsBridgeSimulator.getStopCalls()).isEqualTo(0);
    assertThat(viewsBridgeSimulator.getUpdateViewsCalls()).isEqualTo(0);
  }

  @Test
  public void verify_no_interaction_when_startup_tasks_are_disabled() {
    serverTester.addComponents(viewsBridgeSimulator).start();

    assertThat(viewsBridgeSimulator.isBootstrapCalled()).isFalse();
    assertThat(viewsBridgeSimulator.getStopCalls()).isEqualTo(0);
    assertThat(viewsBridgeSimulator.getUpdateViewsCalls()).isEqualTo(0);
  }

  @Test
  public void verify_bootstrapped_only_with_startup_tasks_and_stopped_with_the_server() {
    serverTester.addComponents(viewsBridgeSimulator).withStartupTasks().start();

    assertThat(viewsBridgeSimulator.isBootstrapCalled()).isTrue();
    assertThat(viewsBridgeSimulator.getStopCalls()).isEqualTo(0);
    assertThat(viewsBridgeSimulator.getUpdateViewsCalls()).isEqualTo(0);

    serverTester.stop();

    assertThat(viewsBridgeSimulator.getStopCalls()).isEqualTo(1);
    assertThat(viewsBridgeSimulator.getUpdateViewsCalls()).isEqualTo(0);
  }

  private static class ViewsBridgeSimulator implements ViewsBridge {
    private boolean bootstrapCalled = false;
    private int stopCalls = 0;
    private int updateViewsCalls = 0;

    @Override
    public void startViews(ComponentContainer parent) {
      checkArgument(!bootstrapCalled, "Bootstrap already called");
      this.bootstrapCalled = true;
    }

    @Override
    public void stopViews() {
      this.stopCalls += 1;
    }

    @Override
    public void updateViews() {
      this.updateViewsCalls += 1;
    }

    public boolean isBootstrapCalled() {
      return bootstrapCalled;
    }

    public int getStopCalls() {
      return stopCalls;
    }

    public int getUpdateViewsCalls() {
      return updateViewsCalls;
    }
  }
}
