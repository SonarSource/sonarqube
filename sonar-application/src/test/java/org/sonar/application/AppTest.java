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
import static org.assertj.core.api.Assertions.entry;
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
  public void start_all_processes_if_cluster_mode_is_disabled() throws Exception {
    Monitor monitor = mock(Monitor.class);
    App app = new App(monitor);
    Props props = initDefaultProps();
    app.start(props);

    ArgumentCaptor<List<JavaCommand>> argument = newJavaCommandArgumentCaptor();
    verify(monitor).start(argument.capture());

    assertThat(argument.getValue()).extracting("processId").containsExactly(ProcessId.ELASTICSEARCH, ProcessId.WEB_SERVER, ProcessId.COMPUTE_ENGINE);

    app.stopAsync();
  }

  @Test
  public void start_only_web_server_node_in_cluster() throws Exception {
    Monitor monitor = mock(Monitor.class);
    App app = new App(monitor);
    Props props = initDefaultProps();
    props.set(ProcessProperties.CLUSTER_ENABLED, "true");
    props.set(ProcessProperties.CLUSTER_CE_DISABLED, "true");
    props.set(ProcessProperties.CLUSTER_SEARCH_DISABLED, "true");
    app.start(props);

    ArgumentCaptor<List<JavaCommand>> argument = newJavaCommandArgumentCaptor();
    verify(monitor).start(argument.capture());

    assertThat(argument.getValue()).extracting("processId").containsOnly(ProcessId.WEB_SERVER);
  }

  @Test
  public void start_only_compute_engine_node_in_cluster() throws Exception {
    Monitor monitor = mock(Monitor.class);
    App app = new App(monitor);
    Props props = initDefaultProps();
    props.set(ProcessProperties.CLUSTER_ENABLED, "true");
    props.set(ProcessProperties.CLUSTER_WEB_DISABLED, "true");
    props.set(ProcessProperties.CLUSTER_SEARCH_DISABLED, "true");
    app.start(props);

    ArgumentCaptor<List<JavaCommand>> argument = newJavaCommandArgumentCaptor();
    verify(monitor).start(argument.capture());

    assertThat(argument.getValue()).extracting("processId").containsOnly(ProcessId.COMPUTE_ENGINE);
  }

  @Test
  public void start_only_elasticsearch_node_in_cluster() throws Exception {
    Monitor monitor = mock(Monitor.class);
    App app = new App(monitor);
    Props props = initDefaultProps();
    props.set(ProcessProperties.CLUSTER_ENABLED, "true");
    props.set(ProcessProperties.CLUSTER_WEB_DISABLED, "true");
    props.set(ProcessProperties.CLUSTER_CE_DISABLED, "true");
    app.start(props);

    ArgumentCaptor<List<JavaCommand>> argument = newJavaCommandArgumentCaptor();
    verify(monitor).start(argument.capture());

    assertThat(argument.getValue()).extracting("processId").containsOnly(ProcessId.ELASTICSEARCH);
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

  @Test
  public void sets_TMPDIR_env_var_of_Web_process() throws Exception {
    Monitor monitor = mock(Monitor.class);
    App app = new App(monitor);
    Props props = initDefaultProps();
    String expectedTmpDir = "expected tmp dir";
    props.set("sonar.path.temp", expectedTmpDir);
    app.start(props);

    ArgumentCaptor<List<JavaCommand>> argument = newJavaCommandArgumentCaptor();
    verify(monitor).start(argument.capture());

    assertThat(argument.getValue().get(1).getEnvVariables()).contains(entry("TMPDIR", expectedTmpDir));
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
