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

import com.google.common.io.Resources;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.platform.ServerFileSystem;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultServerFileSystemTest {

  private static final String PATH = "org/sonar/server/platform/DefaultServerFileSystemTest/";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void find_checkstyle_extensions() throws Exception {
    ServerFileSystem fs = new DefaultServerFileSystem(
      new File(Resources.getResource(PATH + "shouldFindCheckstyleExtensions").toURI()), temp.newFolder(), null);

    List<File> xmls = fs.getExtensions("checkstyle", "xml");
    assertThat(xmls).hasSize(1);

    List<File> all = fs.getExtensions("checkstyle");
    assertThat(all).hasSize(3);
  }

  @Test
  public void not_fail_if_no_checkstyle_extensions() throws Exception {
    ServerFileSystem fs = new DefaultServerFileSystem(
      new File(Resources.getResource(PATH + "shouldNotFailIfNoCheckstyleExtensions").toURI()), temp.newFolder(), null);
    List<File> xmls = fs.getExtensions("checkstyle", "xml");
    assertThat(xmls).isEmpty();

    List<File> jars = fs.getExtensions("checkstyle");
    assertThat(jars).isEmpty();
  }
}
