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

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class ProcessUtils {

  private ProcessUtils() {
    // only static stuff
  }

  /**
   * Do not abuse to this method. It uses exceptions to get status.
   * @return false if process is null or terminated, else true.
   */
  public static boolean isAlive(@Nullable Process process) {
    boolean alive = false;
    if (process != null) {
      try {
        process.exitValue();
      } catch (IllegalThreadStateException ignored) {
        alive = true;
      }
    }
    return alive;
  }

  /**
   * Send kill signal to stop process. Shutdown hooks are executed. It's the equivalent of SIGTERM on Linux.
   * Correctly tested on Java 6 and 7 on both Mac/MSWindows
   * @return true if the signal is sent, false if process is already down
   */
  public static boolean sendKillSignal(@Nullable Process process) {
    boolean sentSignal = false;
    if (isAlive(process)) {
      try {
        process.destroy();
        sentSignal = true;
      } catch (Exception e) {
        LoggerFactory.getLogger(ProcessUtils.class).error("Fail to kill " + process, e);
      }
    }
    return sentSignal;
  }

  public static void closeStreams(@Nullable Process process) {
    if (process!=null) {
      IOUtils.closeQuietly(process.getInputStream());
      IOUtils.closeQuietly(process.getOutputStream());
      IOUtils.closeQuietly(process.getErrorStream());
    }
  }
}
