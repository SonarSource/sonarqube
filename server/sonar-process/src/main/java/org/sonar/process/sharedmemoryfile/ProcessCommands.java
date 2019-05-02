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
package org.sonar.process.sharedmemoryfile;

import java.io.File;

/**
 * Process inter-communication to :
 * <ul>
 *   <li>share status of specific process</li>
 *   <li>stop/restart a specific processes</li>
 * </ul>
 *
 * @see DefaultProcessCommands#main(File, int)
 * @see DefaultProcessCommands#secondary(File, int)
 */
public interface ProcessCommands {

  int MAX_PROCESSES = 5;

  boolean isUp();

  /**
   * To be executed by child process to declare that it is done starting
   */
  void setUp();

  boolean isOperational();

  /**
   * To be executed by child process to declare that it is done starting and fully operational.
   *
   * @throws IllegalStateException if {@link #setUp()} has not been called
   */
  void setOperational();

  void ping();

  long getLastPing();

  void setHttpUrl(String s);

  String getHttpUrl();

  /**
   * To be executed by monitor process to ask for graceful child process termination
   */
  void askForStop();

  boolean askedForStop();

  /**
   * To be executed by monitor process to ask for quick child process termination
   */
  void askForHardStop();

  boolean askedForHardStop();

  /**
   * To be executed by child process to ask for restart of all child processes
   */
  void askForRestart();

  /**
   * Can be called by child or monitor process to know whether child process asked for restart
   */
  boolean askedForRestart();

  /**
   * To be executed by monitor process to acknowledge restart request from child process.
   */
  void acknowledgeAskForRestart();

  void endWatch();
}
