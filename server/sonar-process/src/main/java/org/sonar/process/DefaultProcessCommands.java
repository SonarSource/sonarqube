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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

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
public class DefaultProcessCommands implements ProcessCommands {

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
   *   <li>The next 8 bytes contains a long (System.currentTimeInMillis for ping)</li>
   * </ul>
   */
  final MappedByteBuffer mappedByteBuffer;
  private final RandomAccessFile sharedMemory;
  private static final int BYTE_LENGTH_FOR_ONE_PROCESS = 1 + 1 + 8;

  // With this shared memory we can handle up to MAX_PROCESSES processes
  private static final int MAX_SHARED_MEMORY = BYTE_LENGTH_FOR_ONE_PROCESS * MAX_PROCESSES;

  public static final byte STOP = (byte) 0xFF;
  public static final byte READY = (byte) 0x01;
  public static final byte EMPTY = (byte) 0x00;

  private int processNumber;

  public DefaultProcessCommands(File directory, int processNumber) {
    // processNumber should not excess MAX_PROCESSES and must not be below -1
    assert processNumber <= MAX_PROCESSES : "Incorrect process number";
    assert processNumber >= -1 : "Incorrect process number";

    this.processNumber = processNumber;
    if (!directory.isDirectory() || !directory.exists()) {
      throw new IllegalArgumentException("Not a valid directory: " + directory);
    }

    try {
      sharedMemory = new RandomAccessFile(new File(directory, "sharedmemory"), "rw");
      mappedByteBuffer = sharedMemory.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, MAX_SHARED_MEMORY);
      cleanData();
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to create shared memory : ", e);
    }
  }

  @Override
  public boolean isReady() {
    return canBeMonitored() && mappedByteBuffer.get(offset()) == READY;
  }

  /**
   * To be executed by child process to declare that it's ready
   */
  @Override
  public void setReady() {
    if (canBeMonitored()) {
      mappedByteBuffer.put(offset(), READY);
    }
  }

  @Override
  public void ping() {
    if (canBeMonitored()) {
      mappedByteBuffer.putLong(2 + offset(), System.currentTimeMillis());
    }
  }

  @Override
  public long getLastPing() {
    if (canBeMonitored()) {
      return mappedByteBuffer.getLong(2 + offset());
    } else {
      return -1;
    }
  }

  /**
   * To be executed by monitor process to ask for child process termination
   */
  @Override
  public void askForStop() {
    mappedByteBuffer.put(offset() + 1, STOP);
  }

  @Override
  public boolean askedForStop() {
    return mappedByteBuffer.get(offset() + 1) == STOP;
  }

  @Override
  public void endWatch() {
    IOUtils.closeQuietly(sharedMemory);
  }

  int offset() {
    return BYTE_LENGTH_FOR_ONE_PROCESS * processNumber;
  }

  private boolean canBeMonitored() {
    boolean result = processNumber >= 0 && processNumber < MAX_PROCESSES;
    if (!result) {
      LoggerFactory.getLogger(getClass()).info("This process cannot be monitored. Process Id : [{}]", processNumber);
    }
    return result;
  }

  private void cleanData() {
    for (int i = 0; i < BYTE_LENGTH_FOR_ONE_PROCESS; i++) {
      mappedByteBuffer.put(offset() + i, EMPTY);
    }
  }
}
