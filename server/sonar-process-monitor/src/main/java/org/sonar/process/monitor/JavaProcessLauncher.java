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
package org.sonar.process.monitor;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.process.LoopbackAddress;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class JavaProcessLauncher {

  private final Timeouts timeouts;

  public JavaProcessLauncher(Timeouts timeouts) {
    this.timeouts = timeouts;
  }

  ProcessRef launch(JavaCommand command) {
    Process process = null;
    try {
      ProcessBuilder processBuilder = create(command);
      LoggerFactory.getLogger(getClass()).info("Launch {}: {}",
        command.getKey(), StringUtils.join(processBuilder.command(), " "));
      process = processBuilder.start();
      StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), command.getKey());
      StreamGobbler inputGobbler = new StreamGobbler(process.getInputStream(), command.getKey());
      inputGobbler.start();
      errorGobbler.start();

      return new ProcessRef(command.getKey(), process, errorGobbler, inputGobbler);

    } catch (Exception e) {
      // just in case
      ProcessUtils.destroyQuietly(process);
      throw new IllegalStateException("Fail to launch " + command.getKey(), e);
    }
  }

  private ProcessBuilder create(JavaCommand javaCommand) {
    List<String> commands = new ArrayList<String>();
    commands.add(buildJavaPath());
    commands.addAll(javaCommand.getJavaOptions());
    commands.addAll(buildJmxOptions(javaCommand));
    commands.addAll(buildClasspath(javaCommand));
    commands.add(javaCommand.getClassName());

    // TODO warning - does it work if temp dir contains a whitespace ?
    commands.add(buildPropertiesFile(javaCommand).getAbsolutePath());

    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.command(commands);
    processBuilder.directory(javaCommand.getWorkDir());
    processBuilder.environment().putAll(javaCommand.getEnvVariables());
    return processBuilder;
  }

  private String buildJavaPath() {
    String separator = System.getProperty("file.separator");
    return new File(new File(System.getProperty("java.home")),
      "bin" + separator + "java").getAbsolutePath();
  }

  private List<String> buildJmxOptions(JavaCommand javaCommand) {
    if (javaCommand.getJmxPort() < 1) {
      throw new IllegalStateException("JMX port is not set");
    }
    return Arrays.asList(
      "-Dcom.sun.management.jmxremote",
      "-Dcom.sun.management.jmxremote.port=" + javaCommand.getJmxPort(),
      "-Dcom.sun.management.jmxremote.authenticate=false",
      "-Dcom.sun.management.jmxremote.ssl=false",
      "-Djava.rmi.server.hostname=" + LoopbackAddress.get().getHostAddress());
  }

  private List<String> buildClasspath(JavaCommand javaCommand) {
    return Arrays.asList("-cp", StringUtils.join(javaCommand.getClasspath(), System.getProperty("path.separator")));
  }

  private File buildPropertiesFile(JavaCommand javaCommand) {
    File propertiesFile = null;
    try {
      propertiesFile = File.createTempFile("sq-conf", "properties");
      Properties props = new Properties();
      props.putAll(javaCommand.getArguments());
      props.setProperty(ProcessEntryPoint.PROPERTY_PROCESS_KEY, javaCommand.getKey());
      props.setProperty(ProcessEntryPoint.PROPERTY_AUTOKILL_DISABLED, String.valueOf(javaCommand.isDebugMode()));
      props.setProperty(ProcessEntryPoint.PROPERTY_AUTOKILL_PING_TIMEOUT, String.valueOf(timeouts.getAutokillPingTimeout()));
      props.setProperty(ProcessEntryPoint.PROPERTY_AUTOKILL_PING_INTERVAL, String.valueOf(timeouts.getAutokillPingInterval()));
      props.setProperty(ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT, String.valueOf(timeouts.getTerminationTimeout()));
      OutputStream out = new FileOutputStream(propertiesFile);
      props.store(out, String.format("Temporary properties file for command [%s]", javaCommand.getKey()));
      out.close();
      return propertiesFile;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot write temporary settings to " + propertiesFile, e);
    }
  }
}
