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
package org.sonar.process.monitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.process.AllProcessesCommands;
import org.sonar.process.ProcessCommands;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessUtils;

import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_KEY;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT;

class JavaProcessLauncher {

  private final Timeouts timeouts;
  private final File tempDir;
  private final AllProcessesCommands allProcessesCommands;

  JavaProcessLauncher(Timeouts timeouts, File tempDir) {
    this.timeouts = timeouts;
    this.tempDir = tempDir;
    this.allProcessesCommands = new AllProcessesCommands(tempDir);
  }

  public void close() {
    allProcessesCommands.close();
  }

  ProcessRef launch(JavaCommand command) {
    Process process = null;
    try {
      ProcessCommands commands = allProcessesCommands.createAfterClean(command.getProcessId().getIpcIndex());

      ProcessBuilder processBuilder = create(command);
      LoggerFactory.getLogger(getClass()).info("Launch process[{}]: {}",
        command.getProcessId().getKey(), StringUtils.join(processBuilder.command(), " "));
      process = processBuilder.start();
      StreamGobbler inputGobbler = new StreamGobbler(process.getInputStream(), command.getProcessId().getKey());
      inputGobbler.start();

      return new ProcessRef(command.getProcessId().getKey(), commands, process, inputGobbler);

    } catch (Exception e) {
      // just in case
      ProcessUtils.sendKillSignal(process);
      throw new IllegalStateException("Fail to launch [" + command.getProcessId().getKey() + "]", e);
    }
  }

  private ProcessBuilder create(JavaCommand javaCommand) {
    List<String> commands = new ArrayList<>();
    commands.add(buildJavaPath());
    commands.addAll(javaCommand.getJavaOptions());
    // TODO warning - does it work if temp dir contains a whitespace ?
    commands.add(String.format("-Djava.io.tmpdir=%s", tempDir.getAbsolutePath()));
    commands.add(getJmxAgentCommand());
    commands.addAll(buildClasspath(javaCommand));
    commands.add(javaCommand.getClassName());
    commands.add(buildPropertiesFile(javaCommand).getAbsolutePath());

    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.command(commands);
    processBuilder.directory(javaCommand.getWorkDir());
    processBuilder.environment().putAll(javaCommand.getEnvVariables());
    processBuilder.redirectErrorStream(true);
    return processBuilder;
  }

  /**
   * JVM option to enable the agent that allows inter-process communication through JMX without
   * opening new ports. The agent is available in JRE of OpenJDK/OracleJDK only.
   * @see ProcessEntryPoint
   */
  private static String getJmxAgentCommand() {
    return "-javaagent:" + System.getProperty("java.home") + File.separator + "lib" + File.separator + "management-agent.jar";
  }

  private String buildJavaPath() {
    String separator = System.getProperty("file.separator");
    return new File(new File(System.getProperty("java.home")), "bin" + separator + "java").getAbsolutePath();
  }

  private List<String> buildClasspath(JavaCommand javaCommand) {
    return Arrays.asList("-cp", StringUtils.join(javaCommand.getClasspath(), System.getProperty("path.separator")));
  }

  private File buildPropertiesFile(JavaCommand javaCommand) {
    File propertiesFile = null;
    try {
      propertiesFile = File.createTempFile("sq-process", "properties", tempDir);
      Properties props = new Properties();
      props.putAll(javaCommand.getArguments());
      props.setProperty(PROPERTY_PROCESS_KEY, javaCommand.getProcessId().getKey());
      props.setProperty(PROPERTY_PROCESS_INDEX, Integer.toString(javaCommand.getProcessId().getIpcIndex()));
      props.setProperty(PROPERTY_TERMINATION_TIMEOUT, String.valueOf(timeouts.getTerminationTimeout()));
      props.setProperty(PROPERTY_SHARED_PATH, tempDir.getAbsolutePath());
      try (OutputStream out = new FileOutputStream(propertiesFile)) {
        props.store(out, String.format("Temporary properties file for command [%s]", javaCommand.getProcessId().getKey()));
      }
      return propertiesFile;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot write temporary settings to " + propertiesFile, e);
    }
  }
}
