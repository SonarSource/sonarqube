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
package org.sonar.server.util;

import org.sonar.api.utils.log.Loggers;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

/**
 * Abstract implementation of StoppableExecutorService that implements the
 * stop() method and delegates all methods to the provided ExecutorService instance.
 */
public abstract class AbstractStoppableExecutorService<D extends ExecutorService> implements StoppableExecutorService {
  protected final D delegate;

  public AbstractStoppableExecutorService(D delegate) {
    this.delegate = delegate;
  }

  @Override
  public void stop() {
    // Disable new tasks from being submitted
    delegate.shutdown();
    try {
      // Wait a while for existing tasks to terminate
      if (!delegate.awaitTermination(5, TimeUnit.SECONDS)) {
        // Cancel currently executing tasks
        delegate.shutdownNow();
        // Wait a while for tasks to respond to being canceled
        if (!delegate.awaitTermination(5, TimeUnit.SECONDS)) {
          Loggers.get(getClass()).warn(format("Pool %s did not terminate", getClass().getSimpleName()));
        }
      }
    } catch (InterruptedException ie) {
      Loggers.get(getClass()).warn(format("Termination of pool %s failed", getClass().getSimpleName()), ie);
      // (Re-)Cancel if current thread also interrupted
      delegate.shutdownNow();
    }
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return delegate.submit(task);
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return delegate.submit(task, result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return delegate.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return delegate.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
    long timeout,
    TimeUnit unit) throws InterruptedException {
    return delegate.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return delegate.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
    long timeout,
    TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    delegate.execute(command);
  }
}
