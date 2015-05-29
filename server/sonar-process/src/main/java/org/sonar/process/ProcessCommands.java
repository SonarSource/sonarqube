/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process;

/**
 * Process inter-communication to :
 * <ul>
 *   <li>share status of child process</li>
 *   <li>stop child process</li>
 * </ul>
 *
 * <p/>
 * It relies on files shared by both processes. Following alternatives were considered but not selected :
 * <ul>
 *   <li>JMX beans over RMI: network issues (mostly because of Java reverse-DNS) + requires to configure and open a new port</li>
 *   <li>simple socket protocol: same drawbacks are RMI connection</li>
 *   <li>java.lang.Process#destroy(): shutdown hooks are not executed on some OS (mostly MSWindows)</li>
 *   <li>execute OS-specific commands (for instance kill on *nix): OS-specific, so hell to support. Moreover how to get identify a process ?</li>
 * </ul>
 */
public interface ProcessCommands {

  int MAX_PROCESSES = 50;

  boolean isReady();

  /**
   * To be executed by child process to declare that it's ready
   */
  void setReady();

  void ping();

  long getLastPing();

  /**
   * To be executed by monitor process to ask for child process termination
   */
  void askForStop();

  boolean askedForStop();

  void endWatch();
}
