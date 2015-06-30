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
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.ProcessCommands;
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
      // cleanup existing monitor files
      ProcessCommands commands = new DefaultProcessCommands(command.getTempDir(), command.getProcessIndex());

      ProcessBuilder processBuilder = create(command);
      LoggerFactory.getLogger(getClass()).info("Launch process[{}]: {}",
        command.getKey(), StringUtils.join(processBuilder.command(), " "));
      process = processBuilder.start();
      StreamGobbler inputGobbler = new StreamGobbler(process.getInputStream(), command.getKey());
      inputGobbler.start();

      return new ProcessRef(command.getKey(), commands, process, inputGobbler);

    } catch (Exception e) {
      // just in case
      ProcessUtils.sendKillSignal(process);
      throw new IllegalStateException("Fail to launch " + command.getKey(), e);
    }
  }

  private ProcessBuilder create(JavaCommand javaCommand) {
    List<String> commands = new ArrayList<>();
    commands.add(buildJavaPath());
    commands.addAll(javaCommand.getJavaOptions());
    // TODO warning - does it work if temp dir contains a whitespace ?
    commands.add(String.format("-Djava.io.tmpdir=%s", javaCommand.getTempDir().getAbsolutePath()));
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

  private String buildJavaPath() {
    String separator = System.getProperty("file.separator");
    return new File(new File(System.getProperty("java.home")),
      "bin" + separator + "java").getAbsolutePath();
  }

  private List<String> buildClasspath(JavaCommand javaCommand) {
    return Arrays.asList("-cp", StringUtils.join(javaCommand.getClasspath(), System.getProperty("path.separator")));
  }

  private File buildPropertiesFile(JavaCommand javaCommand) {
    File propertiesFile = null;
    try {
      propertiesFile = File.createTempFile("sq-process", "properties");
      Properties props = new Properties();
      props.putAll(javaCommand.getArguments());
      props.setProperty(ProcessEntryPoint.PROPERTY_PROCESS_KEY, javaCommand.getKey());
      props.setProperty(ProcessEntryPoint.PROPERTY_PROCESS_INDEX, Integer.toString(javaCommand.getProcessIndex()));
      props.setProperty(ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT, String.valueOf(timeouts.getTerminationTimeout()));
      props.setProperty(ProcessEntryPoint.PROPERTY_SHARED_PATH, javaCommand.getTempDir().getAbsolutePath());
      OutputStream out = new FileOutputStream(propertiesFile);
      props.store(out, String.format("Temporary properties file for command [%s]", javaCommand.getKey()));
      out.close();
      return propertiesFile;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot write temporary settings to " + propertiesFile, e);
    }
  }
}
