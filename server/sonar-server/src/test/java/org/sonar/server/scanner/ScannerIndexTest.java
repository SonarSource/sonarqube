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
package org.sonar.server.scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.platform.Server;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScannerIndexTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  File jar;

  Server server = mock(Server.class);

  @Before
  public void prepare_fs() throws IOException {
    File rootDir = temp.newFolder();
    when(server.getRootDir()).thenReturn(rootDir);

    File batchDir = new File(rootDir, "lib/batch");
    FileUtils.forceMkdir(batchDir);
    jar = new File(batchDir, "sonar-batch.jar");
    FileUtils.writeStringToFile(new File(batchDir, "sonar-batch.jar"), "foo");
  }

  @Test
  public void get_index() {
    ScannerIndex scannerIndex = new ScannerIndex(server);
    scannerIndex.start();

    String index = scannerIndex.getIndex();
    assertThat(index).isEqualTo("sonar-batch.jar|acbd18db4cc2f85cedef654fccc4a4d8" + CharUtils.LF);

    scannerIndex.stop();
  }

  @Test
  public void get_file() {
    ScannerIndex scannerIndex = new ScannerIndex(server);
    scannerIndex.start();

    File file = scannerIndex.getFile("sonar-batch.jar");
    assertThat(file).isEqualTo(jar);
  }

  /**
   * Do not allow to download files located outside the directory lib/batch, for example
   * /etc/passwd
   */
  @Test
  public void check_location_of_file() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Bad filename: ../sonar-batch.jar");

    ScannerIndex scannerIndex = new ScannerIndex(server);
    scannerIndex.start();

    scannerIndex.getFile("../sonar-batch.jar");
  }

  @Test
  public void file_does_not_exist() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Bad filename: other.jar");

    ScannerIndex scannerIndex = new ScannerIndex(server);
    scannerIndex.start();

    scannerIndex.getFile("other.jar");
  }
}
