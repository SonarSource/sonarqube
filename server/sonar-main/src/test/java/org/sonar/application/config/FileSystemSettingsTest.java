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
package org.sonar.application.config;

import java.io.File;
import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;
import static org.sonar.process.ProcessProperties.Property.PATH_WEB;

public class FileSystemSettingsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private FileSystemSettings underTest = new FileSystemSettings();
  private File homeDir;

  @Before
  public void setUp() throws Exception {
    homeDir = temp.newFolder();
  }

  @Test
  public void relative_paths_are_converted_to_absolute_paths() {
    Props props = new Props(new Properties());
    props.set(PATH_HOME.getKey(), homeDir.getAbsolutePath());

    // relative paths
    props.set(PATH_DATA.getKey(), "data");
    props.set(PATH_LOGS.getKey(), "logs");
    props.set(PATH_TEMP.getKey(), "temp");

    // already absolute paths
    props.set(PATH_WEB.getKey(), new File(homeDir, "web").getAbsolutePath());

    underTest.accept(props);

    assertThat(props.nonNullValue(PATH_DATA.getKey())).isEqualTo(new File(homeDir, "data").getAbsolutePath());
    assertThat(props.nonNullValue(PATH_LOGS.getKey())).isEqualTo(new File(homeDir, "logs").getAbsolutePath());
    assertThat(props.nonNullValue(PATH_TEMP.getKey())).isEqualTo(new File(homeDir, "temp").getAbsolutePath());
    assertThat(props.nonNullValue(PATH_WEB.getKey())).isEqualTo(new File(homeDir, "web").getAbsolutePath());
  }

}
