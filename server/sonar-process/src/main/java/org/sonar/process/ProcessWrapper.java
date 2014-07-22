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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Fork and monitor a new process
 */
public class ProcessWrapper extends Thread {

  private final static Logger LOGGER = LoggerFactory.getLogger(ProcessWrapper.class);

  private String processName, className;
  private int jmxPort = -1;
  private final List<String> javaOpts = new ArrayList<String>();
  private final List<String> classpath = new ArrayList<String>();
  private final Map<String, String> envProperties = new HashMap<String, String>();
  private final Map<String, String> arguments = new HashMap<String, String>();
  private File workDir;
  private File propertiesFile;
  private java.lang.Process process;
  private volatile Thread processThread;
  private StreamGobbler errorGobbler;
  private StreamGobbler outputGobbler;
  private ProcessMXBean processMXBean;

  public ProcessWrapper(String processName) {
    this.processThread = this;
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

  public ProcessWrapper setArgument(String key, String value) {
    arguments.put(key, value);
    return this;
  }

  public ProcessWrapper setArguments(Map<String, String> args) {
    arguments.clear();
    arguments.putAll(args);
    return this;
  }

  public ProcessWrapper setJavaOpts(List<String> opts) {
    for (String command : opts) {
      addJavaOpts(command);
    }
    return this;
  }

  public ProcessWrapper addJavaOpts(String s) {
    Collections.addAll(javaOpts, s.split(" "));
    return this;
  }

  public ProcessWrapper setClasspath(List<String> l) {
    classpath.addAll(l);
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

  public void execute() {
    List<String> command = new ArrayList<String>();
    command.add(buildJavaCommand());
    command.addAll(javaOpts);
    command.addAll(buildJMXOptions());
    command.addAll(buildClasspath());
    command.add(className);
    command.add(buildPropertiesFile().getAbsolutePath());

    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.command(command);
    processBuilder.directory(workDir);

    try {
      LOGGER.debug("ProcessWrapper::executeProcess() -- Starting process with command '{}'", StringUtils.join(command, " "));
      process = processBuilder.start();
      LOGGER.debug("ProcessWrapper::executeProcess() -- Process started: {}", process.toString());
      errorGobbler = new StreamGobbler(process.getErrorStream(), this.getName() + "-ERROR");
      outputGobbler = new StreamGobbler(process.getInputStream(), this.getName());
      outputGobbler.start();
      errorGobbler.start();
      processMXBean = waitForJMX();

    } catch (IOException e) {
      throw new IllegalStateException("Fail to start process: " + StringUtils.join(command, " "), e);
    }
  }

  @Override
  public void run() {
    LOGGER.trace("ProcessWrapper::run() START");
    try {
      process.waitFor();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      waitUntilFinish(outputGobbler);
      waitUntilFinish(errorGobbler);
      closeStreams(process);
      ProcessWrapper.this.processThread = null;
    }
    LOGGER.trace("ProcessWrapper::run() END");
  }

  public boolean isReady() {
    return processMXBean != null && processMXBean.isReady();
  }

  public ProcessMXBean getProcessMXBean() {
    return processMXBean;
  }

  public Object getThread() {
    return this.processThread;
  }

  private void waitUntilFinish(@Nullable Thread thread) {
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        LOGGER.error("InterruptedException while waiting finish of " + thread.toString(), e);
      }
    }
  }

  private void closeStreams(@Nullable java.lang.Process process) {
    if (process != null) {
      IOUtils.closeQuietly(process.getInputStream());
      IOUtils.closeQuietly(process.getOutputStream());
      IOUtils.closeQuietly(process.getErrorStream());
    }
  }

  private String buildJavaCommand() {
    String separator = System.getProperty("file.separator");
    return System.getProperty("java.home")
      + separator + "bin" + separator + "java";
  }

  private List<String> buildJMXOptions() {
    if (jmxPort < 1) {
      throw new IllegalStateException("JMX port is not set");
    }
    return Arrays.asList(
      "-Dcom.sun.management.jmxremote",
      "-Dcom.sun.management.jmxremote.port=" + jmxPort,
      "-Dcom.sun.management.jmxremote.authenticate=false",
      "-Dcom.sun.management.jmxremote.ssl=false");
  }

  private List<String> buildClasspath() {
    return Arrays.asList("-cp", StringUtils.join(classpath, ";"));
  }

  private File buildPropertiesFile() {
    try {
      propertiesFile = File.createTempFile("sq-conf", "properties");
      Properties props = new Properties();
      props.putAll(arguments);
      props.put(Process.NAME_PROPERTY, processName);
      props.put(Process.PORT_PROPERTY, String.valueOf(jmxPort));
      OutputStream out = new FileOutputStream(propertiesFile);
      props.store(out, "Temporary properties file for Process [" + getName() + "]");
      out.close();
      return propertiesFile;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot write temporary settings to " + propertiesFile, e);
    }
  }

  private ProcessMXBean waitForJMX() {
    Exception exception = null;
    for (int i = 0; i < 5; i++) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new IllegalStateException("Could not connect to JMX server", e);
      }
      LOGGER.debug("Try #{} to connect to JMX server for process '{}'", i, processName);
      try {
        String protocol = "rmi";
        String path = "/jndi/rmi://" + InetAddress.getLocalHost().getHostName() + ":" + jmxPort + "/jmxrmi";
        JMXServiceURL jmxUrl = new JMXServiceURL(protocol, InetAddress.getLocalHost().getHostAddress(), jmxPort, path);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxUrl, null);
        MBeanServerConnection mBeanServer = jmxConnector.getMBeanServerConnection();
        ProcessMXBean bean = JMX.newMBeanProxy(mBeanServer, Process.objectNameFor(processName), ProcessMXBean.class);
        LOGGER.info("ProcessWrapper::waitForJMX -- Connected to JMX Server with URL: {}", jmxUrl.toString());
        return bean;
      } catch (MalformedURLException e) {
        throw new IllegalStateException("JMXUrl is not valid", e);
      } catch (UnknownHostException e) {
        throw new IllegalStateException("Could not get hostname", e);
      } catch (IOException e) {
        exception = e;
      }
    }
    throw new IllegalStateException("Could not connect to JMX service", exception);
  }

  public void terminate() {
    if (processMXBean != null) {
      processMXBean.terminate();
      waitUntilFinish(this);
    }
  }

  private static class StreamGobbler extends Thread {
    private final InputStream is;
    private volatile Exception exception;
    private final String pName;

    StreamGobbler(InputStream is, String name) {
      super(name + "_ProcessStreamGobbler");
      this.is = is;
      this.pName = name;
    }

    @Override
    public void run() {
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);
      try {
        String line;
        while ((line = br.readLine()) != null) {
          LOGGER.info(pName + " > " + line);
        }
      } catch (IOException ioe) {
        exception = ioe;

      } finally {
        IOUtils.closeQuietly(br);
        IOUtils.closeQuietly(isr);
      }
    }

    public Exception getException() {
      return exception;
    }
  }
}
