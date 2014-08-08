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

import javax.annotation.Nullable;

public class ProcessUtils {
  private ProcessUtils() {
    // only static stuff
  }

  public static boolean isAlive(@Nullable Process process) {
    if (process == null) {
      return false;
    }
    try {
      process.exitValue();
      return false;
    } catch (IllegalThreadStateException e) {
      return true;
    }
  }

  public static void destroyQuietly(@Nullable Process process) {
    if (process != null && isAlive(process)) {
      try {
        process.destroy();
      } catch (Exception ignored) {
        // ignored
      }
    }
  }

  public static void addSelfShutdownHook(final Terminable terminable) {
    Thread shutdownHook = new Thread(new Runnable() {
      @Override
      public void run() {
        terminable.terminate();
      }
    });
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  public static void closeStreams(@Nullable Process process) {
    if (process != null) {
      IOUtils.closeQuietly(process.getInputStream());
      IOUtils.closeQuietly(process.getOutputStream());
      IOUtils.closeQuietly(process.getErrorStream());
    }
  }
}
