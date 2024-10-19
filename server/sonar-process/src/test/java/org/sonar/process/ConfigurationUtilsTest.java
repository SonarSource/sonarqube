/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.process;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ConfigurationUtilsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void loadPropsFromCommandLineArgs_missing_argument() {
    try {
      ConfigurationUtils.loadPropsFromCommandLineArgs(new String[0]);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).startsWith("Only a single command-line argument is accepted");
    }
  }

  @Test
  public void loadPropsFromCommandLineArgs_load_properties_from_file() throws Exception {
    File propsFile = temp.newFile();
    FileUtils.write(propsFile, "foo=bar", StandardCharsets.UTF_8);

    Props result = ConfigurationUtils.loadPropsFromCommandLineArgs(new String[] {propsFile.getAbsolutePath()});
    assertThat(result.value("foo")).isEqualTo("bar");
    assertThat(result.rawProperties()).hasSize(1);
  }

  @Test
  public void loadPropsFromCommandLineArgs_file_does_not_exist() throws Exception {
    File propsFile = temp.newFile();
    FileUtils.deleteQuietly(propsFile);

    try {
      ConfigurationUtils.loadPropsFromCommandLineArgs(new String[] {propsFile.getAbsolutePath()});
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Could not read properties from file: " + propsFile.getAbsolutePath());
    }
  }

  @Test
  public void private_constructor() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(ConfigurationUtils.class)).isTrue();
  }
}
