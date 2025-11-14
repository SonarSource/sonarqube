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
package org.sonar.server.batch;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.server.ws.WsActionTester;

import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileActionIT {


  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ServerFileSystem serverFileSystem = mock(ServerFileSystem.class);
  private BatchIndex batchIndex = new BatchIndex(serverFileSystem);

  private File batchDir;

  private WsActionTester tester = new WsActionTester(new FileAction(batchIndex));

  @Before
  public void setUp() throws Exception {
    File homeDir = temp.newFolder();
    when(serverFileSystem.getHomeDir()).thenReturn(homeDir);
    batchDir = new File(homeDir, "lib/scanner");
    FileUtils.forceMkdir(batchDir);
  }

  @Test
  public void download_file() throws Exception {
    writeStringToFile(new File(batchDir, "sonar-batch.jar"), "foo");
    writeStringToFile(new File(batchDir, "other.jar"), "bar");
    batchIndex.start();

    String jar = tester.newRequest().setParam("name", "sonar-batch.jar").execute().getInput();

    assertThat(jar).isEqualTo("foo");
  }

  @Test
  public void throw_NotFoundException_when_file_does_not_exist() throws Exception {
    writeStringToFile(new File(batchDir, "sonar-batch.jar"), "foo");
    batchIndex.start();

    assertThatThrownBy(() -> tester.newRequest().setParam("name", "unknown").execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Bad filename: unknown");
  }

  @Test
  public void throw_IAE_when_no_name_parameter() {
    assertThatThrownBy(() -> tester.newRequest().execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'name' parameter is missing");
  }

  @Test
  public void test_definition() {
    WebService.Action definition = tester.getDef();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).extracting(WebService.Param::key).containsOnly("name");
  }

}
