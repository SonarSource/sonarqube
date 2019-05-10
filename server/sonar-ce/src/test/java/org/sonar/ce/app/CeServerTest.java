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
package org.sonar.ce.app;

import java.util.concurrent.CountDownLatch;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.mockito.Mockito;
import org.sonar.ce.ComputeEngine;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitored;

import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CeServerTest {
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CeServer underTest = null;
  private Thread waitingThread = null;
  private MinimumViableSystem minimumViableSystem = mock(MinimumViableSystem.class, Mockito.RETURNS_MOCKS);

  @After
  public void tearDown() throws Exception {
    if (underTest != null) {
      underTest.hardStop();
    }
    Thread waitingThread = this.waitingThread;
    this.waitingThread = null;
    if (waitingThread != null) {
      waitingThread.join();
    }
  }

  @Test
  public void constructor_does_not_start_a_new_Thread() {
    int activeCount = Thread.activeCount();

    newCeServer();

    assertThat(Thread.activeCount()).isSameAs(activeCount);
  }

  @Test
  public void awaitStop_throws_ISE_if_called_before_start() {
    CeServer ceServer = newCeServer();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("awaitStop() must not be called before start()");

    ceServer.awaitStop();
  }

  @Test
  public void start_starts_a_new_Thread() {
    int activeCount = Thread.activeCount();

    newCeServer().start();

    assertThat(Thread.activeCount()).isSameAs(activeCount + 1);
  }

  @Test
  public void start_throws_ISE_when_called_twice() {
    CeServer ceServer = newCeServer();

    ceServer.start();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("start() can not be called twice");

    ceServer.start();
  }

  @Test
  public void getStatus_throws_ISE_when_called_before_start() {
    CeServer ceServer = newCeServer();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("getStatus() can not be called before start()");

    ceServer.getStatus();
  }

  @Test
  public void getStatus_does_not_return_OPERATIONAL_until_ComputeEngine_startup_returns() {
    BlockingStartupComputeEngine computeEngine = new BlockingStartupComputeEngine(null);
    CeServer ceServer = newCeServer(computeEngine);

    ceServer.start();

    assertThat(ceServer.getStatus()).isEqualTo(Monitored.Status.DOWN);

    // release ComputeEngine startup method
    computeEngine.releaseStartup();

    while (ceServer.getStatus() == Monitored.Status.DOWN) {
      // wait for isReady to change to true, otherwise test will fail with timeout
    }
    assertThat(ceServer.getStatus()).isEqualTo(Monitored.Status.OPERATIONAL);
  }

  @Test
  public void getStatus_returns_OPERATIONAL_when_ComputeEngine_startup_throws_any_Exception_or_Error() {
    Throwable startupException = new Throwable("Faking failing ComputeEngine#startup()");

    BlockingStartupComputeEngine computeEngine = new BlockingStartupComputeEngine(startupException);
    CeServer ceServer = newCeServer(computeEngine);

    ceServer.start();

    assertThat(ceServer.getStatus()).isEqualTo(Monitored.Status.DOWN);

    // release ComputeEngine startup method which will throw startupException
    computeEngine.releaseStartup();

    while (ceServer.getStatus() == Monitored.Status.DOWN) {
      // wait for isReady to change to not DOWN, otherwise test will fail with timeout
    }
    assertThat(ceServer.getStatus()).isEqualTo(Monitored.Status.OPERATIONAL);
  }

  @Test
  public void awaitStop_keeps_blocking_calling_thread_even_if_calling_thread_is_interrupted_but_until_stop_is_called() throws Exception {
    final CeServer ceServer = newCeServer();
    Thread waitingThread = newWaitingThread(ceServer::awaitStop);

    ceServer.start();
    waitingThread.start();

    // interrupts waitingThread 5 times in a row (we really insist)
    for (int i = 0; i < 5; i++) {
      waitingThread.interrupt();
      Thread.sleep(5);
      assertThat(waitingThread.isAlive()).isTrue();
    }

    ceServer.hardStop();
    // wait for waiting thread to stop because we stopped ceServer
    // if it does not, the test will fail with timeout
    waitingThread.join();
  }

  @Test
  public void awaitStop_unblocks_when_waiting_for_ComputeEngine_startup_fails() {
    CeServer ceServer = newCeServer(new ComputeEngine() {
      @Override
      public void startup() {
        throw new Error("Faking ComputeEngine.startup() failing");
      }

      @Override
      public void stopProcessing() {
        throw new UnsupportedOperationException("stopProcessing() should never be called in this test");
      }

      @Override
      public void shutdown() {
        throw new UnsupportedOperationException("shutdown() should never be called in this test");
      }
    });

    ceServer.start();
    // if awaitStop does not unblock, the test will fail with timeout
    ceServer.awaitStop();
  }

  @Test
  public void stop_releases_thread_in_awaitStop_even_when_ComputeEngine_shutdown_fails() throws InterruptedException {
    final CeServer ceServer = newCeServer(new ComputeEngine() {
      @Override
      public void startup() {
        // nothing to do at startup
      }

      @Override
      public void stopProcessing() {
        throw new UnsupportedOperationException("stopProcessing should not be called in this test");
      }

      @Override
      public void shutdown() {
        throw new Error("Faking ComputeEngine.shutdown() failing");
      }
    });
    Thread waitingThread = newWaitingThread(ceServer::awaitStop);

    ceServer.start();
    waitingThread.start();
    ceServer.hardStop();
    // wait for waiting thread to stop because we stopped ceServer
    // if it does not, the test will fail with timeout
    waitingThread.join();
  }

  private CeServer newCeServer() {
    return newCeServer(DoNothingComputeEngine.INSTANCE);
  }

  private CeServer newCeServer(ComputeEngine computeEngine) {
    checkState(this.underTest == null, "Only one CeServer can be created per test method");
    this.underTest = new CeServer(
      computeEngine, minimumViableSystem);
    return underTest;
  }

  private Thread newWaitingThread(Runnable runnable) {
    Thread t = new Thread(runnable);
    checkState(this.waitingThread == null, "Only one waiting thread can be created per test method");
    this.waitingThread = t;
    return t;
  }

  private static class BlockingStartupComputeEngine implements ComputeEngine {
    private final CountDownLatch latch = new CountDownLatch(1);
    @CheckForNull
    private final Throwable throwable;

    public BlockingStartupComputeEngine(@Nullable Throwable throwable) {
      this.throwable = throwable;
    }

    @Override
    public void startup() {
      try {
        latch.await(1000, MILLISECONDS);
      } catch (InterruptedException e) {
        throw new RuntimeException("await failed", e);
      }
      if (throwable != null) {
        if (throwable instanceof Error) {
          throw (Error) throwable;
        } else if (throwable instanceof RuntimeException) {
          throw (RuntimeException) throwable;
        }
      }
    }

    @Override
    public void stopProcessing() {
      // do nothing
    }

    @Override
    public void shutdown() {
      // do nothing
    }

    private void releaseStartup() {
      this.latch.countDown();
    }
  }

  private enum DoNothingComputeEngine implements ComputeEngine {
    INSTANCE;

    @Override
    public void startup() {
      // do nothing
    }

    @Override
    public void stopProcessing() {
      // do nothing
    }

    @Override
    public void shutdown() {
      // do nothing
    }
  }
}
