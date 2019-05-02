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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.rightPad;
import static org.sonar.process.sharedmemoryfile.ProcessCommands.MAX_PROCESSES;

/**
 * Process inter-communication to :
 * <ul>
 *   <li>share status of child process</li>
 *   <li>stop/restart child process</li>
 * </ul>
 *
 * <p>
 * It relies on a single file accessed by all processes through a {@link MappedByteBuffer}.<br/>
 * Following alternatives were considered but not selected :
 * <ul>
 *   <li>JMX beans over RMI: network issues (mostly because of Java reverse-DNS) + requires to configure and open a new port</li>
 *   <li>simple socket protocol: same drawbacks are RMI connection</li>
 *   <li>java.lang.Process#destroy(): shutdown hooks are not executed on some OS (mostly MSWindows)</li>
 *   <li>execute OS-specific commands (for instance kill on *nix): OS-specific, so hell to support. Moreover how to get identify a process ?</li>
 * </ul>
 * </p>
 *
 * <p>
 * The file contains {@link ProcessCommands#MAX_PROCESSES} groups of {@link #BYTE_LENGTH_FOR_ONE_PROCESS} bits.
 * Each group of byte is used as follow:
 * <ul>
 *   <li>First byte contains {@link #EMPTY} until process is UP and writes {@link #UP}</li>
 *   <li>Second byte contains {@link #EMPTY} until any process requests current one to hard stop by writing value {@link #HARD_STOP}</li>
 *   <li>Third byte contains {@link #EMPTY} until any process requests current one to stop by writing value {@link #STOP}</li>
 *   <li>Fourth byte contains {@link #EMPTY} until any process requests current one to restart by writing value {@link #RESTART}.
 *       Process acknowledges restart by writing back {@link #EMPTY}</li>
 *   <li>Fifth byte will always contain {@link #EMPTY} unless process declares that it is operational by writing {@link #OPERATIONAL}.
 *       This does not imply that it is done starting.</li>
 *   <li>The next 8 bytes contains a long (value of {@link System#currentTimeMillis()}) which represents the date of the last ping</li>
 * </ul>
 * </p>
 */
public class AllProcessesCommands implements AutoCloseable {
  private static final int UP_BYTE_OFFSET = 0;
  private static final int HARD_STOP_BYTE_OFFSET = 1;
  private static final int STOP_BYTE_OFFSET = 2;
  private static final int RESTART_BYTE_OFFSET = 3;
  private static final int OPERATIONAL_BYTE_OFFSET = 4;
  private static final int PING_BYTE_OFFSET = 5;
  private static final int SYSTEM_INFO_URL_BYTE_OFFSET = PING_BYTE_OFFSET + 8;

  private static final int SYSTEM_INFO_URL_SIZE_IN_BYTES = 500;

  private static final int BYTE_LENGTH_FOR_ONE_PROCESS = 1 + 1 + 1 + 1 + 1 + 8 + SYSTEM_INFO_URL_SIZE_IN_BYTES;

  // With this shared memory we can handle up to MAX_PROCESSES processes
  private static final int MAX_SHARED_MEMORY = BYTE_LENGTH_FOR_ONE_PROCESS * MAX_PROCESSES;

  private static final byte HARD_STOP = (byte) 0xFF;
  private static final byte STOP = (byte) 0xD2;
  private static final byte RESTART = (byte) 0xAA;
  private static final byte OPERATIONAL = (byte) 0x59;
  private static final byte UP = (byte) 0x01;
  private static final byte EMPTY = (byte) 0x00;

  // VisibleForTesting
  final MappedByteBuffer mappedByteBuffer;
  private final RandomAccessFile sharedMemory;

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

  public void clean() {
    for (int i = 0; i < MAX_PROCESSES; i++) {
      cleanData(i);
    }
  }

  public ProcessCommands create(int processNumber) {
    return createForProcess(processNumber, false);
  }

  public ProcessCommands createAfterClean(int processNumber) {
    return createForProcess(processNumber, true);
  }

  private ProcessCommands createForProcess(int processNumber, boolean clean) {
    checkProcessNumber(processNumber);
    ProcessCommands processCommands = new ProcessCommandsImpl(processNumber);
    if (clean) {
      cleanData(processNumber);
    }
    return processCommands;
  }

  boolean isUp(int processNumber) {
    return readByte(processNumber, UP_BYTE_OFFSET) == UP;
  }

  /**
   * To be executed by child process to declare that it is done starting
   */
  void setUp(int processNumber) {
    writeByte(processNumber, UP_BYTE_OFFSET, UP);
  }

  boolean isOperational(int processNumber) {
    return readByte(processNumber, OPERATIONAL_BYTE_OFFSET) == OPERATIONAL;
  }

  /**
   * To be executed by child process to declare that it is started and fully operational
   */
  void setOperational(int processNumber) {
    writeByte(processNumber, OPERATIONAL_BYTE_OFFSET, OPERATIONAL);
  }

  void ping(int processNumber) {
    writeLong(processNumber, PING_BYTE_OFFSET, System.currentTimeMillis());
  }

  long getLastPing(int processNumber) {
    return readLong(processNumber, PING_BYTE_OFFSET);
  }

  String getSystemInfoUrl(int processNumber) {
    byte[] urlBytes = readBytes(processNumber, SYSTEM_INFO_URL_BYTE_OFFSET, SYSTEM_INFO_URL_SIZE_IN_BYTES);
    return new String(urlBytes, StandardCharsets.US_ASCII).trim();
  }

  void setSystemInfoUrl(int processNumber, String url) {
    byte[] urlBytes = rightPad(url, SYSTEM_INFO_URL_SIZE_IN_BYTES).getBytes(StandardCharsets.US_ASCII);
    if (urlBytes.length > SYSTEM_INFO_URL_SIZE_IN_BYTES) {
      throw new IllegalArgumentException(format("System Info URL is too long. Max is %d bytes. Got: %s", SYSTEM_INFO_URL_SIZE_IN_BYTES, url));
    }
    writeBytes(processNumber, SYSTEM_INFO_URL_BYTE_OFFSET, urlBytes);
  }

  /**
   * To be executed by monitor process to ask for graceful child process termination
   */
  void askForStop(int processNumber) {
    writeByte(processNumber, STOP_BYTE_OFFSET, STOP);
  }

  boolean askedForStop(int processNumber) {
    return readByte(processNumber, STOP_BYTE_OFFSET) == STOP;
  }

  /**
   * To be executed by monitor process to ask for quick child process termination
   */
  void askForHardStop(int processNumber) {
    writeByte(processNumber, HARD_STOP_BYTE_OFFSET, HARD_STOP);
  }

  boolean askedForHardStop(int processNumber) {
    return readByte(processNumber, HARD_STOP_BYTE_OFFSET) == HARD_STOP;
  }

  void askForRestart(int processNumber) {
    writeByte(processNumber, RESTART_BYTE_OFFSET, RESTART);
  }

  boolean askedForRestart(int processNumber) {
    return readByte(processNumber, RESTART_BYTE_OFFSET) == RESTART;
  }

  void acknowledgeAskForRestart(int processNumber) {
    writeByte(processNumber, RESTART_BYTE_OFFSET, EMPTY);
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(sharedMemory);
  }

  public void checkProcessNumber(int processNumber) {
    if (processNumber < 0 || processNumber >= MAX_PROCESSES) {
      throw new IllegalArgumentException(format("Process number %s is not valid", processNumber));
    }
  }

  private void cleanData(int processNumber) {
    for (int i = 0; i < BYTE_LENGTH_FOR_ONE_PROCESS; i++) {
      writeByte(processNumber, i, EMPTY);
    }
  }

  private void writeByte(int processNumber, int offset, byte value) {
    mappedByteBuffer.put(offset(processNumber) + offset, value);
  }

  private void writeBytes(int processNumber, int offset, byte[] value) {
    int bufferOffset = offset(processNumber) + offset;
    for (int i = 0; i < value.length; i++) {
      mappedByteBuffer.put(bufferOffset + i, value[i]);
    }
  }

  private byte readByte(int processNumber, int offset) {
    return mappedByteBuffer.get(offset(processNumber) + offset);
  }

  private byte[] readBytes(int processNumber, int offset, int length) {
    int bufferOffset = offset(processNumber) + offset;
    byte[] bytes = new byte[length];
    for (int i = 0; i < length; i++) {
      bytes[i] = mappedByteBuffer.get(bufferOffset + i);
    }
    return bytes;
  }

  private void writeLong(int processNumber, int offset, long value) {
    mappedByteBuffer.putLong(offset(processNumber) + offset, value);
  }

  private long readLong(int processNumber, int offset) {
    return mappedByteBuffer.getLong(offset(processNumber) + offset);
  }

  // VisibleForTesting
  int offset(int processNumber) {
    return BYTE_LENGTH_FOR_ONE_PROCESS * processNumber;
  }

  private class ProcessCommandsImpl implements ProcessCommands {

    private final int processNumber;

    public ProcessCommandsImpl(int processNumber) {
      this.processNumber = processNumber;
    }

    @Override
    public boolean isUp() {
      return AllProcessesCommands.this.isUp(processNumber);
    }

    @Override
    public void setUp() {
      AllProcessesCommands.this.setUp(processNumber);
    }

    @Override
    public boolean isOperational() {
      return AllProcessesCommands.this.isOperational(processNumber);
    }

    @Override
    public void setOperational() {
      AllProcessesCommands.this.setOperational(processNumber);
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
    public void setHttpUrl(String s) {
      AllProcessesCommands.this.setSystemInfoUrl(processNumber, s);
    }

    @Override
    public String getHttpUrl() {
      return AllProcessesCommands.this.getSystemInfoUrl(processNumber);
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
    public void askForHardStop() {
      AllProcessesCommands.this.askForHardStop(processNumber);
    }

    @Override
    public boolean askedForHardStop() {
      return AllProcessesCommands.this.askedForHardStop(processNumber);
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

  }
}
