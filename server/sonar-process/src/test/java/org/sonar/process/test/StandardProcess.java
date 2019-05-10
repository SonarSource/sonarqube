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
package org.sonar.process.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.sonar.process.Lifecycle.State;
import org.sonar.process.Monitored;
import org.sonar.process.ProcessEntryPoint;

import static com.google.common.base.Preconditions.checkState;

public class StandardProcess implements Monitored {

  private AtomicReference<State> state = new AtomicReference<>(State.INIT);
  private volatile boolean stopped = false;
  private volatile boolean hardStopped = false;
  private CountDownLatch stopLatch = new CountDownLatch(1);

  private final Thread daemon = new Thread() {
    @Override
    public void run() {
      try {
        Thread.sleep(Long.MAX_VALUE);
      } catch (InterruptedException e) {
        interrupt();
      }
    }
  };

  /**
   * Blocks until started()
   */
  @Override
  public void start() {
    state.compareAndSet(State.INIT, State.STARTING);
    daemon.start();
    state.compareAndSet(State.STARTING, State.STARTED);
  }

  @Override
  public Status getStatus() {
    return state.get() == State.STARTED ? Status.OPERATIONAL : Status.DOWN;
  }

  @Override
  public void awaitStop() {
    try {
      stopLatch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void stop() {
    checkState(state.compareAndSet(State.STARTED, State.STOPPING), "not started?!! what?");
    daemon.interrupt();
    stopped = true;
    state.compareAndSet(State.STOPPING, State.STOPPED);
    stopLatch.countDown();
  }

  /**
   * Blocks until stopped
   */
  @Override
  public void hardStop() {
    state.set(State.HARD_STOPPING);
    daemon.interrupt();
    hardStopped = true;
    state.compareAndSet(State.HARD_STOPPING, State.STOPPED);
    stopLatch.countDown();
  }

  public State getState() {
    return state.get();
  }

  public boolean wasStopped() {
    return stopped;
  }

  public boolean wasHardStopped() {
    return hardStopped;
  }

  public static void main(String[] args) {
    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(args);
    entryPoint.launch(new StandardProcess());
    System.exit(0);
  }
}
