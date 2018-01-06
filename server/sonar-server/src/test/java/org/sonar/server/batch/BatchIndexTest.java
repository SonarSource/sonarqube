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
package org.sonar.server.batch;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.platform.ServerFileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchIndexTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private File jar;

  private ServerFileSystem fs = mock(ServerFileSystem.class);

  @Before
  public void prepare_fs() throws IOException {
    File homeDir = temp.newFolder();
    when(fs.getHomeDir()).thenReturn(homeDir);

    File batchDir = new File(homeDir, "lib/scanner");
    FileUtils.forceMkdir(batchDir);
    jar = new File(batchDir, "sonar-batch.jar");
    FileUtils.writeStringToFile(new File(batchDir, "sonar-batch.jar"), "foo");
  }

  @Test
  public void get_index() {
    BatchIndex batchIndex = new BatchIndex(fs);
    batchIndex.start();

    String index = batchIndex.getIndex();
    assertThat(index).isEqualTo("sonar-batch.jar|acbd18db4cc2f85cedef654fccc4a4d8" + CharUtils.LF);

    batchIndex.stop();
  }

  @Test
  public void get_file() {
    BatchIndex batchIndex = new BatchIndex(fs);
    batchIndex.start();

    File file = batchIndex.getFile("sonar-batch.jar");
    assertThat(file).isEqualTo(jar);
  }

  /**
   * Do not allow to download files located outside the directory lib/batch, for example
   * /etc/passwd
   */
  @Test
  public void check_location_of_file() {
    thrown.expect(NotFoundException.class);
    thrown.expectMessage("Bad filename: ../sonar-batch.jar");

    BatchIndex batchIndex = new BatchIndex(fs);
    batchIndex.start();

    batchIndex.getFile("../sonar-batch.jar");
  }

  @Test
  public void file_does_not_exist() {
    thrown.expect(NotFoundException.class);
    thrown.expectMessage("Bad filename: other.jar");

    BatchIndex batchIndex = new BatchIndex(fs);
    batchIndex.start();

    batchIndex.getFile("other.jar");
  }
}
