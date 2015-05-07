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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaCommandTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test_parameters() throws Exception {
    JavaCommand command = new JavaCommand("es");

    command.setArgument("first_arg", "val1");
    Properties args = new Properties();
    args.setProperty("second_arg", "val2");
    command.setArguments(args);

    command.setClassName("org.sonar.ElasticSearch");
    command.setEnvVariable("JAVA_COMMAND_TEST", "1000");
    File tempDir = temp.newFolder();
    command.setTempDir(tempDir);
    File workDir = temp.newFolder();
    command.setWorkDir(workDir);
    command.addClasspath("lib/*.jar");
    command.addClasspath("conf/*.xml");
    command.addJavaOption("-Xmx128m");

    assertThat(command.toString()).isNotNull();
    assertThat(command.getClasspath()).containsOnly("lib/*.jar", "conf/*.xml");
    assertThat(command.getJavaOptions()).containsOnly("-Xmx128m");
    assertThat(command.getWorkDir()).isSameAs(workDir);
    assertThat(command.getTempDir()).isSameAs(tempDir);
    assertThat(command.getClassName()).isEqualTo("org.sonar.ElasticSearch");

    // copy current env variables
    assertThat(command.getEnvVariables().get("JAVA_COMMAND_TEST")).isEqualTo("1000");
    assertThat(command.getEnvVariables().size()).isEqualTo(System.getenv().size() + 1);
  }

  @Test
  public void add_java_options() {
    JavaCommand command = new JavaCommand("foo");
    assertThat(command.getJavaOptions()).isEmpty();

    command.addJavaOptions("");
    assertThat(command.getJavaOptions()).isEmpty();

    command.addJavaOptions("-Xmx512m -Xms256m -Dfoo");
    assertThat(command.getJavaOptions()).containsOnly("-Xmx512m", "-Xms256m", "-Dfoo");
  }
}
