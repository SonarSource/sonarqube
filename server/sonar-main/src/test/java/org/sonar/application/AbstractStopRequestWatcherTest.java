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

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class AbstractStopRequestWatcherTest {

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  private String threadName = RandomStringUtils.randomAlphabetic(12);
  private TestBooleanSupplier booleanSupplier = new TestBooleanSupplier();
  private TestAction stopAction = new TestAction();

  @Test
  public void constructor_sets_thread_name_and_daemon_true() {
    AbstractStopRequestWatcher underTest = new AbstractStopRequestWatcher(threadName, booleanSupplier, stopAction) {

    };
    assertThat(underTest.getName()).isEqualTo(threadName);
    assertThat(underTest.isDaemon()).isTrue();
  }

  @Test
  public void call_stop_action_when_booleanSupplier_returns_true() {
    AbstractStopRequestWatcher underTest = new AbstractStopRequestWatcher(threadName, booleanSupplier, stopAction) {

    };

    underTest.startWatching();
    assertThat(underTest.isAlive()).isTrue();
    assertThat(stopAction.isCalled()).isFalse();

    booleanSupplier.flag = true;
    await().atMost(1, TimeUnit.MINUTES).until(() -> stopAction.isCalled());
    assertThat(stopAction.isCalled()).isTrue();

    underTest.stopWatching();
    await().until(() -> !underTest.isAlive());
  }

  @Test
  public void create_instance_with_default_delay() {
    AbstractStopRequestWatcher underTest = new AbstractStopRequestWatcher(threadName, booleanSupplier, stopAction) {

    };

    assertThat(underTest.getDelayMs()).isEqualTo(500L);
  }

  @Test
  public void create_instance_with_specified_delay() {
    long delayMs = new Random().nextLong();
    AbstractStopRequestWatcher underTest = new AbstractStopRequestWatcher(threadName, booleanSupplier, stopAction, delayMs) {

    };

    assertThat(underTest.getDelayMs()).isEqualTo(delayMs);
  }

  @Test
  public void stop_watching_commands_if_thread_is_interrupted() {
    AbstractStopRequestWatcher underTest = new AbstractStopRequestWatcher(threadName, booleanSupplier, stopAction) {

    };

    underTest.startWatching();
    underTest.interrupt();

    await().until(() -> !underTest.isAlive());
    assertThat(underTest.isAlive()).isFalse();
  }

  private static class TestBooleanSupplier implements BooleanSupplier {
    volatile boolean flag = false;

    @Override
    public boolean getAsBoolean() {
      return flag;
    }

  }

  private static class TestAction implements Runnable {
    volatile boolean called = false;

    @Override
    public void run() {
      called = true;
    }

    public boolean isCalled() {
      return called;
    }
  }
}
