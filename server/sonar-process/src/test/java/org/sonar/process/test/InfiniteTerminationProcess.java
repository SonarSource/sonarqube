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

import org.sonar.process.Monitored;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.Lifecycle.State;

public class InfiniteTerminationProcess implements Monitored {

  private State state = State.INIT;

  private final Thread daemon = new Thread() {
    @Override
    public void run() {
      try {
        while (true) {
          Thread.sleep(100L);
        }
      } catch (InterruptedException e) {
        // return
      }
    }
  };

  /**
   * Blocks until started()
   */
  @Override
  public void start() {
    state = State.STARTING;
    daemon.start();
    state = State.STARTED;
  }

  @Override
  public Status getStatus() {
    return state == State.STARTED ? Status.OPERATIONAL : Status.DOWN;
  }

  @Override
  public void awaitStop() {
    try {
      daemon.join();
    } catch (InterruptedException e) {
      // interrupted by call to terminate()
    }
  }

  /**
   * Blocks until stopped
   */
  @Override
  public void stop() {
    state = State.STOPPING;
    try {
      daemon.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    state = State.STOPPED;
  }

  public static void main(String[] args) {
    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(args);
    entryPoint.launch(new InfiniteTerminationProcess());
  }
}
