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
package org.sonar.server.platform;

import org.junit.Test;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.dialect.Dialect;
import org.sonar.core.persistence.dialect.H2;
import org.sonar.core.persistence.dialect.MySql;
import org.sonar.test.TestUtils;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultServerFileSystemTest {

  private static final String PATH = "/org/sonar/server/platform/DefaultServerFileSystemTest/";

  @Test
  public void get_jdbc_driver() {
    Database database = mock(Database.class);
    when(database.getDialect()).thenReturn(new MySql());
    File driver = new DefaultServerFileSystem(database, TestUtils.getResource(PATH + "testGetJdbcDriver"), null).getJdbcDriver();
    assertThat(driver).isNotNull();
  }

  @Test
  public void get_jdbc_driver_return_null_when_h2() {
    Database database = mock(Database.class);
    when(database.getDialect()).thenReturn(new H2());
    assertThat(new DefaultServerFileSystem(database, (File) null, null).getJdbcDriver()).isNull();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_jdbc_driver_not_found() {
    Database database = mock(Database.class);

    Dialect fakeDialect = mock(Dialect.class);
    when(fakeDialect.getId()).thenReturn("none");
    when(database.getDialect()).thenReturn(fakeDialect);

    new DefaultServerFileSystem(database, TestUtils.getResource(PATH + "testGetJdbcDriver"), null).getJdbcDriver();
  }

  @Test
  public void find_plugins() {
    List<File> plugins = new DefaultServerFileSystem(null, TestUtils.getResource(PATH + "shouldFindPlugins"), null).getUserPlugins();
    assertThat(plugins).hasSize(2);
  }

  @Test
  public void not_fail_if_no_plugins() {
    List<File> plugins = new DefaultServerFileSystem(null, TestUtils.getResource(PATH + "shouldNotFailIfNoPlugins"), null).getUserPlugins();
    assertThat(plugins).isEmpty();
  }

  @Test
  public void find_checkstyle_extensions() {
    ServerFileSystem fs = new DefaultServerFileSystem(null, TestUtils.getResource(PATH + "shouldFindCheckstyleExtensions"), null);

    List<File> xmls = fs.getExtensions("checkstyle", "xml");
    assertThat(xmls).hasSize(1);

    List<File> all = fs.getExtensions("checkstyle");
    assertThat(all).hasSize(3);
  }

  @Test
  public void not_fail_if_no_checkstyle_extensions() {
    ServerFileSystem fs = new DefaultServerFileSystem(null, TestUtils.getResource(PATH + "shouldNotFailIfNoCheckstyleExtensions"), null);
    List<File> xmls = fs.getExtensions("checkstyle", "xml");
    assertThat(xmls).isEmpty();

    List<File> jars = fs.getExtensions("checkstyle");
    assertThat(jars).isEmpty();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_home_directory_not_exists() {
    DefaultServerFileSystem fs = new DefaultServerFileSystem(null, new File("/notexists"), null);
    fs.start();
  }

}
