/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.application.command.EsJvmOptions;
import org.sonar.application.command.EsScriptCommand;
import org.sonar.application.command.JavaCommand;
import org.sonar.application.command.JvmOptions;
import org.sonar.application.es.EsInstallation;
import org.sonar.application.es.EsYmlSettings;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;
import org.sonar.process.sharedmemoryfile.AllProcessesCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcessLauncherImplTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AllProcessesCommands commands = mock(AllProcessesCommands.class, RETURNS_MOCKS);

  @Test
  public void launch_forks_a_new_process() throws Exception {
    File tempDir = temp.newFolder();
    TestProcessBuilder processBuilder = new TestProcessBuilder();
    ProcessLauncher underTest = new ProcessLauncherImpl(tempDir, commands, () -> processBuilder);
    JavaCommand<JvmOptions> command = new JavaCommand<>(ProcessId.ELASTICSEARCH, temp.newFolder());
    command.addClasspath("lib/*.class");
    command.addClasspath("lib/*.jar");
    command.setArgument("foo", "bar");
    command.setClassName("org.sonarqube.Main");
    command.setEnvVariable("VAR1", "valueOfVar1");
    command.setJvmOptions(new JvmOptions<>()
      .add("-Dfoo=bar")
      .add("-Dfoo2=bar2"));

    ProcessMonitor monitor = underTest.launch(command);

    assertThat(monitor).isNotNull();
    assertThat(processBuilder.started).isTrue();
    assertThat(processBuilder.commands.get(0)).endsWith("java");
    assertThat(processBuilder.commands).containsSubsequence(
      "-Dfoo=bar",
      "-Dfoo2=bar2",
      "-cp",
      "lib/*.class" + System.getProperty("path.separator") + "lib/*.jar",
      "org.sonarqube.Main");
    assertThat(processBuilder.dir).isEqualTo(command.getWorkDir());
    assertThat(processBuilder.redirectErrorStream).isTrue();
    assertThat(processBuilder.environment)
      .contains(entry("VAR1", "valueOfVar1"))
      .containsAllEntriesOf(command.getEnvVariables());
  }

  @Test
  public void properties_are_passed_to_command_via_a_temporary_properties_file() throws Exception {
    File tempDir = temp.newFolder();
    TestProcessBuilder processBuilder = new TestProcessBuilder();
    ProcessLauncher underTest = new ProcessLauncherImpl(tempDir, commands, () -> processBuilder);
    JavaCommand<JvmOptions> command = new JavaCommand<>(ProcessId.WEB_SERVER, temp.newFolder());
    command.setReadsArgumentsFromFile(true);
    command.setArgument("foo", "bar");
    command.setArgument("baz", "woo");
    command.setJvmOptions(new JvmOptions<>());

    underTest.launch(command);

    String propsFilePath = processBuilder.commands.get(processBuilder.commands.size() - 1);
    File file = new File(propsFilePath);
    assertThat(file).exists().isFile();
    try (FileReader reader = new FileReader(file)) {
      Properties props = new Properties();
      props.load(reader);
      assertThat(props).containsOnly(
        entry("foo", "bar"),
        entry("baz", "woo"),
        entry("process.terminationTimeout", "60000"),
        entry("process.key", ProcessId.WEB_SERVER.getKey()),
        entry("process.index", String.valueOf(ProcessId.WEB_SERVER.getIpcIndex())),
        entry("process.sharedDir", tempDir.getAbsolutePath()));
    }
  }

  @Test
  public void temporary_properties_file_can_be_avoided() throws Exception {
    File tempDir = temp.newFolder();
    TestProcessBuilder processBuilder = new TestProcessBuilder();
    ProcessLauncher underTest = new ProcessLauncherImpl(tempDir, commands, () -> processBuilder);
    JavaCommand<JvmOptions> command = new JavaCommand<>(ProcessId.WEB_SERVER, temp.newFolder());
    command.setReadsArgumentsFromFile(false);
    command.setArgument("foo", "bar");
    command.setArgument("baz", "woo");
    command.setJvmOptions(new JvmOptions<>());

    underTest.launch(command);

    String propsFilePath = processBuilder.commands.get(processBuilder.commands.size() - 1);
    File file = new File(propsFilePath);
    assertThat(file).doesNotExist();
  }

  @Test
  public void clean_up_old_es_data() throws Exception {
    File tempDir = temp.newFolder();
    File homeDir = temp.newFolder();
    File dataDir = temp.newFolder();
    File logDir = temp.newFolder();
    ProcessLauncher underTest = new ProcessLauncherImpl(tempDir, commands, () -> new TestProcessBuilder());
    EsScriptCommand command = createEsScriptCommand(tempDir, homeDir, dataDir, logDir);

    File outdatedEsDir = new File(dataDir, "es");
    assertThat(outdatedEsDir.mkdir()).isTrue();
    assertThat(outdatedEsDir.exists()).isTrue();

    underTest.launch(command);

    assertThat(outdatedEsDir.exists()).isFalse();
  }

  @Test
  public void do_not_fail_if_outdated_es_directory_does_not_exist() throws Exception {
    File tempDir = temp.newFolder();
    File homeDir = temp.newFolder();
    File dataDir = temp.newFolder();
    File logDir = temp.newFolder();
    ProcessLauncher underTest = new ProcessLauncherImpl(tempDir, commands, () -> new TestProcessBuilder());
    EsScriptCommand command = createEsScriptCommand(tempDir, homeDir, dataDir, logDir);

    File outdatedEsDir = new File(dataDir, "es");
    assertThat(outdatedEsDir.exists()).isFalse();

    underTest.launch(command);

    assertThat(outdatedEsDir.exists()).isFalse();
  }

  @Test
  public void throw_ISE_if_command_fails() throws IOException {
    File tempDir = temp.newFolder();
    ProcessLauncherImpl.ProcessBuilder processBuilder = mock(ProcessLauncherImpl.ProcessBuilder.class, RETURNS_MOCKS);
    when(processBuilder.start()).thenThrow(new IOException("error"));
    ProcessLauncher underTest = new ProcessLauncherImpl(tempDir, commands, () -> processBuilder);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to launch process [es]");

    underTest.launch(new JavaCommand(ProcessId.ELASTICSEARCH, temp.newFolder()));
  }

  private EsScriptCommand createEsScriptCommand(File tempDir, File homeDir, File dataDir, File logDir) throws IOException {
    EsScriptCommand command = new EsScriptCommand(ProcessId.ELASTICSEARCH, temp.newFolder());
    Props props = new Props(new Properties());
    props.set("sonar.path.temp", tempDir.getAbsolutePath());
    props.set("sonar.path.home", homeDir.getAbsolutePath());
    props.set("sonar.path.data", dataDir.getAbsolutePath());
    props.set("sonar.path.logs", logDir.getAbsolutePath());
    command.setEsInstallation(new EsInstallation(props)
      .setEsYmlSettings(mock(EsYmlSettings.class))
      .setEsJvmOptions(mock(EsJvmOptions.class))
      .setLog4j2Properties(new Properties()));
    return command;
  }

  private static class TestProcessBuilder implements ProcessLauncherImpl.ProcessBuilder {
    private List<String> commands = null;
    private File dir = null;
    private Boolean redirectErrorStream = null;
    private final Map<String, String> environment = new HashMap<>();
    private boolean started = false;

    @Override
    public List<String> command() {
      return commands;
    }

    @Override
    public TestProcessBuilder command(List<String> commands) {
      this.commands = commands;
      return this;
    }

    @Override
    public TestProcessBuilder directory(File dir) {
      this.dir = dir;
      return this;
    }

    @Override
    public Map<String, String> environment() {
      return environment;
    }

    @Override
    public TestProcessBuilder redirectErrorStream(boolean b) {
      this.redirectErrorStream = b;
      return this;
    }

    @Override
    public Process start() {
      this.started = true;
      return mock(Process.class);
    }
  }
}
