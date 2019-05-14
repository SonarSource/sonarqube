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
package org.sonar.application.process;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.ProcessId;

abstract class AbstractManagedProcess implements ManagedProcess {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractManagedProcess.class);
  private static final int EXPECTED_EXIT_VALUE = 0;
  private final AtomicBoolean exitValueLogged = new AtomicBoolean(false);
  protected final Process process;
  private final ProcessId processId;

  protected AbstractManagedProcess(Process process, ProcessId processId) {
    this.process = process;
    this.processId = processId;
  }

  public InputStream getInputStream() {
    return process.getInputStream();
  }

  public InputStream getErrorStream() {
    return process.getErrorStream();
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
    int exitValue = process.waitFor();
    if (exitValueLogged.compareAndSet(false, true)) {
      if (exitValue != EXPECTED_EXIT_VALUE) {
        LOG.warn("Process exited with exit value [{}]: {}", processId.getKey(), exitValue);
      } else {
        LOG.debug("Process exited with exit value [{}]: {}", processId.getKey(), exitValue);
      }
    }
  }

  public void waitFor(long timeout, TimeUnit unit) throws InterruptedException {
    process.waitFor(timeout, unit);
  }
}
