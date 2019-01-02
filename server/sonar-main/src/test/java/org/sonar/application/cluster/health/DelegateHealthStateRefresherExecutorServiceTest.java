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
package org.sonar.application.cluster.health;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DelegateHealthStateRefresherExecutorServiceTest {
  private Random random = new Random();
  private Runnable runnable = mock(Runnable.class);
  private Callable callable = mock(Callable.class);
  private Collection<Callable<Object>> callables = IntStream.range(0, random.nextInt(5))
    .mapToObj(i -> (Callable<Object>) mock(Callable.class))
    .collect(Collectors.toList());
  private int initialDelay = random.nextInt(333);
  private int delay = random.nextInt(333);
  private int period = random.nextInt(333);
  private int timeout = random.nextInt(333);
  private Object result = new Object();
  private ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);
  private DelegateHealthStateRefresherExecutorService underTest = new DelegateHealthStateRefresherExecutorService(executorService);

  @Test
  public void schedule() {
    underTest.schedule(runnable, delay, SECONDS);

    verify(executorService).schedule(runnable, delay, SECONDS);
  }

  @Test
  public void schedule1() {
    underTest.schedule(callable, delay, SECONDS);

    verify(executorService).schedule(callable, delay, SECONDS);
  }

  @Test
  public void scheduleAtFixedRate() {
    underTest.scheduleAtFixedRate(runnable, initialDelay, period, SECONDS);
    verify(executorService).scheduleAtFixedRate(runnable, initialDelay, period, SECONDS);
  }

  @Test
  public void scheduleWithFixeddelay() {
    underTest.scheduleWithFixedDelay(runnable, initialDelay, delay, TimeUnit.SECONDS);
    verify(executorService).scheduleWithFixedDelay(runnable, initialDelay, delay, TimeUnit.SECONDS);
  }

  @Test
  public void shutdown() {
    underTest.shutdown();
    verify(executorService).shutdown();
  }

  @Test
  public void shutdownNow() {
    underTest.shutdownNow();
    verify(executorService).shutdownNow();
  }

  @Test
  public void isShutdown() {
    underTest.isShutdown();
    verify(executorService).isShutdown();
  }

  @Test
  public void isTerminated() {
    underTest.isTerminated();
    verify(executorService).isTerminated();
  }

  @Test
  public void awaitTermination() throws InterruptedException {
    underTest.awaitTermination(timeout, TimeUnit.SECONDS);

    verify(executorService).awaitTermination(timeout, TimeUnit.SECONDS);
  }

  @Test
  public void submit() {
    underTest.submit(callable);

    verify(executorService).submit(callable);
  }

  @Test
  public void submit1() {
    underTest.submit(runnable, result);

    verify(executorService).submit(runnable, result);
  }

  @Test
  public void submit2() {
    underTest.submit(runnable);
    verify(executorService).submit(runnable);
  }

  @Test
  public void invokeAll() throws InterruptedException {
    underTest.invokeAll(callables);
    verify(executorService).invokeAll(callables);
  }

  @Test
  public void invokeAll1() throws InterruptedException {
    underTest.invokeAll(callables, timeout, SECONDS);
    verify(executorService).invokeAll(callables, timeout, SECONDS);
  }

  @Test
  public void invokeAny() throws InterruptedException, ExecutionException {
    underTest.invokeAny(callables);
    verify(executorService).invokeAny(callables);
  }

  @Test
  public void invokeAny2() throws InterruptedException, ExecutionException, TimeoutException {
    underTest.invokeAny(callables, timeout, SECONDS);
    verify(executorService).invokeAny(callables, timeout, SECONDS);
  }

}
