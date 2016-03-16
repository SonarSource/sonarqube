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
package org.sonar.application;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;
import org.sonar.process.monitor.JavaCommand;
import org.sonar.process.monitor.Monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AppTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void starPath() throws IOException {
    File homeDir = temp.newFolder();
    String startPath = App.starPath(homeDir, "lib/search");
    assertThat(FilenameUtils.normalize(startPath, true))
      .endsWith("*")
      .startsWith(FilenameUtils.normalize(homeDir.getAbsolutePath(), true));
  }

  @Test
  public void start_all_processes_by_default() throws Exception {
    Monitor monitor = mock(Monitor.class);
    App app = new App(monitor);
    Props props = initDefaultProps();
    app.start(props);

    ArgumentCaptor<List<JavaCommand>> argument = newJavaCommandArgumentCaptor();
    verify(monitor).start(argument.capture());

    assertThat(argument.getValue()).extracting("processId").containsExactly(ProcessId.ELASTICSEARCH, ProcessId.WEB_SERVER, ProcessId.COMPUTE_ENGINE);
  }

  @Test
  public void do_not_start_WebServer_nor_CE_if_elasticsearch_slave() throws Exception {
    Monitor monitor = mock(Monitor.class);
    App app = new App(monitor);
    Props props = initDefaultProps();
    props.set("sonar.cluster.masterHost", "1.2.3.4");
    app.start(props);

    ArgumentCaptor<List<JavaCommand>> argument = newJavaCommandArgumentCaptor();
    verify(monitor).start(argument.capture());

    assertThat(argument.getValue()).extracting("processId").containsOnly(ProcessId.ELASTICSEARCH);
  }

  @Test
  public void all_JavaCommand_have_a_sonar_core_startedAt_property_argument() throws IOException {
    Monitor monitor = mock(Monitor.class);
    App app = new App(monitor);
    Props props = initDefaultProps();
    app.start(props);

    ArgumentCaptor<List<JavaCommand>> argument = newJavaCommandArgumentCaptor();
    verify(monitor).start(argument.capture());

    List<JavaCommand> javaCommands = argument.getValue();
    for (JavaCommand javaCommand : javaCommands) {
      assertThat(javaCommand.getArguments()).containsKey(ProcessProperties.STARTED_AT);
    }
  }

  @Test
  public void add_custom_jdbc_driver_to_tomcat_classpath() throws Exception {
    Monitor monitor = mock(Monitor.class);
    App app = new App(monitor);
    Props props = initDefaultProps();
    props.set("sonar.jdbc.driverPath", "oracle/ojdbc6.jar");
    app.start(props);

    ArgumentCaptor<List<JavaCommand>> argument = newJavaCommandArgumentCaptor();
    verify(monitor).start(argument.capture());

    assertThat(argument.getValue().get(1).getClasspath()).contains("oracle/ojdbc6.jar");
  }

  private Props initDefaultProps() throws IOException {
    Props props = new Props(new Properties());
    ProcessProperties.completeDefaults(props);
    props.set(ProcessProperties.PATH_HOME, temp.newFolder().getAbsolutePath());
    props.set(ProcessProperties.PATH_TEMP, temp.newFolder().getAbsolutePath());
    props.set(ProcessProperties.PATH_LOGS, temp.newFolder().getAbsolutePath());
    return props;
  }

  private ArgumentCaptor<List<JavaCommand>> newJavaCommandArgumentCaptor() {
    Class<List<JavaCommand>> listClass = (Class<List<JavaCommand>>) (Class) List.class;
    return ArgumentCaptor.forClass(listClass);
  }
}
