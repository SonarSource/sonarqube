/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class AppSettingsLoaderImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void load_properties_from_file() throws Exception {
    File homeDir = temp.newFolder();
    File propsFile = new File(homeDir, "conf/sonar.properties");
    FileUtils.write(propsFile, "foo=bar");

    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(new String[0], homeDir);
    AppSettings settings = underTest.load();

    assertThat(settings.getProps().rawProperties()).contains(entry("foo", "bar"));
  }

  @Test
  public void throws_ISE_if_file_fails_to_be_loaded() throws Exception {
    File homeDir = temp.newFolder();
    File propsFileAsDir = new File(homeDir, "conf/sonar.properties");
    FileUtils.forceMkdir(propsFileAsDir);
    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(new String[0], homeDir);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cannot open file " + propsFileAsDir.getAbsolutePath());

    underTest.load();
  }

  @Test
  public void file_is_not_loaded_if_it_does_not_exist() throws Exception {
    File homeDir = temp.newFolder();

    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(new String[0], homeDir);
    AppSettings settings = underTest.load();

    // no failure, file is ignored
    assertThat(settings.getProps()).isNotNull();
  }

  @Test
  public void command_line_arguments_are_included_to_settings() throws Exception {
    File homeDir = temp.newFolder();

    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(new String[] {"-Dsonar.foo=bar", "-Dhello=world"}, homeDir);
    AppSettings settings = underTest.load();

    assertThat(settings.getProps().rawProperties())
      .contains(entry("sonar.foo", "bar"))
      .contains(entry("hello", "world"));
  }

  @Test
  public void command_line_arguments_make_precedence_over_properties_files() throws Exception {
    File homeDir = temp.newFolder();
    File propsFile = new File(homeDir, "conf/sonar.properties");
    FileUtils.write(propsFile, "sonar.foo=file");

    AppSettingsLoaderImpl underTest = new AppSettingsLoaderImpl(new String[]{"-Dsonar.foo=cli"}, homeDir);
    AppSettings settings = underTest.load();

    assertThat(settings.getProps().rawProperties()).contains(entry("sonar.foo", "cli"));
  }

  @Test
  public void detectHomeDir_returns_existing_dir() {
    assertThat(new AppSettingsLoaderImpl(new String[0]).getHomeDir()).exists().isDirectory();

  }
}
