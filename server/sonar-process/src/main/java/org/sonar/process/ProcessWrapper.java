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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Fork and monitor a new process
 */
public class ProcessWrapper extends Thread implements Terminable {

  static {
    // by default there's no timeout on RMI calls on client-side
    // http://docs.oracle.com/javase/6/docs/technotes/guides/rmi/sunrmiproperties.html
    System.setProperty("sun.rmi.transport.connectionTimeout", "5000");
    System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "5000");
    System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "5000");
    System.setProperty("sun.rmi.transport.tcp.readTimeout", "5000");
  }

  private final static Logger LOGGER = LoggerFactory.getLogger(ProcessWrapper.class);

  public static final long READY_TIMEOUT_MS = 300000L;

  private String processName, className;
  private int jmxPort = -1;
  private final List<String> javaOpts = new ArrayList<String>();
  private final List<String> classpath = new ArrayList<String>();
  private final Map<String, String> envProperties = new HashMap<String, String>();
  private final Properties properties = new Properties();
  private File workDir;
  private Process process;
  private StreamGobbler errorGobbler;
  private StreamGobbler outputGobbler;
  private ProcessMXBean processMXBean;

  public ProcessWrapper(String processName) {
    super(processName);
    this.processName = processName;
  }

  public ProcessWrapper setClassName(String s) {
    this.className = s;
    return this;
  }

  public ProcessWrapper setEnvProperty(String key, String value) {
    envProperties.put(key, value);
    return this;
  }

  public ProcessWrapper addProperties(Properties p) {
    properties.putAll(p);
    return this;
  }

  public ProcessWrapper addProperty(String key, String value) {
    properties.setProperty(key, value);
    return this;
  }

  public ProcessWrapper addJavaOpts(String s) {
    Collections.addAll(javaOpts, s.split(" "));
    return this;
  }

  public ProcessWrapper addClasspath(String s) {
    classpath.add(s);
    return this;
  }

  public ProcessWrapper setJmxPort(int i) {
    this.jmxPort = i;
    return this;
  }

  public ProcessWrapper setWorkDir(File d) {
    this.workDir = d;
    return this;
  }

  @CheckForNull
  Process process() {
    return process;
  }

  /**
   * Execute command-line and connects to JMX RMI.
   * @return true on success, false if bad command-line or process failed to start JMX RMI
   */
  public boolean execute() {
    List<String> command = new ArrayList<String>();
    try {
      command.add(buildJavaCommand());
      command.addAll(javaOpts);
      command.addAll(buildJMXOptions());
      command.addAll(buildClasspath());
      command.add(className);
      command.add(buildPropertiesFile().getAbsolutePath());

      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.command(command);
      processBuilder.directory(workDir);
      processBuilder.environment().putAll(envProperties);
      LOGGER.info("starting {}: {}", getName(), StringUtils.join(command, " "));
      process = processBuilder.start();
      errorGobbler = new StreamGobbler(process.getErrorStream(), this.getName() + "-ERROR");
      outputGobbler = new StreamGobbler(process.getInputStream(), this.getName());
      outputGobbler.start();
      errorGobbler.start();
      processMXBean = waitForJMX();
      if (processMXBean == null) {
        terminate();
        return false;
      }
      return true;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to start command: " + StringUtils.join(command, " "), e);
    }
  }

  @Override
  public void run() {
    try {
      if (ProcessUtils.isAlive(process)) {
        process.waitFor();
      }
    } catch (Exception e) {
      LOGGER.info("ProcessThread has been interrupted. Killing process.");
    } finally {
      waitUntilFinish(outputGobbler);
      waitUntilFinish(errorGobbler);
      ProcessUtils.closeStreams(process);
    }
  }

  public boolean isReady() {
    return processMXBean != null && processMXBean.isReady();
  }

  public ProcessMXBean getProcessMXBean() {
    return processMXBean;
  }

  private void waitUntilFinish(@Nullable Thread thread) {
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        LOGGER.error("InterruptedException while waiting finish of " + thread.getName() + " in process '" + getName() + "'", e);
      }
    }
  }

  private String buildJavaCommand() {
    String separator = System.getProperty("file.separator");
    return System.getProperty("java.home")
      + separator + "bin" + separator + "java";
  }

  private List<String> buildJMXOptions() throws Exception {
    if (jmxPort < 1) {
      throw new IllegalStateException("JMX port is not set");
    }
    return Arrays.asList(
      "-Dcom.sun.management.jmxremote",
      "-Dcom.sun.management.jmxremote.port=" + jmxPort,
      "-Dcom.sun.management.jmxremote.authenticate=false",
      "-Dcom.sun.management.jmxremote.ssl=false",
      "-Djava.rmi.server.hostname=" + localAddress());
  }

  private List<String> buildClasspath() {
    return Arrays.asList("-cp", StringUtils.join(classpath, System.getProperty("path.separator")));
  }

  private File buildPropertiesFile() {
    File propertiesFile = null;
    try {
      propertiesFile = File.createTempFile("sq-conf", "properties");
      Properties props = new Properties();
      props.putAll(properties);
      props.put(MonitoredProcess.NAME_PROPERTY, processName);
      OutputStream out = new FileOutputStream(propertiesFile);
      props.store(out, "Temporary properties file for Process [" + getName() + "]");
      out.close();
      return propertiesFile;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot write temporary settings to " + propertiesFile, e);
    }
  }

  /**
   * Wait for JMX RMI to be ready. Return <code>null</code>
   */
  @CheckForNull
  private ProcessMXBean waitForJMX() throws Exception {
    String loopbackAddress = localAddress();
    String path = "/jndi/rmi://" + loopbackAddress + ":" + jmxPort + "/jmxrmi";
    JMXServiceURL jmxUrl = new JMXServiceURL("rmi", loopbackAddress, jmxPort, path);

    for (int i = 0; i < 5; i++) {
      try {
        Thread.sleep(1000L);
        LOGGER.debug("Try #{} to connect to JMX server for process '{}'", i, processName);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxUrl, null);
        MBeanServerConnection mBeanServer = jmxConnector.getMBeanServerConnection();
        ProcessMXBean bean = JMX.newMBeanProxy(mBeanServer, JmxUtils.objectName(processName), ProcessMXBean.class);
        return bean;
      } catch (Exception ignored) {
        // ignored
      }
    }
    // failed to connect
    return null;
  }

  private String localAddress() {
    // TODO to be replaced by InetAddress.getLoopbackAddress() in Java 7
    return "127.0.0.1";
  }

  @Override
  public void terminate() {
    if (processMXBean != null && process != null) {
      LOGGER.info("{} stopping", getName());
      // Send the terminate command to process in order to gracefully shutdown.
      // Then hardly kill it if it didn't terminate in 30 seconds
      ScheduledExecutorService killer = Executors.newScheduledThreadPool(1);
      try {
        Runnable killerTask = new Runnable() {
          @Override
          public void run() {
            ProcessUtils.destroyQuietly(process);
          }
        };

        ScheduledFuture killerFuture = killer.schedule(killerTask, 30, TimeUnit.SECONDS);
        processMXBean.terminate();
        killerFuture.cancel(true);
        LOGGER.info("{} stopped", getName());

      } catch (Exception ignored) {
        // ignore

      } finally {
        killer.shutdownNow();
      }
    } else {
      // process is not monitored through JMX, but killing it though
      ProcessUtils.destroyQuietly(process);
    }
    processMXBean = null;
  }

  public boolean waitForReady() throws InterruptedException {
    if (processMXBean == null) {
      return false;
    }
    long now = 0;
    long wait = 500L;
    while (now < READY_TIMEOUT_MS) {
      try {
        if (processMXBean == null) {
          return false;
        }
        if (processMXBean.isReady()) {
          return true;
        }
      } catch (Exception e) {
        // ignore
      }
      Thread.sleep(wait);
      now += wait;
    }
    return false;
  }

  private static class StreamGobbler extends Thread {
    private final InputStream is;
    private final Logger logger;

    StreamGobbler(InputStream is, String name) {
      super(name + "_ProcessStreamGobbler");
      this.is = is;
      this.logger = LoggerFactory.getLogger(name);
    }

    @Override
    public void run() {
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);
      try {
        String line;
        while ((line = br.readLine()) != null) {
          logger.info(line);
        }
      } catch (Exception ignored) {
        // ignored

      } finally {
        IOUtils.closeQuietly(br);
        IOUtils.closeQuietly(isr);
      }
    }
  }
}
