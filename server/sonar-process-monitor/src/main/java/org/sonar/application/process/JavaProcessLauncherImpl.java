/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.application.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.AllProcessesCommands;
import org.sonar.process.ProcessCommands;

import static java.lang.String.format;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_KEY;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT;

public class JavaProcessLauncherImpl implements JavaProcessLauncher {
  private static final Logger LOG = LoggerFactory.getLogger(JavaProcessLauncherImpl.class);

  private final File tempDir;
  private final AllProcessesCommands allProcessesCommands;
  private final Supplier<SystemProcessBuilder> processBuilderSupplier;

  public JavaProcessLauncherImpl(File tempDir) {
    this(tempDir, new AllProcessesCommands(tempDir), SystemProcessBuilder::new);
  }

  JavaProcessLauncherImpl(File tempDir, AllProcessesCommands allProcessesCommands, Supplier<SystemProcessBuilder> processBuilderSupplier) {
    this.tempDir = tempDir;
    this.allProcessesCommands = allProcessesCommands;
    this.processBuilderSupplier = processBuilderSupplier;
  }

  @Override
  public void close() {
    allProcessesCommands.close();
  }

  @Override
  public ProcessMonitor launch(JavaCommand javaCommand) {
    Process process = null;
    ProcessCommands commands;
    try {
      commands = allProcessesCommands.createAfterClean(javaCommand.getProcessId().getIpcIndex());

      SystemProcessBuilder processBuilder = create(javaCommand);
      LOG.info("Launch process[{}]: {}", javaCommand.getProcessId().getKey(), String.join(" ", processBuilder.command()));
      process = processBuilder.start();
      return new ProcessMonitorImpl(process, commands);

    } catch (Exception e) {
      // just in case
      if (process != null) {
        process.destroyForcibly();
      }
      throw new IllegalStateException(format("Fail to launch process [%s]", javaCommand.getProcessId().getKey()), e);
    }
  }

  private SystemProcessBuilder create(JavaCommand javaCommand) {
    List<String> commands = new ArrayList<>();
    commands.add(buildJavaPath());
    commands.addAll(javaCommand.getJavaOptions());
    // TODO warning - does it work if temp dir contains a whitespace ?
    // TODO move to JavaCommandFactory ?
    commands.add(format("-Djava.io.tmpdir=%s", tempDir.getAbsolutePath()));
    commands.addAll(buildClasspath(javaCommand));
    commands.add(javaCommand.getClassName());
    commands.add(buildPropertiesFile(javaCommand).getAbsolutePath());

    SystemProcessBuilder processBuilder = processBuilderSupplier.get();
    processBuilder.command(commands);
    processBuilder.directory(javaCommand.getWorkDir());
    processBuilder.environment().putAll(javaCommand.getEnvVariables());
    processBuilder.redirectErrorStream(true);
    return processBuilder;
  }

  private static String buildJavaPath() {
    String separator = System.getProperty("file.separator");
    return new File(new File(System.getProperty("java.home")), "bin" + separator + "java").getAbsolutePath();
  }

  private static List<String> buildClasspath(JavaCommand javaCommand) {
    String pathSeparator = System.getProperty("path.separator");
    return Arrays.asList("-cp", String.join(pathSeparator, javaCommand.getClasspath()));
  }

  private File buildPropertiesFile(JavaCommand javaCommand) {
    File propertiesFile = null;
    try {
      propertiesFile = File.createTempFile("sq-process", "properties", tempDir);
      Properties props = new Properties();
      props.putAll(javaCommand.getArguments());
      props.setProperty(PROPERTY_PROCESS_KEY, javaCommand.getProcessId().getKey());
      props.setProperty(PROPERTY_PROCESS_INDEX, Integer.toString(javaCommand.getProcessId().getIpcIndex()));
      // FIXME is it the responsibility of child process to have this timeout (too) ?
      props.setProperty(PROPERTY_TERMINATION_TIMEOUT, "60000");
      props.setProperty(PROPERTY_SHARED_PATH, tempDir.getAbsolutePath());
      try (OutputStream out = new FileOutputStream(propertiesFile)) {
        props.store(out, format("Temporary properties file for command [%s]", javaCommand.getProcessId().getKey()));
      }
      return propertiesFile;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot write temporary settings to " + propertiesFile, e);
    }
  }
}
