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
package org.sonar.application;

import org.junit.Test;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AppStateImplTest {

  private AppStateListener listener = mock(AppStateListener.class);
  private AppStateImpl underTest = new AppStateImpl();

  @Test
  public void get_and_set_operational_flag() {
    assertThat(underTest.isOperational(ProcessId.COMPUTE_ENGINE, true)).isFalse();
    assertThat(underTest.isOperational(ProcessId.ELASTICSEARCH, true)).isFalse();
    assertThat(underTest.isOperational(ProcessId.WEB_SERVER, true)).isFalse();

    underTest.setOperational(ProcessId.ELASTICSEARCH);

    assertThat(underTest.isOperational(ProcessId.COMPUTE_ENGINE, true)).isFalse();
    assertThat(underTest.isOperational(ProcessId.ELASTICSEARCH, true)).isTrue();
    assertThat(underTest.isOperational(ProcessId.WEB_SERVER, true)).isFalse();

    // only local mode is supported. App state = local state
    assertThat(underTest.isOperational(ProcessId.COMPUTE_ENGINE, false)).isFalse();
    assertThat(underTest.isOperational(ProcessId.ELASTICSEARCH, false)).isTrue();
    assertThat(underTest.isOperational(ProcessId.WEB_SERVER, false)).isFalse();
  }

  @Test
  public void notify_listeners_when_a_process_becomes_operational() {
    underTest.addListener(listener);

    underTest.setOperational(ProcessId.ELASTICSEARCH);

    verify(listener).onAppStateOperational(ProcessId.ELASTICSEARCH);
    verifyNoMoreInteractions(listener);
  }

  @Test
  public void tryToLockWebLeader_returns_true_if_first_call() {
    assertThat(underTest.tryToLockWebLeader()).isTrue();

    // next calls return false
    assertThat(underTest.tryToLockWebLeader()).isFalse();
    assertThat(underTest.tryToLockWebLeader()).isFalse();
  }

  @Test
  public void reset_initializes_all_flags() {
    underTest.setOperational(ProcessId.ELASTICSEARCH);
    assertThat(underTest.tryToLockWebLeader()).isTrue();

    underTest.reset();

    assertThat(underTest.isOperational(ProcessId.ELASTICSEARCH, true)).isFalse();
    assertThat(underTest.isOperational(ProcessId.COMPUTE_ENGINE, true)).isFalse();
    assertThat(underTest.isOperational(ProcessId.WEB_SERVER, true)).isFalse();
    assertThat(underTest.tryToLockWebLeader()).isTrue();
  }
}
