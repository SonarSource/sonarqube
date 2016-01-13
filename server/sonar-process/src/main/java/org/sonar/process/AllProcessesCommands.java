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
package org.sonar.process;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.commons.io.IOUtils;

import static org.sonar.process.ProcessCommands.MAX_PROCESSES;

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
public class AllProcessesCommands {

  /**
   * The ByteBuffer will contains :
   * <ul>
   *   <li>First byte will contains 0x00 until stop command is issued = 0xFF</li>
   *   <li>Then each 10 bytes will be reserved for each process</li>
   * </ul>
   *
   * Description of ten bytes of each process :
   * <ul>
   *   <li>First byte will contains the state 0x00 until READY 0x01</li>
   *   <li>The second byte will contains the request for stopping 0x00 or STOP (0xFF)</li>
   *   <li>The second byte will contains the request for restarting 0x00 or RESTART (0xAA)</li>
   *   <li>The next 8 bytes contains a long (System.currentTimeInMillis for ping)</li>
   * </ul>
   */
  final MappedByteBuffer mappedByteBuffer;
  private final RandomAccessFile sharedMemory;
  private static final int BYTE_LENGTH_FOR_ONE_PROCESS = 1 + 1 + 1 + 8;

  // With this shared memory we can handle up to MAX_PROCESSES processes
  private static final int MAX_SHARED_MEMORY = BYTE_LENGTH_FOR_ONE_PROCESS * MAX_PROCESSES;

  public static final byte STOP = (byte) 0xFF;
  public static final byte RESTART = (byte) 0xAA;
  public static final byte READY = (byte) 0x01;
  public static final byte EMPTY = (byte) 0x00;

  public AllProcessesCommands(File directory) {
    if (!directory.isDirectory() || !directory.exists()) {
      throw new IllegalArgumentException("Not a valid directory: " + directory);
    }

    try {
      sharedMemory = new RandomAccessFile(new File(directory, "sharedmemory"), "rw");
      mappedByteBuffer = sharedMemory.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, MAX_SHARED_MEMORY);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to create shared memory : ", e);
    }
  }

  public ProcessCommands getProcessCommand(int processNumber, boolean clean) {
    checkProcessNumber(processNumber);
    ProcessCommands processCommands = new ProcessCommandsImpl(processNumber);
    if (clean) {
      cleanData(processNumber);
    }
    return processCommands;
  }

  boolean isReady(int processNumber) {
    return mappedByteBuffer.get(offset(processNumber)) == READY;
  }

  /**
   * To be executed by child process to declare that it's ready
   */
  void setReady(int processNumber) {
    mappedByteBuffer.put(offset(processNumber), READY);
  }

  void ping(int processNumber) {
    mappedByteBuffer.putLong(2 + offset(processNumber), System.currentTimeMillis());
  }

  long getLastPing(int processNumber) {
    return mappedByteBuffer.getLong(2 + offset(processNumber));
  }

  /**
   * To be executed by monitor process to ask for child process termination
   */
  void askForStop(int processNumber) {
    mappedByteBuffer.put(offset(processNumber) + 1, STOP);
  }

  boolean askedForStop(int processNumber) {
    return mappedByteBuffer.get(offset(processNumber) + 1) == STOP;
  }

  void askForRestart(int processNumber) {
    mappedByteBuffer.put(offset(processNumber) + 3, RESTART);
  }

  boolean askedForRestart(int processNumber) {
    return mappedByteBuffer.get(offset(processNumber) + 3) == RESTART;
  }

  void acknowledgeAskForRestart(int processNumber) {
    mappedByteBuffer.put(offset(processNumber) + 3, EMPTY);
  }

  public void close() {
    IOUtils.closeQuietly(sharedMemory);
  }

  int offset(int processNumber) {
    return BYTE_LENGTH_FOR_ONE_PROCESS * processNumber;
  }

  public void checkProcessNumber(int processNumber) {
    boolean result = processNumber >= 0 && processNumber < MAX_PROCESSES;
    if (!result) {
      throw new IllegalArgumentException(String.format("Process number %s is not valid", processNumber));
    }
  }

  private void cleanData(int processNumber) {
    for (int i = 0; i < BYTE_LENGTH_FOR_ONE_PROCESS; i++) {
      mappedByteBuffer.put(offset(processNumber) + i, EMPTY);
    }
  }

  private class ProcessCommandsImpl implements ProcessCommands {

    private final int processNumber;

    public ProcessCommandsImpl(int processNumber) {
      this.processNumber = processNumber;
    }

    @Override
    public boolean isReady() {
      return AllProcessesCommands.this.isReady(processNumber);
    }

    @Override
    public void setReady() {
      AllProcessesCommands.this.setReady(processNumber);
    }

    @Override
    public void ping() {
      AllProcessesCommands.this.ping(processNumber);
    }

    @Override
    public long getLastPing() {
      return AllProcessesCommands.this.getLastPing(processNumber);
    }

    @Override
    public void askForStop() {
      AllProcessesCommands.this.askForStop(processNumber);
    }

    @Override
    public boolean askedForStop() {
      return AllProcessesCommands.this.askedForStop(processNumber);
    }

    @Override
    public void askForRestart() {
      AllProcessesCommands.this.askForRestart(processNumber);
    }

    @Override
    public boolean askedForRestart() {
      return AllProcessesCommands.this.askedForRestart(processNumber);
    }

    @Override
    public void acknowledgeAskForRestart() {
      AllProcessesCommands.this.acknowledgeAskForRestart(processNumber);
    }

    @Override
    public void endWatch() {
      throw new UnsupportedOperationException("ProcessCommands created from AllProcessesCommands can not be closed directly. Close AllProcessesCommands instead");
    }

    @Override
    public void close() throws Exception {
      endWatch();
    }
  }
}
