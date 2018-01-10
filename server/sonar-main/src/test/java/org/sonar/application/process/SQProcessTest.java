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
package org.sonar.application.process;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.mockito.Mockito;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class SQProcessTest {

  private static final ProcessId A_PROCESS_ID = ProcessId.ELASTICSEARCH;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  @Test
  public void initial_state_is_INIT() {
    SQProcess underTest = SQProcess.builder(A_PROCESS_ID).build();

    assertThat(underTest.getProcessId()).isEqualTo(A_PROCESS_ID);
    assertThat(underTest.getState()).isEqualTo(Lifecycle.State.INIT);
  }

  @Test
  public void start_and_stop_process() {
    ProcessLifecycleListener listener = mock(ProcessLifecycleListener.class);
    SQProcess underTest = SQProcess.builder(A_PROCESS_ID)
      .addProcessLifecycleListener(listener)
      .build();

    try (TestProcess testProcess = new TestProcess()) {
      assertThat(underTest.start(() -> testProcess)).isTrue();
      assertThat(underTest.getState()).isEqualTo(Lifecycle.State.STARTED);
      assertThat(testProcess.isAlive()).isTrue();
      assertThat(testProcess.streamsClosed).isFalse();
      verify(listener).onProcessState(A_PROCESS_ID, Lifecycle.State.STARTED);

      testProcess.close();
      // do not wait next run of watcher threads
      underTest.refreshState();
      assertThat(underTest.getState()).isEqualTo(Lifecycle.State.STOPPED);
      assertThat(testProcess.isAlive()).isFalse();
      assertThat(testProcess.streamsClosed).isTrue();
      verify(listener).onProcessState(A_PROCESS_ID, Lifecycle.State.STOPPED);
    }
  }

  @Test
  public void start_does_not_nothing_if_already_started_once() {
    SQProcess underTest = SQProcess.builder(A_PROCESS_ID).build();

    try (TestProcess testProcess = new TestProcess()) {
      assertThat(underTest.start(() -> testProcess)).isTrue();
      assertThat(underTest.getState()).isEqualTo(Lifecycle.State.STARTED);

      assertThat(underTest.start(() -> {throw new IllegalStateException();})).isFalse();
      assertThat(underTest.getState()).isEqualTo(Lifecycle.State.STARTED);
    }
  }

  @Test
  public void start_throws_exception_and_move_to_state_STOPPED_if_execution_of_command_fails() {
    SQProcess underTest = SQProcess.builder(A_PROCESS_ID).build();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("error");

    underTest.start(() -> {throw new IllegalStateException("error");});
    assertThat(underTest.getState()).isEqualTo(Lifecycle.State.STOPPED);
  }

  @Test
  public void send_event_when_process_is_operational() {
    ProcessEventListener listener = mock(ProcessEventListener.class);
    SQProcess underTest = SQProcess.builder(A_PROCESS_ID)
      .addEventListener(listener)
      .build();

    try (TestProcess testProcess = new TestProcess()) {
      underTest.start(() -> testProcess);

      testProcess.operational = true;
      underTest.refreshState();

      verify(listener).onProcessEvent(A_PROCESS_ID, ProcessEventListener.Type.OPERATIONAL);
    }
    verifyNoMoreInteractions(listener);
  }

  @Test
  public void operational_event_is_sent_once() {
    ProcessEventListener listener = mock(ProcessEventListener.class);
    SQProcess underTest = SQProcess.builder(A_PROCESS_ID)
      .addEventListener(listener)
      .build();

    try (TestProcess testProcess = new TestProcess()) {
      underTest.start(() -> testProcess);
      testProcess.operational = true;

      underTest.refreshState();
      verify(listener).onProcessEvent(A_PROCESS_ID, ProcessEventListener.Type.OPERATIONAL);

      // second run
      underTest.refreshState();
      verifyNoMoreInteractions(listener);
    }
  }

  @Test
  public void send_event_when_process_requests_for_restart() {
    ProcessEventListener listener = mock(ProcessEventListener.class);
    SQProcess underTest = SQProcess.builder(A_PROCESS_ID)
      .addEventListener(listener)
      .setWatcherDelayMs(1L)
      .build();

    try (TestProcess testProcess = new TestProcess()) {
      underTest.start(() -> testProcess);

      testProcess.askedForRestart = true;
      verify(listener, timeout(10_000)).onProcessEvent(A_PROCESS_ID, ProcessEventListener.Type.ASK_FOR_RESTART);

      // flag is reset so that next run does not trigger again the event
      underTest.refreshState();
      verifyNoMoreInteractions(listener);
      assertThat(testProcess.askedForRestart).isFalse();
    }
  }

  @Test
  public void stopForcibly_stops_the_process_without_graceful_request_for_stop() {
    SQProcess underTest = SQProcess.builder(A_PROCESS_ID).build();

    try (TestProcess testProcess = new TestProcess()) {
      underTest.start(() -> testProcess);

      underTest.stopForcibly();
      assertThat(underTest.getState()).isEqualTo(Lifecycle.State.STOPPED);
      assertThat(testProcess.askedForStop).isFalse();
      assertThat(testProcess.destroyedForcibly).isTrue();

      // second execution of stopForcibly does nothing. It's still stopped.
      underTest.stopForcibly();
      assertThat(underTest.getState()).isEqualTo(Lifecycle.State.STOPPED);
    }
  }

  @Test
  public void process_stops_after_graceful_request_for_stop() throws Exception {
    ProcessLifecycleListener listener = mock(ProcessLifecycleListener.class);
    SQProcess underTest = SQProcess.builder(A_PROCESS_ID)
      .addProcessLifecycleListener(listener)
      .build();

    try (TestProcess testProcess = new TestProcess()) {
      underTest.start(() -> testProcess);

      Thread stopperThread = new Thread(() -> underTest.stop(1, TimeUnit.HOURS));
      stopperThread.start();

      // thread is blocked until process stopped
      assertThat(stopperThread.isAlive()).isTrue();

      // wait for the stopper thread to ask graceful stop
      while (!testProcess.askedForStop) {
        Thread.sleep(1L);
      }
      assertThat(underTest.getState()).isEqualTo(Lifecycle.State.STOPPING);
      verify(listener).onProcessState(A_PROCESS_ID, Lifecycle.State.STOPPING);

      // process stopped
      testProcess.close();

      // waiting for stopper thread to detect and handle the stop
      stopperThread.join();

      assertThat(underTest.getState()).isEqualTo(Lifecycle.State.STOPPED);
      verify(listener).onProcessState(A_PROCESS_ID, Lifecycle.State.STOPPED);
    }
  }

  @Test
  public void process_is_stopped_forcibly_if_graceful_stop_is_too_long() throws Exception {
    ProcessLifecycleListener listener = mock(ProcessLifecycleListener.class);
    SQProcess underTest = SQProcess.builder(A_PROCESS_ID)
      .addProcessLifecycleListener(listener)
      .build();

    try (TestProcess testProcess = new TestProcess()) {
      underTest.start(() -> testProcess);

      underTest.stop(1L, TimeUnit.MILLISECONDS);

      testProcess.waitFor();
      assertThat(testProcess.askedForStop).isTrue();
      assertThat(testProcess.destroyedForcibly).isTrue();
      assertThat(testProcess.isAlive()).isFalse();
      assertThat(underTest.getState()).isEqualTo(Lifecycle.State.STOPPED);
      verify(listener).onProcessState(A_PROCESS_ID, Lifecycle.State.STOPPED);
    }
  }

  @Test
  public void process_requests_are_listened_on_regular_basis() {
    ProcessEventListener listener = mock(ProcessEventListener.class);
    SQProcess underTest = SQProcess.builder(A_PROCESS_ID)
      .addEventListener(listener)
      .setWatcherDelayMs(1L)
      .build();

    try (TestProcess testProcess = new TestProcess()) {
      underTest.start(() -> testProcess);

      testProcess.operational = true;

      verify(listener, timeout(1_000L)).onProcessEvent(A_PROCESS_ID, ProcessEventListener.Type.OPERATIONAL);
    }
  }

  @Test
  public void test_toString() {
    SQProcess underTest = SQProcess.builder(A_PROCESS_ID).build();
    assertThat(underTest.toString()).isEqualTo("Process[" + A_PROCESS_ID.getKey() + "]");
  }

  private static class TestProcess implements ProcessMonitor, AutoCloseable {

    private final CountDownLatch alive = new CountDownLatch(1);
    private final InputStream inputStream = mock(InputStream.class, Mockito.RETURNS_MOCKS);
    private final InputStream errorStream = mock(InputStream.class, Mockito.RETURNS_MOCKS);
    private boolean streamsClosed = false;
    private boolean operational = false;
    private boolean askedForRestart = false;
    private boolean askedForStop = false;
    private boolean destroyedForcibly = false;

    @Override
    public InputStream getInputStream() {
      return inputStream;
    }

    @Override
    public InputStream getErrorStream() {
      return errorStream;
    }

    @Override
    public void closeStreams() {
      streamsClosed = true;
    }

    @Override
    public boolean isAlive() {
      return alive.getCount() == 1;
    }

    @Override
    public void askForStop() {
      askedForStop = true;
      // do not stop, just asking
    }

    @Override
    public void destroyForcibly() {
      destroyedForcibly = true;
      alive.countDown();
    }

    @Override
    public void waitFor() throws InterruptedException {
      alive.await();
    }

    @Override
    public void waitFor(long timeout, TimeUnit timeoutUnit) throws InterruptedException {
      alive.await(timeout, timeoutUnit);
    }

    @Override
    public boolean isOperational() {
      return operational;
    }

    @Override
    public boolean askedForRestart() {
      return askedForRestart;
    }

    @Override
    public void acknowledgeAskForRestart() {
      this.askedForRestart = false;
    }

    @Override
    public void close() {
      alive.countDown();
    }
  }
}
