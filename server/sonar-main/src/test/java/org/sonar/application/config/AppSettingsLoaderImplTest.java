/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.extension.ServiceLoaderWrapper;
import org.sonar.process.System2;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppSettingsLoaderImplTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ServiceLoaderWrapper serviceLoaderWrapper = mock(ServiceLoaderWrapper.class);
  private System2 system = mock(System2.class);

  @Before
  public void setup() {
    when(serviceLoaderWrapper.load()).thenReturn(ImmutableSet.of());
  }

  @Test
  public void load_properties_from_file() throws Exception {
    File homeDir = temp.newFolder();
    File propsFile = new File(homeDir, "conf/sonar.properties");
    FileUtils.write(propsFile, "foo=bar", UTF_8);

    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(system, new String[0], homeDir, serviceLoaderWrapper);
    AppSettings settings = underTest.load();

    assertThat(settings.getProps().rawProperties()).contains(entry("foo", "bar"));
  }

  @Test
  public void load_properties_from_env() throws Exception {
    when(system.getenv()).thenReturn(ImmutableMap.of(
      "SONAR_DASHED_PROPERTY", "2",
      "SONAR_JDBC_URL", "some_jdbc_url",
      "SONAR_EMBEDDEDDATABASE_PORT", "8765"));
    when(system.getenv("SONAR_DASHED_PROPERTY")).thenReturn("2");
    when(system.getenv("SONAR_JDBC_URL")).thenReturn("some_jdbc_url");
    when(system.getenv("SONAR_EMBEDDEDDATABASE_PORT")).thenReturn("8765");
    File homeDir = temp.newFolder();
    File propsFile = new File(homeDir, "conf/sonar.properties");
    FileUtils.write(propsFile, "sonar.dashed-property=1", UTF_8);
    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(system, new String[0], homeDir, serviceLoaderWrapper);

    AppSettings settings = underTest.load();

    assertThat(settings.getProps().rawProperties()).contains(
      entry("sonar.dashed-property", "2"),
      entry("sonar.jdbc.url", "some_jdbc_url"),
      entry("sonar.embeddedDatabase.port", "8765"));
  }

  @Test
  public void load_multi_ldap_settings() throws IOException {
    when(system.getenv()).thenReturn(ImmutableMap.of(
      "LDAP_FOO_URL", "url1",
      "LDAP_RANDOM_PROP", "5"));
    when(system.getenv("LDAP_FOO_URL")).thenReturn("url1");
    when(system.getenv("LDAP_RANDOM_PROP")).thenReturn("5");
    File homeDir = temp.newFolder();
    File propsFile = new File(homeDir, "conf/sonar.properties");
    FileUtils.write(propsFile, "ldap.servers=foo,bar\n" +
      "ldap.bar.url=url2", UTF_8);
    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(system, new String[0], homeDir, serviceLoaderWrapper);

    AppSettings settings = underTest.load();

    assertThat(settings.getProps().rawProperties()).contains(
      entry("ldap.servers", "foo,bar"),
      entry("ldap.foo.url", "url1"),
      entry("ldap.bar.url", "url2"));
  }

  @Test
  public void throws_ISE_if_file_fails_to_be_loaded() throws Exception {
    File homeDir = temp.newFolder();
    File propsFileAsDir = new File(homeDir, "conf/sonar.properties");
    FileUtils.forceMkdir(propsFileAsDir);
    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(system, new String[0], homeDir, serviceLoaderWrapper);

    assertThatThrownBy(underTest::load)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Cannot open file " + propsFileAsDir.getAbsolutePath());
  }

  @Test
  public void file_is_not_loaded_if_it_does_not_exist() throws Exception {
    File homeDir = temp.newFolder();

    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(system, new String[0], homeDir, serviceLoaderWrapper);
    AppSettings settings = underTest.load();

    // no failure, file is ignored
    assertThat(settings.getProps()).isNotNull();
  }

  @Test
  public void command_line_arguments_are_included_to_settings() throws Exception {
    File homeDir = temp.newFolder();

    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(system, new String[] {"-Dsonar.foo=bar", "-Dhello=world"}, homeDir, serviceLoaderWrapper);
    AppSettings settings = underTest.load();

    assertThat(settings.getProps().rawProperties())
      .contains(entry("sonar.foo", "bar"))
      .contains(entry("hello", "world"));
  }

  @Test
  public void command_line_arguments_take_precedence_over_properties_files() throws IOException {
    File homeDir = temp.newFolder();
    File propsFile = new File(homeDir, "conf/sonar.properties");
    FileUtils.write(propsFile, "sonar.foo=file", UTF_8);

    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(system, new String[] {"-Dsonar.foo=cli"}, homeDir, serviceLoaderWrapper);
    AppSettings settings = underTest.load();

    assertThat(settings.getProps().rawProperties()).contains(entry("sonar.foo", "cli"));
  }

  @Test
  public void env_vars_take_precedence_over_properties_file() throws Exception {
    when(system.getenv()).thenReturn(ImmutableMap.of("SONAR_CUSTOMPROP", "11"));
    when(system.getenv("SONAR_CUSTOMPROP")).thenReturn("11");
    File homeDir = temp.newFolder();
    File propsFile = new File(homeDir, "conf/sonar.properties");
    FileUtils.write(propsFile, "sonar.customProp=10", UTF_8);

    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(system, new String[0], homeDir, serviceLoaderWrapper);
    AppSettings settings = underTest.load();

    assertThat(settings.getProps().rawProperties()).contains(entry("sonar.customProp", "11"));
  }

  @Test
  public void command_line_arguments_take_precedence_over_env_vars() throws Exception {
    when(system.getenv()).thenReturn(ImmutableMap.of("SONAR_CUSTOMPROP", "11"));
    when(system.getenv("SONAR_CUSTOMPROP")).thenReturn("11");
    File homeDir = temp.newFolder();
    File propsFile = new File(homeDir, "conf/sonar.properties");
    FileUtils.write(propsFile, "sonar.customProp=10", UTF_8);

    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(system, new String[] {"-Dsonar.customProp=9"}, homeDir, serviceLoaderWrapper);
    AppSettings settings = underTest.load();

    assertThat(settings.getProps().rawProperties()).contains(entry("sonar.customProp", "9"));
  }

  @Test
  public void detectHomeDir_returns_existing_dir() {
    assertThat(new AppSettingsLoaderImpl(system, new String[0], serviceLoaderWrapper).getHomeDir()).exists().isDirectory();
  }
}
