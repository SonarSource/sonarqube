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
package org.sonar.process;

import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.test.TestUtils;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ConfigurationUtilsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldInterpolateVariables() {
    Properties input = new Properties();
    input.setProperty("hello", "world");
    input.setProperty("url", "${env:SONAR_JDBC_URL}");
    input.setProperty("do_not_change", "${SONAR_JDBC_URL}");
    Map<String, String> variables = Maps.newHashMap();
    variables.put("SONAR_JDBC_URL", "jdbc:h2:mem");

    Properties output = ConfigurationUtils.interpolateVariables(input, variables);

    assertThat(output).hasSize(3);
    assertThat(output.getProperty("hello")).isEqualTo("world");
    assertThat(output.getProperty("url")).isEqualTo("jdbc:h2:mem");
    assertThat(output.getProperty("do_not_change")).isEqualTo("${SONAR_JDBC_URL}");

    // input is not changed
    assertThat(input).hasSize(3);
    assertThat(input.getProperty("hello")).isEqualTo("world");
    assertThat(input.getProperty("url")).isEqualTo("${env:SONAR_JDBC_URL}");
    assertThat(input.getProperty("do_not_change")).isEqualTo("${SONAR_JDBC_URL}");
  }

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
    FileUtils.write(propsFile, "foo=bar");

    Props result = ConfigurationUtils.loadPropsFromCommandLineArgs(new String[] {propsFile.getAbsolutePath()});
    assertThat(result.value("foo")).isEqualTo("bar");
    assertThat(result.rawProperties()).hasSize(1);
  }

  @Test
  public void loadPropsFromCommandLineArgs_file_does_not_exist() throws Exception {
    File propsFile = temp.newFile();
    FileUtils.deleteQuietly(propsFile);

    try {
      ConfigurationUtils.loadPropsFromCommandLineArgs(new String[]{propsFile.getAbsolutePath()});
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
