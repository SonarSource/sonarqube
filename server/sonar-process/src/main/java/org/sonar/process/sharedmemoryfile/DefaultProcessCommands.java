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
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ProcessCommands} based on a {@link AllProcessesCommands} of which will request a
 * single {@link ProcessCommands} to use as delegate for the specified processNumber.
 */
public class DefaultProcessCommands implements ProcessCommands, AutoCloseable {
  private final AllProcessesCommands allProcessesCommands;
  private final ProcessCommands delegate;

  private DefaultProcessCommands(File directory, int processNumber, boolean clean) {
    this.allProcessesCommands = new AllProcessesCommands(directory);
    this.delegate = clean ? allProcessesCommands.createAfterClean(processNumber) : allProcessesCommands.create(processNumber);
  }

  /**
   * Main DefaultProcessCommands will clear the shared memory space of the specified process number when created and will
   * then write and/or read to it.
   * Therefore there should be only one main DefaultProcessCommands.
   */
  public static DefaultProcessCommands main(File directory, int processNumber) {
    return new DefaultProcessCommands(directory, processNumber, true);
  }

  /**
   * Secondary DefaultProcessCommands will read and write to the shared memory space but will not clear it. Therefore, there
   * can be any number of them.
   */
  public static DefaultProcessCommands secondary(File directory, int processNumber) {
    return new DefaultProcessCommands(directory, processNumber, false);
  }

  /**
   * Clears the shared memory space of the specified process number.
   */
  public static void reset(File directory, int processNumber) {
    try (DefaultProcessCommands processCommands = new DefaultProcessCommands(directory, processNumber, true)) {
      // nothing else to do than open file and reset the space of specified process
    }
  }

  @Override
  public boolean isUp() {
    return delegate.isUp();
  }

  @Override
  public void setUp() {
    delegate.setUp();
  }

  @Override
  public boolean isOperational() {
    return delegate.isOperational();
  }

  @Override
  public void setOperational() {
    delegate.setOperational();
  }

  @Override
  public void ping() {
    delegate.ping();
  }

  @Override
  public long getLastPing() {
    return delegate.getLastPing();
  }

  @Override
  public void setHttpUrl(String s) {
    delegate.setHttpUrl(s);
  }

  @Override
  public String getHttpUrl() {
    return delegate.getHttpUrl();
  }

  @Override
  public void askForStop() {
    delegate.askForStop();
  }

  @Override
  public boolean askedForStop() {
    return delegate.askedForStop();
  }

  @Override
  public void askForHardStop() {
    delegate.askForHardStop();
  }

  @Override
  public boolean askedForHardStop() {
    return delegate.askedForHardStop();
  }

  @Override
  public void askForRestart() {
    delegate.askForRestart();
  }

  @Override
  public boolean askedForRestart() {
    return delegate.askedForRestart();
  }

  @Override
  public void acknowledgeAskForRestart() {
    delegate.acknowledgeAskForRestart();
  }

  @Override
  public void endWatch() {
    try {
      close();
    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).error("Failed to close DefaultProcessCommands", e);
    }
  }

  @Override
  public void close() {
    allProcessesCommands.close();
  }
}
