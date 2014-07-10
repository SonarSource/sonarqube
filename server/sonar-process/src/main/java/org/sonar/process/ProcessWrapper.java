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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class ProcessWrapper extends Thread {

  private final static Logger LOGGER = LoggerFactory.getLogger(ProcessWrapper.class);

  final int port;
  final String className;
  final String[] classPath;
  final Map<String, String> properties;
  java.lang.Process process;

  public ProcessWrapper(String className, String[] classPath, Map<String, String> properties, String name, Integer port) {
    super(name);
    LOGGER.info("Creating Launcher for '{}' with base port: {}", name, port);
    this.port = port;
    this.className = className;
    this.classPath = classPath;
    this.properties = properties;
  }

  public void run() {
    ProcessBuilder processBuilder =
      new ProcessBuilder("java", "-cp",
        StringUtils.join(classPath, ":"),
        className);
    processBuilder.environment().putAll(properties);
    processBuilder.environment().put(Process.NAME_PROPERTY, this.getName());
    processBuilder.environment().put(Process.HEARTBEAT_PROPERTY, Integer.toString(port));

    try {
      process = processBuilder.start();
      StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "ERROR");
      StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT");
      outputGobbler.start();
      errorGobbler.start();
      while (!currentThread().isInterrupted()) {
        process.wait(100);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Io Exception in ProcessWrapper", e);
    } catch (InterruptedException e) {
      LOGGER.warn("Process has been shutdown");
    }
  }

  public void shutdown() {
    this.process.destroy();
  }

  private class StreamGobbler extends Thread {
    InputStream is;
    String type;

    private StreamGobbler(InputStream is, String type) {
      this.is = is;
      this.type = type;
    }

    @Override
    public void run() {
      try {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while ((line = br.readLine()) != null)
          System.out.println(type + "> " + line);
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }
}
