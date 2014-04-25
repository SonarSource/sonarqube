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
package org.sonar.server.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.platform.Server;
import org.sonar.server.ws.WsTester;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchWsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  Server server = mock(Server.class);

  @Before
  public void prepare_fs() throws IOException {
    File rootDir = temp.newFolder();
    when(server.getRootDir()).thenReturn(rootDir);

    File batchDir = new File(rootDir, "lib/batch");
    FileUtils.forceMkdir(batchDir);
    FileUtils.writeStringToFile(new File(batchDir, "sonar-batch.jar"), "foo");
  }

  @Test
  public void download_index() throws Exception {
    BatchWs ws = new BatchWs(server);
    ws.start();
    WsTester tester = new WsTester(ws);

    String index = tester.newRequest("index").execute().outputAsString();
    assertThat(index).isEqualTo("sonar-batch.jar|acbd18db4cc2f85cedef654fccc4a4d8" + CharUtils.LF);

    ws.stop();
  }

  @Test
  public void download_file() throws Exception {
    BatchWs ws = new BatchWs(server);
    ws.start();
    WsTester tester = new WsTester(ws);

    String jar = tester.newRequest("file").setParam("name", "sonar-batch.jar").execute().outputAsString();
    assertThat(jar).isEqualTo("foo");
  }

  /**
   * Do not allow to download files located outside the directory lib/batch, for example
   * /etc/passwd
   */
  @Test
  public void check_location_of_file() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Bad filename: ../sonar-batch.jar");

    BatchWs ws = new BatchWs(server);
    ws.start();
    WsTester tester = new WsTester(ws);

    tester.newRequest("file").setParam("name", "../sonar-batch.jar").execute();
  }

  @Test
  public void file_does_not_exist() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Bad filename: other.jar");

    BatchWs ws = new BatchWs(server);
    ws.start();
    WsTester tester = new WsTester(ws);

    tester.newRequest("file").setParam("name", "other.jar").execute();
  }
}
