/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sonar.ce.ComputeEngine;
import org.sonar.process.MessageException;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitored;

import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class CeServerTest {
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  private CeServer underTest = null;
  private Thread waitingThread = null;
  private final MinimumViableSystem minimumViableSystem = mock(MinimumViableSystem.class, Mockito.RETURNS_MOCKS);

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
    assertThat(ceThreadExists()).isFalse();
    newCeServer();
    assertThat(ceThreadExists()).isFalse();
  }

  @Test
  public void awaitStop_throws_ISE_if_called_before_start() {
    CeServer ceServer = newCeServer();

    assertThatThrownBy(ceServer::awaitStop)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("awaitStop() must not be called before start()");
  }

  @Test
  public void start_starts_a_new_Thread() {
    assertThat(ceThreadExists()).isFalse();
    newCeServer().start();
    assertThat(ceThreadExists()).isTrue();
  }

  @Test
  public void stop_stops_Thread() {
    CeServer ceServer = newCeServer();
    assertThat(ceThreadExists()).isFalse();
    ceServer.start();
    assertThat(ceThreadExists()).isTrue();
    ceServer.stop();
    await().atMost(5, TimeUnit.SECONDS).until(() -> !ceThreadExists());
  }

  @Test
  public void stop_dontDoAnythingIfThreadDoesntExist() {
    CeServer ceServer = newCeServer();
    assertThat(ceThreadExists()).isFalse();

    ceServer.stop();

    //expect no exception and thread still doesn't exist
    assertThat(ceThreadExists()).isFalse();
  }

  private static boolean ceThreadExists() {
    return Thread.getAllStackTraces().keySet()
      .stream()
      .map(Thread::getName)
      .anyMatch("ce-main"::equals);
  }

  @Test
  public void start_throws_ISE_when_called_twice() {
    CeServer ceServer = newCeServer();

    ceServer.start();

    assertThatThrownBy(ceServer::start)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("start() can not be called twice");
  }

  @Test
  public void getStatus_throws_ISE_when_called_before_start() {
    CeServer ceServer = newCeServer();

    assertThatThrownBy(ceServer::getStatus)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("getStatus() can not be called before start()");
  }

  @Test
  public void getStatus_does_not_return_OPERATIONAL_until_ComputeEngine_startup_returns() {
    BlockingStartupComputeEngine computeEngine = new BlockingStartupComputeEngine(null);
    CeServer ceServer = newCeServer(computeEngine);

    ceServer.start();

    assertThat(ceServer.getStatus()).isEqualTo(Monitored.Status.DOWN);

    // release ComputeEngine startup method
    computeEngine.releaseStartup();

    await().atMost(5, TimeUnit.SECONDS).until(() -> ceServer.getStatus() == Monitored.Status.OPERATIONAL);
  }

  @Test
  @UseDataProvider("exceptions")
  public void getStatus_returns_FAILED_when_ComputeEngine_startup_throws_any_Exception_or_Error(RuntimeException exception) {
    BlockingStartupComputeEngine computeEngine = new BlockingStartupComputeEngine(exception);
    CeServer ceServer = newCeServer(computeEngine);

    ceServer.start();

    assertThat(ceServer.getStatus()).isEqualTo(Monitored.Status.DOWN);

    // release ComputeEngine startup method which will throw startupException
    computeEngine.releaseStartup();

    await().atMost(5, TimeUnit.SECONDS).until(() -> ceServer.getStatus() == Monitored.Status.FAILED);
  }

  @DataProvider
  public static Object[] exceptions() {
    return new Object[] {new MessageException("exception"), new IllegalStateException("Faking failing ComputeEngine#startup()")};
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
  public void staticMain_withoutAnyArguments_expectException() {
    String[] emptyArray = {};

    assertThatThrownBy(() -> CeServer.main(emptyArray))
      .hasMessage("Only a single command-line argument is accepted (absolute path to configuration file)");
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
    return newCeServer(mock(ComputeEngine.class));
  }

  private CeServer newCeServer(ComputeEngine computeEngine) {
    checkState(this.underTest == null, "Only one CeServer can be created per test method");
    this.underTest = new CeServer(computeEngine, minimumViableSystem);
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
    private final RuntimeException throwable;

    public BlockingStartupComputeEngine(@Nullable RuntimeException throwable) {
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
        throw throwable;
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
}
