/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

abstract class AbstractProcessMonitor implements ProcessMonitor {
  protected final Process process;

  protected AbstractProcessMonitor(Process process) {
    this.process = process;
  }

  public InputStream getInputStream() {
    return process.getInputStream();
  }

  public void closeStreams() {
    closeQuietly(process.getInputStream());
    closeQuietly(process.getOutputStream());
    closeQuietly(process.getErrorStream());
  }

  private static void closeQuietly(@Nullable Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException ignored) {
      // ignore
    }
  }

  public boolean isAlive() {
    return process.isAlive();
  }

  public void destroyForcibly() {
    process.destroyForcibly();
  }

  public void waitFor() throws InterruptedException {
    process.waitFor();
  }

  public void waitFor(long timeout, TimeUnit unit) throws InterruptedException {
    process.waitFor(timeout, unit);
  }
}
