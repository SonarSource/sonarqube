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
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

public class ProcessLauncherImpl implements ProcessLauncher {
  private static final Logger LOG = LoggerFactory.getLogger(ProcessLauncherImpl.class);

  private final File tempDir;
  private final AllProcessesCommands allProcessesCommands;
  private final Supplier<ProcessBuilder> processBuilderSupplier;

  public ProcessLauncherImpl(File tempDir) {
    this(tempDir, new AllProcessesCommands(tempDir), JavaLangProcessBuilder::new);
  }

  ProcessLauncherImpl(File tempDir, AllProcessesCommands allProcessesCommands, Supplier<ProcessBuilder> processBuilderSupplier) {
    this.tempDir = tempDir;
    this.allProcessesCommands = allProcessesCommands;
    this.processBuilderSupplier = processBuilderSupplier;
  }

  @Override
  public void close() {
    allProcessesCommands.close();
  }

  @Override
  public ProcessMonitor launch(EsCommand esCommand) {
    Process process = null;
    try {
      ProcessBuilder processBuilder = create(esCommand);
      LOG.info("Launch process[{}]: {}", esCommand.getProcessId().getKey(), String.join(" ", processBuilder.command()));

      process = processBuilder.start();

      return new EsProcessMonitor(process, esCommand.getUrl());
    } catch (Exception e) {
      // just in case
      if (process != null) {
        process.destroyForcibly();
      }
      throw new IllegalStateException(format("Fail to launch process [%s]", esCommand.getProcessId().getKey()), e);
    }
  }

  @Override
  public ProcessMonitor launch(JavaCommand javaCommand) {
    Process process = null;
    try {
      ProcessCommands commands = allProcessesCommands.createAfterClean(javaCommand.getProcessId().getIpcIndex());

      ProcessBuilder processBuilder = create(javaCommand);
      LOG.info("Launch process[{}]: {}", javaCommand.getProcessId().getKey(), String.join(" ", processBuilder.command()));
      process = processBuilder.start();
      return new ProcessCommandsProcessMonitor(process, commands);
    } catch (Exception e) {
      // just in case
      if (process != null) {
        process.destroyForcibly();
      }
      throw new IllegalStateException(format("Fail to launch process [%s]", javaCommand.getProcessId().getKey()), e);
    }
  }

  private ProcessBuilder create(EsCommand esCommand) {
    List<String> commands = new ArrayList<>();
    commands.add(esCommand.getExecutable().getAbsolutePath());
    commands.addAll(esCommand.getEsOptions());

    return create(esCommand, commands);
  }

  private ProcessBuilder create(JavaCommand javaCommand) {
    List<String> commands = new ArrayList<>();
    commands.add(buildJavaPath());
    commands.addAll(javaCommand.getJavaOptions());
    // TODO warning - does it work if temp dir contains a whitespace ?
    // TODO move to CommandFactory ?
    commands.add(format("-Djava.io.tmpdir=%s", tempDir.getAbsolutePath()));
    commands.addAll(buildClasspath(javaCommand));
    commands.add(javaCommand.getClassName());
    commands.add(buildPropertiesFile(javaCommand).getAbsolutePath());

    return create(javaCommand, commands);
  }

  private ProcessBuilder create(AbstractCommand<?> javaCommand, List<String> commands) {
    ProcessBuilder processBuilder = processBuilderSupplier.get();
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

  /**
   * An interface of the methods of {@link java.lang.ProcessBuilder} that we use in {@link ProcessLauncherImpl}.
   * <p>Allows testing creating processes without actualling creating them at OS level</p>
   */
  public interface ProcessBuilder {
    List<String> command();

    ProcessBuilder command(List<String> commands);

    ProcessBuilder directory(File dir);

    Map<String, String> environment();

    ProcessBuilder redirectErrorStream(boolean b);

    Process start() throws IOException;
  }

  private static class JavaLangProcessBuilder implements ProcessBuilder {
    private final java.lang.ProcessBuilder builder = new java.lang.ProcessBuilder();

    /**
     * @see java.lang.ProcessBuilder#command()
     */
    @Override
    public List<String> command() {
      return builder.command();
    }

    /**
     * @see java.lang.ProcessBuilder#command(List)
     */
    @Override
    public ProcessBuilder command(List<String> commands) {
      builder.command(commands);
      return this;
    }

    /**
     * @see java.lang.ProcessBuilder#directory(File)
     */
    @Override
    public ProcessBuilder directory(File dir) {
      builder.directory(dir);
      return this;
    }

    /**
     * @see java.lang.ProcessBuilder#environment()
     */
    @Override
    public Map<String, String> environment() {
      return builder.environment();
    }

    /**
     * @see java.lang.ProcessBuilder#redirectErrorStream(boolean)
     */
    @Override
    public ProcessBuilder redirectErrorStream(boolean b) {
      builder.redirectErrorStream(b);
      return this;
    }

    /**
     * @see java.lang.ProcessBuilder#start()
     */
    @Override
    public Process start() throws IOException {
      return builder.start();
    }
  }
}
