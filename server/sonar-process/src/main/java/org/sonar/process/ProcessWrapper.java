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

import com.google.common.io.Closeables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Map;

public class ProcessWrapper extends Thread {

  private final static Logger LOGGER = LoggerFactory.getLogger(ProcessWrapper.class);

  final int port;
  final String workDir;
  final String className;
  final String[] classPath;
  final Map<String, String> properties;

  final java.lang.Process process;
  private volatile Thread processThread;

  private StreamGobbler errorGobbler;
  private StreamGobbler outputGobbler;

  final ProcessMXBean processMXBean;

  public ProcessWrapper(String workDir, String className, Map<String, String> properties, final String name, String... classPath) {
    super(name);
    this.port = NetworkUtils.freePort();
    LOGGER.info("Creating Process for '{}' with workDir: '{}' and monitoring port: {}", name, workDir, port);
    this.workDir = workDir;
    this.className = className;
    this.classPath = classPath;
    this.properties = properties;
    processThread = this;

    this.process = executeProcess();

    processMXBean =  waitForJMX(name, port);
  }

  public ProcessMXBean getProcessMXBean() {
    return processMXBean;
  }

  private ProcessMXBean waitForJMX(String name, Integer port){

    Exception exception = null;
    for(int i=0; i< 10; i++) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new IllegalStateException("Could not connect to JMX server", e);
      }
      LOGGER.info("Try #{} to connect to JMX server for process '{}'", i, name);
      try {
        String protocol = "rmi";
        String path = "/jndi/rmi://" + InetAddress.getLocalHost().getHostName() + ":" + port + "/jmxrmi";
        JMXServiceURL jmxUrl = new JMXServiceURL(protocol, InetAddress.getLocalHost().getHostName(), port, path);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxUrl, null);
        MBeanServerConnection mBeanServer = jmxConnector.getMBeanServerConnection();
        ProcessMXBean bean = JMX.newMBeanProxy(mBeanServer, Process.objectNameFor(name), ProcessMXBean.class);
        LOGGER.info("ProcessWrapper::waitForJMX -- Connected to JMX Server with URL: {}",jmxUrl.toString());
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

  public boolean isReady(){
    return processMXBean != null && processMXBean.isReady();
  }

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
    }
    ProcessWrapper.this.processThread = null;
    LOGGER.trace("ProcessWrapper::run() END");
  }

  public java.lang.Process executeProcess() {
    LOGGER.info("ProcessWrapper::executeProcess() START");
    ProcessBuilder processBuilder =
      new ProcessBuilder("java",
        "-Dcom.sun.management.jmxremote",
        "-Dcom.sun.management.jmxremote.port=" + port,
        "-Dcom.sun.management.jmxremote.authenticate=false",
        "-Dcom.sun.management.jmxremote.ssl=false",
        "-cp",
        StringUtils.join(classPath, ":"),
        className);
    processBuilder.environment().putAll(properties);
    processBuilder.environment().put(Process.SONAR_HOME, workDir);
    processBuilder.environment().put(Process.NAME_PROPERTY, this.getName());
    processBuilder.environment().put(Process.PORT_PROPERTY, Integer.toString(port));

    //check that working directory exists.
    File workDirectory = new File(workDir);
    if (!workDirectory.exists()) {
      throw new IllegalStateException("Work directory does not exist.");
    } else {
      processBuilder.directory(FileUtils.getFile(workDir));
    }

    try {
      LOGGER.debug("ProcessWrapper::executeProcess() -- Starting process with command '{}'",
        StringUtils.join(processBuilder.command()," "));
      java.lang.Process process = processBuilder.start();
      LOGGER.debug("ProcessWrapper::executeProcess() -- Process started: {}",process.toString());
      errorGobbler = new StreamGobbler(process.getErrorStream(), this.getName() + "-ERROR");
      outputGobbler = new StreamGobbler(process.getInputStream(), this.getName());
      outputGobbler.start();
      errorGobbler.start();
      LOGGER.trace("ProcessWrapper::executeProcess() END");
      return process;
    } catch (IOException e) {
      throw new IllegalStateException("Io Exception in ProcessWrapper", e);
    }

  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  private void closeStreams(java.lang.Process process) {
    if (process != null) {
      Closeables.closeQuietly(process.getInputStream());
      Closeables.closeQuietly(process.getOutputStream());
      Closeables.closeQuietly(process.getErrorStream());
    }
  }

  private void waitUntilFinish(Thread thread) {
    if (thread != null) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        LOGGER.error("InterruptedException while waiting finish of " + thread.toString(), e);
      }
    }
  }

  public void terminate() {
    if(this.processMXBean != null) {
      this.processMXBean.terminate();
      waitUntilFinish(this);
    } else {
      process.destroy();
    }
  }

  public Object getThread() {
    return this.processThread;
  }

  private static class StreamGobbler extends Thread {
    private final InputStream is;
    private volatile Exception exception;
    private final String pName;

    StreamGobbler(InputStream is, String name) {
      super(name+"_ProcessStreamGobbler");
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
        Closeables.closeQuietly(br);
        Closeables.closeQuietly(isr);
      }
    }

    public Exception getException() {
      return exception;
    }
  }
}
