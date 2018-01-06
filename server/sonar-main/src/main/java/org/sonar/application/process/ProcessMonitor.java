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
import java.util.concurrent.TimeUnit;

public interface ProcessMonitor {

  /**
   * @see Process#getInputStream()
   */
  InputStream getInputStream();

  /**
   * @see Process#getErrorStream()
   */
  InputStream getErrorStream();

  /**
   * Closes the streams {@link Process#getInputStream()}, {@link Process#getOutputStream()}
   * and {@link Process#getErrorStream()}.
   *
   * No exceptions are thrown in case of errors.
   */
  void closeStreams();

  /**
   * @see Process#isAlive()
   */
  boolean isAlive();

  /**
   * @see Process#destroyForcibly()
   */
  void destroyForcibly();

  /**
   * @see Process#waitFor()
   */
  void waitFor() throws InterruptedException;

  /**
   * @see Process#waitFor(long, TimeUnit)
   */
  void waitFor(long timeout, TimeUnit timeoutUnit) throws InterruptedException;

  /**
   * Whether the process has reach operational state after startup.
   */
  boolean isOperational();

  /**
   * Send request to gracefully stop to the process
   */
  void askForStop();

  /**
   * Whether the process asked for a full restart
   */
  boolean askedForRestart();

  /**
   * Sends a signal to the process to acknowledge that the parent process received the request to restart from the
   * child process send via {@link #askedForRestart()}.
   * <br/>
   * Child process will typically stop sending the signal requesting restart from now on.
   */
  void acknowledgeAskForRestart();

}
