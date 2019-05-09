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
package org.sonar.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Optional;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Shutdowner {
  public static void main(String[] args) {
    try {
      new Shutdowner().run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  void run() throws IOException {
    File homeDir = detectHomeDir();
    Properties p = loadPropertiesFile(homeDir);
    File tmpDir = resolveTempDir(p);

    askForHardStop(tmpDir);
  }

  // assuming jar file is in directory SQ_HOME/lib/

  static File detectHomeDir() {
    try {
      File appJar = new File(Shutdowner.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      return appJar.getParentFile().getParentFile();
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Cannot detect path of shutdowner jar file", e);
    }
  }

  /**
   * Loads the configuration file ${homeDir}/conf/sonar.properties.
   * An empty {@link Properties} is returned if the file does not exist.
   */
  static Properties loadPropertiesFile(File homeDir) {
    Properties p = new Properties();
    File propsFile = new File(new File(homeDir, "conf"), "sonar.properties");
    if (propsFile.exists()) {
      try (Reader reader = new InputStreamReader(new FileInputStream(propsFile), UTF_8)) {
        p.load(reader);
        return p;
      } catch (IOException e) {
        throw new IllegalStateException("Cannot open file " + propsFile, e);
      }
    } else {
      throw new IllegalStateException("Configuration file not found: " + propsFile);
    }
  }

  static File resolveTempDir(Properties p) {
    return new File(Optional.ofNullable(p.getProperty("sonar.path.temp")).orElse("temp"));
  }

  static void askForHardStop(File tmpDir) throws IOException {
    writeToShareMemory(tmpDir, 1, (byte) 0xFF);
  }

  private static void writeToShareMemory(File tmpDir, int offset, byte value) throws IOException {
    try (RandomAccessFile sharedMemory = new RandomAccessFile(new File(tmpDir, "sharedmemory"), "rw")) {
      // Using values from org.sonar.process.ProcessCommands
      MappedByteBuffer mappedByteBuffer = sharedMemory.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 50L * 10);

      // Now we are stopping all processes as quick as possible
      // by asking for stop of "app" process
      mappedByteBuffer.put(offset, value);
    }
  }
}
