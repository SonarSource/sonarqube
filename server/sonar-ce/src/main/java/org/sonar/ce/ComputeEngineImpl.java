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
package org.sonar.ce;

import org.sonar.ce.container.ComputeEngineStatus;
import org.sonar.ce.container.ComputeEngineContainer;
import org.sonar.process.Props;

import static com.google.common.base.Preconditions.checkState;

public class ComputeEngineImpl implements ComputeEngine, ComputeEngineStatus {
  private final Props props;
  private final ComputeEngineContainer computeEngineContainer;

  private Status status = Status.INIT;

  public ComputeEngineImpl(Props props, ComputeEngineContainer computeEngineContainer) {
    this.props = props;
    this.computeEngineContainer = computeEngineContainer;
    computeEngineContainer.setComputeEngineStatus(this);
  }

  @Override
  public void startup() {
    checkState(this.status == Status.INIT, "startup() can not be called multiple times");
    try {
      this.status = Status.STARTING;
      this.computeEngineContainer.start(props);
    } finally {
      this.status = Status.STARTED;
    }
  }

  @Override
  public void stopProcessing() {
    checkState(this.status.ordinal() >= Status.STARTED.ordinal(), "stopProcessing() must not be called before startup()");
    checkState(this.status.ordinal() <= Status.STOPPING.ordinal(), "stopProcessing() can not be called after shutdown()");
    checkState(this.status.ordinal() <= Status.STOPPING_WORKERS.ordinal(), "stopProcessing() can not be called multiple times");

    try {
      this.status = Status.STOPPING_WORKERS;
      this.computeEngineContainer.stopWorkers();
    } finally {
      this.status = Status.WORKERS_STOPPED;
    }
  }

  @Override
  public void shutdown() {
    checkState(this.status.ordinal() >= Status.STARTED.ordinal(), "shutdown() must not be called before startup()");
    checkState(this.status.ordinal() <= Status.STOPPING.ordinal(), "shutdown() can not be called multiple times");

    try {
      this.status = Status.STOPPING;
      this.computeEngineContainer.stop();
    } finally {
      this.status = Status.STOPPED;
    }
  }
  @Override
  public Status getStatus() {
    return status;
  }
}
