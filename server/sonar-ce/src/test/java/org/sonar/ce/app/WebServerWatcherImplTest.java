/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.io.File;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.process.DefaultProcessCommands;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class WebServerWatcherImplTest {
  private static final int WEB_SERVER_PROCESS_NUMBER = 2;

  @Rule
  public Timeout timeout = Timeout.seconds(1);
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File sharedDir;
  private WebServerWatcherImpl underTest;

  @Before
  public void setUp() throws Exception {
    sharedDir = temporaryFolder.newFolder();
    underTest = new WebServerWatcherImpl(sharedDir);
  }

  @Test
  public void waitForOperational_does_not_log_anything_if_WebServer_already_operational() {
    setWebServerOperational();

    underTest.waitForOperational();

    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void waitForOperational_blocks_until_WebServer_is_operational() throws InterruptedException {
    final CountDownLatch startedLatch = new CountDownLatch(1);
    final CountDownLatch doneLatch = new CountDownLatch(1);
    Thread waitingThread = new Thread() {
      @Override
      public void run() {
        startedLatch.countDown();
        underTest.waitForOperational();
        doneLatch.countDown();
      }
    };
    waitingThread.start();

    // wait for waitingThread to be running
    assertThat(startedLatch.await(50, MILLISECONDS)).isTrue();

    // assert that we can wait, in vain, more than 50ms because waitingThread is blocked
    assertThat(doneLatch.await(50 + Math.abs(new Random().nextInt(300)), MILLISECONDS)).isFalse();

    setWebServerOperational();

    // wait up to 400 ms (because polling delay is 200ms) that waitingThread is done running
    assertThat(doneLatch.await(400, MILLISECONDS)).isTrue();

    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Waiting for Web Server to be operational...");
  }

  private void setWebServerOperational() {
    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(sharedDir, WEB_SERVER_PROCESS_NUMBER)) {
      processCommands.setOperational();
    }
  }
}
