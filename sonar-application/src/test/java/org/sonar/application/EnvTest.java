/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.application;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class EnvTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void testName() throws Exception {
    File file = new File(getClass().getResource("/org/sonar/application/LoggingTest/logback-access.xml").toURI());
    assertThat(file.exists()).isTrue();
  }

  @Test
  public void files() throws Exception {
    File home = temp.newFolder();
    File confFile = new File(home, "conf/sonar.properties");
    File logFile = new File(home, "logs/sonar.log");

    FileUtils.touch(confFile);
    FileUtils.touch(logFile);

    Env env = new Env(confFile);

    assertThat(env.rootDir()).isDirectory().exists().isEqualTo(home);
    assertThat(env.file("conf/sonar.properties")).isFile().exists().isEqualTo(confFile);
    assertThat(env.file("logs/sonar.log")).isFile().exists().isEqualTo(logFile);
    assertThat(env.file("xxx/unknown.log")).doesNotExist();
  }

  @Test
  public void fresh_dir() throws Exception {
    File home = temp.newFolder();
    File confFile = new File(home, "conf/sonar.properties");
    File logFile = new File(home, "logs/sonar.log");

    FileUtils.touch(confFile);
    FileUtils.touch(logFile);

    Env env = new Env(confFile);

    File data = env.freshDir("data/h2");
    assertThat(data).isDirectory().exists();
    assertThat(data.getParentFile().getName()).isEqualTo("data");
    assertThat(data.getParentFile().getParentFile()).isEqualTo(home);

    // clean directory
    File logs = env.freshDir("logs");
    assertThat(logs).isDirectory().exists();
    assertThat(logs.listFiles()).isEmpty();
  }

  @Test
  public void temp_dir_should_be_writable() throws Exception {
    new Env(temp.newFile()).verifyWritableTempDir();
    // do not fail
  }
}
