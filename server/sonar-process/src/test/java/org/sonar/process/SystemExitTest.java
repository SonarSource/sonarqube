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
package org.sonar.process;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemExitTest {

  @Test
  public void do_not_exit_if_in_shutdown_hook() {
    SystemExit systemExit = new SystemExit();

    systemExit.setInShutdownHook();
    assertThat(systemExit.isInShutdownHook()).isTrue();

    systemExit.exit(0);
    // still there
  }

  @Test
  public void exit_if_not_in_shutdown_hook() {
    final AtomicInteger got = new AtomicInteger();
    SystemExit systemExit = new SystemExit() {
      @Override
      void doExit(int code) {
        got.set(code);
      }
    };

    assertThat(systemExit.isInShutdownHook()).isFalse();
    systemExit.exit(1);

    assertThat(got.get()).isEqualTo(1);
  }
}
