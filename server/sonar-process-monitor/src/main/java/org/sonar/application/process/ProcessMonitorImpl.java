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
import org.sonar.process.ProcessCommands;

import static java.util.Objects.requireNonNull;

class ProcessMonitorImpl implements ProcessMonitor {

  private final Process process;
  private final ProcessCommands commands;

  ProcessMonitorImpl(Process process, ProcessCommands commands) {
    this.process = requireNonNull(process, "process can't be null");
    this.commands = requireNonNull(commands, "commands can't be null");
  }

  @Override
  public InputStream getInputStream() {
    return process.getInputStream();
  }

  @Override
  public void closeStreams() {
    closeQuietly(process.getInputStream());
    closeQuietly(process.getOutputStream());
    closeQuietly(process.getErrorStream());
  }

  @Override
  public boolean isAlive() {
    return process.isAlive();
  }

  @Override
  public void destroyForcibly() {
    process.destroyForcibly();
  }

  @Override
  public void waitFor() throws InterruptedException {
    process.waitFor();
  }

  @Override
  public void waitFor(long timeout, TimeUnit unit) throws InterruptedException {
    process.waitFor(timeout, unit);
  }

  @Override
  public boolean isOperational() {
    return commands.isOperational();
  }

  @Override
  public void askForStop() {
    commands.askForStop();
  }

  @Override
  public boolean askedForRestart() {
    return commands.askedForRestart();
  }

  @Override
  public void acknowledgeAskForRestart() {
    commands.acknowledgeAskForRestart();
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

}
